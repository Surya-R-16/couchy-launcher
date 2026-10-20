package com.conreo.couchytv.data

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs

/**
 * A snapshot of the device's memory and storage use.
 *
 * Both readings are instant syscalls — no permissions, no polling cost worth
 * mentioning — so they can be shown on the home screen without a background
 * job.
 */
data class SystemStats(
    val ramUsedMb: Int,
    val ramTotalMb: Int,
    val storageFreeMb: Long,
    val storageTotalMb: Long,
) {
    /** 0..1 — how full the RAM is. */
    val ramFraction: Float
        get() = if (ramTotalMb > 0) ramUsedMb.toFloat() / ramTotalMb else 0f

    /** 0..1 — how full the storage is. */
    val storageFraction: Float
        get() = if (storageTotalMb > 0) (storageTotalMb - storageFreeMb).toFloat() / storageTotalMb else 0f
}

/** Read the current memory and storage use. Cheap enough for the main thread. */
fun readSystemStats(context: Context): SystemStats {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val mem = ActivityManager.MemoryInfo()
    am?.getMemoryInfo(mem)
    val totalRam = mem.totalMem / (1024L * 1024L)
    val usedRam = ((mem.totalMem - mem.availMem) / (1024L * 1024L)).coerceAtLeast(0L)

    val stat = StatFs(Environment.getDataDirectory().path)
    val totalStore = stat.totalBytes / (1024L * 1024L)
    val freeStore = stat.availableBytes / (1024L * 1024L)

    return SystemStats(
        ramUsedMb = usedRam.toInt(),
        ramTotalMb = totalRam.toInt(),
        storageFreeMb = freeStore,
        storageTotalMb = totalStore,
    )
}

/**
 * "1.7G" / "853M" — compact units, for a readout that has to stay narrow.
 * Enough precision to be useful, short enough to read across a room.
 */
fun formatBytes(mb: Long): String = when {
    mb >= 1024L -> {
        val gb = mb / 1024.0
        // One decimal below 10 GB, none above — "1.7G", "12G".
        if (gb >= 10.0) "${gb.toInt()}G" else String.format("%.1fG", gb)
    }
    else -> "${mb}M"
}
