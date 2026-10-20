# Couchy Launcher (fork) — working rules for agents

## What this is

A fork of [`conreo/couchy-launcher`](https://github.com/conreo/couchy-launcher) (GPL-3.0),
our fork at **`Surya-R-16/couchy-launcher`**. Upstream's last commit was 2026-08-01 and
it is effectively unmaintained, which is why we forked.

Tags that must NOT change — they are what makes the fork an *upgrade* of the stock app
rather than a second app:

| | |
|---|---|
| applicationId | `com.conreo.couchytv` |
| app label | Couchy Launcher |
| minSdk / targetSdk | 21 / 34 |

Changing the applicationId would force an uninstall, which wipes the user's config
(sections, wallpaper, pinned dock).

## Build

```sh
cd ~/Projects/couchy-launcher
export ANDROID_HOME=$HOME/Android/Sdk JAVA_HOME=$(mise where java)
./gradlew assembleDebug testDebugUnitTest     # build + unit tests
```

The Android SDK was deleted from this machine once and had to be reinstalled. If the
build fails with a missing `android.jar`, reinstall `platforms;android-34` and
`build-tools;34.0.0` with the cmdline-tools `sdkmanager` before assuming a code problem.

`composeCompiler { reportsDestination }` is enabled on purpose: `app/build/compose_reports/`
holds the Compose compiler's own stability inference. Read it instead of guessing
whether a composable can skip.

## The device (the TV)

OnePlus TV model C00, Android 10 (SDK 29), 1920×1080, 1.8 GB RAM, MTK m7632.
ADB over Wi-Fi: **`192.168.1.4:5555`** (the DHCP lease moves — scan with
`/dev/tcp/<ip>/5555` if it stops answering). `adb` lives in `~/Android/Sdk/platform-tools`.

- `screencap` returns an all-black frame right after a cold start — the launcher takes
  ~7 s to appear. Wait, then capture again. Check `mWakefulness` before diagnosing.
- `screencap` returns a 0-byte file over a secure surface (Netflix and other DRM apps).
- `input keyevent` sends down+up, so it can never reproduce a remote's HOLD. It launches
  the app instead of opening a long-press menu. `sendevent` needs root, which this TV
  does not have. **Gestures that need auto-repeat can only be tested by hand.**
- `adb install -r <apk>` streams and leaves nothing behind. Never `adb push` an APK to
  `/data/local/tmp` and `pm install` it — that left 119 MB of old builds behind once.

## Performance — measure, do not guess

Measured with `dumpsys gfxinfo` (see `tools/bench-navigation.sh`):

| | frames | high input latency |
|---|---|---|
| row navigation, 8 presses (before) | 106 | 69 |
| row navigation (now) | 42 | 11 |

**The single biggest cause of lag was the focus animation, not recomposition.**
tv-material's `Surface` runs a hard-coded 300 ms scale tween → 13 frames per key press,
and each frame costs ~34 ms on this TV. Removing the growth and the blur took high
input latency from 69 to 11.

What is expensive here, and why:

- **A full-screen wallpaper composite costs ~17 ms.** Any animation over one misses
  vsync every frame. The answer on this hardware is *less animation*, not faster
  animation.
- `Glow(elevation = 8.dp)` is a Skia **blur mask**, redrawn every frame.
- `Modifier.clip(squircle)` is a generic **path-mask clip**, not Skia's fast round-rect.
  Fine for small cards, expensive over 1920 px — use `RoundedCornerShape` for wide panels.
- `SmoothCornerShape` (androidx.graphics) has **no `equals()`**, and tv-material's
  outline cache keys on the shape *instance* → 3 path rebuilds per card per recomposition.
- A `Dialog` is a **new window**: ~600–800 ms of measure/layout on this TV. Every dialog
  pays it. The settings sheet's ~1.25 s open is mostly this, not app logic.

## TV design rules (from developer.android.com/design/ui/tv)

- **10ft UI.** The viewer is ~3 m away. Text and controls must be legible at that
  distance; limit how much text is on screen.
- **"TV UI must provide instant and distinct feedback when buttons are pressed."**
  This is the official rule, and it is why the focus must not animate for 300 ms.
- Google's own **immersive list** spec says a focused card is emphasised by
  **scaling it by 1.1, using a border, and elevation**. We keep the border (cheap)
  and drop the elevation (a blur mask). The scale is worth revisiting with a *short*
  tween rather than tv-material's hard-coded 300 ms one.
- TV is a **communal device** — avoid surfacing personal information by default.
- D-pad only: every action must be reachable with Up/Down/Left/Right/OK.

## Workflow — read this part twice

1. **The owner does the on-device testing.** Build, install, then hand them a short
   list of what to press and where to look. Do not run a test campaign on the TV.
2. **Do not push without asking.** Commit locally, show the change, wait for an
   explicit go-ahead. Pushing unasked is a bug, not initiative.
3. **After every push, clear the throwaway screenshots** — `tools/clean-screenshots.sh`
   runs from the local `post-push` hook. `/sdcard` on this TV is a MEDIA directory:
   the owner's photos are in `Pictures/`, `DCIM/`, `Movies/`. Only root-level PNGs are ours.

## Where the detail lives

`NOTES.local.md` in the repo root (gitignored) — the fork's work log: what was changed
and why, the device quirks, the upstream issue triage, and the perf measurements.
`~/Work/BOARD.md` — the cross-project board.

## Upstream

`upstream` remote = `conreo/couchy-launcher`. We are ahead, never behind; fetch and
rebase when upstream moves. Issues live upstream but are implemented here — the issue
number is only a label, never a dependency.
