#!/usr/bin/env bash
# Remove the throwaway screenshots taken while working on the launcher.
#
# Where they end up:
#   * on the TV  — loose PNGs in /sdcard (screencap) and /data/local/tmp
#   * locally    — the scratch dir below, plus pulled APKs and dex dumps
#
# The TV is a MEDIA device: /sdcard/Pictures, /sdcard/DCIM, /sdcard/Movies and
# friends hold the owner's photos and are never touched. Only root-level PNGs
# are ours — those are the screencap dumps.
#
# Usage:
#   tools/clean-screenshots.sh [--dry-run] [device]
#
#   device defaults to $ANDROID_SERIAL, then 192.168.1.4:5555 (the TV).
set -u

DRY=0
if [ "${1:-}" = "--dry-run" ]; then DRY=1; shift; fi

DEVICE="${1:-${ANDROID_SERIAL:-192.168.1.4:5555}}"
ADB="${ADB:-adb}"
LOCAL_DIR="${COUCHY_SHOT_DIR:-/tmp/couchy-shots}"

# Local strays from earlier sessions: pulled screenshots, the pulled LiveTV
# APK and its disassembled dex.
LOCAL_GLOBS=(
    "$LOCAL_DIR"
    "/tmp/*.png"
    "/tmp/livetv.apk"
    "/tmp/livetv.txt"
    "/tmp/lt"
)

run() {
    if [ "$DRY" = 1 ]; then
        echo "  would run: $*"
    else
        "$@" 2>&1 | sed 's/^/  /'
    fi
}

echo "== device: $DEVICE =="

echo "-- /sdcard root PNGs (ours; subfolders are the owner's media):"
run "$ADB" -s "$DEVICE" shell "ls /sdcard/*.png 2>/dev/null"
run "$ADB" -s "$DEVICE" shell "rm -f /sdcard/*.png"

echo "-- /data/local/tmp screenshots:"
run "$ADB" -s "$DEVICE" shell "rm -f /data/local/tmp/couchy-shot-*.png"

echo "-- local scratch:"
for g in "${LOCAL_GLOBS[@]}"; do
    # Unquoted in the shell -c so the glob expands before rm sees it.
    run bash -c "ls -d $g 2>/dev/null"
    run bash -c "rm -rf $g"
done

echo "done."
