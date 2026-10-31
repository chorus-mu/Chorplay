package io.github.pushkar.chorplay.data.stats

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.pushkar.chorplay.data.local.safePreferencesDataStore
import io.github.pushkar.chorplay.data.profile.StoreScope
import kotlinx.coroutines.flow.first
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

private val Context.recapPreferences by safePreferencesDataStore(
    name = "chorplay_recap",
    scope = StoreScope.PROFILE_LIBRARY
)
private val LAST_SHOWN = stringPreferencesKey("last_shown_month")

@Singleton
class RecapReadiness
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val videoStats: VideoStatsRecorder,
) {

    suspend fun readyPeriod(now: YearMonth = YearMonth.now()): RecapPeriod? {
        val shown = context.recapPreferences.data.first()[LAST_SHOWN]
        if (now.monthValue == JANUARY) {
            val year = now.year - 1
            val activity = (1..MONTHS_PER_YEAR).sumOf { activityIn(LedgerTime.monthKey(YearMonth.of(year, it))) }
            if (shown != year.toString() && activity >= MIN_YEAR_ACTIVITY) return RecapPeriod.Year(year)
        }
        val closed = now.minusMonths(1)
        val key = LedgerTime.monthKey(closed)
        if (shown == key) return null
        return RecapPeriod.Month(closed).takeIf { activityIn(key) >= MIN_ACTIVITY }
    }

    suspend fun markShown(period: RecapPeriod) {
        val key =
            when (period) {
                is RecapPeriod.Month -> LedgerTime.monthKey(period.month)
                is RecapPeriod.Year -> period.year.toString()
                RecapPeriod.AllTime -> return
            }
        context.recapPreferences.edit { it[LAST_SHOWN] = key }
    }

    private suspend fun activityIn(monthKey: String): Int = videoStats.monthViews(monthKey)

    private companion object {
        const val MIN_ACTIVITY = 20
        const val MIN_YEAR_ACTIVITY = 100
        const val JANUARY = 1
        const val MONTHS_PER_YEAR = 12
    }
}
