package io.github.pushkar.chorplay.data.recommendation

import kotlin.math.abs

data class InterestChipSet(
    val masses: Map<String, Double> = emptyMap(),
    val computedAt: Long = 0L,
)

data class InterestChip(
    val representative: String,
    val label: String,
    val topics: List<String>,
    val category: String? = null,
    val parts: List<InterestChip> = emptyList(),
    val clusters: List<String> = listOf(representative),
)

internal object InterestChips {
    const val MAX_CHIPS = 4
    const val LABEL_MIN_SPECIFICITY = 0.5
    const val MIN_MASS_SHARE = 0.08
    const val RECOMPUTE_AFTER_MS = 24L * 60L * 60L * 1000L
    const val MASS_CHANGE = 0.30
    const val THEME_PREFIX = "theme:"
    const val THEME_MIN_INTERESTS = 3

    data class Group(
        val key: String,
        val members: List<NeuroClusters.TopicCluster>,
        val category: String? = null,
        val eligible: Boolean = true,
    ) {
        val mass: Double get() = members.sumOf { it.mass }
    }

    fun groups(
        clusters: List<NeuroClusters.TopicCluster>,
        hasEvidence: (NeuroClusters.TopicCluster) -> Boolean,
        categoryOf: (NeuroClusters.TopicCluster) -> String? = { null },
    ): List<Group> {
        val total = clusters.sumOf { it.mass }.takeIf { it > 0.0 } ?: return emptyList()
        val eligible = clusters.filter { it.mass / total >= MIN_MASS_SHARE && hasEvidence(it) }
        val themes =
            eligible
                .groupBy { cluster -> categoryOf(cluster)?.takeIf(NeuroCategories::isTheme) }
                .filter { (category, members) ->
                    category != null && (members.size >= 2 || members.sumOf { distinctInterests(it).size } >= THEME_MIN_INTERESTS)
                }.map { (category, members) -> Group(THEME_PREFIX + category, members, category) }
        val themed = themes.flatMapTo(HashSet()) { it.members }
        val singles =
            clusters.filter { it !in themed }.map { Group(it.representative, listOf(it), eligible = it in eligible) }
        return (themes + singles).sortedByDescending { it.mass }
    }

    fun stable(
        previous: InterestChipSet,
        groups: List<Group>,
        now: Long,
    ): InterestChipSet {
        val byKey = groups.associateBy { it.key }
        val fresh = now - previous.computedAt in 0 until RECOMPUTE_AFTER_MS
        val steady =
            previous.masses.isNotEmpty() &&
                    previous.masses.all { (key, mass) ->
                        val current = byKey[key]?.mass ?: return@all false
                        mass > 0.0 && abs(current - mass) / mass <= MASS_CHANGE
                    }
        if (fresh && steady) return previous
        return InterestChipSet(groups.filter { it.eligible }.take(MAX_CHIPS).associate { it.key to it.mass }, now)
    }

    fun distinctInterests(cluster: NeuroClusters.TopicCluster): List<List<String>> {
        fun words(topic: String) = NeuroScoring.stripDomainTag(topic).split(' ')
        val interests = mutableListOf<MutableList<String>>()
        cluster.topics.forEach { topic ->
            val own = words(topic)
            interests.firstOrNull { interest -> interest.any { other -> words(other).any(own::contains) } }?.add(topic)
                ?: interests.add(mutableListOf(topic))
        }
        return interests
    }

    fun chips(
        set: InterestChipSet,
        groups: List<Group>,
        forms: Map<String, DisplayForm> = emptyMap(),
        specificity: (String) -> Double = { 1.0 },
        evidenceOf: (String) -> Int = { 0 },
    ): List<InterestChip> {
        val byKey = groups.associateBy { it.key }

        fun nameable(topics: List<String>) =
            topics.filter { specificity(it) >= LABEL_MIN_SPECIFICITY }.ifEmpty { topics }

        fun chipOf(cluster: NeuroClusters.TopicCluster): InterestChip {
            val candidates = nameable(cluster.topics)
            val named =
                candidates.maxByOrNull(evidenceOf)?.takeIf { evidenceOf(it) > 0 }
                    ?: cluster.representative.takeIf { it in candidates }
                    ?: candidates.first()
            return InterestChip(cluster.representative, NeuroDisplayForms.label(forms, named), cluster.topics)
        }
        return set.masses.keys.mapNotNull { key ->
            val group = byKey[key] ?: return@mapNotNull null
            if (group.category == null) {
                chipOf(group.members.single())
            } else {
                val parts =
                    group.members.flatMap(::distinctInterests).map { topics ->
                        InterestChip(topics.first(), NeuroDisplayForms.label(forms, nameable(topics).first()), topics)
                    }
                val topics = group.members.flatMap { it.topics }
                InterestChip(
                    key,
                    parts.first().label,
                    topics,
                    group.category,
                    parts,
                    group.members.map { it.representative })
            }
        }
    }
}
