package com.conreo.couchytv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.conreo.couchytv.R
import com.conreo.couchytv.data.SystemStats
import com.conreo.couchytv.data.formatBytes

/**
 * Memory and storage, in the top-left of the home screen.
 *
 * Two small glass pills, in the same language as the status bar so the top of
 * the screen reads as one thing. Both numbers move slowly, so they refresh on
 * resume and once a minute — never on a timer that would recompose the screen
 * every second.
 */
@Composable
internal fun SystemStatsRow(
    stats: SystemStats,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StatPill(
            label = stringResource(R.string.stat_ram),
            used = formatBytes(stats.ramUsedMb.toLong()),
            total = formatBytes(stats.ramTotalMb.toLong()),
            fraction = stats.ramFraction,
        )
        StatPill(
            label = stringResource(R.string.stat_storage),
            used = formatBytes(stats.storageTotalMb - stats.storageFreeMb),
            total = formatBytes(stats.storageTotalMb),
            fraction = stats.storageFraction,
        )
    }
}

/**
 * One readout: a label, a thin usage bar and the numbers.
 *
 * The bar is what reads from across the room; the numbers are there for
 * anyone who walks up to the TV.
 */
@Composable
private fun StatPill(
    label: String,
    used: String,
    total: String,
    fraction: Float,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xB3121418))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
        UsageBar(fraction)
        Text(
            text = "$used / $total",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
        )
    }
}

/** A thin bar: filled portion over a faint track. */
@Composable
private fun UsageBar(fraction: Float) {
    val fill = fraction.coerceIn(0f, 1f)
    Box(
        Modifier
            .width(90.dp)
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.White.copy(alpha = 0.16f)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(90.dp * fill)
                .clip(RoundedCornerShape(3.dp))
                .background(Color.White.copy(alpha = 0.85f)),
        )
    }
}
