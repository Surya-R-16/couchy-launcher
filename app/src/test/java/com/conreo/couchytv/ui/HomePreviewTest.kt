package com.conreo.couchytv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * Renders the home screen to PNG on the JVM — no device, no emulator.
 *
 * This is the "see it before it is on the TV" loop: a design change can be
 * rendered and looked at in seconds, without installing anything.
 */
class HomePreviewTest {

    @get:Rule
    val paparazzi = Paparazzi(
        // The TV's EFFECTIVE layout width: the device is 1920x1080 px at
        // 320dpi, and the app's own ScaledUi (uiScale "Auto") turns 960dp into
        // 1200dp. So 1200x675dp is the space the UI really has.
        deviceConfig = DeviceConfig(
            screenHeight = 675,
            screenWidth = 1200,
            density = com.android.resources.Density.XHIGH,
            xdpi = 320,
            ydpi = 320,
            orientation = com.android.resources.ScreenOrientation.LANDSCAPE,
        ),
        theme = "android:Theme.Material.NoActionBar",
        showSystemUi = false,
    )

    @Test
    fun current() {
        paparazzi.snapshot {
            Box(Modifier.fillMaxSize().background(Color(0xFF1A1C22)))
        }
    }

    @Test
    fun home() {
        paparazzi.snapshot {
            HomePreview()
        }
    }
}
