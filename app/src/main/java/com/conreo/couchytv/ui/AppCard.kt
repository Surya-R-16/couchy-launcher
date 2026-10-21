package com.conreo.couchytv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.compose.foundation.BorderStroke
import com.conreo.couchytv.data.AppEntry


/**
 * How a card draws itself. Three candidates were rendered side by side to pick
 * the look; only ONE survives — delete the others once the owner has chosen.
 */
enum class AppCardStyle {
    /** Coloured tile, 2dp accent ring on focus. The current look. */
    Tile,

    /** Graphite tile, heavy corner radius, no ring: the icon carries it. */
    Graphite,

    /** Full-bleed icon, no inset; focus brightens the tile and rings it white. */
    Bare,
}

@Composable
fun AppCard(
    app: AppEntry,
    isMoving: Boolean,
    isHidden: Boolean,
    accent: Color,
    cardWidth: androidx.compose.ui.unit.Dp = 190.dp,
    showLabel: Boolean = true,
    style: AppCardStyle = AppCardStyle.Tile,
    // Square tile (the single-row dock): the app's own launcher icon, centred.
    // A 16:9 banner at dock size would be a sliver, so square is the only
    // readable option there.
    square: Boolean = false,
    modifier: Modifier = Modifier,
    onLaunch: () -> Unit,
    onLongPress: () -> Unit,
) {
    // Long-press handling. The launcher must never do BOTH actions, and the
    // trap is that releasing OK after a hold still arrives as a normal click
    // on the Surface (its own built-in handler) — which launched the app
    // behind the freshly opened menu. So instead of fighting the key events,
    // we detect the hold ourselves and then SUPPRESS the click that follows
    // it, using a short time window that expires on its own (a sticky flag
    // would eat the next legitimate press, since the release is delivered to
    // the menu's window and never comes back to this card).
    // The squircle was rebuilt on EVERY composition, and SmoothCornerShape has
    // no equals() — so tv-material's outline cache missed on the shape
    // INSTANCE and rebuilt the path three times per card (surface, glow,
    // border) on every recomposition. One instance per corner radius fixes it.
    val corner = LocalCornerRadius.current
    // Graphite uses a much rounder tile — it is the whole point of that look.
    val radius = if (style == AppCardStyle.Graphite) corner + 10.dp else corner
    val cardShape = remember(radius) { SmoothCornerShape(radius) }
    var downAt by remember { mutableLongStateOf(0L) }
    var lastKeyDown by remember { mutableLongStateOf(0L) }
    var longPressAt by remember { mutableLongStateOf(0L) }

    fun fireLongPress() {
        longPressAt = SystemClock.uptimeMillis()
        onLongPress()
    }

    // Focus: the card grows a touch, lifts on a soft tight shadow, and gains a
    // thin accent ring — crisp to read across a room.
    val surfaceShape = rememberComputed(cardShape, MaterialTheme.shapes) {
        ClickableSurfaceDefaults.shape(cardShape)
    }
    val tileColor = when (style) {
        AppCardStyle.Tile -> app.tile
        // Near-graphite, a touch of the app's own hue so it is not a grey slab.
        AppCardStyle.Graphite -> Color(0xFF1B1D23)
        AppCardStyle.Bare -> Color(0xFF14161B)
    }
    val focusTile = when (style) {
        AppCardStyle.Tile -> tileColor
        AppCardStyle.Graphite -> Color(0xFF2A2D35)
        AppCardStyle.Bare -> Color(0xFF23262E)
    }
    val surfaceColors = rememberComputed(tileColor, focusTile, MaterialTheme.colorScheme) {
        ClickableSurfaceDefaults.colors(
            containerColor = tileColor,
            focusedContainerColor = focusTile,
            contentColor = Color.White,
            focusedContentColor = Color.White,
        )
    }
    // Focus: no growth, no blur.
    //
    // This was the single biggest source of lag. tv-material's Surface runs a
    // hard-coded 300ms scale tween, so ONE key press produced 13 frames — and
    // each frame costs ~34ms on this TV (the full-screen wallpaper alone needs
    // ~17ms to composite), so the focus took ~440ms to settle. The glow was a
    // Skia blur mask redrawn on top of that, every frame.
    //
    // The accent ring below carries the focus instead: a cheap stroke, and it
    // reads clearly at 3m.
    val surfaceScale = remember { ClickableSurfaceDefaults.scale(focusedScale = 1.0f) }
    val surfaceGlow = remember {
        ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(elevationColor = Color.Transparent, elevation = 0.dp)
        )
    }
    val ringColor = if (style == AppCardStyle.Tile) accent else Color.White.copy(alpha = 0.85f)
    val ringWidth = when {
        isMoving -> 3.dp
        style == AppCardStyle.Tile -> 2.dp
        style == AppCardStyle.Graphite -> 0.dp      // no ring at all: the tile lift does it
        else -> 2.dp
    }
    val surfaceBorder = rememberComputed(isMoving, ringColor, ringWidth, cardShape, MaterialTheme.colorScheme) {
        ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(ringWidth, ringColor), shape = cardShape),
        )
    }
    Surface(
        onClick = {
            // Ignore the click generated by releasing a long press.
            if (SystemClock.uptimeMillis() - longPressAt > 1_000L) onLaunch()
        },
        onLongClick = { fireLongPress() }, // pointer / mouse path
        modifier = modifier
            .onPreviewKeyEvent { e ->
                val select = e.key == Key.DirectionCenter || e.key == Key.Enter ||
                    e.key == Key.NumPadEnter
                if (!select || e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val now = SystemClock.uptimeMillis()
                // Auto-repeats arrive every 50-100ms; a gap >300ms means a new press.
                if (lastKeyDown == 0L || now - lastKeyDown > 300) {
                    downAt = now
                    lastKeyDown = now
                    return@onPreviewKeyEvent false // let the normal click path work
                }
                lastKeyDown = now
                if (now - longPressAt > 1_000L && now - downAt >= 450) {
                    fireLongPress()
                }
                true // swallow the repeats themselves
            }
            .width(cardWidth),
        // The Surface's style objects are allocated per composition otherwise.
        // Same values, same look — they are simply built once and reused.
        shape = surfaceShape,
        colors = surfaceColors,
        scale = surfaceScale,
        glow = surfaceGlow,
        border = surfaceBorder,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(if (square) 1f else 16f / 9f)
        ) {
            if (square) {
                val icon = app.icon
                when {
                    icon != null -> Image(
                        bitmap = icon,
                        contentDescription = app.label,
                        contentScale = ContentScale.Fit,
                        // Air around the glyph, per style: Graphite floats the
                        // icon in its tile, Bare lets it fill the tile.
                        // Own render layer, like the banner path below, so the
                        // bitmap is not re-sampled every frame.
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                cardWidth * when (style) {
                                    AppCardStyle.Tile -> 0.09f
                                    AppCardStyle.Graphite -> 0.22f
                                    AppCardStyle.Bare -> 0.02f
                                },
                            )
                            .graphicsLayer { },
                    )
                    // No launcher icon (rare): crop the banner to the square
                    // instead of letterboxing it into an unreadable band.
                    app.banner != null -> Image(
                        bitmap = app.banner,
                        contentDescription = app.label,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().graphicsLayer { },
                    )
                    else -> Box(Modifier.fillMaxSize().background(app.tile))
                }
            } else if (app.banner != null) {
                Image(
                    bitmap = app.banner,
                    contentDescription = app.label,
                    contentScale = ContentScale.Crop,
                    // Own render layer: the focus scale animation transforms the
                    // cached layer instead of re-drawing (re-sampling) the bitmap.
                    modifier = Modifier.fillMaxSize().graphicsLayer { },
                )
            } else {
                // Banner-less apps: give the flat tile depth with a subtle sheen
                // (light top, dark bottom) so it sits next to real banners.
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.10f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.14f),
                                )
                            )
                        )
                )
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                            )
                        }
                    }
                    if (showLabel) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (isHidden) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                )
            }
        }
    }
}
