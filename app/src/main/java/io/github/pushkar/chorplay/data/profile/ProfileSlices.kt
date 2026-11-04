package io.github.pushkar.chorplay.data.profile

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.pushkar.chorplay.data.local.BackupRepository
import io.github.pushkar.chorplay.data.stats.RecapBackup
import io.github.pushkar.chorplay.data.stats.VideoStatsRecorder
import io.github.pushkar.chorplay.sync2.service.LinkedReset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileSlices
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val repository: BackupRepository,
    private val videoStats: VideoStatsRecorder,
    private val linkedReset: LinkedReset,
) {
    suspend fun export(uri: Uri): Result<Unit> =
        measured("export") { repository.exportMasterBackup(uri, recapBytes()) }

    suspend fun export(file: File): Result<Unit> = export(Uri.fromFile(file)).also { logSize(file) }

    suspend fun exportMaster(uri: Uri): Result<List<String>> =
        withMaster { write ->
            val out =
                context.contentResolver.openOutputStream(uri, "wt")
                    ?: return@withMaster Result.failure(IOException("Could not open $uri"))
            out.use(write)
            Result.success(Unit)
        }

    suspend fun exportMasterToFolder(folder: Uri): Result<List<String>> =
        withMaster { write -> repository.writeToFolder(folder, MASTER_FILE_NAME, MASTER_MIME_TYPE, write) }

    suspend fun writeSwitchSlice() {
        val target = sliceFile(ActiveProfile.get(context))
        val partial = File(target.parentFile, "$SLICE_NAME.partial")
        target.parentFile?.mkdirs()
        export(partial)
            .onSuccess { if (!partial.renameTo(target)) partial.delete() }
            .onFailure {
                partial.delete()
                Log.e(TAG, "Could not keep this profile's slice for the master backup", it)
            }
    }

    private suspend fun withMaster(save: suspend ((OutputStream) -> Unit) -> Result<Unit>): Result<List<String>> =
        withContext(Dispatchers.IO) {
            val running = ActiveProfile.get(context)
            val registry = ActiveProfile.registry(context).state.first()
            val defaultId = registry.defaultId ?: running.id
            val live = File(context.cacheDir, "master_live_slice.zip")
            try {
                export(live).getOrElse { return@withContext Result.failure(it) }
                val slices =
                    registry.profiles
                        .mapNotNull { profile ->
                            val file =
                                if (profile.id ==
                                    running.id
                                ) {
                                    live
                                } else {
                                    sliceFile(RunningProfile(profile.id, profile.id == defaultId))
                                }
                            file.takeIf(File::isFile)?.let { profile.id to it }
                        }.toMap()
                val avatars =
                    registry.profiles
                        .filter { it.avatar.kind == ProfileAvatar.PHOTO }
                        .mapNotNull { profile ->
                            ProfileAvatarPhotos.fileFor(context, profile.id).takeIf(File::isFile)?.let {
                                profile.id to
                                        it
                            }
                        }.toMap()
                val manifest =
                    MasterManifest(defaultId = defaultId, profiles = registry.profiles, included = slices.keys.toList())
                measured("master") {
                    save { out -> MasterBackupZip.write(out, manifest, slices[defaultId], slices - defaultId, avatars) }
                }.map { registry.profiles.filterNot { it.id in slices }.map { it.displayName(context) } }
            } finally {
                live.delete()
            }
        }

    private fun sliceFile(profile: RunningProfile) =
        File(ProfileFiles.noBackupRoot(context.noBackupFilesDir, profile), SLICE_NAME)


    suspend fun writeSeed(
        profileId: String,
        parts: Set<SlicePart>,
    ): Result<Unit> {
        val file = PendingSlice.file(
            ProfileFiles.noBackupRoot(
                context.noBackupFilesDir,
                RunningProfile(profileId, isDefault = false)
            )
        )
        file.parentFile?.mkdirs()
        return measured("seed") {
            repository.exportMasterBackup(
                uri = Uri.fromFile(file),
                recap = recapBytes(),
                seed = parts,
            )
        }.onSuccess { logSize(file) }.onFailure { file.delete() }
    }

    suspend fun import(uri: Uri): Result<Unit> =
        repository.importMasterBackup(
            uri,
            onRecap = ::restoreRecap,
        )

    suspend fun applyPending(file: File) {
        measured("pending import") { import(Uri.fromFile(file)) }
            .onSuccess { linkedReset.dataReplaced() }
            .onFailure { Log.e(TAG, "Could not apply the pending slice", it) }
    }

    suspend fun oldMasterTarget(uri: Uri): String? =
        withContext(Dispatchers.IO) {
            val registry = ActiveProfile.registry(context).state.first()
            if (registry.profiles.size < 2) return@withContext null
            withCopy(uri) { copy ->
                if (MasterBackupZip.readManifest(copy) != null) {
                    null
                } else {
                    registry.profiles.firstOrNull { it.id == ActiveProfile.get(context).id }?.displayName(context)
                }
            }
        }

    suspend fun restoreMaster(uri: Uri): Result<MasterRestored> =
        withContext(Dispatchers.IO) {
            withCopy(uri) { copy -> restoreMasterFrom(copy) }
        }

    private suspend fun restoreMasterFrom(copy: File): Result<MasterRestored> {
        val running = ActiveProfile.get(context)
        val manifest =
            MasterBackupZip.readManifest(copy)
                ?: return import(Uri.fromFile(copy)).map { MasterRestored(restoredNow = true, waiting = emptyList()) }
        val registry = ActiveProfile.registry(context)
        val localDefault = registry.state.first().defaultId ?: running.id
        val incoming = manifest.profiles.filter { it.id in manifest.included }
        val targetOf = { profile: Profile -> if (profile.id == manifest.defaultId) localDefault else profile.id }
        val others = incoming.filter { it.id != manifest.defaultId }
        if (!registry.addRestored(others)) return Result.failure(TooManyProfiles())
        var restoredNow = false
        val waiting = mutableListOf<String>()
        incoming.forEach { profile ->
            val target = targetOf(profile)
            val fromFolder = profile.id.takeUnless { it == manifest.defaultId }
            val local = RunningProfile(target, isDefault = target == localDefault)
            if (target != localDefault &&
                MasterBackupZip.extractAvatar(copy, profile.id, ProfileAvatarPhotos.fileFor(context, target))
            ) {
                registry.setAppearance(target, profile.colorKey, profile.avatar)
            }
            if (target == running.id) {
                val now = File(context.cacheDir, "master_restore_now.zip")
                try {
                    if (MasterBackupZip.extractSlice(copy, fromFolder, now)) {
                        import(Uri.fromFile(now)).getOrElse { return Result.failure(it) }
                        restoredNow = true
                    }
                } finally {
                    now.delete()
                }
            } else if (MasterBackupZip.extractSlice(
                    copy,
                    fromFolder,
                    PendingSlice.file(ProfileFiles.noBackupRoot(context.noBackupFilesDir, local)),
                )
            ) {
                waiting += profile.displayName(context)
            }
        }
        return Result.success(MasterRestored(restoredNow, waiting))
    }

    private suspend fun <T> withCopy(
        uri: Uri,
        read: suspend (File) -> T,
    ): T {
        val copy = File(context.cacheDir, "master_restore.zip")
        try {
            val input = context.contentResolver.openInputStream(uri) ?: throw IOException("Could not read $uri")
            input.use { source -> copy.outputStream().use { source.copyTo(it) } }
            return read(copy)
        } finally {
            copy.delete()
        }
    }

    private suspend fun recapBytes(): ByteArray? =
        runCatching {
            RecapBackup(
                video = videoStats.snapshot()
            ).encode()
        }.getOrNull()

    private suspend fun restoreRecap(bytes: ByteArray) {
        val backup = RecapBackup.decode(bytes) ?: return
        backup.video?.let { videoStats.restore(it) }
    }

    private inline fun <T> measured(
        what: String,
        block: () -> Result<T>,
    ): Result<T> {
        val start = SystemClock.elapsedRealtime()
        return block().also {
            Log.i(
                TAG,
                "Slice $what took ${SystemClock.elapsedRealtime() - start} ms, ok=${it.isSuccess}"
            )
        }
    }

    private fun logSize(file: File) {
        if (file.exists()) Log.i(TAG, "Slice ${file.name} is ${file.length()} bytes")
    }

    private companion object {
        const val TAG = "ProfileSlices"
        const val SLICE_NAME = "slice.zip"
        const val MASTER_FILE_NAME = "chorplay_master_backup.zip"
        const val MASTER_MIME_TYPE = "application/zip"
    }
}

data class MasterRestored(
    val restoredNow: Boolean,
    val waiting: List<String>,
)

class TooManyProfiles : IllegalStateException("A master backup can't add profiles past the limit")
