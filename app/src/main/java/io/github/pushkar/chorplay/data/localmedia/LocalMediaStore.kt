package io.github.pushkar.chorplay.data.localmedia

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

private const val ROTATED_90 = 90
private const val ROTATED_270 = 270
private const val MILLIS_PER_SECOND = 1_000L

/**
 * Reads the device's videos from MediaStore. Every file is returned; what the library
 * hides (voice notes, short clips, hidden folders) is decided later from the viewer's settings.
 */
internal class LocalMediaStore(
    private val resolver: ContentResolver,
) {
    fun videos(): List<LocalMediaItem> {
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val columns =
            buildList {
                add(MediaStore.Video.Media._ID)
                add(MediaStore.Video.Media.TITLE)
                add(MediaStore.Video.Media.DISPLAY_NAME)
                add(MediaStore.Video.Media.DURATION)
                add(MediaStore.Video.Media.SIZE)
                add(MediaStore.Video.Media.DATE_ADDED)
                add(MediaStore.Video.Media.DATE_MODIFIED)
                add(MediaStore.Video.Media.WIDTH)
                add(MediaStore.Video.Media.HEIGHT)
                add(MediaStore.Video.Media.MIME_TYPE)
                add(MediaStore.Video.Media.BUCKET_ID)
                add(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                add(MediaStore.Video.Media.ARTIST)
                addAll(pathColumns())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.Video.Media.ORIENTATION)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) add(MediaStore.MediaColumns.AUTHOR)
            }
        return query(collection, columns, selection = null) { cursor ->
            val id = cursor.long(MediaStore.Video.Media._ID)
            val size = cursor.long(MediaStore.Video.Media.SIZE)
            if (size <= 0L) return@query null
            val rotated = cursor.intOrNull(MediaStore.Video.Media.ORIENTATION).let { it == ROTATED_90 || it == ROTATED_270 }
            val width = cursor.int(MediaStore.Video.Media.WIDTH)
            val height = cursor.int(MediaStore.Video.Media.HEIGHT)
            val fileName = cursor.string(MediaStore.Video.Media.DISPLAY_NAME).orEmpty()
            val title = cursor.title(MediaStore.Video.Media.TITLE, MediaStore.Video.Media.DISPLAY_NAME) ?: return@query null
            LocalMediaItem(
                id = id,
                isVideo = true,
                contentUri = ContentUris.withAppendedId(collection, id).toString(),
                title = repairedTitle(title, fileName),
                fileName = fileName,
                durationMs = cursor.long(MediaStore.Video.Media.DURATION),
                sizeBytes = size,
                dateAddedMs = cursor.long(MediaStore.Video.Media.DATE_ADDED) * MILLIS_PER_SECOND,
                modifiedMs = cursor.long(MediaStore.Video.Media.DATE_MODIFIED) * MILLIS_PER_SECOND,
                width = if (rotated) height else width,
                height = if (rotated) width else height,
                mimeType = cursor.string(MediaStore.Video.Media.MIME_TYPE).orEmpty(),
                folderId = cursor.string(MediaStore.Video.Media.BUCKET_ID).orEmpty(),
                folderName = cursor.string(MediaStore.Video.Media.BUCKET_DISPLAY_NAME).orEmpty(),
                path = cursor.folderPath(),
                artist = (cursor.tag(MediaStore.Video.Media.ARTIST) ?: cursor.tag(MediaStore.MediaColumns.AUTHOR)).orEmpty(),
            )
        }
    }

    /** Where MediaStore says each listed video is, as absolute paths. */
    @Suppress("DEPRECATION")
    fun filePaths(): List<String> {
        val columns = listOf(MediaStore.MediaColumns.DATA)
        val read: (Cursor) -> String? = { cursor -> cursor.string(MediaStore.MediaColumns.DATA)?.takeIf(String::isNotBlank) }
        return query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, columns, selection = null, read)
    }

    private fun pathColumns(): List<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(MediaStore.MediaColumns.RELATIVE_PATH)
        } else {
            @Suppress("DEPRECATION")
            listOf(MediaStore.MediaColumns.DATA)
        }

    private fun <T> query(
        collection: Uri,
        columns: List<String>,
        selection: String?,
        read: (Cursor) -> T?,
    ): List<T> =
        resolver
            .query(collection, columns.toTypedArray(), selection, null, "${MediaStore.MediaColumns.DATE_ADDED} DESC")
            ?.use { cursor -> generateSequence { if (cursor.moveToNext()) cursor else null }.mapNotNull(read).toList() }
            .orEmpty()
}

private fun Cursor.long(column: String): Long = getColumnIndex(column).takeIf { it >= 0 }?.let(::getLong) ?: 0L

private fun Cursor.int(column: String): Int = getColumnIndex(column).takeIf { it >= 0 }?.let(::getInt) ?: 0

private fun Cursor.intOrNull(column: String): Int? = getColumnIndex(column).takeIf { it >= 0 && !isNull(it) }?.let(::getInt)

private fun Cursor.string(column: String): String? = stringOrNull(column)

private fun Cursor.stringOrNull(column: String): String? = getColumnIndex(column).takeIf { it >= 0 }?.let(::getString)

/** An embedded tag, or null when the file has none and MediaStore stored its placeholder. */
private fun Cursor.tag(column: String): String? = string(column)?.trim()?.takeUnless { it.isEmpty() || it == MediaStore.UNKNOWN_STRING }

private fun Cursor.title(
    titleColumn: String,
    nameColumn: String,
): String? = string(titleColumn)?.takeIf(String::isNotBlank) ?: fileNameTitle(string(nameColumn))

@Suppress("DEPRECATION")
private fun Cursor.folderPath(): String =
    stringOrNull(MediaStore.MediaColumns.RELATIVE_PATH)
        ?: stringOrNull(MediaStore.MediaColumns.DATA)?.substringBeforeLast('/')?.plus('/')
        ?: ""
