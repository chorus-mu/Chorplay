package io.github.pushkar.chorplay.data.local

data class MediaCacheLimits(
    val videoBytes: Long,
    val artworkBytes: Long?,
) {
    companion object {
        val DEFAULT =
            MediaCacheLimits(
                videoBytes = MediaCacheSizes.mediaBytes(MediaCacheSizes.DEFAULT_MEDIA_MB),
                artworkBytes = MediaCacheSizes.artworkBytes(MediaCacheSizes.ARTWORK_AUTOMATIC_MB),
            )
    }
}

object MediaCacheSizes {
    const val UNLIMITED_MB = 0
    const val ARTWORK_AUTOMATIC_MB = 0
    const val DEFAULT_MEDIA_MB = 500

    val MEDIA_OPTIONS_MB = listOf(100, 200, 500, 1024, 2048, 5120, UNLIMITED_MB)
    val ARTWORK_OPTIONS_MB = listOf(ARTWORK_AUTOMATIC_MB, 100, 200, 500, 1024)

    private const val BYTES_PER_MB = 1024L * 1024L

    fun mediaBytes(megabytes: Int): Long = if (megabytes <= UNLIMITED_MB) 0L else megabytes * BYTES_PER_MB

    fun artworkBytes(megabytes: Int): Long? = if (megabytes <= ARTWORK_AUTOMATIC_MB) null else megabytes * BYTES_PER_MB
}
