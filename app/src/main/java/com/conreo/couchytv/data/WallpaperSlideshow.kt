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

        val copied = runCatching {
            val input = context.contentResolver.openInputStream(pick.uri)
                ?: error("cannot read ${pick.name}")
            input.use { i ->
                File(context.filesDir, TARGET).outputStream().use { o -> i.copyTo(o) }
            }
        }.isSuccess
        if (copied) runCatching { lastFile.writeText(pick.name) }
        return copied
    }

    private data class Image(val name: String, val uri: android.net.Uri)

    private fun listImages(context: Context): List<Image> =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> queryMediaStore(context)
            else -> listFromFolder()
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
