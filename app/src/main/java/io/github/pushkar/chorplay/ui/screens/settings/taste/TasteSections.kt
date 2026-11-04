package io.github.pushkar.chorplay.ui.screens.settings.taste

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.pushkar.chorplay.R
import io.github.pushkar.chorplay.ui.components.settings.SettingsDestination
import io.github.pushkar.chorplay.ui.components.settings.SettingsListScope
import io.github.pushkar.chorplay.ui.components.settings.info
import io.github.pushkar.chorplay.ui.components.settings.nav
import io.github.pushkar.chorplay.ui.screens.settings.index.DestinationIndex
import io.github.pushkar.chorplay.ui.screens.settings.index.TasteIndex

private const val STRONG_CHANNEL = 0.65f
private const val BALANCED_CHANNEL = 0.4f
private const val OFTEN_WATCHED = 8
private const val SOMETIMES_WATCHED = 3

internal class TasteActions(
    val onTopicPreferred: (String, Boolean) -> Unit,
    val onBlockTopic: (String) -> Unit,
    val onBlockChannel: (String) -> Unit,
    val onForgetChannel: (String) -> Unit,
    val onClearChannelMemory: () -> Unit,
    val onOpenHidden: () -> Unit,
    val onOpenDiscoverChannels: () -> Unit,
    val onOpenRecap: () -> Unit,
    val onExportVideo: () -> Unit,
    val onImportVideo: () -> Unit,
    val onResetVideo: () -> Unit,
)

internal fun SettingsListScope.tasteContent(
    state: TasteState,
    hiddenLabel: String,
    noQueriesLabel: String,
    startersLabel: String?,
    actions: TasteActions,
) {
    state.persona?.let { persona -> item("taste.persona") { PersonaHeroCard(persona, state.maturity) } }
    if (state.profile != null && state.now != null) traits(state.profile, state.now)
    picks(state.picks, actions)
    interests(state.groups, actions)
    channels(state.channels, actions)
    remembered(state.remembered, actions)
    group(key = "taste.more") {
        nav(TasteIndex.recap, onClick = actions.onOpenRecap, icon = Icons.Outlined.Insights)
        nav(TasteIndex.hidden, onClick = actions.onOpenHidden, value = hiddenLabel, icon = Icons.Outlined.VisibilityOff)
        nav(
            DestinationIndex.entry(SettingsDestination.DISCOVER_CHANNELS),
            onClick = actions.onOpenDiscoverChannels,
            icon = Icons.Outlined.PersonSearch,
        )
    }
    state.engine?.let { engineDetails(it, noQueriesLabel, startersLabel) }
    data(actions)
}

internal fun SettingsListScope.learningPaused(
    title: String,
    status: String,
) {
    group(key = "taste.deep_chor") {
        info(key = "taste.deep_chor.status", title = title, value = status, icon = Icons.Outlined.VisibilityOff)
    }
}

private fun SettingsListScope.engineDetails(
    details: EngineDetails,
    noQueries: String,
    starters: String?,
) {
    group(key = "taste.engine", header = R.string.diagnostics_engine_header) {
        info(TasteIndex.engineInteractions, details.interactions.toString())
        info(TasteIndex.engineTopics, details.topics.toString())
        info(TasteIndex.engineChannels, details.channels.toString())
        info(TasteIndex.engineHistory, details.history.toString())
        info(TasteIndex.engineFeedMemory, details.feedMemory.toString())
        info(TasteIndex.engineSuppressed, details.suppressed.toString())
        info(TasteIndex.engineShortsSeen, details.shortsSeen.toString())
        info(TasteIndex.engineQueries, details.recentQueries.joinToString("\n").ifEmpty { noQueries })
        starters?.let { info(TasteIndex.engineStarters, it) }
    }
}

private fun SettingsListScope.traits(
    profile: TasteTraits,
    now: TasteTraits,
) {
    header("taste.shape.header", R.string.taste_shape_header)
    item(TasteIndex.shape.key) { TasteShapeCard(profile, now) }
    val rows =
        listOf(
            R.string.taste_pacing to (profile.pacing to now.pacing),
            R.string.taste_complexity to (profile.complexity to now.complexity),
            R.string.taste_duration to (profile.duration to now.duration),
            R.string.taste_live_affinity to (profile.live to now.live),
            R.string.taste_topic_breadth to (profile.breadth to now.breadth),
        )
    group(key = "taste.traits") {
        rows.forEach { (title, values) ->
            row("taste.trait.$title") { shape ->
                TasteMeterRow(
                    title = stringResource(title),
                    value = values.first,
                    shape = shape,
                    detail = stringResource(R.string.taste_trait_now, values.second.percentLabel()),
                    trailing = { Text(values.first.percentLabel()) },
                )
            }
        }
    }
}

@StringRes
private fun evidenceLabel(evidence: Int): Int =
    when {
        evidence >= OFTEN_WATCHED -> R.string.taste_evidence_often
        evidence >= SOMETIMES_WATCHED -> R.string.taste_evidence_some
        evidence > 0 -> R.string.taste_evidence_once
        else -> R.string.taste_evidence_none
    }

private fun SettingsListScope.picks(
    picks: List<TasteTopic>,
    actions: TasteActions,
) {
    if (picks.isEmpty()) return
    group(key = "taste.picks", header = R.string.taste_picks_header) {
        picks.forEach { pick ->
            row("taste.pick.${pick.id}") { shape ->
                TasteMeterRow(
                    title = pick.label,
                    value = null,
                    shape = shape,
                    detail = stringResource(R.string.taste_topic_boosted) + " · " + stringResource(evidenceLabel(pick.evidence)),
                    trailing = {
                        ItemMenu(
                            label = pick.label,
                            options =
                                listOf(
                                    stringResource(R.string.taste_topic_unboost) to {
                                        actions.onTopicPreferred(
                                            pick.id,
                                            false
                                        )
                                    },
                                    stringResource(R.string.taste_topic_block) to { actions.onBlockTopic(pick.id) },
                                ),
                        )
                    },
                )
            }
        }
    }
}

private fun SettingsListScope.interests(
    groups: List<TasteGroup>,
    actions: TasteActions,
) {
    if (groups.isEmpty()) {
        header(TasteIndex.interests.key, R.string.taste_interests_header)
        item("taste.interests.empty") { TasteNote(stringResource(R.string.taste_interests_empty)) }
        return
    }
    group(
        key = TasteIndex.interests.key,
        header = R.string.taste_interests_header,
        footer = R.string.taste_interests_footer
    ) {
        groups.forEach { group ->
            row("taste.group.${group.id}") { shape ->
                val named = group.members.take(GROUP_MENU_WORDS)
                TasteMeterRow(
                    title = group.label,
                    value = group.share,
                    shape = shape,
                    detail = stringResource(evidenceLabel(group.evidence)),
                    trailing = {
                        ItemMenu(
                            label = group.label,
                            options =
                                listOf(stringResource(R.string.taste_topic_boost) to {
                                    actions.onTopicPreferred(
                                        named.first().id,
                                        true
                                    )
                                }) +
                                        named.map { topic ->
                                            stringResource(
                                                R.string.taste_topic_block_named,
                                                topic.label
                                            ) to { actions.onBlockTopic(topic.id) }
                                        },
                        )
                    },
                )
            }
        }
    }
}

private fun SettingsListScope.channels(
    channels: List<TasteChannel>,
    actions: TasteActions,
) {
    group(key = TasteIndex.channels.key, header = R.string.taste_channels_header) {
        channels.forEach { channel ->
            row("taste.channel.${channel.id}") { shape ->
                TasteMeterRow(
                    title = channel.name,
                    value = channel.score,
                    shape = shape,
                    detail = stringResource(channelStrength(channel.score)),
                    trailing = {
                        ItemMenu(
                            label = channel.name,
                            options = listOf(stringResource(R.string.taste_channel_block) to {
                                actions.onBlockChannel(
                                    channel.id
                                )
                            }),
                        )
                    },
                )
            }
        }
    }
}

private fun SettingsListScope.remembered(
    channels: List<NamedItem>,
    actions: TasteActions,
) {
    if (channels.isEmpty()) {
        header(TasteIndex.remembered.key, R.string.taste_remembered_header)
        item("taste.remembered.empty") { TasteNote(stringResource(R.string.taste_remembered_empty)) }
        return
    }
    group(
        key = TasteIndex.remembered.key,
        header = R.string.taste_remembered_header,
        footer = R.string.taste_remembered_footer
    ) {
        channels.forEach { channel ->
            row("taste.remembered.${channel.id}") { shape ->
                TasteActionRow(
                    name = channel.name,
                    shape = shape,
                    actionLabel = stringResource(R.string.taste_remembered_remove),
                    actionDescription = stringResource(R.string.taste_remembered_remove_item, channel.name),
                ) { actions.onForgetChannel(channel.id) }
            }
        }
        nav(
            TasteIndex.clearRemembered,
            onClick = actions.onClearChannelMemory,
            showChevron = false,
            icon = Icons.Outlined.ClearAll
        )
    }
}

private fun channelStrength(score: Float): Int =
    when {
        score >= STRONG_CHANNEL -> R.string.channel_affinity_strong
        score >= BALANCED_CHANNEL -> R.string.channel_affinity_balanced
        else -> R.string.channel_affinity_cooling
    }

private fun SettingsListScope.data(actions: TasteActions) {
    group(key = "taste.data", header = R.string.taste_data_header) {
        nav(TasteIndex.exportVideo, onClick = actions.onExportVideo, showChevron = false, icon = Icons.Outlined.Upload)
        nav(TasteIndex.importVideo, onClick = actions.onImportVideo, showChevron = false, icon = Icons.Outlined.Download)
        nav(TasteIndex.resetVideo, onClick = actions.onResetVideo, showChevron = false, icon = Icons.Outlined.RestartAlt)
    }
}

private const val GROUP_MENU_WORDS = 3

@Composable
private fun ItemMenu(
    label: String,
    options: List<Pair<String, () -> Unit>>,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.taste_item_actions, label))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEach { (text, action) ->
                DropdownMenuItem(
                    text = { Text(text) },
                    onClick = {
                        open = false
                        action()
                    },
                )
            }
        }
    }
}

@Composable
internal fun hiddenCountLabel(count: Int): String =
    if (count == 0) stringResource(R.string.taste_hidden_none) else pluralStringResource(
        R.plurals.taste_hidden_count,
        count,
        count
    )
