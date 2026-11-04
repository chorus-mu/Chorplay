package io.github.pushkar.chorplay.data.local

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import io.github.pushkar.chorplay.data.local.dao.CacheDao
import io.github.pushkar.chorplay.data.local.dao.DownloadCollectionDao
import io.github.pushkar.chorplay.data.local.dao.DownloadDao
import io.github.pushkar.chorplay.data.local.dao.HomeFeedCacheDao
import io.github.pushkar.chorplay.data.local.dao.NoteDao
import io.github.pushkar.chorplay.data.local.dao.NotificationDao
import io.github.pushkar.chorplay.data.local.dao.PlaylistDao
import io.github.pushkar.chorplay.data.local.dao.SubscriptionGroupDao
import io.github.pushkar.chorplay.data.local.dao.SyncLogDao
import io.github.pushkar.chorplay.data.local.dao.SyncPeerDao
import io.github.pushkar.chorplay.data.local.dao.VideoDao
import io.github.pushkar.chorplay.data.local.dao.WatchHistoryDao
import io.github.pushkar.chorplay.data.local.entity.DownloadCollectionEntity
import io.github.pushkar.chorplay.data.local.entity.DownloadCollectionItemEntity
import io.github.pushkar.chorplay.data.local.entity.DownloadEntity
import io.github.pushkar.chorplay.data.local.entity.DownloadItemEntity
import io.github.pushkar.chorplay.data.local.entity.HomeFeedCacheEntity
import io.github.pushkar.chorplay.data.local.entity.NoteEntity
import io.github.pushkar.chorplay.data.local.entity.NotificationEntity
import io.github.pushkar.chorplay.data.local.entity.PlaylistEntity
import io.github.pushkar.chorplay.data.local.entity.PlaylistVideoCrossRef
import io.github.pushkar.chorplay.data.local.entity.SubscriptionFeedEntity
import io.github.pushkar.chorplay.data.local.entity.SubscriptionGroupEntity
import io.github.pushkar.chorplay.data.local.entity.SyncLogEntity
import io.github.pushkar.chorplay.data.local.entity.SyncPeerEntity
import io.github.pushkar.chorplay.data.local.entity.VideoEntity
import io.github.pushkar.chorplay.data.local.entity.WatchHistoryEntity
import io.github.pushkar.chorplay.data.profile.ProfileFiles

@Database(
    entities = [
        VideoEntity::class,
        PlaylistEntity::class,
        PlaylistVideoCrossRef::class,
        NotificationEntity::class,
        SubscriptionFeedEntity::class,
        DownloadEntity::class,
        DownloadItemEntity::class,
        WatchHistoryEntity::class,
        HomeFeedCacheEntity::class,
        SubscriptionGroupEntity::class,
        SyncLogEntity::class,
        SyncPeerEntity::class,
        NoteEntity::class,
        DownloadCollectionEntity::class,
        DownloadCollectionItemEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun notificationDao(): NotificationDao

    abstract fun noteDao(): NoteDao

    abstract fun cacheDao(): CacheDao

    abstract fun downloadDao(): DownloadDao

    abstract fun downloadCollectionDao(): DownloadCollectionDao

    abstract fun watchHistoryDao(): WatchHistoryDao

    abstract fun homeFeedCacheDao(): HomeFeedCacheDao

    abstract fun subscriptionGroupDao(): SubscriptionGroupDao

    abstract fun syncLogDao(): SyncLogDao

    abstract fun syncPeerDao(): SyncPeerDao

    companion object {
        const val DEFAULT_NAME = "chorplay_database"

        @Volatile
        private var deviceDatabase: AppDatabase? = null

        @Volatile
        private var profileDatabase: AppDatabase? = null

        fun device(context: Context): AppDatabase =
            deviceDatabase ?: synchronized(this) {
                deviceDatabase ?: open(context.applicationContext, DEFAULT_NAME).also { deviceDatabase = it }
            }

        fun profile(context: Context): AppDatabase =
            profileDatabase ?: synchronized(this) {
                profileDatabase ?: ProfileFiles
                    .database(context, DEFAULT_NAME)
                    .let { name ->
                        if (name == DEFAULT_NAME) device(context) else open(
                            context.applicationContext,
                            name
                        )
                    }
                    .also { profileDatabase = it }
            }

        internal fun open(
            context: Context,
            name: String,
        ): AppDatabase =
            databaseBuilder(context, AppDatabase::class.java, name)
                .fallbackToDestructiveMigration(true)
                .build()
    }
}
