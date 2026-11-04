package io.github.pushkar.chorplay.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.pushkar.chorplay.data.profile.StoreScope
import io.github.pushkar.chorplay.platform.AppUiMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.appUiModeDataStore: DataStore<Preferences> by safePreferencesDataStore(name = "app_ui_mode", scope = StoreScope.DEVICE)

class AppUiModePreferences
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) {
        private val appContext = context.applicationContext

        val mode: Flow<AppUiMode> =
            appContext.appUiModeDataStore.data.map { preferences ->
                AppUiMode.fromStorage(preferences[MODE])
            }

        suspend fun setMode(mode: AppUiMode) {
            appContext.appUiModeDataStore.edit { preferences ->
                preferences[MODE] = mode.name
            }
        }

        private companion object {
            val MODE = stringPreferencesKey("mode")
        }
    }
