package com.conreo.couchytv.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.conreo.couchytv.data.AppEntry
import com.conreo.couchytv.data.AppList
import com.conreo.couchytv.data.LauncherConfig
import com.conreo.couchytv.data.NetStatus
import com.conreo.couchytv.data.SystemStats

/**
 * A faithful render of the home screen, for looking at a design WITHOUT
 * installing it on the TV.
 *
 * It draws the REAL composables — the same StatusBar and the same
 * SingleRowArea — with fabricated app icons and a fake wallpaper, so what you
 * see is what the device would show.
 */
@Composable
internal fun HomePreview(
    showClock: Boolean = true,
    showStats: Boolean = true,
    variant: Int = 0,
) {
    val density = LocalDensity.current
    val apps = remember { fakeApps() }
    val config = remember { LauncherConfig() }
    val accent = remember { ACCENTS[0] }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF14213D), Color(0xFF0B1220), Color(0xFF1B2233)),
                ),
            ),
    ) {
        // ---- top bar
        Box(Modifier.fillMaxWidth().height(80.dp)) {
            StatusBar(
                net = NetStatus(wifi = true),
                time = "10:57",
                date = "Mon 20 Oct",
                showVpn = false,
                glass = true,
                onVpnClick = {},
                onNetworkClick = {},
                onSettingsClick = {},
                stats = if (showStats) {
                    SystemStats(
                        ramUsedMb = 847,
                        ramTotalMb = 1769,
                        storageFreeMb = 1229,
                        storageTotalMb = 3900,
                    )
                } else null,
            )
        }
        // ---- the row
        SingleRowArea(
            modifier = Modifier.fillMaxSize(),
            apps = AppList(apps),
            config = config,
            accent = accent,
            movePkg = null,
            moveFocus = remember { androidx.compose.ui.focus.FocusRequester() },
            maxIcon = 120.dp,
            prefGap = 4.dp,
            intro = true,
            onLaunch = {},
            onMenu = {},
        )
    }
}

/** A believable set of apps: real names, real-ish brand colours, drawn icons. */
private fun fakeApps(): List<AppEntry> {
    val specs = listOf(
        Triple("org.smarttube.stable", "SmartTube", 0xFFE53935.toInt()),
        Triple("in.startv.hotstar", "JioHotstar", 0xFF8E24AA.toInt()),
        Triple("com.netflix.ninja", "Netflix", 0xFFB71C1C.toInt()),
        Triple("com.amazon.amazonvideo.livingroom", "Prime Video", 0xFF1565C0.toInt()),
        Triple("com.android.vending", "Play Store", 0xFF2E7D32.toInt()),
        Triple("com.google.android.youtube.tv", "YouTube", 0xFFC62828.toInt()),
        Triple("com.heytap.tv.filebrowser", "Files", 0xFF37474F.toInt()),
    )
    return specs.mapIndexed { i, (pkg, label, color) ->
        AppEntry(
            pkg = pkg,
            label = label,
            banner = null,
            icon = drawIcon(label, color),
            autoCategory = "",
            tile = Color(color),
            stamp = 0L,
            firstInstall = i.toLong(),
        )
    }
}

/** A square tile with the app's initial on it — enough to judge the layout. */
private fun drawIcon(label: String, color: Int): androidx.compose.ui.graphics.ImageBitmap {
    val bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val fill = Paint().apply { this.color = color }
    canvas.drawRect(0f, 0f, 256f, 256f, fill)
    val text = Paint().apply {
        this.color = android.graphics.Color.WHITE
        textSize = 110f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    val m = android.graphics.Paint.FontMetrics()
    text.getFontMetrics(m)
    val baseline = 128f - (m.ascent + m.descent) / 2f
    canvas.drawText(label.take(1).uppercase(), 128f, baseline, text)
    return bmp.asImageBitmap()
}
