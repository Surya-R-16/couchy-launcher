# Previews — see the UI without the TV

`paparazzi` renders the real Compose code to PNG **on the JVM**. No device, no
emulator, no install.

```sh
export ANDROID_HOME=$HOME/Android/Sdk JAVA_HOME=$(mise where java)

./gradlew :app:recordPaparazziDebug    # render → app/src/test/snapshots/images/
./gradlew :app:verifyPaparazziDebug    # fail if the UI changed unexpectedly
```

`HomePreviewTest.home` draws the **actual** `StatusBar` and `SingleRowArea`
composables with fabricated app icons and a fake wallpaper, so the render is
what the TV would show. `HomePreview.kt` is where the fake data lives.

The `DeviceConfig` is the TV's *effective* layout width: the device is
1920×1080 px at 320dpi, and the app's own `ScaledUi` turns 960dp into 1200dp —
so the preview is 1200×675dp, the space the UI really has.

## Why it is worth having

- A design change can be rendered and looked at in ~10 seconds, instead of
  install → screenshot → install → screenshot.
- `verifyPaparazziDebug` makes the rendered PNGs a **screenshot regression
  test**: an accidental visual change fails the build instead of reaching the
  TV.
- It found a real bug on the first run: with a date shown, the clock wraps to
  two lines in the status cluster.
