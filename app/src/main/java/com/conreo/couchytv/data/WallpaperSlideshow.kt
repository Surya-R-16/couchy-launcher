package com.conreo.couchytv.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Wallpaper slideshow: picks a random image from `Pictures/Slideshow` on shared storage and
 * copies it over the launcher's own `files/wallpaper.jpg`, which is what the home screen draws.
 *
 * Copying (rather than pointing at the picked URI) keeps the launcher independent of URI
 * grants, which the vendor galleries hand out inconsistently — the same reason the single
 * photo path copies too. Requires READ_EXTERNAL_STORAGE on API 23..32.
 */
object WallpaperSlideshow {

    /** Folder under the public Pictures directory. */
    const val FOLDER = "Slideshow"

    private const val TARGET = "wallpaper.jpg"
    private const val LAST = "slideshow-last.txt"
    private const val STAMP = "slideshow-changed.at"

    /**
     * Replaces the current wallpaper with a random image from the folder.
     *
     * @return false when the folder is missing, empty or unreadable (nothing is changed then).
     */
    fun applyNext(context: Context): Boolean {
        val images = listImages(context)
        if (images.isEmpty()) return false

        val lastFile = File(context.filesDir, LAST)
        val last = runCatching { lastFile.readText().trim() }.getOrDefault("")
        // Avoid showing the same picture twice in a row when there is a choice.
        val pool = images.filter { it.name != last }.ifEmpty { images }
        val pick = pool.random()

        val stamp = File(context.filesDir, STAMP)
        val copied = runCatching {
            val input = context.contentResolver.openInputStream(pick.uri)
                ?: error("cannot read ${pick.name}")
            writeWallpaper(context, input)
        }.getOrDefault(false)
        if (copied) {
            runCatching { lastFile.writeText(pick.name) }
            runCatching { stamp.writeText(System.currentTimeMillis().toString()) }
        }
        return copied
    }

    /**
     * Copies [input] over files/wallpaper.jpg atomically.
     *
     * Writing the target in place let the launcher decode a half-written file whenever a
     * slideshow change landed while the wallpaper was being read — the home screen showed
     * torn bands of the JPEG. The bytes go to a temporary file first and are renamed over
     * the target, which the filesystem does in one step, so a reader sees either the old
     * image or the new one.
     */
    fun writeWallpaper(context: Context, input: java.io.InputStream): Boolean = runCatching {
        val target = File(context.filesDir, TARGET)
        val tmp = File(context.filesDir, "$TARGET.tmp")
        tmp.outputStream().use { out -> input.use { it.copyTo(out) } }
        if (!tmp.renameTo(target)) {
            target.delete()
            if (!tmp.renameTo(target)) error("rename failed")
        }
        true
    }.getOrDefault(false)

    /** When the wallpaper last changed, epoch millis; 0 when it never has. */
    fun lastChangeMs(context: Context): Long =
        runCatching {
            File(context.filesDir, STAMP).readText().trim().toLong()
        }.getOrDefault(0L)

    private data class Image(val name: String, val uri: android.net.Uri)

    private fun listImages(context: Context): List<Image> {
        val indexed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            queryMediaStore(context)
        } else {
            emptyList()
        }
        // Files copied in by ADB, a USB stick or a file manager are not in the index until
        // the media scanner runs, so an empty result is not the same as an empty folder.
        return indexed.ifEmpty { listFromFolder() }
    }

    /** API 29+: the folder is addressable through RELATIVE_PATH, no file paths involved. */
    private fun queryMediaStore(context: Context): List<Image> {
        val projection = arrayOf(MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME)
        val selection = "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        val args = arrayOf("%Pictures/$FOLDER%")
        val out = mutableListOf<Image>()
        val cursor: Cursor = runCatching {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                args,
                null,
            )
        }.getOrNull() ?: return emptyList()

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameCol = it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                out += Image(
                    name = it.getString(nameCol) ?: id.toString(),
                    uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id,
                    ),
                )
            }
        }
        return out
    }

    /** API 21..28: plain file listing. */
    @Suppress("DEPRECATION")
    private fun listFromFolder(): List<Image> {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            FOLDER,
        )
        val files = dir.listFiles { f ->
            f.isFile && f.name.substringAfterLast('.', "").lowercase() in
                setOf("jpg", "jpeg", "png", "webp")
        } ?: return emptyList()
        return files.map { Image(it.name, android.net.Uri.fromFile(it)) }
    }
}
