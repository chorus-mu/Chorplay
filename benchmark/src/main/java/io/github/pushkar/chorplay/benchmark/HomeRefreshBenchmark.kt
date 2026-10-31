package io.github.pushkar.chorplay.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.TraceSectionMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalMetricApi::class)
class HomeRefreshBenchmark {
    @get:Rule
    val rule = MacrobenchmarkRule()

    @Test
    fun homeLoad() {
        val targetPackage =
            InstrumentationRegistry.getArguments().getString("targetAppId")
                ?: error("targetAppId instrumentation argument was not supplied")

        rule.measureRepeated(
            packageName = targetPackage,
            metrics =
                listOf(
                    TraceSectionMetric("neuro.init", TraceSectionMetric.Mode.First),
                    TraceSectionMetric("home.wave1", TraceSectionMetric.Mode.First),
                    TraceSectionMetric("neuro.rank", TraceSectionMetric.Mode.Sum),
                    TraceSectionMetric("neuro.rank", TraceSectionMetric.Mode.Count),
                ),
            compilationMode = CompilationMode.Partial(baselineProfileMode = BaselineProfileMode.Require),
            startupMode = StartupMode.COLD,
            iterations = 5,
            setupBlock = { pressHome() },
        ) {
            startActivityAndWait { intent ->
                intent.putExtra("io.github.pushkar.chorplay.extra.BENCHMARK_BYPASS_ONBOARDING", true)
            }
            device.wait(Until.hasObject(By.res("home_video_card")), HOME_TIMEOUT_MS)
        }
    }

    private companion object {
        const val HOME_TIMEOUT_MS = 30_000L
    }
}
