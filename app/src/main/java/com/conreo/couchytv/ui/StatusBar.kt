package com.conreo.couchytv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.conreo.couchytv.R
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.conreo.couchytv.data.NetStatus
import com.conreo.couchytv.data.SystemStats

@Composable
fun StatusBar(
    net: NetStatus,
    time: String,
    date: String,
    showVpn: Boolean,
    glass: Boolean,
    onVpnClick: () -> Unit,
    onNetworkClick: () -> Unit,
    onSettingsClick: () -> Unit,
    stats: SystemStats?,
) {
    // Optional glass panel behind the icon cluster — same look as the dock
    // panel, corners follow the roundness setting.
    // A plain rounded rectangle, not the squircle: the pill is a wide
    // container, and a generic path-mask clip re-rasterises the path every
    // frame where the round-rect clip is a cheap corner mask.
    val corner = LocalCornerRadius.current
    val cluster = remember(glass, corner) {
        if (glass) {
            Modifier
                .glassPanel(RoundedCornerShape(corner))
                .padding(horizontal = CHIP_PAD_H, vertical = CHIP_PAD_V)
        } else Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = STATUS_BAR_MARGIN_H, vertical = STATUS_BAR_PAD_V),
        // The readouts sit on the left, the icon cluster on the right — both
        // centred on the same line, so the two ends of the bar line up.
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
      stats?.let {
          SystemStatsChip(
              stats = it,
              modifier = Modifier.padding(end = 12.dp),
          )
      }
      Row(
        // weight(fill = false): the cluster yields width to the readout chip
        // instead of being squeezed. Without it, a chip + date + clock that
        // together exceed the bar squeezes this row and the CLOCK WRAPS onto
        // two lines ("10:" / "57") — it did exactly that.
        modifier = cluster.weight(1f, fill = false),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        // Order: VPN · network · settings · date · clock (clock far right)
        if (showVpn) {
            StatusIcon(
                icon = AppIcons.Vpn,
                active = net.vpn,
                contentDescription = stringResource(R.string.cd_vpn),
                onClick = onVpnClick,
            )
        }
        StatusIcon(
            icon = when {
                net.ethernet -> AppIcons.Ethernet
                net.wifi -> AppIcons.Wifi
                else -> AppIcons.WifiOff
            },
            active = net.connected,
            contentDescription = stringResource(R.string.cd_network),
            onClick = onNetworkClick,
        )
        StatusIcon(
            icon = AppIcons.Gear,
            active = true,
            contentDescription = stringResource(R.string.cd_settings),
            onClick = onSettingsClick,
        )
        if (date.isNotEmpty()) {
            Text(
                text = date,
                style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                color = Color.White.copy(alpha = 0.75f),
                // The date is the first thing to give up room: one line,
                // tail ellipsis, and it absorbs the squeeze. Without the
                // weight the CLOCK gets clipped or wrapped instead.
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .weight(1f, fill = false),
            )
        }
        Text(
            // Tabular figures: digits keep a fixed width so the clock never jitters.
            text = time,
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            color = Color.White,
            // The clock is the one thing that must never wrap or truncate.
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(start = 10.dp),
        )
      }
    }
}

@Composable
private fun StatusIcon(
    icon: ImageVector,
    active: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    // The Surface's style objects are allocated per composition otherwise.
    // Same values, same look — they are simply built once and reused.
    val surfaceShape = rememberComputed(MaterialTheme.shapes) {
        ClickableSurfaceDefaults.shape(CircleShape)
    }
    val surfaceColors = rememberComputed(MaterialTheme.colorScheme) {
        ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            // The focused state is a filled pill: instant (a state change, not an
            // animation), legible at 3 m, and it does not move the glyph. Bumped
            // from 0.22f to 0.35f when the scale went away — the fill is now the
            // whole focus signal, so it has to carry it alone.
            focusedContainerColor = Color.White.copy(alpha = 0.35f),
            contentColor = Color.White,
            focusedContentColor = Color.White,
        )
    }
    // No growth on focus. tv-material's default scale tween runs ~300 ms — 13
    // frames per key press on this TV — for an animation over a region that is
    // 40 dp wide. The fill above carries the focused state instead, the same
    // bargain as the app cards (static ring, no scale).
    val surfaceScale = remember { ClickableSurfaceDefaults.scale(focusedScale = 1f) }
    Surface(
        onClick = onClick,
        shape = surfaceShape,
        colors = surfaceColors,
        scale = surfaceScale,
    ) {
        Box(Modifier.size(CHIP_CONTENT_HEIGHT), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White.copy(alpha = if (active) 0.95f else 0.35f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
