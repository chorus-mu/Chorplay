package io.github.pushkar.chorplay.data.profile

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.await

object ProfileWork {
    const val KEY_PROFILE_ID = "chorplay_profile_id"
    private const val TAG_PREFIX = "profile:"

    fun tag(context: Context): String = TAG_PREFIX + ActiveProfile.get(context).id

    fun data(
        context: Context,
        data: Data = Data.EMPTY,
    ): Data =
        Data
            .Builder()
            .putAll(data)
            .putString(KEY_PROFILE_ID, ActiveProfile.get(context).id)
            .build()

    fun belongsHere(worker: ListenableWorker): Boolean =
        belongs(worker.inputData.getString(KEY_PROFILE_ID), ActiveProfile.get(worker.applicationContext))

    fun queuedByOther(
        context: Context,
        tags: Set<String>,
    ): Boolean = queuedByOther(tags, ActiveProfile.get(context))

    suspend fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(tag(context)).await()
    }

    internal fun belongs(
        stamped: String?,
        running: RunningProfile,
    ): Boolean = stamped?.let { it == running.id } ?: running.isDefault

    internal fun queuedByOther(
        tags: Set<String>,
        running: RunningProfile,
    ): Boolean = !belongs(tags.firstOrNull { it.startsWith(TAG_PREFIX) }?.removePrefix(TAG_PREFIX), running)
}
