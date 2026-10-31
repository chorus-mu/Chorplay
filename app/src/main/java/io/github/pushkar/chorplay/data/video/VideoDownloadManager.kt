package io.github.pushkar.chorplay.data.video

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.pushkar.chorplay.data.local.PlayerPreferences
import io.github.pushkar.chorplay.data.local.dao.DownloadDao
import io.github.pushkar.chorplay.data.local.entity.DownloadEntity
import io.github.pushkar.chorplay.data.local.entity.DownloadFileType
import io.github.pushkar.chorplay.data.local.entity.DownloadItemEntity
import io.github.pushkar.chorplay.data.local.entity.DownloadItemStatus
import io.github.pushkar.chorplay.data.local.entity.DownloadWithItems
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.data.video.downloader.request.DownloadRequest
import io.github.pushkar.chorplay.data.video.downloader.work.DownloadStaging
import io.github.pushkar.chorplay.data.video.storage.DownloadDestination
import io.github.pushkar.chorplay.data.video.storage.DownloadFiles
import io.github.pushkar.chorplay.data.video.storage.DownloadLocation
import io.github.pushkar.chorplay.data.video.storage.resolveDownloadDestination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class DownloadProgressUpdate(
    val videoId: String,
    val itemId: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val status: DownloadItemStatus,
    val isMerging: Boolean = false,
) {
    val progress: Float
        get() = if (totalBytes > 0) downloadedBytes.toFloat() / totalBytes.toFloat() else 0f
}

data class DownloadedVideo(
    val video: Video,
    val filePath: String,
    val downloadedAt: Long = System.currentTimeMillis(),
    val fileSize: Long = 0,
    val downloadId: Long = -1,
    val quality: String = "Unknown",
    val isAudioOnly: Boolean = false,
)

@Singleton
class VideoDownloadManager
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
    private val offlineSubtitleStore: OfflineSubtitleStore,
    private val preferences: PlayerPreferences,
) {
    companion object {
        private const val TAG = "VideoDownloadManager"
        const val VIDEO_DIR = "Chorplay"
        const val AUDIO_DIR = "Chorplay"
        private const val STAGING_DIR = "staging"
        private const val INTERNAL_DIR = "downloads"

        @Deprecated("Use Hilt injection instead", level = DeprecationLevel.ERROR)
        fun getInstance(context: Context): VideoDownloadManager =
            throw UnsupportedOperationException(
                "VideoDownloadManager is now Hilt-managed. Use @Inject instead of getInstance().",
            )
    }

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val recentlyDeletedPaths: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val tombstonePrefs by lazy {
        context.getSharedPreferences("chorplay_file_tombstones", Context.MODE_PRIVATE)
    }

    private val _undeletable = MutableSharedFlow<List<String>>(extraBufferCapacity = 8)

    val undeletable: SharedFlow<List<String>> = _undeletable.asSharedFlow()

    private val _progressUpdates = MutableSharedFlow<DownloadProgressUpdate>(extraBufferCapacity = 64)
    val progressUpdates: SharedFlow<DownloadProgressUpdate> = _progressUpdates.asSharedFlow()

    fun emitProgress(update: DownloadProgressUpdate) {
        _progressUpdates.tryEmit(update)
    }

    fun hasAllFilesAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {

            false
        }

    fun requestAllFilesAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
            try {
                val intent =
                    Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                context.startActivity(intent)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to open MANAGE_ALL_FILES_ACCESS settings, trying fallback", e)
                try {
                    val intent =
                        Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    context.startActivity(intent)
                } catch (e2: Exception) {
                    Log.e(TAG, "Could not open file access settings", e2)
                }
            }
        }
    }

    fun scanFile(
        filePath: String,
        mimeType: String = "video/mp4",
    ) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                Log.e(TAG, "scanFile: File does not exist: $filePath")
                return
            }

            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf(mimeType),
            ) { path, uri ->
                Log.d(TAG, "scanFile: Scanned $path: -> uri=$uri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "scanFile failed", e)
        }
    }

    fun stagingDirectory(): File =
        File(context.getExternalFilesDir(null) ?: context.filesDir, STAGING_DIR).apply { mkdirs() }

    suspend fun savedLocation(isMusic: Boolean = false): DownloadLocation =
        preferences.downloadLocation.first()

    fun resolveDestination(
        fileType: DownloadFileType,
        location: DownloadLocation = DownloadLocation.DEFAULT,
    ): DownloadDestination =
        resolveDownloadDestination(
            chosen = location,
            defaults = defaultDirectories(fileType),
            staging = stagingDirectory(),
            isUsableDirectory = ::isUsableDirectory,
            hasTreeAccess = { DownloadFiles.hasTreeAccess(context, it) },
        )

    private fun defaultDirectories(fileType: DownloadFileType): List<File> =
        buildList {
            if (fileType == DownloadFileType.AUDIO) {
                publicDirectory(Environment.DIRECTORY_MUSIC, AUDIO_DIR)?.let(::add)
                publicDirectory(Environment.DIRECTORY_DOWNLOADS, AUDIO_DIR)?.let(::add)
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.let { add(File(it, AUDIO_DIR)) }
            } else {
                publicDirectory(Environment.DIRECTORY_DOWNLOADS, VIDEO_DIR)?.let(::add)
                if (hasAllFilesAccess()) publicDirectory(Environment.DIRECTORY_MOVIES, VIDEO_DIR)?.let(::add)
                context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.let { add(File(it, VIDEO_DIR)) }
            }
            add(File(context.filesDir, INTERNAL_DIR))
        }

    private fun publicDirectory(
        type: String,
        folder: String,
    ): File? = runCatching { File(Environment.getExternalStoragePublicDirectory(type), folder) }.getOrNull()

    private fun isUsableDirectory(directory: File): Boolean =
        runCatching { (directory.mkdirs() || directory.isDirectory) && directory.canWrite() }.getOrDefault(false)

    val allDownloads: Flow<List<DownloadWithItems>> = downloadDao.getAllDownloadsWithItems()
    val videoDownloads: Flow<List<DownloadWithItems>> = downloadDao.getVideoDownloads()
    val audioOnlyDownloads: Flow<List<DownloadWithItems>> = downloadDao.getAudioOnlyDownloads()
    val activeDownloads: Flow<List<DownloadWithItems>> = downloadDao.getActiveDownloads()
    val downloadedVideos: Flow<List<DownloadedVideo>>
        get() =
            allDownloads
                .map { list ->
                    list
                        .filter { dwi ->
                            dwi.overallStatus == DownloadItemStatus.COMPLETED &&
                                    !dwi.isAudioOnly &&
                                    dwi.primaryFilePath?.let { DownloadFiles.exists(context, it) } == true
                        }.map { toDownloadedVideo(it) }
                }.flowOn(Dispatchers.IO)

    suspend fun saveDownload(
        video: Video,
        items: List<DownloadItemEntity>,
    ) {
        downloadDao.replaceDownload(
            DownloadEntity(
                videoId = video.id,
                title = video.title,
                uploader = video.channelName,
                duration = video.duration.toLong(),
                thumbnailUrl = video.thumbnailUrl,
                createdAt = System.currentTimeMillis(),
            ),
            items,
        )
    }

    suspend fun insertItem(item: DownloadItemEntity): Int = downloadDao.insertItem(item).toInt()
    suspend fun updateProgress(
        itemId: Int,
        downloadedBytes: Long,
        status: DownloadItemStatus,
    ) {
        downloadDao.updateProgress(itemId, downloadedBytes, status)
    }

    suspend fun updateItemFull(
        itemId: Int,
        downloadedBytes: Long,
        totalBytes: Long,
        status: DownloadItemStatus,
    ) {
        downloadDao.updateItemFull(itemId, downloadedBytes, totalBytes, status)
    }

    suspend fun updateStatus(
        itemId: Int,
        status: DownloadItemStatus,
    ) {
        downloadDao.updateStatus(itemId, status)
    }

    suspend fun updateAllItemsStatus(
        videoId: String,
        status: DownloadItemStatus,
    ) {
        downloadDao.updateAllItemsStatus(videoId, status)
    }

    suspend fun isDownloaded(videoId: String): Boolean = downloadDao.isDownloaded(videoId)
    suspend fun saveSponsorBlockData(
        videoId: String,
        json: String,
    ) {
        downloadDao.updateSponsorBlockData(videoId, json)
    }

    suspend fun getSponsorBlockData(videoId: String): String? = downloadDao.getSponsorBlockData(videoId)
    suspend fun getDownloadWithItems(videoId: String): DownloadWithItems? = downloadDao.getDownloadWithItems(videoId)
    suspend fun findLocalCopy(videoId: String): DownloadedVideo? =
        withContext(Dispatchers.IO) {
            downloadDao
                .getDownloadWithItems(videoId)
                ?.takeIf { it.overallStatus == DownloadItemStatus.COMPLETED && !it.isAudioOnly }
                ?.takeIf { download -> download.primaryFilePath?.let { DownloadFiles.exists(context, it) } == true }
                ?.let(::toDownloadedVideo)
        }

    suspend fun localCopyPath(videoId: String): String? = findLocalCopy(videoId)?.filePath
    suspend fun updateItemLocation(
        itemId: Int,
        filePath: String,
        fileName: String,
    ) {
        downloadDao.updateItemLocation(itemId, filePath, fileName)
    }

    suspend fun discardForRetry(videoId: String) =
        withContext(Dispatchers.IO) {
            val download = downloadDao.getDownloadWithItems(videoId) ?: return@withContext
            val filePaths = download.items.flatMap { artifactPathsFor(it.filePath) }.distinct()
            downloadDao.deleteDownload(videoId)
            filePaths.forEach { deleteFileFromDisk(it) }
        }

    suspend fun deleteDownload(videoId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val download =
                    downloadDao.getDownloadWithItems(videoId)
                        ?: return@withContext false

                val subtitleFile = DownloadRequest.decode(download.download.requestJson)?.subtitleFile
                val filePaths =
                    (download.items.flatMap { artifactPathsFor(it.filePath) } + listOfNotNull(subtitleFile)).distinct()
                val thumbPath = download.download.thumbnailPath

                recentlyDeletedPaths.addAll(filePaths)

                downloadDao.deleteDownload(videoId)

                ioScope.launch {
                    val left = mutableListOf<String>()
                    filePaths.forEach { path ->
                        val fileGone = deleteFileFromDisk(path)
                        if (fileGone) {
                            recentlyDeletedPaths.remove(path)
                            tombstonePrefs.edit().remove(path).apply()
                            if (!DownloadFiles.isDocument(path)) MediaScannerConnection.scanFile(
                                context,
                                arrayOf(path),
                                null,
                                null
                            )
                            Log.d(TAG, "Deleted: $path")
                        } else {
                            tombstonePrefs.edit().putBoolean(path, true).apply()
                            Log.w(TAG, "File not deleted (kept in guard + tombstoned): $path")
                            if (!DownloadFiles.isDocument(path)) left += path
                        }
                    }
                    if (left.isNotEmpty()) _undeletable.tryEmit(left)
                    thumbPath?.let { tp ->
                        try {
                            File(tp).takeIf { it.exists() }?.delete()
                        } catch (_: Exception) {
                        }
                    }
                    offlineSubtitleStore.delete(videoId)
                    DownloadStaging(stagingDirectory(), videoId).clear()
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete download: $videoId", e)
                false
            }
        }

    suspend fun deleteIncompleteDownloads(): Int =
        withContext(Dispatchers.IO) {
            val incomplete =
                downloadDao
                    .getAllDownloadsWithItemsOnce()
                    .filter { download ->
                        download.items.isEmpty() ||
                                download.items.any { it.status != DownloadItemStatus.COMPLETED }
                    }

            var deleted = 0
            incomplete.forEach { download ->
                if (deleteDownload(download.download.videoId)) deleted++
            }
            deleted
        }

    private fun artifactPathsFor(path: String): List<String> =
        if (DownloadFiles.isDocument(path)) listOf(path) else listOf(path, "$path.video.tmp", "$path.audio.tmp")

    private fun deleteFileFromDisk(path: String): Boolean {
        if (DownloadFiles.isDocument(path)) return DownloadFiles.deleteDocument(context, path)
        val file = File(path)
        if (!file.exists()) return true
        if (file.delete()) return true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                context.contentResolver
                    .query(
                        contentUri,
                        arrayOf(MediaStore.MediaColumns._ID),
                        "${MediaStore.MediaColumns.DATA} = ?",
                        arrayOf(path),
                        null,
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val id = cursor.getLong(0)
                            val fileUri = ContentUris.withAppendedId(contentUri, id)
                            if (context.contentResolver.delete(fileUri, null, null) > 0) {
                                return !file.exists()
                            }
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "MediaStore delete fallback failed for: $path", e)
            }
        }

        return !file.exists()
    }

    suspend fun removeDownloadedVideo(videoId: String) {
        deleteDownload(videoId)
    }

    suspend fun getTotalDownloadSize(): Long = downloadDao.getTotalDownloadSize()

    fun toDownloadedVideo(dwi: DownloadWithItems): DownloadedVideo =
        DownloadedVideo(
            video =
                Video(
                    id = dwi.download.videoId,
                    title = dwi.download.title,
                    channelName = dwi.download.uploader,
                    channelId = dwi.download.channelId,
                    thumbnailUrl = dwi.download.thumbnailPath?.let { "file://$it" } ?: dwi.download.thumbnailUrl,
                    duration = dwi.download.duration.toInt(),
                    viewCount = dwi.download.viewCount,
                    uploadDate = "",
                    description = dwi.download.description,
                    likeCount = dwi.download.likeCount,
                ),
            filePath = dwi.primaryFilePath ?: "",
            downloadedAt = dwi.download.createdAt,
            fileSize = dwi.totalSize,
            downloadId = dwi.download.createdAt,
            quality = dwi.items.firstOrNull()?.quality ?: "Unknown",
            isAudioOnly = dwi.isAudioOnly,
        )

    fun generateFileName(
        title: String,
        quality: String,
        extension: String = "mp4",
    ): String {
        val safeTitle =
            title
                .replace(Regex("[^\\p{L}\\p{M}\\p{N}\\s._-]"), "_")
                .replace(Regex("\\s+"), " ")
                .replace(Regex("_+"), "_")
                .trim('_', ' ')
                .take(100)
                .ifEmpty { "video" }
        return "${safeTitle}_$quality.$extension"
    }

    internal fun isBeingDeleted(path: String): Boolean = path in recentlyDeletedPaths
    internal fun retryTombstonedDelete(path: String): Boolean {
        if (!tombstonePrefs.contains(path)) return false
        if (deleteFileFromDisk(path)) tombstonePrefs.edit().remove(path).apply()
        return true
    }
}
