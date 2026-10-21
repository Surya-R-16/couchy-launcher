package com.conreo.couchytv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.conreo.couchytv.R
import com.conreo.couchytv.data.SystemStats
import com.conreo.couchytv.data.formatBytes

/**
 * Memory and storage, in the top-left corner of the home screen.
 *
 * ONE compact glass pill, the same height and material as the status cluster
 * on the right, so the two ends of the top bar line up. Text only — a bar
 * looked busy and the pill grew too wide for what it said.
 *
 * The numbers move slowly, so they refresh on resume and once a minute; a
 * per-second timer would recompose the home screen every second for figures
 * that never change.
 */
@Composable
internal fun SystemStatsChip(
    stats: SystemStats,
    modifier: Modifier = Modifier,
) {
    // Same corner radius and material as the status cluster beside it.
    val corner = LocalCornerRadius.current
    Row(
        modifier = modifier
            .glassPanel(RoundedCornerShape(corner))
            .padding(horizontal = CHIP_PAD_H, vertical = CHIP_PAD_V),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // The inner row is forced to the height of a status icon (40dp) so the
        // chip is exactly as tall as the glass cluster on the other side.
        Row(
            modifier = Modifier.height(CHIP_CONTENT_HEIGHT),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatValue(
                label = stringResource(R.string.stat_ram),
                value = "${formatBytes(stats.ramUsedMb.toLong())}/${formatBytes(stats.ramTotalMb.toLong())}",
            )
            Divider()
            StatValue(
                label = stringResource(R.string.stat_storage),
                value = "${formatBytes(stats.storageFreeMb)} free",
            )
        }
    }
}

/** A dim label with the value beside it. */
@Composable
private fun StatValue(label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.55f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** A hairline between the two readouts. */
@Composable
private fun Divider() {
    Box(
        Modifier
            .width(1.dp)
            .height(14.dp)
            .background(Color.White.copy(alpha = 0.18f)),
    )
}
