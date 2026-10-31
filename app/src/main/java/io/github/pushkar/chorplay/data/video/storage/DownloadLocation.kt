package io.github.pushkar.chorplay.data.video.storage

import java.io.File

data class DownloadLocation(
    val path: String? = null,
    val treeUri: String? = null,
) {
    val isSet: Boolean get() = !path.isNullOrBlank() || !treeUri.isNullOrBlank()

    companion object {
        val DEFAULT = DownloadLocation()
    }
}

data class DownloadDestination(
    val directory: File,
    val exportTreeUri: String? = null,
    val fellBack: Boolean = false,
)

fun resolveDownloadDestination(
    chosen: DownloadLocation,
    defaults: List<File>,
    staging: File,
    isUsableDirectory: (File) -> Boolean,
    hasTreeAccess: (String) -> Boolean,
): DownloadDestination {
    chosen.path
        ?.takeIf { it.isNotBlank() }
        ?.let(::File)
        ?.takeIf(isUsableDirectory)
        ?.let { return DownloadDestination(it) }
    chosen.treeUri
        ?.takeIf { it.isNotBlank() && hasTreeAccess(it) }
        ?.let { return DownloadDestination(staging, exportTreeUri = it) }
    val fallback = defaults.firstOrNull(isUsableDirectory) ?: defaults.last()
    return DownloadDestination(fallback, fellBack = chosen.isSet)
}

fun documentIdToPath(
    documentId: String,
    primaryRoot: String,
): String? {
    if (documentId.startsWith(RAW_PREFIX)) return documentId.removePrefix(RAW_PREFIX).takeIf { it.startsWith("/") }
    val separator = documentId.indexOf(':')
    if (separator <= 0) return null
    val volume = documentId.substring(0, separator)
    val relative = documentId.substring(separator + 1).trim('/')
    val root =
        when {
            volume.equals(PRIMARY_VOLUME, ignoreCase = true) -> primaryRoot.trimEnd('/')
            VolumeUuid.matches(volume) -> "/storage/$volume"
            else -> return null
        }
    return if (relative.isEmpty()) root else "$root/$relative"
}

private const val RAW_PREFIX = "raw:"
private const val PRIMARY_VOLUME = "primary"
private val VolumeUuid = Regex("[0-9A-Fa-f]+-[0-9A-Fa-f]+")
