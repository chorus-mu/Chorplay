package io.github.pushkar.chorplay.benchmark

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test

class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startup() =
        collectProfile(includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait { it.putExtra(BYPASS_ONBOARDING, true) }
        }

    @Test
    fun scrollFeed() =
        collectProfile(includeInStartupProfile = false) {
            pressHome()
            startActivityAndWait { it.putExtra(BYPASS_ONBOARDING, true) }
            device.waitForIdle(UI_SETTLE_MS)

            flingFeed(Direction.DOWN, times = 3)
            flingFeed(Direction.UP, times = 1)
        }

    private fun collectProfile(
        includeInStartupProfile: Boolean,
        journey: MacrobenchmarkScope.() -> Unit,
    ) = rule.collect(
        packageName =
            InstrumentationRegistry.getArguments().getString("targetAppId")
                ?: error("targetAppId instrumentation argument was not supplied"),
        includeInStartupProfile = includeInStartupProfile,
        profileBlock = journey,
    )

    private fun MacrobenchmarkScope.flingFeed(
        direction: Direction,
        times: Int,
    ) {
        repeat(times) {
            val feed =
                device.wait(Until.findObject(By.scrollable(true)), CONTENT_TIMEOUT_MS)
                    ?: return
            runCatching {
                feed.setGestureMargin(device.displayWidth / 5)
                feed.fling(direction)
            }
            device.waitForIdle(UI_SETTLE_MS)
        }
    }

    private companion object {
        const val BYPASS_ONBOARDING = "io.github.pushkar.chorplay.extra.BENCHMARK_BYPASS_ONBOARDING"
        const val UI_SETTLE_MS = 1_500L
        const val CONTENT_TIMEOUT_MS = 10_000L
    }
}
