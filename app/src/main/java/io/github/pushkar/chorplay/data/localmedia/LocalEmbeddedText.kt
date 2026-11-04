package io.github.pushkar.chorplay.data.localmedia

import kotlinx.serialization.Serializable

@Serializable
data class EmbeddedText(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
) {
    val isEmpty: Boolean get() = title == null && artist == null && album == null
}

internal fun LocalMediaItem.withEmbeddedText(
    text: EmbeddedText,
    titleless: Boolean = false,
): LocalMediaItem {
    val untagged = if (titleless) fileTitle ?: title else title
    val updated = copy(title = text.title ?: untagged, artist = text.artist ?: artist, album = text.album ?: album)
    return if (updated == this) this else updated
}

internal val LocalMediaItem.fileTitle: String? get() = fileNameTitle(fileName)

internal fun fileNameTitle(fileName: String?): String? = fileName?.substringBeforeLast('.')?.takeIf(String::isNotBlank)

internal fun LocalMediaItem.looksDamaged(): Boolean = listOf(title, artist, album).any { field -> field.any { it == '?' || it == '�' } }

internal val LocalMediaItem.fileStamp: String get() = "$modifiedMs:$sizeBytes"

internal fun embeddedField(value: CharSequence?): String? =
    value
        ?.toString()
        ?.trimEnd('\u0000')
        ?.trim()
        ?.ifEmpty { null }
