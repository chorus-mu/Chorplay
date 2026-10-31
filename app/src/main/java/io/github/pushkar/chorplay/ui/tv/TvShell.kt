package io.github.pushkar.chorplay.ui.tv

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.ui.TabScrollEventBus
import io.github.pushkar.chorplay.ui.components.layout.ChorplayBottomInsets
import io.github.pushkar.chorplay.ui.components.layout.LocalChorplayBottomInsets
import io.github.pushkar.chorplay.ui.components.settings.SettingsTarget
import io.github.pushkar.chorplay.ui.screens.home.HomeViewModel
import io.github.pushkar.chorplay.ui.screens.search.SearchViewModel
import io.github.pushkar.chorplay.ui.screens.subscriptions.SubscriptionsViewModel
import io.github.pushkar.chorplay.ui.tv.components.TvNavRail
import io.github.pushkar.chorplay.ui.tv.components.TvRailProfile
import io.github.pushkar.chorplay.ui.tv.navigation.TvNavHost
import io.github.pushkar.chorplay.ui.tv.navigation.TvNavigation
import io.github.pushkar.chorplay.ui.tv.navigation.TvRoutes
import io.github.pushkar.chorplay.ui.tv.theme.LocalTvDimens

/**
 * TV browse shell: content laid out against the collapsed rail width; the rail lies over both,
 * so its focus expansion never reflows the page. Coming back from the rail returns focus to the
 * item it left.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun TvShell(
    navigation: TvNavigation,
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    subscriptionsViewModel: SubscriptionsViewModel,
    searchViewModel: SearchViewModel,
    onPlayVideo: (Video) -> Unit,
    onPlayPlaylist: (videos: List<Video>, startIndex: Int, title: String, shuffle: Boolean) -> Unit,
    onPlayVideoId: (String) -> Unit,
    modifier: Modifier = Modifier,
    startSettings: SettingsTarget? = null,
) {
    LaunchedEffect(startSettings) { startSettings?.let { navController.navigate(TvRoutes.settingsPage(it)) } }
    val dimens = LocalTvDimens.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val rootRoutes = navigation.rootRoutes
    val isOnDetailRoute = currentRoute != null && currentRoute !in rootRoutes
    var lastRoot by remember { mutableStateOf(navigation.startTab.route) }
    LaunchedEffect(currentRoute) {
        if (currentRoute != null && currentRoute in rootRoutes) lastRoot = currentRoute
    }
    val currentRoot = currentRoute?.takeIf { it in rootRoutes } ?: lastRoot
    val bottomInsets =
        remember {
            ChorplayBottomInsets(
                systemInset = mutableStateOf(0.dp),
                barHeight = mutableStateOf(0.dp),
                barShown = mutableStateOf(false),
                miniPlayerHeight = mutableStateOf(0.dp),
                miniPlayerShown = mutableStateOf(false),
                barFraction = { 0f },
                miniPlayerFraction = { 0f },
            )
        }
    var railHasFocus by remember { mutableStateOf(false) }
    var railHoldsFocusForPage by remember { mutableStateOf(false) }
    var contentHasFocus by remember { mutableStateOf(false) }
    val railFocusRequester = remember { FocusRequester() }
    val pageFocusRequester = remember { FocusRequester() }
    val contentFocusRequester = remember { FocusRequester() }

    BackHandler(enabled = !isOnDetailRoute && currentRoot == navigation.startTab.route && !railHasFocus) {
        railFocusRequester.requestFocus(FocusDirection.Left)
    }

    fun navigateToRoot(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun selectRoot(route: String) {
        val popped =
            when {
                route == currentRoute -> {
                    TabScrollEventBus.emitScrollToTop(route)
                    true
                }

                route == currentRoot -> {
                    navController.popBackStack(route, inclusive = false)
                }

                route == navigation.startTab.route -> {
                    navController.popBackStack(route, inclusive = false, saveState = true)
                }

                else -> {
                    false
                }
            }
        if (!popped) navigateToRoot(route)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(start = dimens.railCollapsedWidth)
                    .onFocusChanged { contentHasFocus = it.hasFocus }
                    .onGloballyPositioned {
                        if ((!railHasFocus || railHoldsFocusForPage) && !contentHasFocus) contentFocusRequester.requestFocus()
                    }.focusRequester(contentFocusRequester)
                    .focusRestorer()
                    .focusGroup(),
        ) {
            CompositionLocalProvider(LocalChorplayBottomInsets provides bottomInsets) {
                TvNavHost(
                    navController = navController,
                    startRoute = navigation.startTab.route,
                    homeViewModel = homeViewModel,
                    subscriptionsViewModel = subscriptionsViewModel,
                    searchViewModel = searchViewModel,
                    onPlayVideo = onPlayVideo,
                    onPlayPlaylist = onPlayPlaylist,
                    onPlayVideoId = onPlayVideoId,
                    onSelectRoot = ::selectRoot,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .focusRequester(pageFocusRequester)
                            .focusRestorer()
                            .focusGroup(),
                )
            }
        }
        AnimatedVisibility(
            visible = railHasFocus,
            enter = fadeIn(MaterialTheme.motionScheme.defaultEffectsSpec()),
            exit = fadeOut(MaterialTheme.motionScheme.fastEffectsSpec()),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(WideNavigationRailDefaults.colors().modalScrimColor),
            )
        }
        TvNavRail(
            tabs = navigation.tabs,
            selectedRoute = currentRoot,
            onSelect = ::selectRoot,
            onFocusChanged = {
                railHasFocus = it
                if (!it) railHoldsFocusForPage = false
            },
            focusRequester = railFocusRequester,
            onOtherEntry = { contentFocusRequester.requestFocus().also { taken -> railHoldsFocusForPage = !taken } },
            profile = { expanded -> TvRailProfile(expanded, onOpenSettings = { navController.navigate(TvRoutes.settingsPage(it)) }) },
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .onPreviewKeyEvent {
                        railHoldsFocusForPage = false
                        false
                    },
        )
    }
}
