package io.github.pushkar.chorplay.data.backup

import io.github.pushkar.chorplay.data.profile.ProfileFolders
import java.io.File

internal object RestoredDataCleanup {
    private val FILES = listOf("user_neuro_brain.json")
    private val DATASTORE_FILES =
        listOf(
            "chorplay_neuro_brain_v7.json",
            "chorplay_neuro_brain_v8.json",
            "chorplay_neuro_brain_v9.json",
            "chorplay_neuro_brain_v10.json",
            "chorplay_neuro_content_v1.json",
            "chorplay_neuro_categories_v1.json",
            "chorplay_neuro_cowatch_v1.json",
            "chorplay_neuro_neighbours_v1.json",
            "sync_identity.preferences_pb",
            "sync_brain_crdt.preferences_pb",
        )
    private const val SYNC_LEDGER_PREFIX = "sync_ledger_"

    fun deleteUnlistedProfiles(
        folders: ProfileFolders,
        profileIds: Set<String>,
    ): List<String> = (folders.idsOnDisk() - profileIds).flatMap(folders::delete).map { it.name }

    fun deleteInstallOnlyFiles(filesDir: File): List<String> {
        val datastore = File(filesDir, "datastore")
        val ledgers = datastore.listFiles { file -> file.name.startsWith(SYNC_LEDGER_PREFIX) }.orEmpty().toList()
        return (FILES.map { File(filesDir, it) } + DATASTORE_FILES.map { File(datastore, it) } + ledgers)
            .filter { it.exists() && it.delete() }
            .map { it.name }
    }
}
