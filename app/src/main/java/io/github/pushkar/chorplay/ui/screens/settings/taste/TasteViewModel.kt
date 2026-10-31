package io.github.pushkar.chorplay.ui.screens.settings.taste

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.pushkar.chorplay.data.backup.BackupCoordinator
import io.github.pushkar.chorplay.data.local.PlayerPreferences
import io.github.pushkar.chorplay.data.local.dao.WatchHistoryDao
import io.github.pushkar.chorplay.data.recommendation.BlockedChannelNames
import io.github.pushkar.chorplay.data.recommendation.ChannelMemoryRepository
import io.github.pushkar.chorplay.data.recommendation.ContentVector
import io.github.pushkar.chorplay.data.recommendation.ChorplayNeuroEngine
import io.github.pushkar.chorplay.data.recommendation.ChorPersona
import io.github.pushkar.chorplay.data.recommendation.NeighbourChannelsRepository
import io.github.pushkar.chorplay.data.recommendation.TimeBucket
import io.github.pushkar.chorplay.data.recommendation.UserBrain
import io.github.pushkar.chorplay.data.recommendation.snapshot.HomeSnapshotStore
import io.github.pushkar.chorplay.data.stats.VideoStatsRecorder
import io.github.pushkar.chorplay.ui.components.shared.DeepChorplayState
import io.github.pushkar.chorplay.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Immutable
data class TasteTraits(
    val pacing: Float,
    val complexity: Float,
    val duration: Float,
    val live: Float,
    val breadth: Float,
) {
    val values: List<Float> get() = listOf(pacing, complexity, duration, live, breadth)
}

@Immutable
data class TasteGroup(
    val id: String,
    val label: String,
    val share: Float,
    val evidence: Int,
    val members: List<TasteTopic>,
)

@Immutable
data class TasteTopic(
    val id: String,
    val label: String,
    val share: Float?,
    val preferred: Boolean,
    val evidence: Int = 0,
)

@Immutable
data class TasteChannel(
    val id: String,
    val name: String,
    val score: Float,
)

@Immutable
data class NamedItem(
    val id: String,
    val name: String,
)

@Immutable
data class HiddenContent(
    val topics: List<String> = emptyList(),
    val channels: List<NamedItem> = emptyList(),
    val resolvingNames: Boolean = false,
) {
    val count: Int get() = topics.size + channels.size
}

@Immutable
data class TasteState(
    val loading: Boolean = true,
    val persona: ChorPersona? = null,
    val maturity: Float = 0f,
    val profile: TasteTraits? = null,
    val now: TasteTraits? = null,
    val picks: List<TasteTopic> = emptyList(),
    val topics: List<TasteTopic> = emptyList(),
    val groups: List<TasteGroup> = emptyList(),
    val channels: List<TasteChannel> = emptyList(),
    val hidden: HiddenContent = HiddenContent(),
    val remembered: List<NamedItem> = emptyList(),
    val engine: EngineDetails? = null,
)

@HiltViewModel
class TasteViewModel
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val videoStats: VideoStatsRecorder,
    private val watchHistoryDao: WatchHistoryDao,
    private val backup: BackupCoordinator,
    private val channelMemory: ChannelMemoryRepository,
    private val homeSnapshots: HomeSnapshotStore,
    private val neighbours: NeighbourChannelsRepository,
    private val preferences: PlayerPreferences,
    private val blockedNames: BlockedChannelNames,
) : SettingsViewModel() {
    private val _state = MutableStateFlow(TasteState())
    private var resolveJob: Job? = null
    val state: StateFlow<TasteState> = _state.asStateFlow()
    val deepChor: StateFlow<DeepChorplayState> =
        combine(
            preferences.deepChorActive,
            preferences.deepChorActivatedAt,
            preferences.deepChorExpireHours,
        ) { active, activatedAt, expireHours ->
            DeepChorplayState(active = active, activatedAt = activatedAt, expireHours = expireHours)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), DeepChorplayState())
    val operation = backup.operation

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            _state.value = withContext(Dispatchers.Default) { load() }
        }
    }

    fun setTopicPreferred(
        topic: String,
        preferred: Boolean,
    ) = act {
        if (preferred) ChorplayNeuroEngine.addPreferredTopic(
            context,
            topic
        ) else ChorplayNeuroEngine.removePreferredTopic(context, topic)
    }

    fun blockTopic(topic: String) = act { ChorplayNeuroEngine.addBlockedTopic(context, topic) }

    fun unblockTopic(topic: String) = act { ChorplayNeuroEngine.removeBlockedTopic(context, topic) }

    fun blockChannel(channelId: String) =
        act {
            ChorplayNeuroEngine.blockChannel(context, channelId)
            _state.value.channels
                .firstOrNull { it.id == channelId && it.name != channelId }
                ?.let { blockedNames.remember(channelId, it.name) }
        }

    fun unblockChannel(channelId: String) =
        act {
            ChorplayNeuroEngine.unblockChannel(context, channelId)
            blockedNames.forget(channelId)
        }

    fun resolveMissingNames() {
        if (resolveJob?.isActive == true) return
        resolveJob =
            viewModelScope.launch {
                val loaded = _state.first { !it.loading }
                val missing =
                    loaded.hidden.channels
                        .filter { it.name.isBlank() }
                        .map { it.id }
                if (missing.isEmpty()) return@launch
                _state.update { it.copy(hidden = it.hidden.copy(resolvingNames = true)) }
                try {
                    val blocked = ChorplayNeuroEngine.getSavedBrainSnapshot().blockedChannels
                    blockedNames.resolve(missing, blocked) { names ->
                        _state.update { state ->
                            val channels = state.hidden.channels.map { item ->
                                names[item.id]?.let { item.copy(name = it) } ?: item
                            }
                            state.copy(hidden = state.hidden.copy(channels = channels))
                        }
                    }
                } finally {
                    _state.update {
                        it.copy(
                            hidden = it.hidden.copy(
                                channels = hiddenChannelOrder(it.hidden.channels),
                                resolvingNames = false
                            ),
                        )
                    }
                }
            }
    }


    fun forgetChannel(channelId: String) = act { channelMemory.forget(channelId) }

    fun clearChannelMemory() = act { channelMemory.clear() }

    fun resetVideoProfile() =
        act {
            ChorplayNeuroEngine.resetBrain(context)
            preferences.setOnboardingDone(false)
            homeSnapshots.clear()
            neighbours.clear()
        }


    fun exportVideoProfile(uri: Uri) = backup.exportEngine(uri)

    fun importVideoProfile(uri: Uri) = backup.importEngine(uri)


    fun dismissOperation() = backup.dismiss()

    private fun act(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }
            _state.value = withContext(Dispatchers.Default) { load() }
        }
    }

    private suspend fun load(): TasteState {
        ChorplayNeuroEngine.initialize(context)
        val brain = ChorplayNeuroEngine.getSavedBrainSnapshot()
        val names = channelNames()
        return TasteState(
            loading = false,
            persona = ChorplayNeuroEngine.getPersona(brain),
            maturity = (brain.totalInteractions / MATURE_INTERACTIONS).coerceIn(0f, 1f),
            profile = brain.globalVector.traits(),
            now = (brain.timeVectors[TimeBucket.current()] ?: ContentVector()).traits(),
            picks = tastePicks(brain, ChorplayNeuroEngine.pickKeysByLabel(brain)),
            topics = tasteTopics(
                brain,
                TOPIC_COUNT,
                ChorplayNeuroEngine.pickKeys(brain),
                ChorplayNeuroEngine::specificity
            ),
            groups =
                tasteGroups(
                    brain,
                    runCatching { ChorplayNeuroEngine.interestClusters(brain) }.getOrDefault(emptyList()),
                    ChorplayNeuroEngine.pickKeys(brain),
                    ChorplayNeuroEngine::specificity,
                    GROUP_COUNT,
                ),
            channels = tasteChannels(brain, names),
            hidden = hidden(brain, names),
            remembered =
                runCatching { channelMemory.remembered() }
                    .getOrDefault(emptyList())
                    .map { NamedItem(it.channelId, it.name.ifBlank { names[it.channelId] ?: it.channelId }) },
            engine = EngineDetails.of(brain),
        )
    }

    private suspend fun tasteChannels(
        brain: UserBrain,
        names: Map<String, String>,
    ): List<TasteChannel> {
        val top =
            brain.channelScores.entries
                .filter { it.key !in brain.blockedChannels }
                .sortedByDescending { it.value }
                .take(CHANNEL_COUNT)
        val known = names + blockedNames.known(top.map { it.key }.filterTo(HashSet()) { it !in names })
        return top.map { TasteChannel(it.key, known[it.key] ?: it.key, it.value.toFloat()) }
    }

    private suspend fun hidden(
        brain: UserBrain,
        names: Map<String, String>,
    ): HiddenContent {
        val known = names + blockedNames.known(brain.blockedChannels)
        return HiddenContent(
            topics = brain.blockedTopics.sorted(),
            channels = hiddenChannelOrder(brain.blockedChannels.map { NamedItem(it, known[it].orEmpty()) }),
            resolvingNames = resolveJob?.isActive == true,
        )
    }

    private suspend fun channelNames(): Map<String, String> {
        val names = HashMap<String, String>()
        runCatching { watchHistoryDao.getChannelVideoCounts(NAME_LOOKUP_LIMIT) }
            .getOrDefault(emptyList())
            .forEach { if (it.channelName.isNotBlank()) names[it.channelId] = it.channelName }
        runCatching { videoStats.snapshot() }
            .getOrNull()
            ?.months
            ?.toSortedMap()
            ?.values
            ?.forEach { names.putAll(it.channelNames) }
        return names
    }

    private fun ContentVector.traits() =
        TasteTraits(
            pacing = pacing.toFloat(),
            complexity = complexity.toFloat(),
            duration = duration.toFloat(),
            live = isLive.toFloat(),
            breadth = (topics.size / BREADTH_TOPICS).coerceIn(0f, 1f),
        )

    private companion object {
        const val MATURE_INTERACTIONS = 250f
        const val BREADTH_TOPICS = 50f
        const val TOPIC_COUNT = 12
        const val GROUP_COUNT = 8
        const val CHANNEL_COUNT = 10
        const val GENRE_COUNT = 8
        const val NAME_LOOKUP_LIMIT = 2_000
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

internal fun hiddenChannelOrder(channels: List<NamedItem>): List<NamedItem> {
    val (named, unnamed) = channels.partition { it.name.isNotBlank() }
    return named.sortedBy { it.name.lowercase() } + unnamed.sortedBy { it.id }
}
