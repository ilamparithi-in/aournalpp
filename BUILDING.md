# Building Aournal++

This guide outlines the prerequisites, cloning instructions, and steps required to build **Aournal++** from source.

---

## 1. Prerequisites & Environment Setup

Building Aournal++ requires both standard Android development tools and host-level native compilation utilities (for compiling X11/Xorg and OpenGL stub libraries).

### Host System Tools (Linux / macOS / WSL)
* **JDK 17**: Android Gradle Plugin requires Java 17 (e.g. OpenJDK 17).
* **Python 3**: Used by CMake to generate OpenGL/EGL dispatch headers (`libepoxy/src/gen_dispatch.py`).
* **Bison (`bison`)**: Parser generator required by Xorg / `xkbcomp`.
* **Patch (`patch`)**: Required by CMake to apply in-tree compatibility patches to submodules at build time.

On Debian/Ubuntu-based systems:
```bash
sudo apt update
sudo apt install -y openjdk-17-jdk python3 bison patch cmake build-essential
```

### Android SDK & NDK
* **Android SDK**: Compile SDK `34`, Min SDK `26`.
* **Android NDK**: Version **`25.1.8937393`** (installable via Android Studio SDK Manager or `sdkmanager "ndk;25.1.8937393"`).
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

## 4. Building the Project

### Command Line (Gradle)

To build the debug APK:
```bash
./gradlew assembleDebug
```

The output APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

To build and install directly to a connected Android device / emulator:
```bash
./gradlew installDebug
```

### Android Studio

1. Open Android Studio and select **Open**, then choose the `aournalpp` directory.
2. Android Studio will automatically run a Gradle sync.
3. If prompted for missing SDK/NDK components, open **SDK Manager** > **SDK Tools** > Check **Show Package Details** and ensure **NDK (Side by side) 25.1.8937393** and **CMake 3.22.1** are installed.
4. Select the `app` run configuration and click **Run** (or `Shift + F10`).

---

## 5. Syncing with Upstream `termux-x11`

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

## 6. Troubleshooting

* **`bison: command not found` or `patch: command not found`**:
  Ensure `bison` and `patch` are installed on your host system and accessible in your `$PATH`.
* **NDK Version Mismatch**:
  Ensure `local.properties` points to your Android SDK and that NDK `25.1.8937393` is present under `$ANDROID_SDK_ROOT/ndk/25.1.8937393`.
* **CMake Header Generation Errors**:
  Ensure Python 3 is installed and executable as `/usr/bin/python3` or in your system PATH.
