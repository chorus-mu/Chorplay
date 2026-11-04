package io.github.pushkar.chorplay.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import io.github.pushkar.chorplay.data.local.entity.DownloadEntity
import io.github.pushkar.chorplay.data.local.entity.DownloadItemEntity
import io.github.pushkar.chorplay.data.local.entity.DownloadItemStatus
import io.github.pushkar.chorplay.data.local.entity.DownloadWithItems
import kotlinx.coroutines.flow.Flow

data class QueuedDownload(
    val videoId: String,
    val createdAt: Long,
    val status: DownloadItemStatus,
)

@Dao
interface DownloadDao {
    @Upsert
    suspend fun insertDownload(download: DownloadEntity)

    @Transaction
    suspend fun replaceDownload(
        download: DownloadEntity,
        items: List<DownloadItemEntity>,
    ) {
        deleteItemsFor(download.videoId)
        insertDownload(download)
        insertItems(items)
    }

    @Query("DELETE FROM download_items WHERE videoId = :videoId")
    suspend fun deleteItemsFor(videoId: String)

    @Query("DELETE FROM downloads WHERE videoId = :videoId")
    suspend fun deleteDownload(videoId: String)

    @Query("SELECT * FROM downloads WHERE videoId = :videoId")
    suspend fun getDownloadByVideoId(videoId: String): DownloadEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM downloads WHERE videoId = :videoId)")
    suspend fun exists(videoId: String): Boolean

    @Query("UPDATE downloads SET sponsorBlockSegmentsJson = :json WHERE videoId = :videoId")
    suspend fun updateSponsorBlockData(
        videoId: String,
        json: String,
    )

    @Query("SELECT sponsorBlockSegmentsJson FROM downloads WHERE videoId = :videoId")
    suspend fun getSponsorBlockData(videoId: String): String?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(item: DownloadItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItems(items: List<DownloadItemEntity>)

    @Update
    suspend fun updateItem(item: DownloadItemEntity)

    @Query("UPDATE download_items SET downloadedBytes = :downloadedBytes, status = :status WHERE id = :itemId")
    suspend fun updateProgress(
        itemId: Int,
        downloadedBytes: Long,
        status: DownloadItemStatus,
    )

    @Query("UPDATE download_items SET status = :status WHERE id = :itemId")
    suspend fun updateStatus(
        itemId: Int,
        status: DownloadItemStatus,
    )

    @Query("UPDATE download_items SET status = :status WHERE videoId = :videoId")
    suspend fun updateAllItemsStatus(
        videoId: String,
        status: DownloadItemStatus,
    )

    @Query("UPDATE download_items SET downloadedBytes = :downloadedBytes, totalBytes = :totalBytes, status = :status WHERE id = :itemId")
    suspend fun updateItemFull(
        itemId: Int,
        downloadedBytes: Long,
        totalBytes: Long,
        status: DownloadItemStatus,
    )

    @Query("UPDATE download_items SET filePath = :filePath, fileName = :fileName WHERE id = :itemId")
    suspend fun updateItemLocation(
        itemId: Int,
        filePath: String,
        fileName: String,
    )

    @Query("SELECT * FROM download_items WHERE id = :itemId")
    suspend fun getItemById(itemId: Int): DownloadItemEntity?

    @Query("SELECT * FROM download_items WHERE videoId = :videoId")
    suspend fun getItemsByVideoId(videoId: String): List<DownloadItemEntity>

    @Query("DELETE FROM download_items WHERE id = :itemId")
    suspend fun deleteItem(itemId: Int)

    @Transaction
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloadsWithItems(): Flow<List<DownloadWithItems>>

    @Transaction
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    suspend fun getAllDownloadsWithItemsOnce(): List<DownloadWithItems>

    @Transaction
    @Query("SELECT * FROM downloads WHERE videoId = :videoId")
    suspend fun getDownloadWithItems(videoId: String): DownloadWithItems?

    @Transaction
    @Query("SELECT * FROM downloads WHERE videoId = :videoId")
    fun getDownloadWithItemsFlow(videoId: String): Flow<DownloadWithItems?>

    @Transaction
    @Query(
        """
        SELECT DISTINCT d.* FROM downloads d 
        INNER JOIN download_items di ON d.videoId = di.videoId 
        WHERE di.fileType = 'VIDEO' 
        ORDER BY d.createdAt DESC
    """,
    )
    fun getVideoDownloads(): Flow<List<DownloadWithItems>>

    @Transaction
    @Query(
        """
        SELECT DISTINCT d.* FROM downloads d 
        INNER JOIN download_items di ON d.videoId = di.videoId 
        WHERE di.fileType = 'AUDIO' 
        AND d.videoId NOT IN (
            SELECT videoId FROM download_items WHERE fileType = 'VIDEO'
        )
        ORDER BY d.createdAt DESC
    """,
    )
    fun getAudioOnlyDownloads(): Flow<List<DownloadWithItems>>

    @Transaction
    @Query(
        """
        SELECT DISTINCT d.* FROM downloads d 
        INNER JOIN download_items di ON d.videoId = di.videoId 
        WHERE di.status IN ('DOWNLOADING', 'PENDING', 'PAUSED')
        ORDER BY d.createdAt DESC
    """,
    )
    fun getActiveDownloads(): Flow<List<DownloadWithItems>>

    @Query(
        """
        SELECT filePath FROM download_items
        WHERE videoId = :videoId AND fileType = 'AUDIO' AND status = 'COMPLETED'
        AND videoId NOT IN (SELECT videoId FROM download_items WHERE fileType = 'VIDEO')
        LIMIT 1
    """,
    )
    fun completedAudioPathBlocking(videoId: String): String?

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM download_items 
            WHERE videoId = :videoId AND status = 'COMPLETED'
        )
    """,
    )
    suspend fun isDownloaded(videoId: String): Boolean

    @Query(
        """
        UPDATE download_items SET filePath = :filePath, fileName = :fileName, format = :format, mimeType = :mimeType,
            quality = :quality, downloadedBytes = :size, totalBytes = :size, status = 'COMPLETED'
        WHERE id = :itemId
        """,
    )
    suspend fun completeItem(
        itemId: Int,
        filePath: String,
        fileName: String,
        format: String,
        mimeType: String,
        quality: String,
        size: Long,
    )

    @Query("UPDATE downloads SET thumbnailPath = :path WHERE videoId = :videoId")
    suspend fun updateThumbnailPath(
        videoId: String,
        path: String,
    )

    @Query(
        """
        UPDATE downloads SET title = :title, uploader = :uploader, channelId = :channelId, description = :description,
            releaseDate = :releaseDate, viewCount = :viewCount, likeCount = :likeCount, requestJson = :requestJson
        WHERE videoId = :videoId
        """,
    )
    suspend fun updateMetadata(
        videoId: String,
        title: String,
        uploader: String,
        channelId: String,
        description: String,
        releaseDate: String?,
        viewCount: Long,
        likeCount: Long,
        requestJson: String,
    )

    @Query("UPDATE downloads SET requestJson = :requestJson WHERE videoId = :videoId")
    suspend fun updateRequest(
        videoId: String,
        requestJson: String,
    )

    @Query("SELECT EXISTS(SELECT 1 FROM download_items WHERE status IN ('PENDING', 'DOWNLOADING'))")
    suspend fun hasQueuedDownloads(): Boolean

    @Query(
        """
        SELECT d.videoId AS videoId, MIN(d.createdAt) AS createdAt, di.status AS status FROM downloads d
        INNER JOIN download_items di ON d.videoId = di.videoId
        GROUP BY d.videoId
        ORDER BY createdAt
        """,
    )
    fun observeQueue(): Flow<List<QueuedDownload>>

    @Query("UPDATE download_items SET status = 'PENDING' WHERE status = 'DOWNLOADING'")
    suspend fun requeueInterrupted()

    @Query("SELECT COALESCE(SUM(totalBytes), 0) FROM download_items WHERE status = 'COMPLETED'")
    suspend fun getTotalDownloadSize(): Long

    @Query("SELECT COUNT(DISTINCT videoId) FROM download_items WHERE status = 'COMPLETED'")
    suspend fun getCompletedDownloadCount(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM download_items WHERE filePath = :filePath)")
    suspend fun existsByFilePath(filePath: String): Boolean
}
