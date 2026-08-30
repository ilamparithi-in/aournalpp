#!/usr/bin/env python3
"""
Automated recursive dependency resolver and packager for xopp-android.
Extracts runtime dependencies directly from official Termux repositories
for any requested architecture (aarch64, x86_64, arm, i686).
"""

import os
import sys
import io
import re
import lzma
import gzip
import shutil
import tarfile
import urllib.request
import argparse
from typing import Dict, Set, List

# Seed packages required for Xournal++ and Matchbox / Openbox
ROOT_PACKAGES = [
    "xournalpp",
    "openbox",
    "xdotool",
    "librsvg",
    "adwaita-icon-theme",
    "shared-mime-info"
]

ARCH_MAPPINGS = {
    "aarch64": {"termux": "aarch64", "clang": "aarch64-linux-android26-clang", "abi": "arm64-v8a", "is_64bit": True},
    "arm64": {"termux": "aarch64", "clang": "aarch64-linux-android26-clang", "abi": "arm64-v8a", "is_64bit": True},
    "arm64-v8a": {"termux": "aarch64", "clang": "aarch64-linux-android26-clang", "abi": "arm64-v8a", "is_64bit": True},
    "x86_64": {"termux": "x86_64", "clang": "x86_64-linux-android26-clang", "abi": "x86_64", "is_64bit": True},
    "x86-64": {"termux": "x86_64", "clang": "x86_64-linux-android26-clang", "abi": "x86_64", "is_64bit": True},
    "x64": {"termux": "x86_64", "clang": "x86_64-linux-android26-clang", "abi": "x86_64", "is_64bit": True},
    "arm": {"termux": "arm", "clang": "armv7a-linux-androideabi26-clang", "abi": "armeabi-v7a", "is_64bit": False},
    "armv7": {"termux": "arm", "clang": "armv7a-linux-androideabi26-clang", "abi": "armeabi-v7a", "is_64bit": False},
    "armeabi-v7a": {"termux": "arm", "clang": "armv7a-linux-androideabi26-clang", "abi": "armeabi-v7a", "is_64bit": False},
    "x86": {"termux": "i686", "clang": "i686-linux-android26-clang", "abi": "x86", "is_64bit": False},
    "i686": {"termux": "i686", "clang": "i686-linux-android26-clang", "abi": "x86", "is_64bit": False},
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


def provision_xournalpp_translations(staging_usr: str, cache_dir: str):
    """Downloads official Xournal++ source .po translations and compiles .mo catalogs into share/locale/"""
    print("[*] Provisioning Xournal++ gettext translations (share/locale)...")
    locale_base = os.path.join(staging_usr, "share", "locale")
    os.makedirs(locale_base, exist_ok=True)

    src_tar = os.path.join(cache_dir, "xournalpp-1.3.7-src.tar.gz")
    if not os.path.exists(src_tar):
        src_url = "https://github.com/xournalpp/xournalpp/archive/refs/tags/v1.3.7.tar.gz"
        print(f"[*] Downloading Xournal++ source translation catalog from {src_url}...")
        try:
            req = urllib.request.Request(src_url, headers={"User-Agent": "xopp-builder"})
            with urllib.request.urlopen(req, timeout=60) as resp:
                with open(src_tar, "wb") as f:
                    f.write(resp.read())
        except Exception as e:
            print(f"[!] Warning: Could not download xournalpp source translations: {e}")
            return

    try:
        with tarfile.open(src_tar, "r:gz") as tar:
            for member in tar.getmembers():
                if "/po/" in member.name and member.name.endswith(".po"):
                    po_name = os.path.basename(member.name)
                    lang = po_name[:-3]
                    extracted_po = tar.extractfile(member)
                    if extracted_po:
                        po_content = extracted_po.read()
                        target_langs = [lang]
                        if "_" not in lang:
                            target_langs.append(f"{lang}_{lang.upper()}")
                        elif lang == "zh_CN":
                            target_langs.append("zh")
                        elif lang == "no":
                            target_langs.extend(["nb_NO", "nn_NO"])
                        elif lang == "uk_UA":
                            target_langs.append("uk")

                        for tlang in target_langs:
                            mo_dir = os.path.join(locale_base, tlang, "LC_MESSAGES")
                            os.makedirs(mo_dir, exist_ok=True)
                            mo_path = os.path.join(mo_dir, "xournalpp.mo")

                            compiled = False
                            if shutil.which("msgfmt"):
                                temp_po = os.path.join(mo_dir, "temp.po")
                                with open(temp_po, "wb") as f:
                                    f.write(po_content)
                                res = os.system(f"msgfmt -o '{mo_path}' '{temp_po}' 2>/dev/null")
                                if os.path.exists(temp_po):
                                    os.remove(temp_po)
                                if res == 0 and os.path.exists(mo_path) and os.path.getsize(mo_path) > 0:
                                    compiled = True

                            if not compiled:
                                try:
                                    lines = po_content.decode("utf-8", errors="ignore").splitlines()
                                    entries = {}
                                    msgid = ""
                                    msgstr = ""
                                    state = None
                                    for l in lines:
                                        l_str = l.strip()
                                        if l_str.startswith("msgid "):
                                            if state == "msgstr" and msgid:
                                                entries[msgid] = msgstr
                                            msgid = l_str[6:].strip('"')
                                            msgstr = ""
                                            state = "msgid"
                                        elif l_str.startswith("msgstr "):
                                            msgstr = l_str[7:].strip('"')
                                            state = "msgstr"
                                        elif l_str.startswith('"') and l_str.endswith('"'):
                                            val = l_str[1:-1]
                                            if state == "msgid":
                                                msgid += val
                                            elif state == "msgstr":
                                                msgstr += val
                                    if state == "msgstr" and msgid:
                                        entries[msgid] = msgstr

                                    import struct
                                    keys = sorted(entries.keys())
                                    offsets = []
                                    ids = b""
                                    strs = b""
                                    for k in keys:
                                        v = entries[k]
                                        kb = k.encode("utf-8") + b"\x00"
                                        vb = v.encode("utf-8") + b"\x00"
                                        offsets.append((len(kb) - 1, len(ids), len(vb) - 1, len(strs)))
                                        ids += kb
                                        strs += vb
                                    keystart = 7 * 4 + 16 * len(keys)
                                    valuestart = keystart + len(ids)
                                    koffsets = [(l1, o1 + keystart) for l1, o1, _, _ in offsets]
                                    voffsets = [(l2, o2 + valuestart) for _, _, l2, o2 in offsets]
                                    with open(mo_path, "wb") as f:
                                        f.write(struct.pack("Iiiiiii", 0x950412de, 0, len(keys), 7 * 4, 7 * 4 + 8 * len(keys), 0, 0))
                                        for l, o in koffsets:
                                            f.write(struct.pack("ii", l, o))
                                        for l, o in voffsets:
                                            f.write(struct.pack("ii", l, o))
                                        f.write(ids)
                                        f.write(strs)
                                except Exception:
                                    pass

        print("[*] Successfully provisioned Xournal++ translations in share/locale/")
    except Exception as e:
        print(f"[!] Warning: Failed extracting/compiling translations: {e}")


class RepositoryIndex:
    def __init__(self, repo_bases: Dict[str, str]):
        self.repo_bases = repo_bases
        self.packages: Dict[str, dict] = {}

    def load_index(self, repo_key: str, url: str):
        print(f"[*] Fetching package index: {repo_key} ({url})...")
        req = urllib.request.Request(url + "Packages.gz", headers={"User-Agent": "xopp-builder"})
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = gzip.decompress(resp.read()).decode("utf-8", errors="ignore")

        current_pkg = {}
        for line in data.splitlines():
            if not line.strip():
                if "Package" in current_pkg:
                    pkg_name = current_pkg["Package"]
                    current_pkg["_repo_base"] = self.repo_bases[repo_key]
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

            pkg_lookup = pkg_name.split("|")[0].strip().split(":")[0].strip()
            
            if pkg_lookup not in self.packages:
                print(f"[!] Warning: Package '{pkg_lookup}' not found in indexes (may be virtual or system-provided).")
                resolved.add(pkg_lookup)
                continue

            resolved.add(pkg_lookup)
            pkg_meta = self.packages[pkg_lookup]
            raw_deps = pkg_meta.get("Depends", "")

            if raw_deps:
                dep_items = [re.sub(r"\(.*?\)", "", d).strip() for d in raw_deps.split(",")]
                for dep in dep_items:
                    clean_dep = dep.split("|")[0].strip().split(":")[0].strip()
                    if clean_dep and clean_dep not in resolved:
                        queue.append(clean_dep)

        return resolved


def find_ndk_clang(clang_target: str) -> str:
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
            for ver in ["27.0.12077973", "27.1.12297006", "27.2.12479018", "28.0.13004108", "25.1.8937393", "25.2.9519653", "26.1.10909125"]:
                candidates.append(os.path.join(sdk, "ndk", ver))
            try:
                for entry in sorted(os.listdir(os.path.join(sdk, "ndk")), reverse=True):
                    candidates.append(os.path.join(sdk, "ndk", entry))
            except OSError:
                pass

    for cand in candidates:
        if cand and os.path.exists(cand):
            clang = os.path.join(cand, "toolchains", "llvm", "prebuilt", "linux-x86_64", "bin", clang_target)
            if os.path.exists(clang):
                return clang

    which_clang = shutil.which(clang_target)
    if which_clang:
        return which_clang

    return ""


def main():
    parser = argparse.ArgumentParser(description="Build bootstrap.tar.xz for Xournal++ Android (Multi-Arch)")
    parser.add_argument("--arch", "-a", default="aarch64", help="Target architecture (aarch64, x86_64, arm, i686 / arm64-v8a, etc.)")
    parser.add_argument("--output", "-o", required=True, help="Destination path for bootstrap.tar.xz")
    parser.add_argument("--staging", default=None, help="Working staging directory")
    parser.add_argument("--cache-dir", default=None, help="Local .deb package cache directory")
    parser.add_argument("--jnilibs-dir", default=None, help="Output directory for packaged native library executables (lib*.so)")
    args = parser.parse_args()

    arch_key = args.arch.lower().strip()
    if arch_key not in ARCH_MAPPINGS:
        print(f"[!] Error: Unsupported architecture '{args.arch}'. Supported: {list(ARCH_MAPPINGS.keys())}")
        sys.exit(1)

    arch_info = ARCH_MAPPINGS[arch_key]
    termux_arch = arch_info["termux"]
    clang_target = arch_info["clang"]
    abi_name = arch_info["abi"]

    print(f"==================================================")
    print(f"[*] Building Bootstrap Archive")
    print(f"[*] Target Architecture : {args.arch} -> Termux [{termux_arch}], ABI [{abi_name}]")
    print(f"[*] Output Destination  : {args.output}")
    print(f"==================================================")

    staging_dir = args.staging or os.path.join("build", f"bootstrap_staging_{termux_arch}")
    cache_dir = args.cache_dir or os.path.join("build", "deb_cache", termux_arch)
    os.makedirs(cache_dir, exist_ok=True)
    os.makedirs(staging_dir, exist_ok=True)

    repos = {
        "main": f"https://packages.termux.dev/apt/termux-main/dists/stable/main/binary-{termux_arch}/",
        "x11": f"https://packages.termux.dev/apt/termux-x11/dists/x11/main/binary-{termux_arch}/"
    }
    repo_bases = {
        "main": "https://packages.termux.dev/apt/termux-main/",
        "x11": "https://packages.termux.dev/apt/termux-x11/"
    }

    index = RepositoryIndex(repo_bases)
    for key, url in repos.items():
        index.load_index(key, url)

    print(f"[*] Resolving recursive dependency graph for: {ROOT_PACKAGES}")
    all_packages = index.resolve_dependencies(ROOT_PACKAGES)
    print(f"[+] Resolved {len(all_packages)} total packages (including transitive dependencies).")

    staging_usr = os.path.join(staging_dir, "usr")
    os.makedirs(staging_usr, exist_ok=True)

    for pkg_name in sorted(all_packages):
        if pkg_name not in index.packages:
            continue
        meta = index.packages[pkg_name]
        download_url = meta["_repo_base"] + meta["Filename"]
        deb_filename = os.path.basename(meta["Filename"])
        cached_deb_path = os.path.join(cache_dir, deb_filename)
        expected_size = int(meta.get("Size", 0))

        deb_bytes = None
        if os.path.exists(cached_deb_path) and (expected_size == 0 or os.path.getsize(cached_deb_path) == expected_size):
            print(f" -> [Cache Hit] Extracting: {pkg_name} ({meta.get('Version')})")
            with open(cached_deb_path, "rb") as f:
                deb_bytes = f.read()
        else:
            print(f" -> [Download] {pkg_name} ({meta.get('Version')}) from {download_url}")
            req = urllib.request.Request(download_url, headers={"User-Agent": "xopp-builder"})
            with urllib.request.urlopen(req, timeout=45) as resp:
                deb_bytes = resp.read()
                with open(cached_deb_path, "wb") as f:
                    f.write(deb_bytes)

        DebExtractor.extract_data_tar(deb_bytes, staging_dir)

    # Move extracted Termux rootfs to the standard usr/ prefix
    termux_usr = os.path.join(staging_dir, "data", "data", "com.termux", "files", "usr")
    if os.path.exists(termux_usr):
        os.system(f"cp -a {termux_usr}/* {staging_usr}/ 2>/dev/null")
        os.system(f"rm -rf {os.path.join(staging_dir, 'data')}")

    # Compile native helper binaries & stubs for the target architecture with 16KB max-page-size
    ndk_clang = find_ndk_clang(clang_target)
    if not ndk_clang:
        print(f"[!] Warning: NDK Clang compiler '{clang_target}' not found. Cannot compile native helper binaries.")
    else:
        print(f"[*] Found NDK Clang: {ndk_clang}")
        scripts_dir = os.path.dirname(os.path.abspath(__file__))
        page_size_flags = "-Wl,-z,max-page-size=16384"

        # 1. PortAudio Stub
        stub_c = os.path.join(scripts_dir, "portaudio_stub.c")
        out_pa = os.path.join(staging_usr, "lib", "libportaudio.so.2")
        if os.path.exists(stub_c):
            print("[*] Compiling libportaudio stub (16KB aligned)...")
            os.system(f"{ndk_clang} -shared -fPIC {page_size_flags} -Wl,-soname,libportaudio.so.2 -o '{out_pa}' '{stub_c}'")
            os.system(f"cp '{out_pa}' '{os.path.join(staging_usr, 'lib', 'libportaudio.so')}'")

        # 2. GTK Android IME Bridge Module
        ime_c = os.path.join(scripts_dir, "gtk-android-ime.c")
        out_ime = os.path.join(staging_usr, "lib", "libgtk-android-ime.so")
        gtk_mod_dir = os.path.join(staging_usr, "lib", "gtk-3.0", "modules")
        os.makedirs(gtk_mod_dir, exist_ok=True)
        if os.path.exists(ime_c):
            print("[*] Compiling GTK IME bridge module (libgtk-android-ime.so, 16KB aligned)...")
            os.system(f"{ndk_clang} -shared -fPIC {page_size_flags} -I'{staging_usr}/include' -I'{staging_usr}/include/gtk-3.0' -I'{staging_usr}/include/glib-2.0' -I'{staging_usr}/lib/glib-2.0/include' -L'{staging_usr}/lib' -lgtk-3 -lgobject-2.0 -lglib-2.0 -Wl,-soname,libgtk-android-ime.so -o '{out_ime}' '{ime_c}' -ldl")
            os.system(f"cp '{out_ime}' '{os.path.join(gtk_mod_dir, 'libgtk-android-ime.so')}'")

        # 3. xopp-title-watcher Binary
        watcher_c = os.path.join(scripts_dir, "xopp-title-watcher.c")
        out_watcher = os.path.join(staging_usr, "bin", "xopp-title-watcher")
        if os.path.exists(watcher_c):
            print("[*] Compiling xopp-title-watcher binary (16KB aligned)...")
            os.makedirs(os.path.join(staging_usr, "bin"), exist_ok=True)
            res = os.system(f"{ndk_clang} -O2 {page_size_flags} -I'{staging_usr}/include' -L'{staging_usr}/lib' -lX11 -o '{out_watcher}' '{watcher_c}'")
            if res == 0 and os.path.exists(out_watcher):
                os.chmod(out_watcher, 0o755)

        # 4. xopp-wallpaper Binary
        wallpaper_c = os.path.join(scripts_dir, "xopp-wallpaper.c")
        out_wallpaper = os.path.join(staging_usr, "bin", "xopp-wallpaper")
        if os.path.exists(wallpaper_c):
            print("[*] Compiling xopp-wallpaper binary (16KB aligned)...")
            os.makedirs(os.path.join(staging_usr, "bin"), exist_ok=True)
            res = os.system(f"{ndk_clang} -O2 {page_size_flags} -I'{staging_usr}/include' -L'{staging_usr}/lib' -lX11 -o '{out_wallpaper}' '{wallpaper_c}'")
            if res == 0 and os.path.exists(out_wallpaper):
                os.chmod(out_wallpaper, 0o755)

        # 5. Lightweight /proc/self/exe translation shim (LD_PRELOAD)
        shim_c = os.path.join(scripts_dir, "xopp-shim.c")
        out_shim = os.path.join(staging_usr, "lib", "libxopp-shim.so")
        if os.path.exists(shim_c):
            print("[*] Compiling xopp-shim library (libxopp-shim.so, 16KB aligned)...")
            os.system(f"{ndk_clang} -shared -fPIC {page_size_flags} -Wl,-soname,libxopp_shim.so -o '{out_shim}' '{shim_c}' -ldl")

    # Clean unneeded headers, docs, and static archives to keep payload lean
    print("[*] Stripping documentation, headers, and static archives...")
    for root, dirs, files in os.walk(staging_usr, topdown=False):
        for name in files:
            if name.endswith(".a") or name.endswith(".la"):
                try:
                    os.remove(os.path.join(root, name))
                except OSError:
                    pass
        if os.path.basename(root) in ["man", "doc", "include", "gtk-doc"]:
            os.system(f"rm -rf '{root}'")

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
    os.makedirs(os.path.join(staging_usr, "etc", "fonts"), exist_ok=True)
    os.makedirs(os.path.join(staging_usr, "tmp"), exist_ok=True)
    os.makedirs(os.path.join(staging_usr, "var", "cache", "fontconfig"), exist_ok=True)

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

    # Provision Xournal++ official gettext translation catalogs (share/locale)
    provision_xournalpp_translations(staging_usr, cache_dir)

    if args.jnilibs_dir:
        target_abi_dir = os.path.join(args.jnilibs_dir, abi_name)
        os.makedirs(target_abi_dir, exist_ok=True)
        # Export executables with lib*.so naming convention so Android extracts them into nativeLibraryDir
        bin_mappings = [
            ("xournalpp", "libxournalpp.so"),
            ("openbox", "libopenbox.so"),
            ("xopp-title-watcher", "libxopp_title_watcher.so"),
            ("xopp-wallpaper", "libxopp_wallpaper.so"),
            ("gdk-pixbuf-query-loaders", "libgdk_pixbuf_query_loaders.so"),
            ("glib-compile-schemas", "libglib_compile_schemas.so"),
            ("xdotool", "libxdotool.so")
        ]
        for bin_name, so_name in bin_mappings:
            src_bin = os.path.join(staging_usr, "bin", bin_name)
            if os.path.exists(src_bin):
                dest_so = os.path.join(target_abi_dir, so_name)
                shutil.copy2(src_bin, dest_so)
                os.chmod(dest_so, 0o755)
                print(f"[*] Exported native executable: {bin_name} -> {dest_so}")

        src_ime = os.path.join(staging_usr, "lib", "libgtk-android-ime.so")
        if os.path.exists(src_ime):
            dest_ime = os.path.join(target_abi_dir, "libgtk-android-ime.so")
            shutil.copy2(src_ime, dest_ime)
            os.chmod(dest_ime, 0o755)
            print(f"[*] Exported native module: libgtk-android-ime.so -> {dest_ime}")

        src_shim = os.path.join(staging_usr, "lib", "libxopp-shim.so")
        if os.path.exists(src_shim):
            dest_shim = os.path.join(target_abi_dir, "libxopp_shim.so")
            shutil.copy2(src_shim, dest_shim)
            os.chmod(dest_shim, 0o755)
            print(f"[*] Exported native shim: libxopp-shim.so -> {dest_shim}")

    print(f"[*] Packaging final archive to {args.output}...")
    os.makedirs(os.path.dirname(os.path.abspath(args.output)), exist_ok=True)
    os.system(f"tar -cf - -C '{staging_dir}' usr | xz -T0 > '{args.output}'")

    print(f"[✔] Bootstrap packaging complete for {termux_arch} ({abi_name}).")


if __name__ == "__main__":
    main()
