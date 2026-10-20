#!/usr/bin/env bash
# Measure how the launcher FEELS: how long a key press takes to land.
#
# "High input latency" is the counter that matters — it counts frames where
# the app took more than one vsync to respond to the key event. That is the
# lag you feel, not the frame time.
#
# Usage:
#   tools/bench-navigation.sh [device]        # row navigation
#   tools/bench-navigation.sh --settings      # settings sheet
#   tools/bench-navigation.sh --both
set -u

DEVICE="${ANDROID_SERIAL:-192.168.1.4:5555}"
ADB="${ADB:-adb}"
PKG="com.conreo.couchytv"
MODE="row"

while [ $# -gt 0 ]; do
    case "$1" in
        --settings) MODE="settings"; shift ;;
        --both) MODE="both"; shift ;;
        *) DEVICE="$1"; shift ;;
    esac
done

stats() {
    $ADB -s "$DEVICE" shell "dumpsys gfxinfo $PKG" 2>/dev/null | grep -E \
        "Total frames|Janky frames|50th percentile|90th percentile|95th percentile|99th percentile|Number High input latency" \
        | sed 's/^ */  /'
}

bench_row() {
    echo "== row navigation (8 presses) =="
    $ADB -s "$DEVICE" shell "am force-stop $PKG" >/dev/null 2>&1
    sleep 1
    $ADB -s "$DEVICE" shell "am start -n $PKG/.MainActivity" >/dev/null 2>&1
    sleep 8
    $ADB -s "$DEVICE" shell "dumpsys gfxinfo $PKG reset" >/dev/null 2>&1
    sleep 1
    for _ in 1 2 3 4; do
        $ADB -s "$DEVICE" shell "input keyevent KEYCODE_DPAD_RIGHT" >/dev/null 2>&1
        sleep 0.35
    done
    for _ in 1 2 3 4; do
        $ADB -s "$DEVICE" shell "input keyevent KEYCODE_DPAD_LEFT" >/dev/null 2>&1
        sleep 0.35
    done
    sleep 1
    stats
}

bench_settings() {
    echo "== settings (open + 5 moves) =="
    $ADB -s "$DEVICE" shell "am force-stop $PKG" >/dev/null 2>&1
    sleep 1
    $ADB -s "$DEVICE" shell "am start -n $PKG/.MainActivity" >/dev/null 2>&1
    sleep 8
    $ADB -s "$DEVICE" shell "dumpsys gfxinfo $PKG reset" >/dev/null 2>&1
    sleep 1
    $ADB -s "$DEVICE" shell "input keyevent KEYCODE_MENU" >/dev/null 2>&1
    sleep 2.5
    for _ in 1 2 3 4 5; do
        $ADB -s "$DEVICE" shell "input keyevent KEYCODE_DPAD_DOWN" >/dev/null 2>&1
        sleep 0.4
    done
    sleep 1
    stats
}

echo "device: $DEVICE"
case "$MODE" in
    row) bench_row ;;
    settings) bench_settings ;;
    both) bench_row; echo; bench_settings ;;
esac
