package io.github.pushkar.chorplay.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.pushkar.chorplay.data.profile.StoreScope
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.savedPlaylistSyncDataStore: DataStore<Preferences> by
    safePreferencesDataStore(name = "saved_playlist_sync", scope = StoreScope.PROFILE_LIBRARY)

@Singleton
class SavedPlaylistSyncStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val dataStore = context.applicationContext.savedPlaylistSyncDataStore

        suspend fun syncedAt(playlistId: String): Long? = dataStore.data.first()[key(playlistId)]

        suspend fun markSynced(
            playlistId: String,
            atMillis: Long,
        ) {
            dataStore.edit { it[key(playlistId)] = atMillis }
        }

        private fun key(playlistId: String) = longPreferencesKey("synced_at:$playlistId")
    }
