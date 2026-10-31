package io.github.pushkar.chorplay.discord

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DiscordFossIsolationTest {
    @Test
    fun `foss classpath excludes functional Discord implementation`() {
        val forbiddenClasses = listOf(
            "io.github.pushkar.chorplay.discord.DiscordTokenStore",
            "io.github.pushkar.chorplay.discord.DiscordAuthTokens",
            "io.github.pushkar.chorplay.discord.DiscordPlaybackSource",
            "io.github.pushkar.chorplay.discord.DiscordPresenceCoordinator",
            "io.github.pushkar.chorplay.discord.KizzyDiscordPresenceTransport",
            "io.github.pushkar.chorplay.discord.KizzyGatewayProtocol",
        )

        forbiddenClasses.forEach { className ->
            assertThat(runCatching { Class.forName(className) }.isFailure).isTrue()
        }
    }

    @Test
    fun `foss runtime reports Discord unavailable`() {
        assertThat(DiscordPresenceRuntime.settingsState.value.isAvailable).isFalse()
        assertThat(DiscordPresenceRuntime.settingsState.value.isEnabled).isFalse()
        assertThat(DiscordPresenceRuntime.settingsState.value.summary)
            .isEqualTo(DiscordSettingsSummary.UNAVAILABLE)
    }
}
