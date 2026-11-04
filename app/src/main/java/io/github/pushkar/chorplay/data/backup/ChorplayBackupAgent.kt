package io.github.pushkar.chorplay.data.backup

import android.app.backup.BackupAgent
import android.app.backup.BackupDataInput
import android.app.backup.BackupDataOutput
import android.os.ParcelFileDescriptor
import android.util.Log
import io.github.pushkar.chorplay.data.profile.ActiveProfile
import io.github.pushkar.chorplay.data.profile.ProfileFolders
import kotlinx.coroutines.runBlocking

class ChorplayBackupAgent : BackupAgent() {
    override fun onBackup(
        oldState: ParcelFileDescriptor?,
        data: BackupDataOutput?,
        newState: ParcelFileDescriptor?,
    ) = Unit

    override fun onRestore(
        data: BackupDataInput?,
        appVersionCode: Int,
        newState: ParcelFileDescriptor?,
    ) = Unit

    override fun onRestoreFinished() {
        val deleted = RestoredDataCleanup.deleteInstallOnlyFiles(filesDir)
        if (deleted.isNotEmpty()) Log.i(TAG, "Removed restored install-only files: $deleted")
        val registry = runBlocking { ActiveProfile.registry(this@ChorplayBackupAgent).reopenInDefault() }
        val unlisted =
            RestoredDataCleanup.deleteUnlistedProfiles(
                folders = ProfileFolders.of(this),
                profileIds = registry.profiles.mapTo(HashSet()) { it.id },
            )
        if (unlisted.isNotEmpty()) Log.i(TAG, "Removed profiles the restored registry doesn't list: $unlisted")
    }

    private companion object {
        const val TAG = "ChorplayBackupAgent"
    }
}
