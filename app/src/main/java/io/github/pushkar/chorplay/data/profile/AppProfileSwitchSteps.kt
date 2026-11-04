package io.github.pushkar.chorplay.data.profile

import android.app.ActivityOptions
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.window.SplashScreen
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.pushkar.chorplay.data.backup.BackupCoordinator
import io.github.pushkar.chorplay.data.recommendation.ChorplayNeuroEngine
import io.github.pushkar.chorplay.data.stats.VideoStatsRecorder
import io.github.pushkar.chorplay.notification.NotificationHelper
import io.github.pushkar.chorplay.notification.WellbeingAlarms
import io.github.pushkar.chorplay.player.PlaybackShutdown
import io.github.pushkar.chorplay.sync.SyncManager
import io.github.pushkar.chorplay.utils.potoken.PoTokenGenerator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.io.File
import javax.inject.Inject

class AppProfileSwitchSteps
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val neuroEngine: ChorplayNeuroEngine,
    private val videoStats: VideoStatsRecorder,
    private val sync: dagger.Lazy<SyncManager>,
    private val backups: dagger.Lazy<BackupCoordinator>,
    private val slices: dagger.Lazy<ProfileSlices>,
) : ProfileSwitchSteps {
    private val registry: ProfileRegistry get() = ActiveProfile.registry(context)

    override suspend fun refusal(target: String): SwitchRefusal? =
        when {
            target == ActiveProfile.get(context).id -> SwitchRefusal.ALREADY_ACTIVE

            sync.get().isBusy -> SwitchRefusal.SYNC_RUNNING

            backups.get().isRunning -> SwitchRefusal.BACKUP_RUNNING

            registry.state
                .first()
                .profiles
                .none { it.id == target } -> SwitchRefusal.UNKNOWN_PROFILE

            else -> null
        }

    private var gateClosedAt = 0L

    override fun closeGate(target: String) {
        gateClosedAt = SystemClock.elapsedRealtime()
        ProfileSwitchGate.close(target)
    }

    override fun reopenGate() {
        ProfileSwitchGate.reopen()
        SwitchIntentQueue.drop()
    }

    override fun finishOpenSessions() = BeforeProfileSwitch.run()

    override suspend fun saveQueue() = Unit

    override suspend fun stopPlayback() = PlaybackShutdown.stopForSwitch(context)

    override suspend fun awaitWrites() {
        neuroEngine.awaitWrites()
        videoStats.awaitWrites()
    }

    override suspend fun writeSlice() = slices.get().writeSwitchSlice()

    override suspend fun cancelProfileWork() {
        ProfileWork.cancelAll(context)
        WellbeingAlarms.cancel(context)
        NotificationHelper.cancelProfileNotifications(context)
    }

    override suspend fun clearCaches() {
        listOf("innertube_http_cache", "recap", "playlists").forEach { File(context.cacheDir, it).deleteRecursively() }
        if (ProfileWebView.clearedAtSwitch) PoTokenGenerator.resetSession()
    }

    override suspend fun persistActive(target: String) {
        registry.setActive(target)
    }

    override suspend fun restart() {
        delay(MIN_SWITCHING_SCREEN_MS - (SystemClock.elapsedRealtime() - gateClosedAt))
        val relaunch = SwitchIntentQueue.relaunchIntent(context)
        val options =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityOptions.makeBasic().setSplashScreenStyle(SplashScreen.SPLASH_SCREEN_STYLE_ICON).toBundle()
            } else {
                null
            }
        context.startActivity(relaunch, options)
        Runtime.getRuntime().exit(0)
    }
}

private const val MIN_SWITCHING_SCREEN_MS = 500L
