package com.conreo.couchytv.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val ACCENTS = listOf(
    Color(0xFF8AB4F8),
    Color(0xFF81C995),
    Color(0xFFFDD663),
    Color(0xFFF28B82),
    Color(0xFFD7AEFB),
    Color(0xFF78D9EC),
)

data class WallpaperPreset(val name: String, val colors: List<Color>)

val WALLPAPERS = listOf(
    WallpaperPreset("Midnight", listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
    WallpaperPreset("Aurora", listOf(Color(0xFF13547A), Color(0xFF2B825B), Color(0xFF80D0C7))),
    WallpaperPreset("Sunset", listOf(Color(0xFF41295A), Color(0xFF8F4A3E), Color(0xFFD76D77))),
    WallpaperPreset("Deep", listOf(Color(0xFF000428), Color(0xFF004683))),
    WallpaperPreset("Charcoal", listOf(Color(0xFF16161A), Color(0xFF232526), Color(0xFF2F3437))),
)

fun WallpaperPreset.brush(): Brush = Brush.linearGradient(colors)

/* ---------------- chrome: the one glass surface ----------------
 * The dock panel, the status bar cluster and the stats chip are all the same
 * material. It used to be hard-coded at each of those sites, which is how the
 * dock drifted to a different shape and a different distance from the screen
 * edge than the status bar. Everything visual about the chrome lives here now.
 */

/** Translucent tint behind the chrome. */
val ChromeGlass = Color(0xB3121418)

/** Hairline sheen over [ChromeGlass]; keeps the panels from reading as flat black. */
const val CHROME_SHEEN = 0.06f

/** The glass material itself: tint + sheen, clipped to [shape]. */
fun Modifier.glassPanel(shape: Shape): Modifier = this
    .clip(shape)
    .background(ChromeGlass)
    .background(Color.White.copy(alpha = CHROME_SHEEN))

/* ---------------- dock geometry ----------------
 * The dock is a floating island, so it has to clear the screen edge by more
 * than its own corner radius — otherwise the bezel eats the bottom corners and
 * the panel reads as a bar sliding off the screen. At the default roundness the
 * panel radius is 18 + 14 = 32 dp, so the bottom margin is 32 dp: tangent, and
 * about 5% of a 1080p panel, which is the overscan-safe guide.
 */

/** Distance from the screen edge to the floating dock, left and right. */
val DOCK_MARGIN_H = 24.dp

/** Distance from the bottom edge. Must exceed the panel radius (see above). */
val DOCK_MARGIN_BOTTOM = 32.dp

/** Padding between the dock's glass edge and the cards inside it. */
val DOCK_INNER_PADDING = 12.dp

/** Panel radius = card radius + this. A panel corner tighter than the cards it
 *  holds looks pinched; the extra roundness is what makes it read as a surface. */
val DOCK_PANEL_RADIUS_EXTRA = 14.dp

/** Deterministic tile color for apps that ship no banner artwork. */
fun tileColor(pkg: String): Color {
    val palette = listOf(
        Color(0xFF37474F), Color(0xFF4E342E), Color(0xFF1B5E20),
        Color(0xFF0D47A1), Color(0xFF4A148C), Color(0xFF880E4F),
        Color(0xFF3E2723), Color(0xFF263238), Color(0xFF33691E),
    )
    var h = 0
    for (c in pkg) h = h * 31 + c.code
    return palette[((h % palette.size) + palette.size) % palette.size]
}

@Composable
fun LiteTvTheme(accent: Color, content: @Composable () -> Unit) {
    // The scheme is remembered: a FRESH instance on every theme recomposition
    // would invalidate every reader of MaterialTheme.colorScheme in the app
    // (the cards, the settings sheet) even though no colour had changed.
    val scheme = remember(accent) {
        darkColorScheme(
            primary = accent,
            surface = Color(0xEE1C1E24),
            onSurface = Color(0xFFE8EAED),
            surfaceVariant = Color(0xFF303440),
            onSurfaceVariant = Color(0xFF9AA0A6),
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        content = content,
    )
}
