package io.github.pushkar.chorplay.data.profile

import io.github.pushkar.chorplay.data.local.BackupData
import io.github.pushkar.chorplay.data.local.SettingsBackup

enum class SlicePart {
    SETTINGS,
    SUBSCRIPTIONS,
    RECOMMENDATIONS,
}

private const val ONBOARDING_DONE = "onboarding_done"
private val ProfileStateKeys =
    setOf(
        "upcoming_video_reminder_ids",
        "upcoming_video_reminders",
        "deep_chor_active",
        "deep_chor_activated_at",
        "app_icon_suffix",
    )

internal fun BackupData.seedFor(parts: Set<SlicePart>): BackupData {
    val settings = settings?.takeIf { SlicePart.SETTINGS in parts }?.withoutProfileState() ?: SettingsBackup()
    return BackupData(
        viewHistory = null,
        searchHistory = null,
        subscriptions = subscriptions.takeIf { SlicePart.SUBSCRIPTIONS in parts },
        playlists = null,
        playlistVideos = null,
        videos = null,
        subscriptionGroups = subscriptionGroups.takeIf { SlicePart.SUBSCRIPTIONS in parts },
        notes = null,
        likedVideos = null,
        contentPreferences = contentPreferences.takeIf { SlicePart.RECOMMENDATIONS in parts },
        settings = settings.copy(booleans = settings.booleans + (ONBOARDING_DONE to (SlicePart.RECOMMENDATIONS in parts))),
    )
}

private fun SettingsBackup.withoutProfileState() =
    SettingsBackup(
        strings = strings - ProfileStateKeys,
        booleans = booleans - ProfileStateKeys,
        ints = ints - ProfileStateKeys,
        floats = floats - ProfileStateKeys,
        longs = longs - ProfileStateKeys,
        stringSets = stringSets - ProfileStateKeys,
    )
