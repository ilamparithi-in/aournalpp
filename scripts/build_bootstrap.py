#!/usr/bin/env python3
"""
Automated recursive dependency resolver and packager for xopp-android.
Extracts aarch64 runtime dependencies directly from official Termux repositories.
"""

import os
import sys
import io
import re
import lzma
import tarfile
import urllib.request
import argparse
from typing import Dict, Set, List

# Seed packages required for Xournal++ and Matchbox
ROOT_PACKAGES = [
    "xournalpp",
    "openbox",
    "xdotool",
    "librsvg",
    "adwaita-icon-theme",
    "shared-mime-info"
]

REPOS = {
    "main": "https://packages.termux.dev/apt/termux-main/dists/stable/main/binary-aarch64/",
    "x11": "https://packages.termux.dev/apt/termux-x11/dists/x11/main/binary-aarch64/"
}

REPO_BASES = {
    "main": "https://packages.termux.dev/apt/termux-main/",
    "x11": "https://packages.termux.dev/apt/termux-x11/"
}

class DebExtractor:
    """Minimal in-memory Debian (.deb) archive payload extractor."""
    @staticmethod
    def extract_data_tar(deb_bytes: bytes, dest_dir: str):
        stream = io.BytesIO(deb_bytes)
        magic = stream.read(8)
        if magic != b"!<arch>\n":
            raise ValueError("Invalid .deb ar archive header")
        
        while True:
            header = stream.read(60)
            if len(header) < 60:
                break
            filename = header[:16].decode("ascii", errors="ignore").strip()
            size = int(header[48:58].decode("ascii", errors="ignore").strip())
            file_data = stream.read(size)
            if size % 2 != 0:
                stream.read(1)  # ar 2-byte alignment padding

            if filename.startswith("data.tar"):
                filename = filename.rstrip("/")
                # Handle gz, xz, or zst payloads
                if filename.endswith(".xz"):
                    decompressed = lzma.decompress(file_data)
                    with tarfile.open(fileobj=io.BytesIO(decompressed), mode="r:") as tar:
                        tar.extractall(path=dest_dir, filter='fully_trusted')
                    return
                elif filename.endswith(".gz"):
                    with tarfile.open(fileobj=io.BytesIO(file_data), mode="r:gz") as tar:
                        tar.extractall(path=dest_dir, filter='fully_trusted')
                    return
                elif filename.endswith(".tar"):
                    with tarfile.open(fileobj=io.BytesIO(file_data), mode="r:") as tar:
                        tar.extractall(path=dest_dir, filter='fully_trusted')
                    return
                else:
                    raise RuntimeError(f"Unsupported payload format: {filename}")
        raise RuntimeError("No data.tar payload found inside .deb package")


class RepositoryIndex:
    def __init__(self):
        self.packages: Dict[str, dict] = {}

    def load_index(self, repo_key: str, url: str):
        print(f"[*] Fetching package index: {repo_key}...")
        req = urllib.request.Request(url + "Packages.gz", headers={"User-Agent": "xopp-builder"})
        import gzip
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = gzip.decompress(resp.read()).decode("utf-8", errors="ignore")

        current_pkg = {}
        for line in data.splitlines():
            if not line.strip():
                if "Package" in current_pkg:
                    pkg_name = current_pkg["Package"]
                    current_pkg["_repo_base"] = REPO_BASES[repo_key]
                    self.packages[pkg_name] = current_pkg
                current_pkg = {}
                continue
            if ": " in line:
                key, val = line.split(": ", 1)
                current_pkg[key] = val

    def resolve_dependencies(self, seeds: List[str]) -> Set[str]:
        resolved: Set[str] = set()
        queue = list(seeds)

        while queue:
            pkg_name = queue.pop(0)
            if pkg_name in resolved:
                continue

            # Strip alternative or architecture tokens e.g. "package:arm64 | package"
            pkg_lookup = pkg_name.split("|")[0].strip().split(":")[0].strip()
            
            if pkg_lookup not in self.packages:
                print(f"[!] Warning: Package '{pkg_lookup}' not found in indexes (may be virtual or provided by system).")
                resolved.add(pkg_lookup)
                continue

            resolved.add(pkg_lookup)
            pkg_meta = self.packages[pkg_lookup]
            raw_deps = pkg_meta.get("Depends", "")

            if raw_deps:
                # Extract clean dependency names ignoring version brackets (>= 1.0)
                dep_items = [re.sub(r"\(.*?\)", "", d).strip() for d in raw_deps.split(",")]
                for dep in dep_items:
                    clean_dep = dep.split("|")[0].strip().split(":")[0].strip()
                    if clean_dep and clean_dep not in resolved:
                        queue.append(clean_dep)

        return resolved


def main():
    parser = argparse.ArgumentParser(description="Build bootstrap.tar.xz for Xournal++ Android")
    parser.add_argument("--output", required=True, help="Destination path for bootstrap.tar.xz")
    parser.add_argument("--staging", default="build/bootstrap_staging", help="Working staging directory")
    args = parser.parse_args()

    index = RepositoryIndex()
    for key, url in REPOS.items():
        index.load_index(key, url)

    print(f"[*] Resolving recursive dependency graph for: {ROOT_PACKAGES}")
    all_packages = index.resolve_dependencies(ROOT_PACKAGES)
    print(f"[+] Resolved {len(all_packages)} total packages (including transitive dependencies).")

    staging_usr = os.path.join(args.staging, "usr")
    os.makedirs(staging_usr, exist_ok=True)

    for pkg_name in sorted(all_packages):
        if pkg_name not in index.packages:
            continue
        meta = index.packages[pkg_name]
        download_url = meta["_repo_base"] + meta["Filename"]
        print(f" -> Downloading & Extracting: {pkg_name} ({meta.get('Version')})")

        req = urllib.request.Request(download_url, headers={"User-Agent": "xopp-builder"})
        with urllib.request.urlopen(req, timeout=30) as resp:
            deb_bytes = resp.read()
            DebExtractor.extract_data_tar(deb_bytes, args.staging)

    # Move extracted Termux rootfs to the standard usr/ prefix
    termux_usr = os.path.join(args.staging, "data", "data", "com.termux", "files", "usr")
    if os.path.exists(termux_usr):
        os.system(f"cp -a {termux_usr}/* {staging_usr}/ 2>/dev/null")
        os.system(f"rm -rf {os.path.join(args.staging, 'data')}")

    # Clean unneeded assets to keep APK size lean
    print("[*] Stripping documentation, headers, and static archives...")
    for root, dirs, files in os.walk(staging_usr, topdown=False):
        for name in files:
            if name.endswith(".a") or name.endswith(".la"):
                os.remove(os.path.join(root, name))
        if os.path.basename(root) in ["man", "doc", "include", "gtk-doc"]:
            os.system(f"rm -rf '{root}'")

    # Compile portaudio stub
    ndk_clang = None
    candidates = [
        os.environ.get("ANDROID_NDK_HOME"),
        os.environ.get("ANDROID_NDK_ROOT"),
    ]
    sdk_roots = [os.environ.get("ANDROID_SDK_ROOT"), os.environ.get("ANDROID_HOME")]
    local_props = os.path.join(os.path.dirname(__file__), "..", "local.properties")
    if os.path.exists(local_props):
        with open(local_props) as f:
            for line in f:
                if line.startswith("sdk.dir="):
                    sdk_roots.append(line.strip().split("=", 1)[1].replace("\\:", ":").replace("\\\\", "/"))
                elif line.startswith("ndk.dir="):
                    candidates.append(line.strip().split("=", 1)[1].replace("\\:", ":").replace("\\\\", "/"))
    for sdk in sdk_roots:
        if sdk and os.path.exists(os.path.join(sdk, "ndk")):
            for ver in ["25.1.8937393", "25.2.9519653", "26.1.10909125", "27.0.12077973"]:
                candidates.append(os.path.join(sdk, "ndk", ver))
            try:
                for entry in os.listdir(os.path.join(sdk, "ndk")):
                    candidates.append(os.path.join(sdk, "ndk", entry))
            except OSError:
                pass
    for cand in candidates:
        if cand and os.path.exists(cand):
            clang = os.path.join(cand, "toolchains", "llvm", "prebuilt", "linux-x86_64", "bin", "aarch64-linux-android26-clang")
            if os.path.exists(clang):
                ndk_clang = clang
                break
    if not ndk_clang:
        import shutil
        ndk_clang = shutil.which("aarch64-linux-android26-clang")

    stub_c = os.path.join(os.path.dirname(__file__), "portaudio_stub.c")
    out_pa = os.path.join(staging_usr, "lib", "libportaudio.so.2")
    if ndk_clang and os.path.exists(ndk_clang) and os.path.exists(stub_c):
        print("[*] Compiling libportaudio stub...")
        os.system(f"{ndk_clang} -shared -fPIC -Wl,-soname,libportaudio.so.2 -o {out_pa} {stub_c}")
        os.system(f"cp {out_pa} {os.path.join(staging_usr, 'lib', 'libportaudio.so')}")

    # Patch libxcb.so socket path
    libxcb_path = os.path.join(staging_usr, "lib", "libxcb.so")
    if os.path.exists(libxcb_path):
        with open(libxcb_path, "rb") as f:
            xcb_data = f.read()
        old_str = b"/data/data/com.termux/files/usr/tmp/.X11-unix/X\x00"
        new_str = b"/data/local/tmp/.X11-unix/X\x00".ljust(len(old_str), b"\x00")
        if old_str in xcb_data:
            xcb_data = xcb_data.replace(old_str, new_str)
            with open(libxcb_path, "wb") as f:
                f.write(xcb_data)

    # Generate font config and skeleton paths
    os.makedirs(os.path.join(staging_usr, "etc/fonts"), exist_ok=True)
    os.makedirs(os.path.join(staging_usr, "tmp"), exist_ok=True)
    os.makedirs(os.path.join(staging_usr, "var/cache/fontconfig"), exist_ok=True)

    fonts_conf_path = os.path.join(staging_usr, "etc", "fonts", "fonts.conf")
    with open(fonts_conf_path, "w") as f:
        f.write("""<?xml version="1.0"?>
<!DOCTYPE fontconfig SYSTEM "urn:fontconfig:fonts.dtd">
<fontconfig>
    <dir>/system/fonts</dir>
    <dir prefix="default">share/fonts</dir>
    <cachedir prefix="default">var/cache/fontconfig</cachedir>
    <cachedir>/data/local/tmp</cachedir>
</fontconfig>
""")

    # Pre-compile GSettings schemas if glib-compile-schemas is available on the host
    schemas_dir = os.path.join(staging_usr, "share", "glib-2.0", "schemas")
    if os.path.exists(schemas_dir):
        print("[*] Pre-compiling GSettings schemas...")
        os.system(f"glib-compile-schemas '{schemas_dir}' 2>/dev/null || true")

    # Patch Xournal++ mainmenubar.xml with <Ctrl>comma shortcut and _Preferences mnemonic
    menu_path = os.path.join(staging_usr, "share", "xournalpp", "ui", "mainmenubar.xml")
    if os.path.exists(menu_path):
        print("[*] Patching mainmenubar.xml with preferences shortcut...")
        with open(menu_path, "r", encoding="utf-8") as f:
            menu_data = f.read()
        target_block = """    <item>
     <attribute name="label" translatable="yes">Preferences</attribute>
     <attribute name="action">app.preferences</attribute>
     <attribute name="hidden-when">macos-menubar</attribute>
    </item>"""
        replacement_block = """    <item>
     <attribute name="label" translatable="yes">_Preferences</attribute>
     <attribute name="action">app.preferences</attribute>
     <attribute name="accel">&lt;Ctrl&gt;comma</attribute>
     <attribute name="hidden-when">macos-menubar</attribute>
    </item>"""
        if target_block in menu_data:
            menu_data = menu_data.replace(target_block, replacement_block)
            with open(menu_path, "w", encoding="utf-8") as f:
                f.write(menu_data)

    print(f"[*] Packaging final archive to {args.output}...")
    os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)
    os.system(f"tar -cf - -C '{args.staging}' usr | xz -T0 > '{args.output}'")

    print("[✔] Bootstrap packaging complete.")

if __name__ == "__main__":
    main()
