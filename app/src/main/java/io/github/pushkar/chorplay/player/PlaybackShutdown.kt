package io.github.pushkar.chorplay.player

import android.app.ActivityManager
import android.content.Context

/**
 * Ends video playback the way closing it yourself does, so its service stops and its
 * notification goes. It holds no Activity, so it works however the timer was set and whether or not
 * a screen is still open.
 */
internal object PlaybackShutdown {
    /** The sleep timer's Close app: also removes Chorplay's tasks. The process is left to Android: with nothing running it is only cached. */
    fun closeApp(context: Context) {
        stopVideo(context)
        context.getSystemService(ActivityManager::class.java)?.appTasks?.forEach { it.finishAndRemoveTask() }
    }

    /**
     * Before a profile switch restarts the process.
     */
    suspend fun stopForSwitch(context: Context) {
        stopVideo(context)
    }

    private fun stopVideo(context: Context) {
        EnhancedPlayerManager.getInstance().stopBackgroundService()
        GlobalPlayerState.release()
    }
}
