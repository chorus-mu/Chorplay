package io.github.pushkar.chorplay.ui.screens.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.data.audio.eq.EqLimits
import io.github.pushkar.chorplay.data.audio.eq.EqMode
import io.github.pushkar.chorplay.data.audio.eq.EqState
import io.github.pushkar.chorplay.data.audio.eq.GraphicEq
import io.github.pushkar.chorplay.data.audio.eq.canSaveActive
import io.github.pushkar.chorplay.data.audio.eq.isEdited
import io.github.pushkar.chorplay.data.audio.eq.processingSpec
import io.github.pushkar.chorplay.ui.components.equalizer.EqBandEditor
import io.github.pushkar.chorplay.ui.components.equalizer.EqBandRow
import io.github.pushkar.chorplay.ui.components.equalizer.EqBassBoostSlider
import io.github.pushkar.chorplay.ui.components.equalizer.EqGraphicBands
import io.github.pushkar.chorplay.ui.components.equalizer.EqResponseGraph
import io.github.pushkar.chorplay.ui.components.equalizer.PanelRow
import io.github.pushkar.chorplay.ui.components.equalizer.activePresetName
import io.github.pushkar.chorplay.ui.components.equalizer.formatGain
import io.github.pushkar.chorplay.ui.components.layout.chorBottomContentPadding
import io.github.pushkar.chorplay.ui.components.shared.ChorplayConnectedToggleGroup
import io.github.pushkar.chorplay.ui.components.shared.ChorMaxContentWidth
import io.github.pushkar.chorplay.ui.components.shared.ChorNavRow
import io.github.pushkar.chorplay.ui.components.shared.ChorPaneState
import io.github.pushkar.chorplay.ui.components.shared.ChorRowGroup
import io.github.pushkar.chorplay.ui.components.shared.ChorSectionHeader
import io.github.pushkar.chorplay.ui.components.shared.ChorSegmentedGap
import io.github.pushkar.chorplay.ui.components.shared.ChorplaySidePanes
import io.github.pushkar.chorplay.ui.components.shared.ChorplaySwitchRow
import io.github.pushkar.chorplay.ui.components.shared.ChorToggleOption
import io.github.pushkar.chorplay.ui.components.shared.chorRowGroupShape

private val ItemInset = 12.dp
private val PhoneGraphHeight = 220.dp
private val PaneGraphHeight = 320.dp
private val SidePaneWidth = 440.dp
private val BottomSpacing = 32.dp
private val PanelPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)

/**
 * The equalizer page body. On a phone it is one list and a band opens in a sheet; beside a second
 * pane the curve and levels sit on the leading side and the bands, with the chosen band's controls,
 * on the other.
 */
@Composable
internal fun EqualizerMainContent(
    state: EqState,
    viewModel: EqualizerViewModel,
    selectedBand: Int?,
    onSelectBand: (Int?) -> Unit,
    panes: ChorPaneState,
    twoPane: Boolean,
    onOpenPresets: () -> Unit,
    onSaveAs: () -> Unit,
    onSystemEqualizer: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val bottom = chorBottomContentPadding(BottomSpacing)
    val list: @Composable (LazyListScope.() -> Unit) -> Unit = { content ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(ChorSegmentedGap),
            contentPadding = PaddingValues(bottom = bottom),
            content = content,
        )
    }
    val section = EqSections(state, viewModel, selectedBand, onSelectBand, onOpenPresets, onSaveAs, onSystemEqualizer)

    if (!twoPane) {
        Box(modifier) {
            list {
                with(section) {
                    header()
                    curve(PhoneGraphHeight)
                    mode()
                    if (state.mode == EqMode.PARAMETRIC) bands() else graphic()
                    sound()
                    system()
                }
            }
        }
        return
    }
    ChorplaySidePanes(
        panes = panes,
        sidePaneWidth = SidePaneWidth,
        modifier = modifier,
        sidePane = {
            list {
                with(section) {
                    header()
                    curve(PaneGraphHeight)
                    mode()
                    sound()
                    system()
                }
            }
        },
        mainPane = {
            list {
                with(section) {
                    if (state.mode == EqMode.PARAMETRIC) {
                        selectedEditor()
                        bands()
                    } else {
                        graphic()
                    }
                }
            }
        },
    )
}

/** Each part of the page as list items, shared by the one-pane and two-pane layouts. */
private class EqSections(
    val state: EqState,
    val viewModel: EqualizerViewModel,
    val selectedBand: Int?,
    val onSelectBand: (Int?) -> Unit,
    val onOpenPresets: () -> Unit,
    val onSaveAs: () -> Unit,
    val onSystemEqualizer: (() -> Unit)?,
) {
    private val bandList = state.active.curve.bands
    private val parametric = state.mode == EqMode.PARAMETRIC

    fun LazyListScope.header() {
        item("header") {
            EqFramed(Modifier.padding(top = 8.dp)) {
                ChorRowGroup {
                    ChorplaySwitchRow(
                        title = stringResource(R.string.equalizer),
                        checked = state.enabled,
                        onCheckedChange = viewModel::setEnabled,
                        leadingIcon = Icons.Rounded.GraphicEq,
                        shape = chorRowGroupShape(0, 2),
                    )
                    val edited = state.isEdited
                    ChorNavRow(
                        title = activePresetName(state),
                        supportingText = stringResource(if (edited) R.string.eq_edited else R.string.eq_presets),
                        onClick = onOpenPresets,
                        leadingIcon = Icons.Outlined.LibraryMusic,
                        shape = chorRowGroupShape(1, 2),
                        trailingContent =
                            if (edited) {
                                {
                                    Row {
                                        if (state.active.presetId != null) {
                                            TextButton(onClick = viewModel::revert) { Text(stringResource(R.string.eq_revert)) }
                                        }
                                        if (state.canSaveActive) {
                                            TextButton(onClick = viewModel::save) { Text(stringResource(R.string.save)) }
                                        } else {
                                            TextButton(onClick = onSaveAs) { Text(stringResource(R.string.eq_save_as)) }
                                        }
                                    }
                                }
                            } else {
                                null
                            },
                    )
                }
            }
        }
    }

    fun LazyListScope.curve(height: Dp) {
        item("curve") {
            EqFramed(Modifier.padding(horizontal = ItemInset, vertical = 8.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    EqResponseGraph(
                        bands = bandList,
                        bassBoost = state.bassBoost,
                        selectedIndex = selectedBand,
                        showPoints = parametric,
                        editable = parametric,
                        onBandClick = { onSelectBand(it) },
                        onAddBand = { frequency, gain -> viewModel.addBand(frequency, gain)?.let(onSelectBand) },
                        onBandPreview = viewModel::previewBand,
                        onBandCommit = viewModel::updateBand,
                        modifier = Modifier.fillMaxWidth().height(height),
                    )
                    if (parametric && bandList.isEmpty()) {
                        Text(
                            text = stringResource(R.string.eq_graph_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }
                }
            }
        }
    }

    fun LazyListScope.mode() {
        item("mode") {
            EqFramed(Modifier.padding(horizontal = ItemInset)) {
                ChorplayConnectedToggleGroup(
                    options =
                        listOf(
                            ChorToggleOption(EqMode.PARAMETRIC, stringResource(R.string.eq_mode_parametric), Icons.Rounded.Tune),
                            ChorToggleOption(EqMode.GRAPHIC, stringResource(R.string.eq_mode_graphic), Icons.Rounded.Equalizer),
                        ),
                    selected = state.mode,
                    onSelected = viewModel::setMode,
                )
            }
        }
    }

    fun LazyListScope.bands() {
        item("bands-header") {
            EqFramed {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ChorSectionHeader(stringResource(R.string.eq_bands), Modifier.weight(1f))
                    Text(
                        text = stringResource(R.string.eq_band_count, bandList.size, EqLimits.MAX_BANDS),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 20.dp, top = 14.dp),
                    )
                }
            }
        }
        itemsIndexed(bandList, key = { index, _ -> "band-$index" }) { index, band ->
            EqFramed(Modifier.padding(horizontal = ItemInset)) {
                EqBandRow(
                    index = index,
                    band = band,
                    shape = chorRowGroupShape(index, bandList.size + 1),
                    selected = selectedBand == index,
                    onClick = { onSelectBand(index) },
                    onToggle = { viewModel.toggleBand(index) },
                )
            }
        }
        item("add-band") {
            EqFramed(Modifier.padding(horizontal = ItemInset)) {
                ChorNavRow(
                    title = stringResource(R.string.eq_add_band),
                    onClick = { viewModel.addBand()?.let(onSelectBand) },
                    leadingIcon = Icons.Rounded.Add,
                    showChevron = false,
                    enabled = bandList.size < EqLimits.MAX_BANDS,
                    shape = chorRowGroupShape(bandList.size, bandList.size + 1),
                )
            }
        }
    }

    fun LazyListScope.selectedEditor() {
        val index = selectedBand ?: return
        if (index !in bandList.indices) return
        item("editor") {
            EqFramed(Modifier.padding(horizontal = ItemInset, vertical = 8.dp)) {
                PanelRow(shape = MaterialTheme.shapes.large) {
                    EqBandEditor(
                        index = index,
                        bands = bandList,
                        bassBoost = state.bassBoost,
                        showGraph = false,
                        onChange = { viewModel.updateBand(index, it) },
                        onRemove = {
                            onSelectBand(null)
                            viewModel.removeBand(index)
                        },
                        modifier = Modifier.padding(PanelPadding),
                    )
                }
            }
        }
    }

    fun LazyListScope.graphic() {
        item("graphic") {
            EqFramed(Modifier.padding(horizontal = ItemInset, vertical = 8.dp)) {
                PanelRow(shape = MaterialTheme.shapes.large) {
                    EqGraphicBands(
                        gains = GraphicEq.gainsOf(state.graphic.curve),
                        onGainChange = viewModel::setGraphicGain,
                        modifier = Modifier.padding(PanelPadding),
                    )
                }
            }
        }
    }

    fun LazyListScope.sound() {
        item("sound") {
            EqFramed {
                Column {
                    ChorSectionHeader(stringResource(R.string.eq_settings_section))
                    val rows = if (state.autoPreamp) 2 else 3
                    ChorRowGroup {
                        ChorplaySwitchRow(
                            title = stringResource(R.string.eq_auto_preamp),
                            supportingText = autoPreampSummary(state),
                            checked = state.autoPreamp,
                            onCheckedChange = viewModel::setAutoPreamp,
                            shape = chorRowGroupShape(0, rows),
                        )
                        if (!state.autoPreamp) {
                            PanelRow(shape = chorRowGroupShape(1, rows)) {
                                ManualPreamp(value = state.active.curve.preamp, onValueChange = viewModel::setPreamp)
                            }
                        }
                        PanelRow(shape = chorRowGroupShape(rows - 1, rows)) {
                            EqBassBoostSlider(
                                value = state.bassBoost,
                                onValueChange = viewModel::setBassBoost,
                                modifier = Modifier.padding(PanelPadding),
                            )
                        }
                    }
                }
            }
        }
    }

    fun LazyListScope.system() {
        val open = onSystemEqualizer ?: return
        item("system") {
            EqFramed(Modifier.padding(top = 16.dp)) {
                ChorRowGroup {
                    ChorNavRow(
                        title = stringResource(R.string.eq_system),
                        supportingText = stringResource(R.string.eq_system_desc),
                        onClick = open,
                        leadingIcon = Icons.AutoMirrored.Outlined.OpenInNew,
                        showChevron = false,
                        shape = chorRowGroupShape(0, 1),
                    )
                }
            }
        }
    }
}

@Composable
private fun autoPreampSummary(state: EqState): String {
    val preamp =
        remember(state.active.curve, state.bassBoost) {
            state.copy(enabled = true, autoPreamp = true).processingSpec(bypass = false).preampDb
        }
    return if (preamp < -AUDIBLE_DB) {
        stringResource(R.string.eq_auto_preamp_active, formatGain(preamp), formatGain(-preamp))
    } else {
        stringResource(R.string.eq_auto_preamp_idle)
    }
}

@Composable
private fun ManualPreamp(
    value: Double,
    onValueChange: (Double) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(PanelPadding)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.eq_preamp), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(formatGain(value), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toDouble()) },
            valueRange = EqLimits.MIN_PREAMP.toFloat()..EqLimits.MAX_PREAMP.toFloat(),
        )
    }
}

/** Centres [content] and caps it at the app's reading width, as the settings pages do. */
@Composable
internal fun EqFramed(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Box(modifier = Modifier.widthIn(max = ChorMaxContentWidth).fillMaxWidth().then(modifier)) { content() }
    }
}

private const val AUDIBLE_DB = 0.05
