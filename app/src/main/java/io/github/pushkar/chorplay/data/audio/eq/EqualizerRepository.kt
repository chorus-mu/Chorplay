package io.github.pushkar.chorplay.data.audio.eq

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EqualizerRepository internal constructor(
    private val persistence: EqStatePersistence,
    private val scope: CoroutineScope,
    computeDispatcher: CoroutineDispatcher,
) {
    @Inject
    constructor(persistence: EqStatePersistence) : this(
        persistence,
        CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        Dispatchers.Default,
    )

    private val _state = MutableStateFlow(EqState())
    val state: StateFlow<EqState> = _state.asStateFlow()

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded.asStateFlow()

    private val preview = MutableStateFlow<EqCurve?>(null)
    private val bypass = MutableStateFlow(false)
    private var saveJob: Job? = null
    private val earlyEdits = mutableListOf<(EqState) -> EqState>()

    val processingSpec: StateFlow<EqProcessingSpec> =
        combine(_state, preview, bypass) { state, previewCurve, bypassed ->
            state.processingSpec(bypass = bypassed, preview = previewCurve)
        }.conflate()
            .distinctUntilChanged()
            .flowOn(computeDispatcher)
            .stateIn(scope, SharingStarted.Eagerly, EqProcessingSpec.OFF)

    val needsProcessing: StateFlow<Boolean> =
        _state
            .map { it.changesSound }
            .distinctUntilChanged()
            .stateIn(scope, SharingStarted.Eagerly, false)

    init {
        scope.launch {
            val restored = persistence.load()
            if (_loaded.value) return@launch
            val early = synchronized(earlyEdits) { earlyEdits.toList().also { earlyEdits.clear() } }
            _state.value = early.fold(restored) { state, edit -> edit(state) }
            _loaded.value = true
            if (early.isNotEmpty()) scheduleSave()
        }
    }

    fun update(transform: (EqState) -> EqState) {
        if (!_loaded.value) {
            synchronized(earlyEdits) { earlyEdits += transform }
            _state.value = transform(_state.value)
            return
        }
        val before = _state.value
        val after = transform(before)
        if (after == before) return
        _state.value = after
        preview.value = null
        scheduleSave()
    }

    fun restore(state: EqState) = update { state }

    fun preview(curve: EqCurve?) {
        preview.value = curve?.sanitized()
    }

    fun setBypass(bypassed: Boolean) {
        bypass.value = bypassed
    }

    suspend fun exportJson(): String {
        _loaded.first { it }
        return EqStateJson.encode(_state.value)
    }

    suspend fun importJson(raw: String): Boolean {
        val decoded = EqStateJson.decode(raw) ?: return false
        _loaded.value = true
        _state.value = decoded
        saveJob?.cancel()
        persistence.save(decoded)
        return true
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob =
            scope.launch {
                delay(SAVE_DEBOUNCE_MS)
                persistence.save(_state.value)
            }
    }

    private companion object {
        const val SAVE_DEBOUNCE_MS = 400L
    }
}

@Module
@InstallIn(SingletonComponent::class)
internal interface EqualizerModule {
    @Binds
    fun bindPersistence(store: EqStateStore): EqStatePersistence
}
