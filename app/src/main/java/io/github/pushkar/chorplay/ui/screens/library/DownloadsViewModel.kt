package io.github.pushkar.chorplay.ui.screens.library

import android.content.Context
import android.content.IntentSender
import android.os.StatFs
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.pushkar.chorplay.data.local.PlayerPreferences
import io.github.pushkar.chorplay.data.local.dao.DownloadCollectionSummary
import io.github.pushkar.chorplay.data.local.entity.DownloadFileType
import io.github.pushkar.chorplay.data.local.entity.DownloadItemStatus
import io.github.pushkar.chorplay.data.local.entity.DownloadWithItems
import io.github.pushkar.chorplay.data.video.DownloadProgressUpdate
import io.github.pushkar.chorplay.data.video.DownloadRecoveryScanner
import io.github.pushkar.chorplay.data.video.DownloadedVideo
import io.github.pushkar.chorplay.data.video.VideoDownloadManager
import io.github.pushkar.chorplay.data.video.downloader.collection.DownloadedCollections
import io.github.pushkar.chorplay.data.video.downloader.work.DownloadController
import io.github.pushkar.chorplay.data.video.storage.deleteRequestFor
import io.github.pushkar.chorplay.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DownloadsViewModel
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
    private val videoDownloadManager: VideoDownloadManager,
    private val recoveryScanner: DownloadRecoveryScanner,
    private val downloadController: DownloadController,
    private val collections: DownloadedCollections,
    preferences: PlayerPreferences,
    @param:ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DownloadsUiState())
    val uiState: StateFlow<DownloadsUiState> = _uiState.asStateFlow()

    private val pendingDeleteIds = MutableStateFlow<Set<String>>(emptySet())

    private val awaitingUndo = MutableStateFlow<Set<String>>(emptySet())
    private val hiddenIds = combine(pendingDeleteIds, awaitingUndo) { pending, undoable -> pending + undoable }
    private val _deleted = MutableSharedFlow<Int>(extraBufferCapacity = 1)

    val deleted: SharedFlow<Int> = _deleted.asSharedFlow()

    val showStorageCard: StateFlow<Boolean> =
        preferences.downloadsStorageCard.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val confirmDelete: StateFlow<Boolean> =
        preferences.downloadsConfirmDelete.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    private val allVideos = MutableStateFlow<List<DownloadedVideo>>(emptyList())
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(DownloadSort.NEWEST)

    private val refreshTick = MutableStateFlow(0)

    init {
        observeDownloads()
        downloadController.ensureQueueRunning()
        downloadController.scheduleRetagOnce()
        scanIfMoreIsReadable()
    }

    fun setQuery(value: String) {
        query.value = value
        _uiState.update { it.copy(query = value) }
    }

    fun setSort(value: DownloadSort) {
        sort.value = value
        _uiState.update { it.copy(sort = value) }
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            combine(refreshTick.flatMapLatest { videoDownloadManager.downloadedVideos }, hiddenIds) { videos, pending ->
                videos.filter { it.video.id !in pending }
            }.collect { allVideos.value = it }
        }
        viewModelScope.launch {
            combine(allVideos, query, sort) { videos, text, order ->
                videos.filterAndSort(text, order) to videos.size
            }.collect { (videos, count) ->
                _uiState.update {
                    it.copy(
                        downloadedVideos = videos,
                        totalVideoCount = count,
                    )
                }
            }
        }
        viewModelScope.launch {
            combine(allVideos, refreshTick) { videos, _ ->
                videos.sumOf { it.fileSize }
            }.collect { videoBytes ->
                val free = freeBytes()
                _uiState.update { it.copy(storage = DownloadStorage(videoBytes, free)) }
            }
        }
        viewModelScope.launch {
            combine(videoDownloadManager.allDownloads, pendingDeleteIds) { downloads, pending -> downloads to pending }
                .collect { (downloads, pending) ->
                    val present = downloads.mapTo(HashSet()) { it.download.videoId }
                    if (!present.containsAll(pending)) pendingDeleteIds.update { it.intersect(present) }
                    val incomplete =
                        downloads.filter { it.download.videoId !in pending && it.overallStatus != DownloadItemStatus.COMPLETED }
                    _uiState.update { state ->
                        val activeIds = incomplete.mapTo(HashSet()) { it.download.videoId }
                        state.copy(
                            incompleteVideoDownloads = incomplete,
                            mergingVideoIds = state.mergingVideoIds.intersect(activeIds),
                            progress = state.progress.filterKeys { it in activeIds },
                        )
                    }
                }
        }
        viewModelScope.launch {
            collections.summaries.collect { summaries ->
                _uiState.update { it.copy(videoCollections = summaries) }
            }
        }
        viewModelScope.launch {
            videoDownloadManager.progressUpdates.collect { update ->
                _uiState.update { state ->
                    state.copy(
                        progress = state.progress + (update.videoId to update),
                        mergingVideoIds =
                            if (update.isMerging) state.mergingVideoIds + update.videoId else state.mergingVideoIds - update.videoId,
                    )
                }
            }
        }
    }

    private suspend fun freeBytes(): Long =
        withContext(Dispatchers.IO) {
            runCatching {
                val folder =
                    videoDownloadManager.resolveDestination(
                        DownloadFileType.VIDEO,
                        videoDownloadManager.savedLocation(),
                    )
                StatFs(folder.directory.path).availableBytes
            }.getOrDefault(0L)
        }


    fun deleteDownloads(ids: Set<String>) {
        if (ids.isEmpty()) return
        commitDeletes()
        awaitingUndo.value = ids
        _deleted.tryEmit(ids.size)
    }

    fun undoDelete() {
        awaitingUndo.value = emptySet()
    }

    fun commitDeletes() {
        val waiting = awaitingUndo.value
        if (waiting.isEmpty()) return
        pendingDeleteIds.update { it + waiting }
        awaitingUndo.value = emptySet()
        waiting.forEach { deleteVideoDownload(it) }
    }

    fun cancelDownloads(ids: Set<String>) {
        pendingDeleteIds.update { it + ids }
        ids.forEach(::deleteVideoDownload)
    }

    override fun onCleared() {
        commitDeletes()
    }

    /** A finished download is deleted here; one still in the queue is cancelled, which stops it first. */
    private fun deleteVideoDownload(videoId: String) {
        applicationScope.launch {
            val download = videoDownloadManager.getDownloadWithItems(videoId)
            if (download?.overallStatus == DownloadItemStatus.COMPLETED) {
                videoDownloadManager.deleteDownload(videoId)
            } else {
                downloadController.cancel(videoId)
            }
        }
    }

    fun pauseVideoDownload(videoId: String) {
        downloadController.pause(videoId)
    }

    fun resumeVideoDownload(videoId: String) {
        downloadController.resume(videoId)
    }

    fun retryVideoDownload(videoId: String) {
        downloadController.retry(videoId)
    }

    fun removeIncompleteDownloads(audioOnly: Boolean = false) {
        val state = _uiState.value
        val ids = state.incompleteVideoDownloads.map { it.download.videoId }
        if (ids.isEmpty()) return
        pendingDeleteIds.update { it + ids }
        ids.forEach { videoId -> downloadController.cancel(videoId) }
    }

    fun removeCollection(
        id: String,
        deleteFiles: Boolean,
    ) {
        viewModelScope.launch { collections.remove(id, deleteFiles) }
    }

    val deleteConsent: Flow<IntentSender> =
        videoDownloadManager.undeletable.mapNotNull { context.deleteRequestFor(it) }.flowOn(Dispatchers.IO)

    private val _recovered = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val recovered: SharedFlow<Int> = _recovered.asSharedFlow()

    fun rescan() = scan(reportNone = false)

    fun findEarlierDownloads() = scan(reportNone = true)

    fun scanIfMoreIsReadable() {
        if (recoveryScanner.needsScan()) scan(reportNone = false)
    }

    private fun scan(reportNone: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            val count = recoveryScanner.scanAndRecoverDownloads()
            refreshTick.update { it + 1 }
            _uiState.update { it.copy(isScanning = false) }
            if (count > 0 || reportNone) _recovered.tryEmit(count)
        }
    }
}

data class DownloadsUiState(
    val downloadedVideos: List<DownloadedVideo> = emptyList(),
    val totalVideoCount: Int = 0,
    val incompleteVideoDownloads: List<DownloadWithItems> = emptyList(),
    val videoCollections: List<DownloadCollectionSummary> = emptyList(),
    val progress: Map<String, DownloadProgressUpdate> = emptyMap(),
    val mergingVideoIds: Set<String> = emptySet(),
    val storage: DownloadStorage = DownloadStorage(),
    val query: String = "",
    val sort: DownloadSort = DownloadSort.NEWEST,
    val isScanning: Boolean = false,
)
