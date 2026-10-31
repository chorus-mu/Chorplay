package io.github.pushkar.chorplay.data.video.storage

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.pushkar.chorplay.data.local.dao.DownloadCollectionDao
import io.github.pushkar.chorplay.data.local.dao.DownloadDao
import io.github.pushkar.chorplay.data.local.entity.DownloadCollectionEntity
import io.github.pushkar.chorplay.data.local.entity.DownloadFileType
import io.github.pushkar.chorplay.data.video.VideoDownloadManager
import io.github.pushkar.chorplay.data.video.downloader.request.DownloadRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

data class PlacedFile(
    val path: String,
    val fileName: String,
    val fellBackTo: String? = null,
)

@Singleton
class DownloadPlacement
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val downloads: VideoDownloadManager,
    private val downloadDao: DownloadDao,
    private val collectionDao: DownloadCollectionDao,
) {

    private val naming = Mutex()

    suspend fun place(
        staged: File,
        request: DownloadRequest,
        extension: String,
    ): PlacedFile? =
        naming.withLock {
            val fileType = if (request.wantsAudioOnly) DownloadFileType.AUDIO else DownloadFileType.VIDEO
            val destination = downloads.resolveDestination(fileType, downloads.savedLocation())
            val collection = request.collectionId?.let { collectionDao.getCollection(it) }
            val name =
                DownloadNaming.fileName(
                    title = request.tags.title,
                    fallback = request.videoId,
                    extension = extension,
                )
            val tree = destination.exportTreeUri
            val placed =
                if (tree != null) {
                    placeInTree(staged, tree, collection, name)
                } else {
                    placeInDirectory(staged, destination.directory, collection, name)
                }
            if (placed !=
                null
            ) {
                return@withLock placed.copy(fellBackTo = destination.directory.absolutePath.takeIf { destination.fellBack })
            }

            val fallback = downloads.resolveDestination(fileType).directory
            placeInDirectory(staged, fallback, collection = null, name = name)?.copy(fellBackTo = fallback.absolutePath)
        }

    private suspend fun placeInDirectory(
        staged: File,
        base: File,
        collection: DownloadCollectionEntity?,
        name: String,
    ): PlacedFile? {
        val directory =
            collection?.let {
                File(
                    base,
                    folderFor(it) { candidate ->
                        File(base, candidate).let { f ->
                            f.exists() && !f.isDirectory
                        }
                    },
                )
            }
                ?: base
        if (!directory.isDirectory && !directory.mkdirs()) return null
        collection?.let { collectionDao.setFolder(it.id, directory.name, directory.absolutePath) }
        val unique = DownloadNaming.unique(name) { candidate -> isTaken(File(directory, candidate).absolutePath) }
        val moved = DownloadFiles.moveInto(staged, directory, unique) ?: return null
        return PlacedFile(moved.absolutePath, moved.name)
    }

    private suspend fun placeInTree(
        staged: File,
        tree: String,
        collection: DownloadCollectionEntity?,
        name: String,
    ): PlacedFile? {
        val parent =
            collection?.let { collectionEntity ->
                val folder = collectionEntity.folderLocation?.takeIf { DownloadFiles.exists(context, it) }
                folder ?: DownloadFiles
                    .ensureTreeDirectory(context, tree, folderFor(collectionEntity) { false })
                    ?.also { collectionDao.setFolder(collectionEntity.id, collectionEntity.folderName, it) }
                ?: return null
            }
        val unique = DownloadNaming.unique(name) { DownloadFiles.treeHasChild(context, tree, parent, it) }
        val document = DownloadFiles.exportToTree(context, staged, tree, unique, parent) ?: return null
        staged.delete()
        return PlacedFile(document, DownloadFiles.displayName(context, document) ?: unique)
    }

    private suspend fun folderFor(
        collection: DownloadCollectionEntity,
        isOccupied: (String) -> Boolean,
    ): String {
        if (collection.folderLocation != null) return collection.folderName
        val others = collectionDao.folderNames().toSet() - collection.folderName
        return DownloadNaming.unique(collection.folderName, hasExtension = false) { it in others || isOccupied(it) }
    }

    suspend fun placeBeside(
        video: PlacedFile,
        request: DownloadRequest,
        name: String,
        text: String,
    ): String? =
        naming.withLock {
            val staged = File(context.cacheDir, name)
            try {
                staged.writeText(text)
                if (!DownloadFiles.isDocument(video.path)) {
                    val folder = File(video.path).parentFile ?: return@withLock null
                    File(folder, name).takeIf { it.exists() }?.delete()
                    DownloadFiles.moveInto(staged, folder, name)?.absolutePath
                } else {
                    val fileType = if (request.wantsAudioOnly) DownloadFileType.AUDIO else DownloadFileType.VIDEO
                    val tree =
                        downloads.resolveDestination(fileType, downloads.savedLocation()).exportTreeUri
                            ?: return@withLock null
                    val parent = request.collectionId?.let { collectionDao.getCollection(it)?.folderLocation }
                    DownloadFiles.exportToTree(context, staged, tree, name, parent)
                }
            } finally {
                staged.delete()
            }
        }

    private suspend fun isTaken(path: String): Boolean = File(path).exists() || downloadDao.existsByFilePath(path)
}
