package io.github.pushkar.chorplay.data.recommendation

internal data class ChannelCategory(
    val id: String,
    val confidence: Double,
)

internal object NeuroCategories {
    private val IDS =
        mapOf(
            "film & animation" to "film",
            "autos & vehicles" to "autos",
            "music" to "music",
            "pets & animals" to "pets",
            "sports" to "sports",
            "travel & events" to "travel",
            "gaming" to "gaming",
            "people & blogs" to "people",
            "comedy" to "comedy",
            "entertainment" to "entertainment",
            "news & politics" to "news",
            "howto & style" to "howto",
            "education" to "education",
            "science & technology" to "science",
            "nonprofits & activism" to "nonprofits",
        )

    private val SPECIFICITY = mapOf("people" to 0.3, "entertainment" to 0.5)

    const val MAX_PENALTY = 0.35
    const val MIN_SIGNALS = 20.0
    const val FAMILIAR_SHARE = 0.10

    private const val MAX_SIGNALS = 400.0

    fun isTheme(id: String): Boolean = id in IDS.values && id !in SPECIFICITY

    private const val CLUSTER_MAJORITY = 0.6

    fun clusterCategory(
        topics: Collection<String>,
        channelTopicProfiles: Map<String, Map<String, Double>>,
        categoryOf: (channelId: String) -> ChannelCategory?,
    ): String? {
        val votes = HashMap<String, Double>()
        channelTopicProfiles.forEach { (channelId, profile) ->
            val overlap = topics.sumOf { profile[it] ?: 0.0 }
            if (overlap <= 0.0) return@forEach
            val category = categoryOf(channelId) ?: return@forEach
            votes.merge(category.id, overlap * category.confidence, Double::plus)
        }
        val total = votes.values.sum()
        val top = votes.maxByOrNull { it.value } ?: return null
        return top.key.takeIf { top.value / total >= CLUSTER_MAJORITY }
    }

    fun idOf(name: String?): String? = name?.trim()?.lowercase()?.let(IDS::get)

    fun learn(
        affinity: Map<String, Double>,
        category: String,
    ): Map<String, Double> {
        val counted = affinity + (category to (affinity[category] ?: 0.0) + 1.0)
        return if (counted.values.sum() > MAX_SIGNALS) counted.mapValues { it.value / 2 } else counted
    }

    fun factor(
        affinity: Map<String, Double>,
        category: ChannelCategory,
    ): Double {
        val total = affinity.values.sum()
        if (total < MIN_SIGNALS) return 1.0
        val share = (affinity[category.id] ?: 0.0) / total
        val mismatch = 1.0 - (share / FAMILIAR_SHARE).coerceAtMost(1.0)
        return 1.0 - MAX_PENALTY * category.confidence.coerceIn(0.0, 1.0) * mismatch * (SPECIFICITY[category.id] ?: 1.0)
    }
}
