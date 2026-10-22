package com.conreo.couchytv.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.conreo.couchytv.ui.NeutralTile
import com.conreo.couchytv.ui.tileColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.File

/**
 * @Immutable lets Compose SKIP recomposing cards whose entry didn't change —
 * without it every focus move re-rendered (and re-uploaded) every visible
 * bitmap, which was the main source of jank on weak TV GPUs.
 */
@Immutable
data class AppEntry(
    val pkg: String,
    val label: String,
    /** 16:9 leanback banner if the app ships one, pre-rasterized (drawable released) */
    val banner: ImageBitmap?,
    val icon: ImageBitmap?,
    val autoCategory: String,
    /** Tile color precomputed at scan time — never hashed during composition. */
    val tile: Color,
    /** lastUpdateTime of the package — used to reuse entries across rescans. */
    val stamp: Long,
    /** firstInstallTime — tiebreak so a newly installed app sorts last in its section. */
    val firstInstall: Long,
    /**
     * True when [icon] is one of the launcher's own glyphs (see IconOverrides)
     * rather than the package's own launcher icon. Part of the reuse check: an
     * app that gains or loses an override must not be served a stale entry.
     */
    val iconOverride: Boolean = false,
)

object AppRepository {

    /** Launcher icons are rasterized at this size — the single-row dock draws
     *  square tiles that can reach ~150dp, wider than the old 128px. */
    const val ICON_PX = 256

    /** Reserved id for the auto-filled "All apps" section (every launchable app). */
    const val ALL_APPS_ID = "__all__"

    /** Well-known packages -> category id */
    private val KNOWN = mapOf(
        "com.netflix.ninja" to "streaming",
        "com.google.android.youtube.tv" to "streaming",
        "com.amazon.amazonvideo.livingroom" to "streaming",
        "com.disney.disneyplus" to "streaming",
        "com.wbd.stream" to "streaming",
        "com.hbo.hbonow" to "streaming",
        "com.plexapp.android" to "streaming",
        "tv.twitch.android.viewer" to "streaming",
        "org.jellyfin.androidtv" to "streaming",
        "com.teamsmart.videomanager.tv" to "streaming",
        "org.xbmc.kodi" to "streaming",
        "com.spotify.tv.android" to "music",
        "com.google.android.youtube.tvmusic" to "music",
        "com.aspiro.tidal" to "music",
        "com.valvesoftware.steamlink" to "games",
        "com.limelight" to "games",
        "com.nvidia.geforcenow" to "games",
        "com.retroarch" to "games",
    )

    /** Last successful scan — lets a relaunched activity render instantly. */
    @Volatile
    var memoryCache: List<AppEntry>? = null
        private set

    // Scanning decodes + lossless-WebP-compresses every banner; on the plain
    // 64-thread Dispatchers.IO that pins every core on a weak TV and starves the
    // render thread, so the breathing loading logo drops frames. Cap it to leave
    // a core free — still concurrent, just not scorched-earth.
    // ponytail: cores-1 (min 2); revisit only if scan wall-time regresses.
    private val scanDispatcher = Dispatchers.IO.limitedParallelism(
        (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(2)
    )

    suspend fun scan(context: Context): List<AppEntry> = coroutineScope {
        val pm = context.packageManager
        val cacheDir = File(context.filesDir, "iconcache").apply { mkdirs() }

        // Raster banners to the pixel size they actually paint at in big-icon
        // mode, so a 720p box stays lean and a 4K/hi-density UI stays crisp —
        // no fixed size that's wrong on some tier. 270dp = the largest card
        // (ICON_SIZES last). The banner's graphicsLayer caps sampling at the
        // layout size, so focus-zoom needs no extra headroom. Clamp: never
        // below the 320px spec, capped so an extreme density can't blow up RAM.
        val density = context.resources.displayMetrics.density
        val bannerW = (270 * density).toInt().coerceIn(320, 720)
        val bannerH = bannerW * 9 / 16
        val validCacheNames = java.util.Collections.synchronizedSet(HashSet<String>())

        // 1) Fast pass: collect unique launchable activities.
        val candidates = LinkedHashMap<String, android.content.pm.ResolveInfo>()
        val intents = listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
        )
        for (intent in intents) {
            for (ri in pm.queryIntentActivities(intent, 0)) {
                val pkg = ri.activityInfo?.packageName ?: continue
                if (pkg != context.packageName && pkg !in candidates) candidates[pkg] = ri
            }
        }

        // 2) Heavy pass IN PARALLEL: labels + artwork, one coroutine per app.
        //    Entries whose package is unchanged since the previous scan are
        //    REUSED as-is — no disk read, no decode, no new bitmaps, and the
        //    same object identity so Compose skips their cards entirely.
        val previous = memoryCache?.associateBy { it.pkg }
        val entries = candidates.map { (pkg, ri) ->
            async(scanDispatcher) {
                val ai = ri.activityInfo
                val pkgInfo = runCatching { pm.getPackageInfo(pkg, 0) }.getOrNull()
                val stamp = pkgInfo?.lastUpdateTime ?: 0L
                val firstInstall = pkgInfo?.firstInstallTime ?: 0L
                // Size is baked into the name: a density change (or the earlier
                // fixed-size caches) regenerates instead of loading a stale size.
                val bannerName = "$pkg-$stamp-b$bannerW.webp"
                val override = ICON_OVERRIDES[pkg]
                // The override version and the flag are in the name: changing a
                // glyph, or an app joining the table, regenerates exactly that
                // app's bitmap and leaves every other cache alone.
                val iconName = if (override != null) {
                    "$pkg-$stamp-o$ICON_OVERRIDE_VERSION-$ICON_PX.webp"
                } else {
                    "$pkg-$stamp-i$ICON_PX.webp"
                }

                // A banner app used to skip the icon entirely (icon = null).
                // The single-row layout shows square launcher icons, so every app
                // needs one. Entries cached before that change are stale, and so
                // are the old 128px ones — the width check invalidates both.
                previous?.get(pkg)?.takeIf {
                    it.stamp == stamp &&
                        it.iconOverride == (override != null) &&
                        !(it.banner != null && it.icon == null) &&
                        (it.icon == null || it.icon.width == ICON_PX)
                }?.let { cached ->
                    if (cached.banner != null) validCacheNames.add(bannerName)
                    if (cached.icon != null) validCacheNames.add(iconName)
                    return@async cached
                }

                // Show the banner whenever the app ships one (matches the
                // original behaviour — a size threshold wrongly demoted good
                // banners whose loaded bitmap is below 320px on this density).
                val bannerDrawable = runCatching { ai.loadBanner(pm) }.getOrNull()
                val banner = if (bannerDrawable == null) null else
                    cachedBitmap(cacheDir, bannerName, Bitmap.Config.ARGB_8888) {
                        // Full 8-bit, sized to the display (see bannerW above):
                        // crisp for vector/hi-res banners, lean on low-density TVs.
                        runCatching {
                            bannerDrawable.toBitmap(bannerW, bannerH, Bitmap.Config.ARGB_8888)
                        }.getOrNull()
                    }
                // 256px: the single-row dock draws square icons that can reach
                // ~150dp, which at TV density is wider than 128px. Sharp on the
                // fallback tile even on 4K panels.
                val icon = cachedBitmap(cacheDir, iconName, Bitmap.Config.ARGB_8888) {
                    if (override != null) rasterizeGlyph(context, override.glyph, ICON_PX)
                    else runCatching { ri.loadIcon(pm)?.toBitmap(ICON_PX, ICON_PX) }.getOrNull()
                }
                if (banner != null) validCacheNames.add(bannerName)
                if (icon != null) validCacheNames.add(iconName)
                // Upload textures ahead of first draw so the GPU never stalls
                // mid-frame on a fresh bitmap.
                banner?.prepareToDraw()
                icon?.prepareToDraw()

                AppEntry(
                    pkg = pkg,
                    label = runCatching { ri.loadLabel(pm)?.toString() }.getOrNull() ?: pkg,
                    banner = banner?.asImageBitmap(),
                    icon = icon?.asImageBitmap(),
                    autoCategory = autoCategory(ai.applicationInfo),
                    // An override carries its own colour, so the container behind
                    // it is the shared neutral tile: one visual system, not a row
                    // of differently coloured blocks.
                    tile = if (override != null) NeutralTile else tileColor(pkg),
                    stamp = stamp,
                    firstInstall = firstInstall,
                    iconOverride = override != null,
                )
            }
        }.awaitAll()

        // Drop cache entries for uninstalled or updated apps (this also
        // sweeps away the old .png files after the WebP migration).
        cacheDir.listFiles()?.forEach { f ->
            if (f.name !in validCacheNames) runCatching { f.delete() }
        }
        val result = entries.sortedBy { it.label.lowercase() }
        // Same content as before → return the SAME instance, so downstream
        // remember{}/State comparisons skip and nothing recomposes.
        if (result == memoryCache) return@coroutineScope memoryCache!!
        memoryCache = result
        result
    }

    /**
     * Reads a bitmap from [dir]/[name]; on miss, runs [create] and persists it
     * as LOSSLESS WebP — smaller than PNG on disk, faster to decode, and (unlike
     * the old lossy q90) never adds gradient banding to icons/banners.
     * RAM/GPU cost is unchanged either way: it's set by the decoded size.
     */
    private fun cachedBitmap(
        dir: File,
        name: String,
        config: Bitmap.Config,
        create: () -> Bitmap?,
    ): Bitmap? {
        val f = File(dir, name)
        if (f.exists()) {
            val opts = BitmapFactory.Options().apply { inPreferredConfig = config }
            BitmapFactory.decodeFile(f.absolutePath, opts)?.let { return it }
        }
        val bmp = create() ?: return null
        runCatching {
            f.outputStream().use { out ->
                if (Build.VERSION.SDK_INT >= 30) {
                    bmp.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, out)
                } else {
                    @Suppress("DEPRECATION")
                    bmp.compress(Bitmap.CompressFormat.WEBP, 100, out)
                }
            }
        }
        return bmp
    }

    /**
     * Rasterises one of the launcher's own vector glyphs at [size] px.
     *
     * Runs inside [cachedBitmap], i.e. on the scan dispatcher and only on a cache
     * miss — never during composition, never on a key press. The result is a plain
     * ARGB_8888 bitmap like any other icon, so drawing it is the same texture blit
     * the app already does.
     */
    private fun rasterizeGlyph(context: Context, @androidx.annotation.DrawableRes res: Int, size: Int): Bitmap? =
        runCatching {
            val d = context.getDrawable(res) ?: return null
            d.toBitmap(size, size, Bitmap.Config.ARGB_8888)
        }.getOrNull()

    private fun autoCategory(ai: ApplicationInfo?): String {
        ai ?: return "apps"
        KNOWN[ai.packageName]?.let { return it }
        if (Build.VERSION.SDK_INT >= 26) {
            when (ai.category) {
                ApplicationInfo.CATEGORY_VIDEO -> return "streaming"
                ApplicationInfo.CATEGORY_GAME -> return "games"
                ApplicationInfo.CATEGORY_AUDIO -> return "music"
            }
        }
        @Suppress("DEPRECATION")
        if (ai.flags and ApplicationInfo.FLAG_IS_GAME != 0) return "games"
        return "apps"
    }

    /**
     * The section ids a config can hold: every category EXCEPT the auto-filled
     * All-apps section, which is never a manual/fallback target.
     *
     * Hoisted out of [sectionsOf] so a list of rows can compute it once per
     * config instead of rebuilding the set for every row.
     */
    fun validSectionIds(config: LauncherConfig): Set<String> =
        config.categories.map { it.id }.filter { it != ALL_APPS_ID }.toSet()

    /** The section an app with no valid auto-category falls back to. */
    fun fallbackSectionId(config: LauncherConfig): String =
        config.categories.lastOrNull { it.id != ALL_APPS_ID }?.id ?: "apps"

    /**
     * The sections an app effectively belongs to. A stored EMPTY set is
     * respected (user removed the app from every section — it appears
     * nowhere); only apps with no stored entry fall back to auto-category.
     */
    fun sectionsOf(app: AppEntry, config: LauncherConfig): Set<String> =
        sectionsOf(app, config, validSectionIds(config), fallbackSectionId(config))

    /**
     * [sectionsOf] with the config-wide parts — the valid section ids and the
     * fallback section — already computed by the caller. Same result, but a
     * list of rows no longer rebuilds the id set for every row.
     */
    fun sectionsOf(
        app: AppEntry,
        config: LauncherConfig,
        validIds: Set<String>,
        fallbackId: String,
    ): Set<String> {
        // A stored ALL_APPS_ID is not a section membership: that section shows every app
        // regardless. The section editor used to write it when a tile was tapped inside
        // "All apps", which left the app in no section at all ("selected but never appears").
        // Ignoring it here also repairs configs that were already saved that way.
        val user = config.sections[app.pkg]?.takeIf { ALL_APPS_ID !in it }
        if (user != null) return user.filter { it in validIds }.toSet()
        return setOf(if (app.autoCategory in validIds) app.autoCategory else fallbackId)
    }

    /**
     * Groups scanned apps into the configured categories. An app may appear
     * in several sections; explicit ordering and the hidden flag are applied.
     */
    fun categorize(
        apps: List<AppEntry>,
        config: LauncherConfig,
    ): List<Pair<CategoryCfg, List<AppEntry>>> {
        val byCat = HashMap<String, MutableList<AppEntry>>()
        for (app in apps) {
            for (catId in sectionsOf(app, config)) {
                byCat.getOrPut(catId) { mutableListOf() }.add(app)
            }
        }
        return config.categories.map { cat ->
            // The All-apps section is auto-filled with every app, alphabetically,
            // ignoring per-section assignment (the hidden flag still applies).
            if (cat.id == ALL_APPS_ID) {
                val all = if (config.showHidden) apps
                else apps.filter { it.pkg !in config.hidden }
                return@map cat to all.sortedBy { it.label.lowercase() }
            }
            val list = byCat[cat.id].orEmpty()
            val explicit = config.order[cat.id].orEmpty()
            // Pinned apps keep their explicit order; the rest follow install time
            // (stable sort → same-time preloaded apps stay put, a new install lands last).
            val ordered = list.sortedWith(
                compareBy<AppEntry> {
                    val i = explicit.indexOf(it.pkg)
                    if (i >= 0) i else Int.MAX_VALUE
                }.thenBy { it.firstInstall }
            )
            val visible =
                if (config.showHidden) ordered
                else ordered.filter { it.pkg !in config.hidden }
            cat to visible
        }
    }
}
