package io.github.pushkar.chorplay.data.local

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import io.github.pushkar.chorplay.data.profile.ProfileFiles
import io.github.pushkar.chorplay.data.profile.StoreScope
import kotlinx.coroutines.flow.first
import java.io.File
import java.io.IOException

internal val Context.devicePreferencesDataStore: DataStore<Preferences> by safePreferencesDataStore(
    name = "device_preferences",
    scope = StoreScope.DEVICE,
    produceMigrations = { context ->
        listOf(
            DeviceKeysCopy(
                listOf(
                    DeviceKeysCopy.Source(
                        store = context.playerPreferencesDataStore,
                        names = PlayerPreferences.DEVICE_KEY_NAMES,
                        file = ProfileFiles.preferences(context, StoreScope.PROFILE_SETTINGS, "player_preferences"),
                    ),
                    DeviceKeysCopy.Source(
                        store = context.dataStore,
                        names = LocalDataManager.DEVICE_KEY_NAMES,
                        file = ProfileFiles.preferences(context, StoreScope.PROFILE_SETTINGS, "chorplay_preferences"),
                    ),
                ),
            ),
        )
    },
)

internal class DeviceKeysCopy(
    private val sources: List<Source>,
) : DataMigration<Preferences> {
    class Source(
        val store: DataStore<Preferences>,
        val names: Set<String>,
        val file: File,
    )

    override suspend fun shouldMigrate(currentData: Preferences): Boolean = currentData[COPIED] != true

    override suspend fun migrate(currentData: Preferences): Preferences {
        val copied = currentData.toMutablePreferences()
        for (source in sources) {
            val stored =
                source.store.data
                    .first()
                    .asMap()
            if (stored.isEmpty() && source.file.length() > 0) throw IOException("${source.file.name} is unreadable, copying later")
            stored.forEach { (key, value) ->
                if (key.name in source.names && !copied.asMap().containsKey(key)) copied.putUnchecked(key, value)
            }
        }
        copied[COPIED] = true
        return copied.toPreferences()
    }

    override suspend fun cleanUp() = Unit

    companion object {
        val COPIED = booleanPreferencesKey("device_keys_copied")
    }
}

@Suppress("UNCHECKED_CAST")
private fun MutablePreferences.putUnchecked(
    key: Preferences.Key<*>,
    value: Any,
) {
    this[key as Preferences.Key<Any>] = value
}
