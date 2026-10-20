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

## UI/UX rules for changes to the look

Goal: modernize the UI with **small, reviewable** changes. Never break D-pad
navigation, and never regress the measured performance above.

### Read these before touching anything visual

- Android TV design hub: https://developer.android.com/design/ui/tv
- TV colour system: https://developer.android.com/design/ui/tv/guides/styles/color-system
- Compose for TV: https://developer.android.com/training/tv/playback/compose
- TV layouts and overscan: https://developer.android.com/training/tv/playback/compose/layouts
- Adaptive TV guide (focus, overscan): https://developer.android.com/develop/adaptive-apps/guides/tv/build-adaptive-apps-for-tv
- Fire TV guidelines: https://developer.amazon.com/docs/fire-tv/design-and-user-experience-guidelines.html
- tv-material releases: https://developer.android.com/jetpack/androidx/releases/tv

### Use the TV design system, not the phone one

**`androidx.tv.material3.*` — never `androidx.compose.material3.*`.** The phone
material3 artifact is not even a dependency here, and its `MaterialTheme.typography`
does not exist in this theme. (This was got wrong once already, in the status
readout.) `androidx.tv:tv-material:1.0.0` does ship `Card`, `Button`, `Chip`,
`Carousel`, `ListItem`, `NavigationDrawer`, `Switch`, `Tab`, `IconButton`,
`Checkbox`, `RadioButton` — verified against the artifact, not assumed.

The `theming/styles` skill in `.pi/skills/` needs an **alpha** Compose and the
experimental Styles API. We are on stable (BOM 2024.09.03) — it does not apply
to this project.

### Design rules

- **10ft UI.** The viewer is ~3 m away: large, bold, sans-serif. No thin weights.
  tv-material's default type scale is already TV-sized; we do not override it
  (only three hard-coded `sp` values exist in the whole app).
- **Bold contrast.** Subtle colour differences vanish on a TV, and the launcher
  draws over arbitrary video wallpapers. The status and dock glass exists for
  exactly this reason.
- **Low information density.** Uncluttered beats clever.
- **5% overscan-safe margin.** The TV design guidance asks for it; **we do not
  currently keep it** — the status bar uses 40 dp of 1200 dp (3.3%) and the
  single-row panel 24 dp (2%). This TV has no overscan so it looks fine, but on
  a set that does, the clock and the row's end icons would be clipped. Fixing it
  is a deliberate decision, not an oversight — the layout gets smaller.
- **Tokens in ONE place.** Colours, type, corner radius, spacing and the glass
  material belong in `ui/Theme.kt` and nowhere else. Today the glass colour
  `Color(0xB3121418)` is hard-coded in four files — that is the debt to pay
  during the visual refresh.

### Focus — read this before "improving" the focus state

Google's own immersive-list spec says a focused card is emphasised by
**scaling it by 1.1, using a border, and elevation**. We currently use **only
the border**, because the scale is a hard-coded 300 ms tween in tv-material
(13 frames per key press, ~34 ms each on this TV) and the elevation is a Skia
blur mask. Removing them took high input latency from 69 to 11.

The border stays. If the scale is brought back, it needs a **short tween we
control** (100–150 ms), not tv-material's default — and it must be re-measured
with `tools/bench-navigation.sh`.

### Navigation and focus — do not regress

- Every element reachable by D-pad. No focus traps. Back always works.
- Restore focus when returning to a row or a screen (`focusRestorer`).
- Set the initial focus explicitly on every screen.
- The long-press menu and "move" mode rely on **auto-repeat** (the remote
  holding OK). `input keyevent` cannot reproduce it — see the device section.

### Workflow for a visual change

1. **Pick a direction first.** "Modernize it" produces beige. `design-refs/`
   holds the before/after screenshots; match the *feel* of a reference, not a
   description of one.
2. **One surface at a time** — cards, then the row, then the status bar, then
   settings. Small diffs stay reviewable; a big redesign breaks focus.
3. **Build, install, and hand the owner a list of what to press.** They test.
4. **Re-measure.** `tools/bench-navigation.sh` before and after.

## Tooling

**`android` CLI** (Google's, installed to `~/.local/bin/android` — already on PATH,
no profile file was modified). It talks to the TV over the same adb as always:

```sh
android info                                          # sdk, version
android layout --device=192.168.1.4:5555 --pretty    # the view/semantics tree as JSON
android screen capture --device=... --output=x.png    # screenshot
android screen capture --device=... --annotate        # + numbered bounding boxes
android screen resolve --screenshot=x.png --string="tap the gear at #12"
android input ...                                     # gestures and key events
```

**`android layout` is the important one.** It returns every element with its
exact bounds, so "is this inside the 5% safe zone" and "do these two pills
line up" become arithmetic instead of eyeballing a screenshot. It reads the
Compose semantics tree, so it sees what a D-pad user sees.

`android screen capture --annotate` + `resolve` closes the loop: look at the
numbered screenshot, refer to `#12`, get real coordinates back.

Note `android skills` installs into the CLI's own skill directory, which pi does
not read — that is why the relevant skills are **vendored into `.pi/skills/`**
instead.

## Where the detail lives

`NOTES.local.md` in the repo root (gitignored) — the fork's work log: what was changed
and why, the device quirks, the upstream issue triage, and the perf measurements.
`~/Work/BOARD.md` — the cross-project board.

`.pi/skills/` — Google's official Android agent skills (Apache-2.0, from
`github.com/android/skills`), vendored into the repo: Compose performance,
adaptive layouts, navigation events, and the Android CLI. Read the relevant one
before a change in its area.

## Upstream

`upstream` remote = `conreo/couchy-launcher`. We are ahead, never behind; fetch and
rebase when upstream moves. Issues live upstream but are implemented here — the issue
number is only a label, never a dependency.
