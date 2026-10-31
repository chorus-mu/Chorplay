package io.github.pushkar.chorplay.ui.screens.settings.integrations

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh

import androidx.compose.material.icons.outlined.ThumbDownOffAlt
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.pushkar.chorplay.BuildConfig
import io.github.pushkar.chorplay.R

import io.github.pushkar.chorplay.ui.components.settings.SettingsPage
import io.github.pushkar.chorplay.ui.components.settings.SettingsTarget
import io.github.pushkar.chorplay.ui.components.settings.nav
import io.github.pushkar.chorplay.ui.components.settings.switch

import io.github.pushkar.chorplay.ui.screens.settings.index.IntegrationsIndex

@Composable
internal fun IntegrationsScreen(
    onBack: (() -> Unit)?,
    highlight: String?,
    onNavigate: (SettingsTarget) -> Unit,
    viewModel: IntegrationsViewModel = hiltViewModel(),
) {
    val sponsorBlock by viewModel.sponsorBlock.collectAsStateWithLifecycle()
    val submitButton by viewModel.submitButton.collectAsStateWithLifecycle()
    val userId by viewModel.userId.collectAsStateWithLifecycle()
    val segments by viewModel.segments.collectAsStateWithLifecycle()
    val deArrow by viewModel.deArrow.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var dialog by rememberSaveable { mutableStateOf<IntegrationsDialog?>(null) }
    var colourCategory by rememberSaveable { mutableStateOf<String?>(null) }


    SettingsPage(
        title = stringResource(R.string.settings_integrations_title),
        onBack = onBack,
        highlight = highlight,
        snackbarHostState = snackbarHostState,
    ) {
        sponsorBlockSection(
            viewModel = viewModel,
            enabled = sponsorBlock,
            submitButton = submitButton,
            userId = userId,
            segments = segments,
            onEditUserId = { dialog = IntegrationsDialog.USER_ID },
            onPickColour = { colourCategory = it },
        )
        group(key = "integrations.dearrow.group", header = R.string.player_settings_dearrow) {
            switch(
                IntegrationsIndex.deArrow,
                viewModel.deArrow,
                viewModel::setDeArrow,
                icon = Icons.Outlined.AutoFixHigh
            )
            switch(
                IntegrationsIndex.deArrowRandomFrame,
                viewModel.deArrowRandomFrame,
                viewModel::setDeArrowRandomFrame,
                enabled = deArrow
            )
            switch(
                IntegrationsIndex.deArrowBadge,
                viewModel.deArrowBadge,
                viewModel::setDeArrowBadge,
                enabled = deArrow
            )
        }
        group(key = "integrations.dislikes.group", header = R.string.player_settings_rytd_title) {
            switch(
                IntegrationsIndex.dislikes,
                viewModel.dislikes,
                viewModel::setDislikes,
                icon = Icons.Outlined.ThumbDownOffAlt
            )
        }
    }

    IntegrationsDialogs(
        dialog = dialog,
        userId = userId,
        viewModel = viewModel,
        onDismiss = { dialog = null },
    )
    colourCategory?.let { category ->
        SegmentColourDialog(
            category = category,
            current = segments[category]?.colorArgb,
            onSelect = { viewModel.setSegmentColor(category, it) },
            onDismiss = { colourCategory = null },
        )
    }
}
