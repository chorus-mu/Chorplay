package io.github.pushkar.chorplay.data.profile

import android.content.Context
import android.util.Log
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.IOException

object ActiveProfile {
    private const val TAG = "ActiveProfile"

    @Volatile
    private var registry: ProfileRegistry? = null

    @Volatile
    private var running: RunningProfile? = null

    fun attach(base: Context) {
        get(base)
    }

    fun get(context: Context): RunningProfile =
        running ?: synchronized(this) {
            running ?: read(context).also { running = it }
        }

    private fun read(context: Context): RunningProfile =
        ProfileRegistry.peek(registryFile(context)) ?: try {
            runBlocking { registry(context).running() }
        } catch (e: IOException) {
            Log.e(TAG, "Profile registry unreadable, running as Default", e)
            RunningProfile(id = "", isDefault = true)
        }

    internal fun registry(context: Context): ProfileRegistry =
        registry ?: synchronized(this) {
            registry ?: ProfileRegistry(registryFile(context)).also { registry = it }
        }

    private fun registryFile(context: Context) = File(context.filesDir, "datastore/${ProfileRegistry.FILE_NAME}")
}
