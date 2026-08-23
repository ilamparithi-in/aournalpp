# Aournal++

> **Aournal++** is a modern Android companion and wrapper for [Xournal++](https://xournalpp.github.io/), powered by an embedded Termux & X11 environment with enhanced pen/stylus support, Material You (M3) UX, and optimized low-latency note-taking workflows.

---

## ✨ Features

* **Embedded X11 Runtime**: Self-contained Xorg server based on Termux-X11 with hardware-accelerated rendering and direct Xournal++ integration.
* **First-Class Stylus & Pen Support**:
  * Low-latency pen and eraser routing.
  * Specialized Lenovo Precision Pen gesture and button mapping (single, double, triple, long-press actions).
  * Hover, pressure, tilt, and orientation handling.
* **Modern Material 3 Interface**:
  * Dynamic Document Hub with recent notes, templates, and quick actions.
  * Material You theming and responsive tablet/foldable layouts.
  * In-app runtime manager, package setup, and configuration editor.

---

## 🛠️ Getting Started & Building

To build Aournal++ from source, clone the repository with its submodules:

```bash
git clone --recurse-submodules https://github.com/ilamparithi-in/aournalpp.git
cd aournalpp
./gradlew assembleDebug
```

For complete prerequisites, host-tool requirements (Bison, Python 3, NDK), and Android Studio setup instructions, check out the **[Building Guide](BUILDING.md)**.

---

## 📁 Repository Structure

* **`app/`**: Main Android application module containing the Jetpack Compose UI, Document Hub, and lifecycle management.
* **`x11-core/`**: Core X11 display viewport, preference bridge, and native integration layer.
* **`runtime-manager/`**: Package management, rootfs bootstrap routines, and process launcher for Xournal++.
* **`submodules/termux-x11/`**: Customized Termux-X11 submodule fork (`aournalpp-main` branch) with stylus threading fixes and button listeners.
* **`patches/termux-x11/`**: Standalone patch series archive for upstream synchronization and reproducibility.

---

## 📄 License

This project is licensed under the [GPL-3.0 License](LICENSE) or respective upstream licenses for subcomponents.
