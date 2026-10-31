package io.github.pushkar.chorplay.data.recommendation

import io.github.pushkar.chorplay.data.model.Video
import java.util.Calendar

internal class NeuroDiscovery(
    private val topicCategories: List<TopicCategory>,
    private val tokenizer: NeuroTokenizer,
) {
    companion object {
        val STARTER_QUERIES = listOf("Music", "Science", "Technology", "Education", "Nature")
        private const val SPECIFIC_QUERY_MAX_WORDS = 4
        private const val QUERY_MIN_SPECIFICITY = 0.65
        private const val PHRASE_SHARE = 0.5
        private const val FORMAT_QUERY_MIN_AFFINITY = 0.2
    }

    private data class MatureTopic(
        val name: String,
        val score: Double,
        val maturityLevel: TopicMaturity,
        val categorySupport: Int,
        val hasTimeContext: Boolean,
        val hasDiscoveryEvidence: Boolean,
        val clusterKey: String? = null,
    )

    private enum class TopicMaturity {
        EMERGING,
        DEVELOPING,
        ESTABLISHED,
        CORE,
    }

    private fun analyzeMatureTopics(
        brain: UserBrain,
        timeTopics: Set<String>,
    ): List<MatureTopic> {
        val allTopics = brain.interests.topics
        if (allTopics.isEmpty()) return emptyList()

        return allTopics.entries
            .filter { isSubstantialTopic(it.key) }
            .map { (name, score) ->
                val maturity =
                    when {
                        score >= 0.70 -> TopicMaturity.CORE
                        score >= 0.40 -> TopicMaturity.ESTABLISHED
                        score >= 0.20 -> TopicMaturity.DEVELOPING
                        else -> TopicMaturity.EMERGING
                    }

                val categorySupport =
                    topicCategories.count { cat ->
                        val catTopics = cat.topics.map { tokenizer.keyOf(it) }
                        catTopics.contains(name) &&
                                catTopics.count { it in allTopics } >= 2
                    }

                MatureTopic(
                    name = name,
                    score = score,
                    maturityLevel = maturity,
                    categorySupport = categorySupport,
                    hasTimeContext = name in timeTopics,
                    hasDiscoveryEvidence = hasDiscoveryEvidence(name, brain),
                )
            }.sortedWith(
                compareByDescending<MatureTopic> { it.maturityLevel.ordinal }
                    .thenByDescending { it.score }
                    .thenByDescending { it.categorySupport },
            )
    }

    private data class ClusterChoice(
        val topic: MatureTopic,
        val comboWith: String?,
        val specificQuery: String? = null,
    )

    private data class TopicSelection(
        val choices: List<ClusterChoice>,
        val emerging: List<MatureTopic>,
        val crossCategory: List<MatureTopic>,
    ) {
        val primary: List<MatureTopic> get() = choices.take(1).map { it.topic }
        val secondary: List<MatureTopic> get() = choices.drop(1).map { it.topic }

        fun allTopics(): List<MatureTopic> =
            (choices.map { it.topic } + emerging + crossCategory).distinctBy { it.name }

        fun uniqueTopicCount(): Int = allTopics().map { it.name }.distinct().size
    }

    private fun emptySelection() = TopicSelection(emptyList(), emptyList(), emptyList())

    private fun specificMember(
        anchorName: String,
        members: List<MatureTopic>,
    ): String? {
        val anchor = NeuroScoring.stripDomainTag(anchorName)
        if (' ' in anchor) return null
        val others =
            members
                .drop(1)
                .map { NeuroScoring.stripDomainTag(it.name) }
                .filter { it != anchor && it !in queryNoiseWords && it.split(' ').size < SPECIFIC_QUERY_MAX_WORDS }
        return others.firstOrNull { ' ' in it && anchor in it.split(' ') }
            ?: others.firstOrNull { ' ' in it || NeuroText.isTopicSized(it) }
    }

    private fun specificQuery(
        anchorName: String,
        member: String,
    ): String {
        val anchor = NeuroScoring.stripDomainTag(anchorName)
        return if (anchor in member.split(' ')) member else "$anchor $member"
    }

    private fun selectDiverseTopics(
        matureTopics: List<MatureTopic>,
        brain: UserBrain,
        now: Long,
        depth: Int,
        maxClusters: Int,
        channelEdges: Map<String, Double>,
    ): TopicSelection {
        if (matureTopics.isEmpty()) return emptySelection()

        val clusters =
            NeuroClusters.buildClusters(
                topicScores = matureTopics.associate { it.name to it.score },
                affinities = brain.topicAffinities,
                channelTopicProfiles = brain.channelTopicProfiles,
                categories = topicCategories,
                normalizeLemma = tokenizer::keyOf,
                tagAffinities = brain.tagAffinities,
                channelEdges = channelEdges,
            )
        if (clusters.isEmpty()) return emptySelection()

        val scheduled =
            NeuroClusters.schedule(clusters, brain.clusterRotation, now) { brain.topicEvidence[it]?.firstSeenAt }

        val byBase = HashMap<String, MatureTopic>()
        matureTopics.forEach { topic ->
            byBase.putIfAbsent(NeuroScoring.stripDomainTag(topic.name), topic)
        }

        val budget = maxClusters.coerceAtLeast(1)
        val majorKeys =
            clusters
                .sortedByDescending { it.mass }
                .take(NeuroScoring.MAJOR_CLUSTER_SLOTS.coerceAtMost(budget))
                .filter { it.mass > 0.0 }
                .map { it.representative }
                .toSet()
        val served =
            (
                    scheduled.filter { it.representative in majorKeys } +
                            scheduled.filter { it.representative !in majorKeys && it.mass > 0.0 }
                    ).take(budget)
        val choices =
            served.mapNotNull { cluster ->
                val members = cluster.topics.mapNotNull { byBase[it] }
                if (members.isEmpty()) return@mapNotNull null
                val anchor = members.first().copy(clusterKey = cluster.representative)
                val surfaceMember = specificMember(anchor.name, members)
                val others =
                    members
                        .drop(1)
                        .map { NeuroScoring.stripDomainTag(it.name) }
                        .filter { it != NeuroScoring.stripDomainTag(anchor.name) }
                val usedAt = others.indexOf(surfaceMember)
                val branches = if (usedAt < 0) others else others.drop(usedAt + 1) + others.take(usedAt + 1)
                val comboWith = if (depth > 0 && branches.isNotEmpty()) branches[(depth - 1) % branches.size] else null
                val surfaceQuery = if (depth == 0) surfaceMember?.let { specificQuery(anchor.name, it) } else null
                ClusterChoice(anchor, comboWith, surfaceQuery)
            }
        if (choices.isEmpty()) return emptySelection()

        val representedNames = choices.map { it.topic.name }.toSet()
        val leftoverReps =
            scheduled
                .drop(served.size)
                .mapNotNull { cluster ->
                    cluster.topics
                        .firstNotNullOfOrNull { byBase[it] }
                        ?.copy(clusterKey = cluster.representative)
                }.filter { it.name !in representedNames }

        val emerging = leftoverReps.take(2)
        val representedCategories =
            (choices.map { it.topic } + emerging)
                .mapNotNull { categoryNameOf(it.name) }
                .toSet()
        val crossCategory =
            leftoverReps
                .filter { topic ->
                    topic !in emerging &&
                            categoryNameOf(topic.name)?.let { it !in representedCategories } == true
                }.take(2)

        return TopicSelection(
            choices = choices,
            emerging = emerging,
            crossCategory = crossCategory,
        )
    }

    private fun categoryNameOf(topicName: String): String? {
        val base = NeuroScoring.stripDomainTag(topicName)
        return topicCategories
            .find { cat ->
                cat.topics.any { tokenizer.keyOf(it) == base }
            }?.name
    }

    private fun isDiscoveryEligible(
        topic: MatureTopic,
        isMatureBrain: Boolean = false,
    ): Boolean {
        if (topic.hasDiscoveryEvidence) return true
        if (topic.maturityLevel >= TopicMaturity.DEVELOPING) return true
        if (isMatureBrain && topic.score >= 0.10) return true
        return false
    }

    private fun hasDiscoveryEvidence(
        topic: String,
        brain: UserBrain,
    ): Boolean {
        val base = NeuroScoring.stripDomainTag(topic)
        val preferred =
            brain.preferredTopics.any {
                tokenizer.keyOf(it).equals(base, ignoreCase = true)
            }
        if (preferred) return true

        val evidence =
            brain.topicEvidence[base] ?: brain.topicEvidence[topic]
            ?: return false

        return evidence.explicitSignals > 0 ||
                evidence.watchSignals >= 2 ||
                evidence.videoIds.size >= 2 ||
                evidence.positiveScore >= 1.2 ||
                (evidence.positiveSignals >= 2 && evidence.positiveScore >= 0.5)
    }

    private val freshnessWords = listOf("2025", "2026", "new", "latest")

    private val longFormWords =
        listOf(
            "documentary",
            "deep dive",
            "essay",
            "full",
            "breakdown",
        )

    private val shortFormWords =
        listOf(
            "highlights",
            "best moments",
            "compilation",
        )

    internal val curatedWords: Set<String> get() = ambiguousQueryWords + queryNoiseWords + fillerWords

    private val genericTitleVerbs =
        hashSetOf("test", "built", "make", "made", "tri", "use", "get", "got", "show", "watch")

    private val ambiguousQueryWords =
        hashSetOf(
            "code",
            "design",
            "build",
            "run",
            "play",
            "model",
            "train",
            "stream",
            "fire",
            "rock",
            "metal",
            "spring",
            "cell",
            "plant",
            "pitch",
            "jam",
            "bar",
            "wave",
            "track",
            "scale",
            "craft",
            "mine",
            "host",
            "board",
            "drop",
            "lead",
            "light",
            "block",
            "bass",
            "clip",
            "fan",
            "gear",
            "kit",
            "log",
            "net",
            "pad",
            "port",
            "rig",
            "set",
            "tap",
            "tip",
            "web",
            "chor",
            "mix",
            "beat",
            "sound",
            "work",
            "world",
            "life",
            "point",
            "style",
            "power",
            "space",
            "match",
        )

    internal fun enclosingPhrase(
        word: String,
        brain: UserBrain,
    ): String? {
        val alone =
            brain.globalVector.topics.entries
                .sumOf { (key, value) -> if (NeuroScoring.stripDomainTag(key) == word) value else 0.0 }
        return brain.globalVector.topics.entries
            .map { (key, value) -> NeuroScoring.stripDomainTag(key) to value }
            .filter { (key, value) -> ' ' in key && word in key.split(' ') && value >= alone * PHRASE_SHARE }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun needsQueryEnrichment(topic: String): Boolean {
        val base = NeuroScoring.stripDomainTag(topic)
        return base in ambiguousQueryWords || base in tokenizer.polysemousWords
    }

    private fun buildNaturalQuery(
        topic: String,
        brain: UserBrain,
    ): String {
        val base = NeuroScoring.stripDomainTag(topic)

        if (' ' !in base) enclosingPhrase(base, brain)?.let { return it }

        if (!needsQueryEnrichment(base)) return base
        if (topic.contains(":")) {
            val domain = topic.substringAfter(":")
            val qualifier = domainToQueryWord[domain] ?: domain
            return "$base $qualifier"
        }

        brain.topicAffinities.entries
            .filter { (key, value) ->
                val parts = key.split("|")
                parts.size == 2 &&
                        (parts[0] == base || parts[1] == base) &&
                        value > 0.10
            }.sortedByDescending { it.value }
            .firstOrNull()
            ?.let { (key, _) ->
                val parts = key.split("|")
                val partner = if (parts[0] == base) parts[1] else parts[0]
                if (isSubstantialTopic(partner)) return "$base $partner"
            }

        brain.globalVector.topics.entries
            .filter { (k, v) ->
                val kBase = NeuroScoring.stripDomainTag(k)
                kBase != base && v > 0.05 && isSubstantialTopic(kBase)
            }.sortedByDescending { it.value }
            .firstOrNull()
            ?.let { (k, _) ->
                val kBase = NeuroScoring.stripDomainTag(k)
                return "$base $kBase"
            }

        topicCategories
            .find { cat ->
                cat.topics.any { tokenizer.keyOf(it) == base }
            }?.let { cat ->
                val catWord =
                    cat.topics
                        .firstOrNull { tokenizer.keyOf(it) != base && it.length > 3 }
                if (catWord != null) return "$base $catWord"
            }

        return base
    }

    private val domainToQueryWord =
        mapOf(
            "programming" to "programming",
            "music" to "music",
            "gaming" to "gaming",
            "tech" to "technology",
            "sport" to "sports",
            "fitness" to "fitness",
            "science" to "science",
            "nature" to "nature",
            "fishing" to "fishing",
            "climbing" to "climbing",
            "live" to "livestream",
            "ai" to "artificial intelligence",
            "fashion" to "fashion",
            "hobby" to "hobby",
            "season" to "season",
            "biology" to "biology",
            "energy" to "energy",
            "botany" to "plants",
            "industrial" to "industrial",
            "business" to "business",
            "pc" to "pc build",
            "construction" to "construction",
            "car" to "car build",
            "graphic" to "graphic design",
            "interior" to "interior design",
            "game" to "game design",
            "diy" to "diy crafts",
            "promo" to "deals",
            "entertainment" to "movie",
            "hair" to "hairstyle",
        )

    fun generateQueries(
        brain: UserBrain,
        now: Long = System.currentTimeMillis(),
        depth: Int = 0,
        channelEdges: Map<String, Double> = emptyMap(),
        personaProvider: (UserBrain) -> ChorPersona,
    ): List<DiscoveryQuery> {
        val persona = personaProvider(brain)
        val blocked = brain.blockedTopics
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val isMatureBrain = brain.totalInteractions > 50
        val bucket = TimeBucket.current()
        val timeVector = brain.timeVectors[bucket] ?: ContentVector()
        val timeTopicSet =
            timeVector.topics.entries
                .sortedByDescending { it.value }
                .take(5)
                .map { it.key }
                .filter { isSubstantialTopic(it) }
                .toSet()

        val matureTopics = analyzeMatureTopics(brain, timeTopicSet)
        val maxClusters =
            when (persona) {
                ChorPersona.SPECIALIST -> 3
                else -> NeuroScoring.MAX_CLUSTERS_PER_REFRESH
            }
        val selection = selectDiverseTopics(matureTopics, brain, now, depth, maxClusters, channelEdges)
        val queries = mutableListOf<DiscoveryQuery>()

        addDirectQueries(queries, selection, brain, isMatureBrain, depth)
        addCombinationQueries(queries, selection, isMatureBrain)
        addAffinityQueries(queries, brain)
        addTimeContextQueries(queries, brain, bucket, selection)
        addChannelQueries(queries, brain)
        addFreshQueries(queries, selection, currentYear, brain, isMatureBrain)
        addFormatQueries(queries, selection, brain, persona, isMatureBrain)
        addExplorationQueries(queries, brain)
        if (isMatureBrain) addPreferredTopicAnchors(queries, brain)

        val filtered =
            queries
                .filter { q ->
                    !blocked.any { b -> q.query.lowercase().contains(b) }
                }.mapNotNull { q ->
                    sanitizeQuery(NeuroDisplayForms.searchText(brain.displayForms, q.query))?.let { q.copy(query = it) }
                }

        return balanceQueryStrategies(filtered, selection.uniqueTopicCount())
    }

    private fun addDirectQueries(
        queries: MutableList<DiscoveryQuery>,
        selection: TopicSelection,
        brain: UserBrain,
        isMatureBrain: Boolean,
        depth: Int,
    ) {

        selection.choices.forEachIndexed { index, choice ->
            val topic = choice.topic
            if (!isDiscoveryEligible(topic, isMatureBrain)) return@forEachIndexed

            val anchorBase = NeuroScoring.stripDomainTag(topic.name)
            val query =
                when {
                    choice.comboWith != null -> "$anchorBase ${choice.comboWith}"
                    choice.specificQuery != null -> choice.specificQuery
                    else -> buildNaturalQuery(topic.name, brain)
                }
            val label = if (index == 0) "Core interest" else "Cluster interest"
            val branch = choice.comboWith?.let { " → $it" } ?: ""
            queries.add(
                DiscoveryQuery(
                    query,
                    QueryStrategy.DEEP_DIVE,
                    calculateConfidence(topic) - (if (index == 0) 0.0 else 0.05) - depth * 0.02,
                    "$label: ${topic.name}$branch",
                    clusterKey = topic.clusterKey,
                ),
            )
        }
    }

    private fun addCombinationQueries(
        queries: MutableList<DiscoveryQuery>,
        selection: TopicSelection,
        isMatureBrain: Boolean,
    ) {
        val primary = selection.primary.firstOrNull { isDiscoveryEligible(it, isMatureBrain) } ?: return
        val secondary = selection.secondary.filter { isDiscoveryEligible(it, isMatureBrain) }

        if (secondary.isEmpty()) return

        val primaryName = NeuroScoring.stripDomainTag(primary.name)

        secondary.take(2).forEach { sec ->
            val secName = NeuroScoring.stripDomainTag(sec.name)
            queries.add(
                DiscoveryQuery(
                    "$primaryName $secName",
                    QueryStrategy.CROSS_TOPIC,
                    0.60,
                    "Combination: ${primary.name} + ${sec.name}",
                    clusterKey = sec.clusterKey,
                ),
            )
        }

        if (secondary.size >= 2) {
            queries.add(
                DiscoveryQuery(
                    "${NeuroScoring.stripDomainTag(secondary[0].name)} ${NeuroScoring.stripDomainTag(secondary[1].name)}",
                    QueryStrategy.CROSS_TOPIC,
                    0.50,
                    "Secondary pair: ${secondary[0].name} + ${secondary[1].name}",
                    clusterKey = secondary[0].clusterKey,
                ),
            )
        }

        selection.crossCategory.take(1).forEach { cross ->
            queries.add(
                DiscoveryQuery(
                    "$primaryName ${NeuroScoring.stripDomainTag(cross.name)}",
                    QueryStrategy.CROSS_TOPIC,
                    0.45,
                    "Cross-category: ${primary.name} + ${cross.name}",
                    clusterKey = cross.clusterKey,
                ),
            )
        }
    }

    private fun addAffinityQueries(
        queries: MutableList<DiscoveryQuery>,
        brain: UserBrain,
    ) {
        brain.topicAffinities.entries
            .filter { it.value > 0.15 }
            .sortedByDescending { it.value }
            .take(3)
            .forEach { (key, score) ->
                val parts = key.split("|")
                if (parts.size != 2) return@forEach
                val (t1, t2) = parts
                if (!isSubstantialTopic(t1) ||
                    !isSubstantialTopic(t2)
                ) {
                    return@forEach
                }

                queries.add(
                    DiscoveryQuery(
                        "$t1 $t2",
                        QueryStrategy.CROSS_TOPIC,
                        0.55 + (score * 0.25),
                        "Co-watched (${"%.2f".format(score)}): $t1 + $t2",
                    ),
                )
            }
    }

    private fun addTimeContextQueries(
        queries: MutableList<DiscoveryQuery>,
        brain: UserBrain,
        bucket: TimeBucket,
        selection: TopicSelection,
    ) {
        val timeVector = brain.timeVectors[bucket] ?: return
        if (timeVector.topics.isEmpty()) return

        val timeTopics =
            timeVector.topics.entries
                .sortedByDescending { it.value }
                .take(5)
                .filter { isSubstantialTopic(it.key) }
                .map { it.key }

        if (timeTopics.isEmpty()) return

        val globalTopics = brain.globalVector.topics
        val confirmed =
            timeTopics.filter { topic ->
                val globalScore = globalTopics[topic] ?: 0.0
                globalScore > 0.10 && hasDiscoveryEvidence(topic, brain)
            }

        val usableTopics = confirmed
        if (usableTopics.isEmpty()) return

        val primaryName = selection.primary.firstOrNull()?.name

        usableTopics.firstOrNull()?.let { timeTop ->
            if (timeTop != primaryName) {
                queries.add(
                    DiscoveryQuery(
                        timeTop,
                        QueryStrategy.CONTEXTUAL,
                        0.60,
                        "Your ${formatBucketName(bucket)} interest: $timeTop",
                    ),
                )
            }
        }

        if (usableTopics.size >= 2 &&
            usableTopics[0] != usableTopics[1]
        ) {
            queries.add(
                DiscoveryQuery(
                    "${usableTopics[0]} ${usableTopics[1]}",
                    QueryStrategy.CONTEXTUAL,
                    0.50,
                    "Time combination: ${usableTopics[0]} + ${usableTopics[1]}",
                ),
            )
        }
    }

    private fun formatBucketName(bucket: TimeBucket): String =
        when (bucket) {
            TimeBucket.WEEKDAY_MORNING,
            TimeBucket.WEEKEND_MORNING,
                -> "morning"

            TimeBucket.WEEKDAY_AFTERNOON,
            TimeBucket.WEEKEND_AFTERNOON,
                -> "afternoon"

            TimeBucket.WEEKDAY_EVENING,
            TimeBucket.WEEKEND_EVENING,
                -> "evening"

            TimeBucket.WEEKDAY_NIGHT,
            TimeBucket.WEEKEND_NIGHT,
                -> "night"
        }

    private fun addChannelQueries(
        queries: MutableList<DiscoveryQuery>,
        brain: UserBrain,
    ) {
        val topChannels =
            brain.channelScores.entries
                .filter { it.value > 0.5 }
                .sortedByDescending { it.value }
                .take(3)

        topChannels.forEach { (channelId, score) ->
            val profile =
                brain.channelTopicProfiles[channelId]
                    ?: return@forEach
            if (profile.size < 2) return@forEach

            val topTopics =
                profile.entries
                    .sortedByDescending { it.value }
                    .take(2)
                    .map { it.key }
                    .filter { isSubstantialTopic(it) && hasDiscoveryEvidence(it, brain) }

            if (topTopics.size >= 2) {
                queries.add(
                    DiscoveryQuery(
                        "${topTopics[0]} ${topTopics[1]}",
                        QueryStrategy.CHANNEL_DISCOVERY,
                        0.50 + (score * 0.15),
                        "Channel signature: $channelId",
                    ),
                )
            }
        }

        val topNiche =
            brain.channelTopicProfiles.values
                .flatMap { it.entries }
                .groupBy { it.key }
                .mapValues { (_, entries) -> entries.sumOf { it.value } }
                .filter { isSubstantialTopic(it.key) && hasDiscoveryEvidence(it.key, brain) }
                .maxByOrNull { it.value }

        if (topNiche != null) {
            queries.add(
                DiscoveryQuery(
                    topNiche.key,
                    QueryStrategy.CHANNEL_DISCOVERY,
                    0.50,
                    "Top channel niche: ${topNiche.key}",
                ),
            )
        }
    }

    private fun addFreshQueries(
        queries: MutableList<DiscoveryQuery>,
        selection: TopicSelection,
        currentYear: Int,
        brain: UserBrain,
        isMatureBrain: Boolean,
    ) {
        val established =
            selection
                .allTopics()
                .filter { it.maturityLevel >= TopicMaturity.ESTABLISHED && isDiscoveryEligible(it, isMatureBrain) }
                .take(2)

        established.forEachIndexed { index, topic ->
            val baseName = buildNaturalQuery(topic.name, brain)
            val qualifier =
                if (index == 0) {
                    currentYear.toString()
                } else {
                    freshnessWords.random()
                }

            queries.add(
                DiscoveryQuery(
                    "$baseName $qualifier",
                    QueryStrategy.TRENDING,
                    calculateConfidence(topic) - 0.05,
                    "Fresh: ${topic.name} $qualifier",
                    clusterKey = topic.clusterKey,
                ),
            )
        }
    }

    private fun addFormatQueries(
        queries: MutableList<DiscoveryQuery>,
        selection: TopicSelection,
        brain: UserBrain,
        persona: ChorPersona,
        isMatureBrain: Boolean,
    ) {
        val primary = selection.primary.firstOrNull { isDiscoveryEligible(it, isMatureBrain) } ?: return
        val v = brain.globalVector

        val favourite =
            brain.formatAffinity
                .filterValues { it >= FORMAT_QUERY_MIN_AFFINITY }
                .maxByOrNull { it.value }
                ?.key
        val formatWord =
            when {
                favourite != null -> {
                    NeuroFormats.searchWord(favourite)
                }

                v.duration > 0.75 ||
                        persona == ChorPersona.DEEP_DIVER ||
                        persona == ChorPersona.SCHOLAR -> {
                    longFormWords.random()
                }

                v.duration < 0.30 ||
                        persona == ChorPersona.SKIMMER -> {
                    shortFormWords.random()
                }

                else -> {
                    return
                }
            }

        queries.add(
            DiscoveryQuery(
                "${primary.name} $formatWord",
                QueryStrategy.FORMAT_DRIVEN,
                0.55,
                "Format: ${primary.name} $formatWord",
                clusterKey = primary.clusterKey,
            ),
        )
    }

    private fun addExplorationQueries(
        queries: MutableList<DiscoveryQuery>,
        brain: UserBrain,
    ) {

        val explorationBudget = if (brain.totalInteractions > 80) 1 else 2
        explorationTopics(brain).shuffled().take(explorationBudget).forEach { topic ->
            queries.add(
                DiscoveryQuery(
                    buildNaturalQuery(topic, brain),
                    QueryStrategy.ADJACENT_EXPLORATION,
                    0.35,
                    "Adjacent: $topic",
                ),
            )
        }
    }

    fun explorationQueries(
        brain: UserBrain,
        limit: Int,
    ): List<String> = explorationTopics(brain).take(limit).map { buildNaturalQuery(it, brain) }

    private fun explorationTopics(brain: UserBrain): List<String> {
        val blocked = brain.blockedTopics

        fun globalScore(base: String): Double {
            brain.globalVector.topics[base]?.let { return it }
            return brain.globalVector.topics.entries
                .firstOrNull { NeuroScoring.stripDomainTag(it.key) == base }
                ?.value ?: 0.0
        }

        val adjacentWeights = mutableMapOf<String, Double>()

        brain.topicAffinities.forEach { (key, affinity) ->
            val parts = key.split("|")
            if (parts.size != 2) return@forEach
            for ((anchor, partner) in listOf(parts[0] to parts[1], parts[1] to parts[0])) {
                if (!isSubstantialTopic(partner)) continue
                if (globalScore(anchor) >= 0.15 &&
                    globalScore(partner) < NeuroScoring.EXPLORATION_SCORE_THRESHOLD
                ) {
                    adjacentWeights.merge(partner, affinity * globalScore(anchor), Double::plus)
                }
            }
        }

        brain.channelTopicProfiles.forEach { (channelId, profile) ->
            val channelQuality = brain.channelScores[channelId] ?: return@forEach
            if (channelQuality < 0.55) return@forEach
            profile.entries
                .sortedByDescending { it.value }
                .take(4)
                .forEach { (topic, weight) ->
                    val base = NeuroScoring.stripDomainTag(topic)
                    if (isSubstantialTopic(base) &&
                        globalScore(base) < NeuroScoring.EXPLORATION_SCORE_THRESHOLD
                    ) {
                        adjacentWeights.merge(base, weight * channelQuality, Double::plus)
                    }
                }
        }

        val blockedPhraseWords = blocked.filter { ' ' in it }.flatMapTo(HashSet()) { tokenizer.tokenize(it) }

        fun namesSubject(topic: String) =
            ' ' in topic || (topic !in ambiguousQueryWords && topic !in tokenizer.polysemousWords && topic !in genericTitleVerbs)

        val picks =
            adjacentWeights.entries
                .filter { (topic, _) ->
                    namesSubject(topic) &&
                            !blocked.any { b -> topic.contains(b) || tokenizer.keyOf(topic).contains(b) } &&
                            tokenizer.keyOf(topic) !in blockedPhraseWords &&
                            !tokenizer.isNoiseTopic(topic)
                }.sortedByDescending { it.value }
                .take(6)
        return picks.map { it.key }
    }

    private fun addPreferredTopicAnchors(
        queries: MutableList<DiscoveryQuery>,
        brain: UserBrain,
    ) {
        if (brain.preferredTopics.isEmpty()) return

        val existingTokens =
            queries
                .flatMap { q ->
                    q.query
                        .lowercase()
                        .split(NeuroTokenizer.WHITESPACE_REGEX)
                        .filter(NeuroText::isTopicSized)
                        .map { tokenizer.keyOf(it) }
                }.toSet()

        val blocked = brain.blockedTopics
        val missing =
            brain.preferredTopics
                .map { it.trim() }
                .filter { pref ->
                    val lemma = tokenizer.keyOf(pref)
                    NeuroText.isTopicSized(lemma) &&
                            lemma !in existingTokens &&
                            !blocked.any { b -> lemma.contains(b) }
                }.shuffled()
                .take(3)

        missing.forEach { topic ->
            queries.add(
                DiscoveryQuery(
                    topic,
                    QueryStrategy.DEEP_DIVE,
                    0.45,
                    "Preferred anchor: $topic",
                ),
            )
        }
    }

    private fun calculateConfidence(topic: MatureTopic): Double {
        val maturityBase =
            when (topic.maturityLevel) {
                TopicMaturity.CORE -> 0.90
                TopicMaturity.ESTABLISHED -> 0.75
                TopicMaturity.DEVELOPING -> 0.55
                TopicMaturity.EMERGING -> 0.35
            }

        val supportBonus =
            (topic.categorySupport * 0.03)
                .coerceAtMost(0.10)
        val timeBonus = if (topic.hasTimeContext) 0.05 else 0.0

        return (maturityBase + supportBonus + timeBonus)
            .coerceIn(0.20, 0.95)
    }

    private val yearRegex = Regex("^20[2-9]\\d$")

    private val queryNoiseWords =
        hashSetOf(
            "prompt",
            "prompts",
            "prompting",
            "use",
            "used",
            "using",
            "guide",
            "tutorial",
            "tips",
            "tricks",
            "thing",
            "things",
            "stuff",
            "way",
            "ways",
            "type",
            "types",
            "kind",
            "level",
            "sensei",
            "guru",
            "master",
            "pro",
            "official",
            "studio",
            "studios",
            "media",
            "network",
        )

    internal fun confidenceTier(confidence: Double): Int =
        when {
            confidence >= 0.7 -> 0
            confidence >= 0.4 -> 1
            else -> 2
        }

    internal fun sanitizeQuery(raw: String): String? {
        val words = raw.trim().split(NeuroTokenizer.WHITESPACE_REGEX).map { it.substringBefore(':') }
        val deduped = LinkedHashSet(words)
        val cleaned =
            deduped.filter { word ->
                val lower = word.lowercase()
                lower.isNotEmpty() &&
                        lower !in queryNoiseWords &&
                        !yearRegex.matches(lower)
            }

        if (cleaned.isEmpty()) return null
        val result = cleaned.joinToString(" ")
        if (result.length > 60) {
            return result
                .take(60)
                .substringBeforeLast(" ")
        }
        return result
    }

    private fun isSubstantialTopic(topic: String): Boolean {
        if (!NeuroText.isTopicSized(topic)) return false
        val lower = topic.lowercase()
        if (lower in queryNoiseWords) return false
        if (yearRegex.matches(lower)) return false
        if (lower.all { it.isDigit() }) return false
        val base = if (lower.contains(":")) lower.substringBefore(":") else lower
        if (!NeuroText.isTopicSized(base)) return false
        return tokenizer.specificity(lower) >= QUERY_MIN_SPECIFICITY
    }

    private val fillerWords =
        hashSetOf(
            "best",
            "new",
            "top",
            "how",
            "what",
            "why",
            "complete",
            "full",
            "advanced",
            "beginner",
            "learn",
            "understand",
            "understanding",
            "explained",
            "explains",
            "explanation",
            "morning",
            "evening",
            "night",
            "afternoon",
            "late",
            "early",
            "chill",
            "relaxing",
            "quick",
            "fast",
            "slow",
            "must",
            "watch",
            "see",
            "latest",
        )

    private fun balanceQueryStrategies(
        queries: List<DiscoveryQuery>,
        availableTopicCount: Int,
    ): List<DiscoveryQuery> {

        val deduped = mutableListOf<DiscoveryQuery>()
        val seenTokenSets = mutableListOf<Set<String>>()
        val sorted =
            queries.sortedWith(
                compareByDescending<DiscoveryQuery> { it.strategy == QueryStrategy.DEEP_DIVE && it.clusterKey != null }
                    .thenByDescending { it.confidence },
            )

        for (query in sorted) {
            val tokens =
                query.query
                    .lowercase()
                    .split(NeuroTokenizer.WHITESPACE_REGEX)
                    .filter(NeuroText::isTopicSized)
                    .map { tokenizer.keyOf(it) }
                    .toSet()

            val isDuplicate =
                seenTokenSets.any { existing ->
                    if (existing.isEmpty() || tokens.isEmpty()) return@any false
                    val intersection = tokens.intersect(existing).size
                    val union = tokens.union(existing).size
                    (intersection.toDouble() / union) > 0.3
                }

            val keepForClusterCoverage =
                isDuplicate &&
                        query.clusterKey != null &&
                        deduped.none { it.clusterKey == query.clusterKey }

            if (!isDuplicate || keepForClusterCoverage) {
                deduped.add(query)
                seenTokenSets.add(tokens)
            }
        }

        val minDistinctTopics =
            when {
                availableTopicCount >= 6 -> 4
                availableTopicCount >= 3 -> 3
                else -> availableTopicCount.coerceAtLeast(1)
            }

        val maxQueries = 12
        val balanced = mutableListOf<DiscoveryQuery>()
        val topicsCovered = mutableSetOf<String>()

        deduped
            .filter { it.clusterKey != null }
            .groupBy { it.clusterKey }
            .forEach { (_, clusterQueries) ->
                val best = clusterQueries.maxByOrNull { it.confidence } ?: return@forEach
                if (best !in balanced) {
                    balanced.add(best)
                    extractTopicRoot(best.query)?.let { topicsCovered.add(it) }
                }
            }

        val strategyPriority =
            listOf(
                QueryStrategy.DEEP_DIVE,
                QueryStrategy.CROSS_TOPIC,
                QueryStrategy.TRENDING,
                QueryStrategy.CONTEXTUAL,
                QueryStrategy.CHANNEL_DISCOVERY,
                QueryStrategy.ADJACENT_EXPLORATION,
                QueryStrategy.FORMAT_DRIVEN,
            )

        val byStrategy = deduped.groupBy { it.strategy }
        strategyPriority.forEach { strategy ->
            byStrategy[strategy]?.firstOrNull { it !in balanced }?.let { best ->
                if (balanced.none { it.strategy == strategy }) {
                    balanced.add(best)
                    extractTopicRoot(best.query)?.let {
                        topicsCovered.add(it)
                    }
                }
            }
        }

        if (topicsCovered.size < minDistinctTopics) {
            val remaining = deduped.filter { it !in balanced }
            for (query in remaining) {
                val topicRoot = extractTopicRoot(query.query)
                if (topicRoot != null && topicRoot !in topicsCovered) {
                    balanced.add(query)
                    topicsCovered.add(topicRoot)
                    if (topicsCovered.size >= minDistinctTopics) break
                }
            }
        }

        val topicCountInOutput = mutableMapOf<String, Int>()
        balanced.forEach { q ->
            extractTopicRoot(q.query)?.let { root ->
                topicCountInOutput[root] =
                    (topicCountInOutput[root] ?: 0) + 1
            }
        }

        val used = balanced.toSet()
        val rest =
            deduped
                .filter { it !in used }
                .sortedByDescending { it.confidence }

        for (query in rest) {
            if (balanced.size >= maxQueries) break

            val topicRoot = extractTopicRoot(query.query)
            val topicCount =
                if (topicRoot != null) {
                    topicCountInOutput[topicRoot] ?: 0
                } else {
                    0
                }

            if (topicCount >= 2) continue

            val strategyCount =
                balanced
                    .count { it.strategy == query.strategy }
            if (strategyCount >= 3) continue

            balanced.add(query)
            if (topicRoot != null) {
                topicCountInOutput[topicRoot] =
                    (topicCountInOutput[topicRoot] ?: 0) + 1
            }
        }

        return balanced
            .groupBy { confidenceTier(it.confidence) }
            .toSortedMap()
            .values
            .flatMap { it.shuffled() }
    }

    private fun extractTopicRoot(query: String): String? {
        val words =
            query
                .lowercase()
                .split(NeuroTokenizer.WHITESPACE_REGEX)
                .filter(NeuroText::isTopicSized)
                .map { tokenizer.keyOf(it) }
                .filter { it !in fillerWords }

        if (words.isEmpty()) return null
        return words.sorted().joinToString("|")
    }
}
