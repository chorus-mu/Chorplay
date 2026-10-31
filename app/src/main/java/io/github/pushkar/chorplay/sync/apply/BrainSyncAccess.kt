package io.github.pushkar.chorplay.sync.apply

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.pushkar.chorplay.data.recommendation.ChorplayNeuroEngine
import io.github.pushkar.chorplay.data.recommendation.NeuroKeySpec
import io.github.pushkar.chorplay.sync.canonical.BrainCounters
import io.github.pushkar.chorplay.sync.canonical.CanonicalBrain
import io.github.pushkar.chorplay.sync.canonical.GCounter
import io.github.pushkar.chorplay.sync.identity.Hlc
import io.github.pushkar.chorplay.sync.mapping.BrainMapper
import io.github.pushkar.chorplay.sync.merge.BrainCrdtState
import io.github.pushkar.chorplay.sync.merge.BrainCrdtStore
import io.github.pushkar.chorplay.sync.merge.BrainKeyRekey
import io.github.pushkar.chorplay.sync.merge.BrainMerger
import io.github.pushkar.chorplay.sync.merge.BrainVectorBlend
import io.github.pushkar.chorplay.sync.merge.CounterOwnership
import io.github.pushkar.chorplay.sync.merge.DeviceVectors
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The two stateful brain collections. Both keep a CRDT sidecar next to the engine's own file, and
 * both credit local counter growth to this device's HLC node id ([myNode]) on send and receive
 * alike: earlier builds credited the full device id on receive, so that entry stays frozen and is
 * still treated as this device's own.
 */
@Singleton
class BrainSyncAccess
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val brainCrdtStore: BrainCrdtStore,
    ) {
        private val neuroEngine: ChorplayNeuroEngine by lazy { ChorplayNeuroEngine.getInstance(context) }

        // --- ChorNeuro ---

        /** This device's snapshot, carrying the shared CRDT state, then one vector snapshot per peer. */
        suspend fun exportBrain(
            myNode: String,
            hlc: String,
        ): List<CanonicalBrain> {
            val local = parseLocalBrain(exportLocalBrainJson())
            val attributed = attributeLocalEdits(loadState(), myNode, local, hlc)
            val own = BrainMapper.toCanonical(local, myNode, hlc, attributed)
            val devices = withOwnVectors(attributed.deviceVectors, own)
            brainCrdtStore.save(attributed.copy(deviceVectors = devices))
            val peers =
                devices
                    .filterKeys { it != myNode }
                    .map { (deviceId, dv) ->
                        CanonicalBrain(
                            schema = own.schema,
                            deviceId = deviceId,
                            hlc = dv.hlc,
                            vectors = dv.vectors,
                            counters = BrainCounters(idfTotalDocuments = GCounter(mapOf(deviceId to dv.weight.coerceAtLeast(1)))),
                        )
                    }
            return (peers + own).sortedBy { it.deviceId }
        }

        /** CRDT-merge every snapshot the peer sent into the local brain, then persist + reload the engine. */
        suspend fun mergeBrain(
            remotes: List<CanonicalBrain>,
            senderId: String,
            myNode: String,
            myDeviceId: String,
            hlc: String,
        ) {
            if (remotes.isEmpty()) return
            val localJson = exportLocalBrainJson()
            val local = parseLocalBrain(localJson)
            val attributed = attributeLocalEdits(loadState(), myNode, local, hlc)
            val own = BrainMapper.toCanonical(local, myNode, hlc, attributed)
            val keys = neuroEngine.topicKeyFunction()
            val incoming = remotes.map { BrainKeyRekey.canonical(BrainMapper.normalizeIncoming(it), keys) }

            val counters =
                CounterOwnership.brain(
                    merged = incoming.fold(own, BrainMerger::merge),
                    local = own,
                    remotes = incoming,
                    own = setOf(myNode, myDeviceId),
                    sender = setOf(senderId, Hlc.nodeFromDeviceId(senderId)),
                )
            var devices = withOwnVectors(attributed.deviceVectors, own)
            for (snapshot in incoming) {
                devices =
                    BrainVectorBlend.upsert(
                        devices,
                        snapshot.deviceId,
                        DeviceVectors(snapshot.hlc, BrainVectorBlend.weightOf(snapshot), snapshot.vectors),
                    )
            }
            val merged = counters.copy(vectors = BrainVectorBlend.effective(devices))
            val mergedBrain = BrainMapper.writeBack(merged, local)
            neuroEngine.importBrainFromStream(ByteArrayInputStream(BrainMapper.serializeOver(localJson, mergedBrain)), afterSync = true)
            brainCrdtStore.save(BrainCrdtState.afterMerge(attributed, merged, devices))
        }

        /** The language this brain's topic keys are stemmed for. */
        suspend fun keyLanguage(): String = neuroEngine.keyLanguage()

        /** The sidecar, its topic keys read the way this brain reads them now. */
        private suspend fun loadState(): BrainCrdtState {
            val state = brainCrdtStore.load()
            val language = neuroEngine.keyLanguage()
            if (state.keySpec == NeuroKeySpec.VERSION && state.keyLanguage == language) return state
            return BrainKeyRekey.state(state, neuroEngine.topicKeyFunction()).copy(keySpec = NeuroKeySpec.VERSION, keyLanguage = language)
        }

        private fun withOwnVectors(
            devices: Map<String, DeviceVectors>,
            own: CanonicalBrain,
        ): Map<String, DeviceVectors> = devices + (own.deviceId to DeviceVectors(own.hlc, BrainVectorBlend.weightOf(own), own.vectors))

        private suspend fun exportLocalBrainJson(): ByteArray {
            var exported = false
            val bytes =
                ByteArrayOutputStream().use { bos ->
                    exported = neuroEngine.exportBrainToStream(bos)
                    bos.toByteArray()
                }
            if (!exported) throw IllegalStateException("could not read the local ChorNeuro brain")
            return bytes
        }

        private fun parseLocalBrain(bytes: ByteArray): BrainMapper.SBrain =
            runCatching { BrainMapper.parse(bytes) }
                .getOrElse { throw IllegalStateException("the local ChorNeuro brain could not be parsed", it) }

        /**
         * Fold everything that changed locally since the last sync into the sidecar: counter growth
         * becomes this device's G-Counter sub-count, and blocklist/preference edits become OR-Set add
         * or remove stamps. Both are diffs against the last-synced state, so re-running with no local
         * activity is a no-op.
         */
        private fun attributeLocalEdits(
            state: BrainCrdtState,
            myNode: String,
            brain: BrainMapper.SBrain,
            hlc: String,
        ): BrainCrdtState {
            val withCounters =
                BrainCrdtState.attributeLocal(
                    state = state,
                    myDevice = myNode,
                    idfDocsScalar = brain.idfTotalDocuments.toLong(),
                    interactionsScalar = brain.interactions.toLong(),
                    idfWordCounts = brain.idfWordFrequency.mapValues { it.value.toLong() },
                )
            return BrainCrdtState.attributeSets(
                state = withCounters,
                blockedTopics = brain.blockedTopics,
                blockedChannels = brain.blockedChannels,
                preferredTopics = brain.preferredTopics,
                hlc = hlc,
            )
        }


    }
