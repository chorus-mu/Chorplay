package io.github.pushkar.chorplay.data.recommendation

import kotlin.math.sqrt

internal object NeuroInterestShare {
    const val MIN_GROUP_SHARE = 0.08

    fun weights(clusters: List<NeuroClusters.TopicCluster>): Map<String, Double> {
        val total = clusters.sumOf { it.mass }.takeIf { it > 0.0 } ?: return emptyMap()
        return clusters.filter { it.mass / total >= MIN_GROUP_SHARE }.associate { it.representative to sqrt(it.mass) }
    }

    fun groupOf(
        topics: Map<String, Double>,
        groupByTopic: Map<String, String>,
    ): String? {
        val byGroup = HashMap<String, Double>()
        topics.forEach { (key, weight) ->
            groupByTopic[NeuroScoring.stripDomainTag(key)]?.let {
                byGroup.merge(
                    it,
                    weight,
                    Double::plus
                )
            }
        }
        return byGroup.maxByOrNull { it.value }?.key
    }

    fun <T> interleave(
        ranked: List<T>,
        weights: Map<String, Double>,
        groupOf: (T) -> String?,
    ): List<T> {
        if (ranked.size < 2 || weights.size < 2) return ranked
        val queues = LinkedHashMap<String?, ArrayDeque<T>>()
        ranked.forEach { item ->
            queues.getOrPut(groupOf(item)?.takeIf { it in weights }) { ArrayDeque() }.addLast(item)
        }
        if (queues.keys.count { it != null } < 2) return ranked
        val grouped = ranked.size - (queues[null]?.size ?: 0)
        val known = queues.keys.filterNotNull().sumOf { weights.getValue(it) }
        val weightOf =
            { group: String? -> group?.let(weights::getValue) ?: (known * (ranked.size - grouped) / grouped) }
        val served = HashMap<String?, Int>()
        val out = ArrayList<T>(ranked.size)
        while (out.size < ranked.size) {
            val next = queues.filterValues { it.isNotEmpty() }.keys.minBy { group ->
                ((served[group] ?: 0) + 1) / weightOf(group)
            }
            out += queues.getValue(next).removeFirst()
            served.merge(next, 1, Int::plus)
        }
        return out
    }
}
