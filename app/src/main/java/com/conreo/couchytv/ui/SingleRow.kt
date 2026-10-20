package com.conreo.couchytv.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.conreo.couchytv.data.AppEntry

/**
 * The single-row layout: every app in ONE line at the bottom of the screen,
 * icons shrunk to fit — Apple-TV style, no expand gesture, no scrolling.
 *
 * This file holds the sizing maths on its own, free of any Android or Compose
 * runtime state, so it can be unit-tested on the JVM.
 */

/**
 * Which apps the single row shows.
 *
 * [pinned] is the owner's curated list, in dock order. Two fallbacks keep the
 * home screen from ever being empty: no pins at all (a fresh install) and
 * pins that no longer resolve to an installed app (everything uninstalled).
 * The row must never be blank — a launcher with nothing on it is broken.
 */
fun selectRowApps(
    all: List<AppEntry>,
    apps: List<AppEntry>,
    pinned: List<String>,
): List<AppEntry> {
    if (pinned.isEmpty()) return all
    val byPkg = apps.associateBy { it.pkg }
    val chosen = pinned.mapNotNull { byPkg[it] }
    return if (chosen.isEmpty()) all else chosen
}

/** How [fitSingleRow] laid the apps out. */
data class SingleRowFit(
    /** edge length of one square icon */
    val icon: Dp,
    /** gap between two icons */
    val gap: Dp,
    /** how many apps fit — the rest are dropped (never overflow off-screen) */
    val visibleCount: Int,
)

/**
 * Fit [n] icons into [available] width as ONE row.
 *
 * The icon and the gap shrink by the SAME factor, so the row keeps its
 * proportions instead of turning into a picket fence. [maxIcon] is the user's
 * icon-size setting used as a CEILING, [prefGap] their preferred spacing —
 * both keep working, they just cap instead of dictate.
 *
 * EVERY app stays in the row: an app that is not on the home screen is an app
 * the owner cannot launch, so the icons shrink all the way down to [hardMin]
 * before anything is dropped. [minIcon] is the point at which the icons are
 * getting too small to read — the caller reports it, but the row still holds
 * every app.
 */
fun fitSingleRow(
    available: Dp,
    n: Int,
    maxIcon: Dp,
    prefGap: Dp,
    minIcon: Dp = 20.dp,
    hardMin: Dp = 8.dp,
): SingleRowFit {
    if (n <= 0) return SingleRowFit(0.dp, 0.dp, 0)
    // Plain float math — the Dp arithmetic operators are extension functions
    // and this file does not import them.
    val w = available.value
    val s0 = maxIcon.value
    val g0 = prefGap.value
    val total = n * s0 + (n - 1) * g0
    if (total <= w) {
        // Fits at the requested size: spread the leftover as extra gap, but
        // never wider than the preferred gap — the row stays a tight group.
        val leftover = w - n * s0
        val gap = if (n > 1) minOf(g0, leftover / (n - 1)) else 0f
        return SingleRowFit(maxIcon, gap.dp, n)
    }
    val k = w / total
    val icon = s0 * k
    val gap = g0 * k
    if (icon >= hardMin.value) return SingleRowFit(icon.dp, gap.dp, n)
    // Pathological: more apps than one line can hold at a readable size.
    val fit = ((w + hardMin.value) / (hardMin.value + gap)).toInt().coerceAtLeast(1)
    return SingleRowFit(hardMin, gap.dp, fit.coerceAtMost(n))
}
