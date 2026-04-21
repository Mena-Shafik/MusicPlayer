#!/usr/bin/env bash
set -euo pipefail

# Simple helper script to run instrumentation tests (connectedAndroidTest) and
# pull screenshots written by the instrumentation test to a local folder.
#
# Usage:
#   bash scripts/capture_screenshots.sh
#
# It will attempt to run Gradle connectedAndroidTest, then pull /sdcard/shot-screenshots
# from the connected device/emulator into ./app/screenshots. If no screenshots are
# present it will also attempt to capture the current device screen(s) directly.

OUT_DIR="app/screenshots"
DEVICE_DIR="/sdcard/shot-screenshots"

mkdir -p "${OUT_DIR}"

echo "Running instrumentation tests (connectedAndroidTest) to generate screenshots..."
if ./gradlew connectedAndroidTest --no-daemon; then
  echo "connectedAndroidTest finished"
else
  echo "connectedAndroidTest failed or no device/emulator present — continuing to try to pull existing screenshots"
fi

echo "Waiting for device..."
adb wait-for-device

echo "Listing devices:"
adb devices

echo "Attempting to pull screenshots from device: ${DEVICE_DIR} -> ${OUT_DIR}"
if adb shell "ls ${DEVICE_DIR}" >/dev/null 2>&1; then
  adb pull "${DEVICE_DIR}" "${OUT_DIR}" || true
  echo "Pulled screenshots to ${OUT_DIR}/shot-screenshots"
else
  echo "No instrumentation screenshots folder found on device. Falling back to single-screenshot capture."
  # Capture current device screen to a timestamped file
  TIMESTAMP=$(date +%Y%m%d-%H%M%S)
  OUTFILE="${OUT_DIR}/screenshot-${TIMESTAMP}.png"
  adb exec-out screencap -p > "${OUTFILE}"
  echo "Saved fallback screenshot to ${OUTFILE}"
fi

echo "Done. Check ${OUT_DIR} for screenshots."

