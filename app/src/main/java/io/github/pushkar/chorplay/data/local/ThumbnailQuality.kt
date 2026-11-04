package io.github.pushkar.chorplay.data.local

enum class ThumbnailQuality {
    HIGH,
    MEDIUM,
    LOW,
    OFF,
    ;

    companion object {
        fun fromName(name: String?): ThumbnailQuality = entries.firstOrNull { it.name == name } ?: HIGH

        fun effective(
            isWifi: Boolean,
            wifi: ThumbnailQuality,
            cellular: ThumbnailQuality,
        ): ThumbnailQuality = if (isWifi) wifi else cellular
    }
}
