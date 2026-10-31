package io.github.pushkar.chorplay.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.pushkar.chorplay.data.local.PlayerPreferences
import io.github.pushkar.chorplay.data.repository.YouTubeRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideYouTubeRepository(playerPreferences: PlayerPreferences): YouTubeRepository =
        YouTubeRepository.getInstance(playerPreferences)

    @Provides
    @Singleton
    fun provideSubscriptionRepository(
        @ApplicationContext context: Context,
    ): io.github.pushkar.chorplay.data.local.SubscriptionRepository =
        io.github.pushkar.chorplay.data.local.SubscriptionRepository
            .getInstance(context)

    @Provides
    @Singleton
    fun provideLikedVideosRepository(
        @ApplicationContext context: Context,
    ): io.github.pushkar.chorplay.data.local.LikedVideosRepository =
        io.github.pushkar.chorplay.data.local.LikedVideosRepository
            .getInstance(context)

    @Provides
    @Singleton
    fun provideViewHistory(
        @ApplicationContext context: Context,
    ): io.github.pushkar.chorplay.data.local.ViewHistory =
        io.github.pushkar.chorplay.data.local.ViewHistory
            .getInstance(context)

    @Provides
    @Singleton
    fun provideHomeFeedCacheRepository(
        @ApplicationContext context: Context,
    ): io.github.pushkar.chorplay.data.local.HomeFeedCacheRepository =
        io.github.pushkar.chorplay.data.local
            .HomeFeedCacheRepository(context)

    @Provides
    @Singleton
    fun providePlayerPreferences(
        @ApplicationContext context: Context,
    ): io.github.pushkar.chorplay.data.local.PlayerPreferences =
        io.github.pushkar.chorplay.data.local
            .PlayerPreferences(context)

    @Provides
    @Singleton
    fun provideBackupRepository(
        @ApplicationContext context: Context,
    ): io.github.pushkar.chorplay.data.local.BackupRepository =
        io.github.pushkar.chorplay.data.local
            .BackupRepository(context)
}
