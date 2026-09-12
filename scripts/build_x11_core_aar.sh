#!/usr/bin/env bash
set -e

# Change to repo root
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$ROOT_DIR"

echo "==> Building :x11-core:assembleRelease from source..."
./gradlew :x11-core:assembleRelease -PusePrebuiltX11=false

mkdir -p libs
cp x11-core/build/outputs/aar/x11-core-release.aar libs/x11-core-release.aar

echo "==> Successfully created libs/x11-core-release.aar"
echo "==> Local and CI builds will now automatically use this prebuilt AAR and skip C++ compilation!"
