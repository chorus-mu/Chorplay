package io.github.pushkar.chorplay.data.stats

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class RecapBackupTest {
    private val now = 1_790_000_000_000L

    @Test
    fun `video ledger round trips through backup entry`() {
        val video = VideoStatsLedger()
        VideoStatsLedgerOps.recordView(
            video,
            now,
            ViewEvent("v1", "T", "UCa", "A", ViewFormat.LONG, 60_000L, counted = true, skipped = false),
            listOf("space"),
        )

        val backup = RecapBackup(video = video.toSnapshot())
        val restored = RecapBackup.decode(backup.encode())

        assertThat(restored).isEqualTo(backup)
    }

    @Test
    fun `an unreadable entry restores nothing`() {
        assertThat(RecapBackup.decode("not json".encodeToByteArray())).isNull()
    }
}
