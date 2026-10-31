package io.github.pushkar.chorplay.ui.screens.settings.downloads

import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pushkar.chorplay.data.local.MediaCacheSizes
import io.github.pushkar.chorplay.data.local.PlayerPreferences
import io.github.pushkar.chorplay.data.repository.MediaCacheRepository
import io.github.pushkar.chorplay.data.repository.MediaCacheType
import io.github.pushkar.chorplay.data.repository.MediaCacheUsage
import io.github.pushkar.chorplay.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DownloadCacheViewModel
@Inject
constructor(
    private val preferences: PlayerPreferences,
    private val repository: MediaCacheRepository,
) : SettingsViewModel() {
    val videoLimitMb = preferences.mediaCacheSizeMb.asState(MediaCacheSizes.DEFAULT_MEDIA_MB)
    val artworkLimitMb = preferences.artworkCacheSizeMb.asState(MediaCacheSizes.ARTWORK_AUTOMATIC_MB)

    private val measured = MutableStateFlow<MediaCacheUsage?>(null)

    val usage: StateFlow<MediaCacheUsage?> = measured.asStateFlow()

    fun measure() = write { measured.value = repository.usage() }
    fun clear(type: MediaCacheType) =
        write {
            repository.clear(type)
            measured.value = repository.usage()
        }

    fun setLimit(
        type: MediaCacheType,
        megabytes: Int,
    ) = write {
        when (type) {
            MediaCacheType.VIDEOS -> preferences.setMediaCacheSizeMb(megabytes)
            MediaCacheType.ARTWORK -> preferences.setArtworkCacheSizeMb(megabytes)
            MediaCacheType.OTHER -> Unit
        }
    }
}
