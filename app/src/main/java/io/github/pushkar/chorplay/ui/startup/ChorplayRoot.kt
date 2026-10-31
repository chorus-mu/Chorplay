package io.github.pushkar.chorplay.ui.startup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import io.github.pushkar.chorplay.data.local.AppUiModePreferences
import io.github.pushkar.chorplay.data.recommendation.ChorplayNeuroEngine
import io.github.pushkar.chorplay.data.repository.DeArrowRepository
import io.github.pushkar.chorplay.data.repository.OriginalTitleRepository
import io.github.pushkar.chorplay.platform.AppIconController
import io.github.pushkar.chorplay.platform.AppUiMode
import io.github.pushkar.chorplay.platform.AppUiRoot
import io.github.pushkar.chorplay.platform.DeviceFormFactorDetector
import io.github.pushkar.chorplay.ui.ChorplayApp
import io.github.pushkar.chorplay.ui.IncomingLinks
import io.github.pushkar.chorplay.ui.components.profiles.ProfileSwitchingOverlay
import io.github.pushkar.chorplay.ui.components.shared.ProvideChannelGroupLabels
import io.github.pushkar.chorplay.ui.components.shared.ProvideDateDisplaySettings
import io.github.pushkar.chorplay.ui.components.shared.card.ProvideVideoCardState
import io.github.pushkar.chorplay.ui.screens.crash.CrashReportScreen
import io.github.pushkar.chorplay.ui.theme.FlowFontFamily
import io.github.pushkar.chorplay.ui.tv.ChorplayTvApp
import io.github.pushkar.chorplay.ui.tv.screens.TvProfileGate
import io.github.pushkar.chorplay.ui.utils.ProvideWindowSizeClass
import io.github.pushkar.chorplay.utils.ChorplayCrashHandler
import kotlinx.coroutines.flow.StateFlow

/**
 * Everything MainActivity shows: the theme gate, the phone or TV app, a crash report from the last session, and the
 * providers every screen reads. [onUiRoot] reports which of phone or TV is showing.
 */
@Composable
internal fun ChorplayRoot(
    storedTheme: StateFlow<ThemeSettings?>,
    links: IncomingLinks,
    splashController: SplashController,
    originalTitles: OriginalTitleRepository,
    deArrow: DeArrowRepository,
    appIconController: AppIconController,
    onUiRoot: (AppUiRoot) -> Unit,
) {
    // Nothing is composed until the theme is known: the splash covers the wait, and the app
    // composes once in the right theme instead of twice.
    val theme = storedTheme.collectAsState().value ?: return

    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val configuration = LocalConfiguration.current
    val uiPreferences = remember { AppUiModePreferences(applicationContext) }
    val appUiMode by uiPreferences.mode.collectAsState(initial = AppUiMode.AUTOMATIC)
    val deviceFormFactor =
        remember(configuration.uiMode, context) {
            DeviceFormFactorDetector.detect(context)
        }
    val appUiRoot = appUiMode.resolve(deviceFormFactor)
    SideEffect { onUiRoot(appUiRoot) }

    // Check for a crash that happened last session.
    // If found, show the CrashReportScreen instead of the normal UI. It is cleared as soon
    // as it is read, so leaving the screen any way shows it once, not on every launch.
    var pendingCrashLog by remember {
        mutableStateOf(
            ChorplayCrashHandler.getLastCrash(applicationContext)?.also {
                ChorplayCrashHandler.clearLastCrash(applicationContext)
            },
        )
    }

    if (pendingCrashLog != null) {
        SideEffect { splashController.contentReady = true }
        ChorplayTheme(theme.copy(fontFamily = FlowFontFamily)) {
            CrashReportScreen(
                report = pendingCrashLog!!,
                onContinue = { pendingCrashLog = null },
            )
        }
        return
    }

    // Initialize Chorplay Neuro Engine
    LaunchedEffect(Unit) {
        ChorplayNeuroEngine.initialize(applicationContext)
    }

    ChorplayTheme(theme) {
        val splashTone = splashTone(MaterialTheme.colorScheme.background)
        val selectedIcon by appIconController.selected.collectAsState(initial = null)
        LaunchedEffect(splashTone, selectedIcon) { splashController.rememberTheme(splashTone, appIconController.activeSuffix()) }

        // Date preferences: five DataStore flows used to be opened per video card,
        // metadata line, info section, description sheet and info dialog.
        ProvideWindowSizeClass {
            ProvideDateDisplaySettings {
                // Card preferences and watch progress are collected once here. Cards used to
                // collect them individually, so a feed of ten opened ten Room observers and
                // fifty DataStore collectors.
                ProvideVideoCardState(originalTitles, deArrow) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .semantics { testTagsAsResourceId = true },
                    ) {
                        // 1. MAIN APP (Home/NavHost)
                        // This loads *behind* the splash screen immediately.
                        // By the time splash fades, this is ready.
                        val deeplinkVideoId by links.videoId
                        val isDeeplinkShort by links.isShort
                        val isDeeplinkShared by links.isShared
                        val openMusicPlayerRequest by links.musicPlayerRequest
                        val pendingRoute by links.route
                        val downloadVideoId by links.downloadVideoId

                        ProvideChannelGroupLabels {
                            if (appUiRoot == AppUiRoot.TV) {
                                SideEffect { splashController.contentReady = true }
                                TvProfileGate(hasLink = deeplinkVideoId != null) { startSettings ->
                                    ChorplayTvApp(
                                        deeplinkVideoId = deeplinkVideoId,
                                        isShort = isDeeplinkShort,
                                        onDeeplinkConsumed = links::consumeVideo,
                                        startSettings = startSettings,
                                    )
                                }
                            } else {
                                ChorplayApp(
                                    currentTheme = theme.themeMode,
                                    themeVariant = theme.themeVariant,
                                    systemLightThemeMode = theme.systemLightThemeMode,
                                    systemDarkThemeMode = theme.systemDarkThemeMode,
                                    deeplinkVideoId = deeplinkVideoId,
                                    isShort = isDeeplinkShort,
                                    isSharedLink = isDeeplinkShared,
                                    openMusicPlayerRequest = openMusicPlayerRequest,
                                    onDeeplinkConsumed = links::consumeVideo,
                                    pendingRoute = pendingRoute,
                                    onPendingRouteConsumed = links::consumeRoute,
                                    downloadVideoId = downloadVideoId,
                                    onDownloadConsumed = links::consumeDownload,
                                    onStartDestinationKnown = { splashController.contentReady = true },
                                )
                            }
                        }
                        ProfileSwitchingOverlay(
                            currentTone = splashTone.name,
                            onSplashTone = { name ->
                                SplashTone.entries.firstOrNull { it.name == name }?.let {
                                    splashController.rememberTheme(
                                        it,
                                        appIconController.activeSuffix(),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
