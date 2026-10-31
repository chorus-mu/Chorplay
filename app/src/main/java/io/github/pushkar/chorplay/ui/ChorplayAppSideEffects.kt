package io.github.pushkar.chorplay.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import io.github.pushkar.chorplay.data.subscriptions.refreshSubscriptionsAtStartup
import io.github.pushkar.chorplay.player.DeepChorplayManager
import io.github.pushkar.chorplay.player.SleepTimerManager
import io.github.pushkar.chorplay.ui.components.layout.navigation.ChorplayTab
import kotlinx.coroutines.flow.collectLatest

/** App-wide effects with no UI of their own: preference propagation and the global snackbar feeds. */
@Composable
internal fun ChorplayAppSideEffects(
    snackbarHostState: SnackbarHostState,
    sleepTimerCloseAppOnExpiry: Boolean,
    subscriptionRefreshOnStartup: Boolean,
) {
    val context = LocalContext.current

    LaunchedEffect(sleepTimerCloseAppOnExpiry) {
        SleepTimerManager.updatePreferredCloseAppOnExpiry(sleepTimerCloseAppOnExpiry)
    }

    LaunchedEffect(subscriptionRefreshOnStartup) {
        if (subscriptionRefreshOnStartup) {
            refreshSubscriptionsAtStartup(context.applicationContext)
        }
    }

    LaunchedEffect(snackbarHostState) {
        DeepChorplayManager.messages.collectLatest { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }
}

/** Detail screens keep the tab they were opened from, so this holds the last tab root navigated to. */
@Composable
internal fun rememberSelectedChorplayTab(
    navController: NavHostController,
    defaultTab: ChorplayTab,
): State<ChorplayTab> {
    val selectedTab = remember { mutableStateOf(defaultTab) }
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry -> entry.chorTab()?.let { selectedTab.value = it } }
    }
    return selectedTab
}

/** Follows a changed default tab, and leaves Home for the default tab once the user hides Home. */
@Composable
internal fun ChorStartTabEffects(
    navController: NavHostController,
    currentRoute: MutableState<String>,
    defaultTab: ChorplayTab,
    isHomeNavigationEnabled: Boolean,
    needsOnboarding: Boolean?,
) {
    LaunchedEffect(defaultTab) {
        currentRoute.value = defaultTab.route
    }

    LaunchedEffect(isHomeNavigationEnabled, currentRoute.value, defaultTab, needsOnboarding) {
        if (needsOnboarding == false && !isHomeNavigationEnabled && currentRoute.value == "home") {
            currentRoute.value = defaultTab.route
            navController.navigate(defaultTab.route) {
                popUpTo("home") { inclusive = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }
}
