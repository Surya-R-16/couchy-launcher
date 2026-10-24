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
 * ## The glyph rules (keep every override inside them)
 *
 * 1. OPTICAL BOX — every glyph is drawn inside x/y 4..20 of the 24-unit
 *    viewport, so all marks share the same optical size regardless of shape.
 * 2. WEIGHT — at 100 dp on a 3 m TV, hairlines vanish. Minimum filled-band
 *    thickness is ~2.8 units; a "stroke" is drawn as a filled crescent or a
 *    swept path, never a 1-unit hairline.
 * 3. ACCENT — one brand hue per glyph, only where it carries recognition
 *    (Netflix red, prime blue, Play's three hues). Everything else is white
 *    on the neutral tile. No grey-washed logos: recognition beats uniformity.
 * 4. TILE — every overridden app sits on [com.conreo.couchytv.ui.NeutralTile];
 *    the accent comes from the glyph, not the container.
 * 5. FALLBACK — an app whose mark does not survive reduction keeps its real
 *    icon. A clean glyph that reads as two other things is worse than the
 *    original (the empty-refrigerator rule).
 *
 * To extend: drop a vector drawable in res/drawable and add one row. Bump
 * [ICON_OVERRIDE_VERSION] so the cached WebP for that app is regenerated
 * instead of the stale glyph being shown.
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
    IconOverride("com.amazon.amazonvideo.livingroom", R.drawable.ic_glyph_prime),
    IconOverride("com.android.vending", R.drawable.ic_glyph_store),
)

val ICON_OVERRIDES: Map<String, IconOverride> = OVERRIDES.associateBy { it.pkg }

/**
 * Part of the cache file name. Bump when a glyph or the table changes shape:
 * the old bitmap is then dropped rather than reused, without invalidating the
 * caches of apps that are not overridden.
 */
const val ICON_OVERRIDE_VERSION = 2
