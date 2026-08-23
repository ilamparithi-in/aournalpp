# Building Aournal++

This guide outlines the prerequisites, cloning instructions, and steps required to build **Aournal++** from source across multiple architectures (**ARM64** and **x86_64**).

---

## 1. Prerequisites & Environment Setup

Building Aournal++ requires both standard Android development tools and host-level native compilation utilities (for compiling X11/Xorg and OpenGL stub libraries).

### Host System Tools (Linux / macOS / WSL)
* **JDK 17**: Android Gradle Plugin requires Java 17 (e.g. OpenJDK 17).
* **Python 3**: Used by CMake to generate OpenGL dispatch headers and by the bootstrap packager (`scripts/build_bootstrap.py`).
* **Bison (`bison`)**: Parser generator required by Xorg / `xkbcomp`.
* **Patch (`patch`)**: Required by CMake to apply in-tree compatibility patches to submodules at build time.

On Debian/Ubuntu-based systems:
```bash
sudo apt update
sudo apt install -y openjdk-17-jdk python3 bison patch cmake build-essential
```

### Android SDK & NDK
* **Android SDK**: Compile SDK `34`, Min SDK `26`.
* **Android NDK**: Version **`27.0.12077973`** (installable via Android Studio SDK Manager or `sdkmanager "ndk;27.0.12077973"`).
* **CMake**: Version `3.22.1` or newer.

---

## 2. Cloning the Repository

Aournal++ relies on `termux-x11`, which itself contains nested submodules for native X11 libraries (`pixman`, `libxkbfile`, `libxtrans`, `libepoxy`, etc.). 

**Always clone with `--recurse-submodules`:**

```bash
git clone --recurse-submodules https://github.com/ilamparithi-in/aournalpp.git
cd aournalpp
```

> [!TIP]
> If you already cloned the repository without `--recurse-submodules`, initialize all submodules recursively by running:
> ```bash
> git submodule update --init --recursive
> ```

---

## 3. Submodule & Patch Architecture

Understanding how native dependencies are structured:

1. **`submodules/termux-x11`**:
   * Tracks the fork [`ilamparithi-in/termux-x11`](https://github.com/ilamparithi-in/termux-x11) on branch `aournalpp-main`.
   * Includes pen/stylus enhancements, event loop threading fixes, and UI preferences.
   * A full standalone patch archive is also maintained in [`patches/termux-x11/`](./patches/termux-x11/) for reproducibility.

2. **Nested C/C++ Submodules (`pixman`, `libxkbfile`, `libxtrans`, `libepoxy`)**:
   * These live inside `submodules/termux-x11/lorie/src/main/cpp/`.
   * They are clean upstream mirrors. When Gradle / CMake builds the native code, `target_apply_patch` in `CMakeLists.txt` automatically applies the required Android compatibility patches in-place.

---

## 4. Building the Project (Multi-Architecture)

Aournal++ provides separate product flavors for **ARM64** (`arm64-v8a`) and **x86_64** to keep APK sizes lightweight (~100MB per APK).

### Command Line (Gradle)

#### Debug APKs

* **ARM64 (`arm64-v8a`)**:
  ```bash
  ./gradlew assembleArm64Debug
  ```
  Output APK: `app/build/outputs/apk/arm64/debug/app-arm64-debug.apk`

* **x86_64**:
  ```bash
  ./gradlew assembleX86_64Debug
  ```
  Output APK: `app/build/outputs/apk/x86_64/debug/app-x86_64-debug.apk`

* **All Flavors**:
  ```bash
  ./gradlew assembleDebug
  ```

#### Release APKs & Signing

To build signed release APKs, pass signing credentials via environment variables, `local.properties`, or command-line parameters:

```bash
export KEYSTORE_FILE="/path/to/release.jks"
export KEYSTORE_PASSWORD="your_keystore_password"
export KEY_ALIAS="your_key_alias"
export KEY_PASSWORD="your_key_password"

./gradlew assembleArm64Release
./gradlew assembleX86_64Release
```

Or via `local.properties`:
```properties
release.keystore.path=/path/to/release.jks
release.keystore.password=your_keystore_password
release.key.alias=your_key_alias
release.key.password=your_key_password
```

Outputs:
* `app/build/outputs/apk/arm64/release/app-arm64-release.apk`
* `app/build/outputs/apk/x86_64/release/app-x86_64-release.apk`

*(If no signing credentials are provided, unsigned release APKs will be generated.)*

---

### Android Studio

1. Open Android Studio and select **Open**, then choose the `aournalpp` directory.
2. Android Studio will automatically run a Gradle sync.
3. Open the **Build Variants** tool window (bottom-left) and select your target variant:
   - `arm64Debug` (or `arm64Release`)
   - `x86_64Debug` (or `x86_64Release`)
4. Click **Run** (or `Shift + F10`).

---

## 5. Standalone Bootstrap Packaging

The embedded userland bootstrap archive can also be built standalone using Python:

```bash
# Package ARM64 (aarch64) userland
python3 scripts/build_bootstrap.py --arch aarch64 --output build/bootstrap-arm64.tar.xz

# Package x86_64 userland
python3 scripts/build_bootstrap.py --arch x86_64 --output build/bootstrap-x86_64.tar.xz
```

Or via Gradle helper tasks:
```bash
./gradlew buildBootstrapArm64
./gradlew buildBootstrapX86_64
```

---

## 6. Syncing with Upstream `termux-x11`

If new features or fixes arrive in upstream `termux/termux-x11`:

1. Fetch and rebase inside the submodule:
   ```bash
   cd submodules/termux-x11
   git fetch upstream master
   git rebase upstream/master
   git push origin aournalpp-main --force-with-lease
   cd ../..
   ```

2. Export the updated patch files and commit the submodule pointer:
   ```bash
   git -C submodules/termux-x11 format-patch upstream/master..aournalpp-main -o ../../patches/termux-x11/
   git add submodules/termux-x11 patches/termux-x11
   git commit -m "chore: sync termux-x11 with latest upstream"
   git push origin main
   ```

---

## 7. Troubleshooting

* **`bison: command not found` or `patch: command not found`**:
  Ensure `bison` and `patch` are installed on your host system and accessible in your `$PATH`.
* **NDK Version Mismatch**:
  Ensure `local.properties` points to your Android SDK and that NDK `27.0.12077973` is present under `$ANDROID_SDK_ROOT/ndk/27.0.12077973`.
* **CMake Header Generation Errors**:
  Ensure Python 3 is installed and executable as `/usr/bin/python3` or in your system PATH.
