package io.github.pushkar.chorplay.discord

import android.content.Context
import io.github.pushkar.chorplay.BuildConfig
import okhttp3.OkHttpClient

class DiscordPlatformTransportFactory : DiscordPresenceTransportFactory {
    override fun create(
        context: Context,
        okHttpClient: OkHttpClient,
        tokenStore: DiscordTokenStore,
    ): DiscordPresenceTransport = KizzyDiscordPresenceTransport(
        context = context.applicationContext,
        client = okHttpClient,
        tokenStore = tokenStore,
        applicationId = BuildConfig.DISCORD_APPLICATION_ID,
    )
}
