package com.conreo.couchytv.data

import androidx.annotation.DrawableRes
import com.conreo.couchytv.R

/**
 * Local icon overrides: a deterministic, hand-written table of package -> glyph.
 *
 * Why a table and not image processing: recolouring a 256px launcher icon at
 * runtime is both fragile (some marks depend on their own colours to be
 * recognisable — a play triangle turned grey stops reading as "play") and
 * expensive (it would sit on the navigation path). Instead each app in this
 * table gets a purpose-drawn vector glyph, rasterised ONCE at scan time and
 * cached like every other icon. Nothing here touches the installed APKs, the
 * original icons stay as the fallback, and no network or new dependency is
 * involved.
 *
 * Deliberately NOT overridden: Prime Video. Its mark is a smile, and a smile
 * simplified to a 100 dp tile reads as a "u with a tick" at 3 m — so that app
 * keeps its own launcher icon. Fallback is the honest answer when a brand does
 * not survive reduction; it also demonstrates the fallback path in the dock.
 *
 * To extend: drop a vector drawable in res/drawable and add one row. Bump
 * [ICON_OVERRIDE_VERSION] so the cached WebP for that app is regenerated
 * instead of the stale glyph being shown.
 *
 * [neutralTile] is the container every override is drawn on; it replaces the
 * per-package colour so the row reads as one system.
 */
data class IconOverride(
    val pkg: String,
    @DrawableRes val glyph: Int,
)

/** Package -> glyph. Deterministic: same table, same icons, always. */
private val OVERRIDES = listOf(
    IconOverride("com.netflix.ninja", R.drawable.ic_glyph_netflix),
    IconOverride("org.smarttube.stable", R.drawable.ic_glyph_play),
    IconOverride("in.startv.hotstar", R.drawable.ic_glyph_star),
    IconOverride("com.heytap.tv.filebrowser", R.drawable.ic_glyph_folder),
    IconOverride("com.android.vending", R.drawable.ic_glyph_store),
)

val ICON_OVERRIDES: Map<String, IconOverride> = OVERRIDES.associateBy { it.pkg }

/**
 * Part of the cache file name. Bump when a glyph or the table changes shape:
 * the old bitmap is then dropped rather than reused, without invalidating the
 * caches of apps that are not overridden.
 */
const val ICON_OVERRIDE_VERSION = 1
