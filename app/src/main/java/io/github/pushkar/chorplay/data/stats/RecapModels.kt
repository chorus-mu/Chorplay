package io.github.pushkar.chorplay.data.stats

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

sealed interface RecapPeriod {
    data class Month(
        val month: YearMonth,
    ) : RecapPeriod

    data class Year(
        val year: Int,
    ) : RecapPeriod

    data object AllTime : RecapPeriod

    fun previous(): RecapPeriod? =
        when (this) {
            is Month -> Month(month.minusMonths(1))
            is Year -> Year(year - 1)
            AllTime -> null
        }

    fun contains(month: YearMonth): Boolean =
        when (this) {
            is Month -> this.month == month
            is Year -> month.year == year
            AllTime -> true
        }
}

enum class RankedKind { CHANNEL, VIDEO, ARTIST, TRACK, ALBUM, OTHER }

data class RankedItem(
    val id: String,
    val name: String,
    val count: Int,
    val durationMs: Long = 0L,
    val detail: String = "",
    val imageUrl: String = "",
    val kind: RankedKind = RankedKind.OTHER,
)

data class TrackAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val imageUrl: String,
)

data class ActivityPattern(
    val totalMs: Long,
    val dayMs: Map<LocalDate, Long>,
    val hourCounts: List<Int>,
    val weekdayCounts: List<Int>,
) {
    val activeDays: Int get() = dayMs.count { it.value > 0L }
    val busiestDay: Pair<LocalDate, Long>? get() = dayMs.maxByOrNull { it.value }?.toPair()
    val longestStreak: Int
        get() {
            val days = dayMs.filterValues { it > 0L }.keys.sorted()
            var best = 0
            var run = 0
            var previous: LocalDate? = null
            for (day in days) {
                run = if (previous != null && previous.plusDays(1) == day) run + 1 else 1
                best = maxOf(best, run)
                previous = day
            }
            return best
        }

    companion object {
        val Empty = ActivityPattern(0L, emptyMap(), List(24) { 0 }, List(7) { 0 })

        fun weekdayIndex(day: DayOfWeek): Int = day.value - 1
    }
}

data class VideoRecap(
    val views: Int,
    val sessions: Int,
    val activity: ActivityPattern,
    val formatViews: Map<ViewFormat, Int>,
    val formatMs: Map<ViewFormat, Long>,
    val topChannels: List<RankedItem>,
    val topVideos: List<RankedItem>,
    val topTopics: List<RankedItem>,
    val newTopics: List<String>,
    val discoveredChannels: List<RankedItem>,
    val skippedVideos: List<RankedItem>,
    val skippedChannels: List<RankedItem>,
    val dislikes: List<DislikedVideo>,
    val actions: Map<LedgerAction, Int>,
    val topQueries: List<RankedItem>,
    val sponsorSkippedMs: Map<String, Long>,
) {
    val sponsorSavedMs: Long get() = sponsorSkippedMs.values.sum()
    val isEmpty: Boolean get() = views == 0 && activity.totalMs == 0L
}

enum class RecapInsight { NIGHT_OWL, EARLY_BIRD, WEEKEND_WATCHER, LOYAL, EXPLORER, MARATHON, TIME_SAVER }

data class RecapSummary(
    val period: RecapPeriod,
    val video: VideoRecap,
    val combined: ActivityPattern,
    val previousTotalMs: Long?,
    val insights: List<RecapInsight>,
) {
    val isEmpty: Boolean get() = video.isEmpty
}
