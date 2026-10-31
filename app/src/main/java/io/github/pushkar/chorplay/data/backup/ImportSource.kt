package io.github.pushkar.chorplay.data.backup

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.pushkar.chorplay.R

enum class ImportSource(
    @StringRes val titleRes: Int,
    @StringRes val shortTitleRes: Int,
    @DrawableRes val iconRes: Int?,
    val kinds: List<ImportKind>,
) {
    CHORPLAY(
        R.string.settings_backup_restore_chorplay,
        R.string.onboarding_import_source_chorplay,
        R.drawable.ic_chorplay_logo,
        listOf(ImportKind.CHORPLAY_BACKUP, ImportKind.MASTER, ImportKind.ENGINE),
    ),
    YOUTUBE(
        R.string.settings_backup_source_youtube,
        R.string.onboarding_import_source_youtube,
        R.drawable.ic_youtube,
        listOf(
            ImportKind.TAKEOUT,
            ImportKind.YOUTUBE_SUBSCRIPTIONS,
            ImportKind.YOUTUBE_HISTORY,
            ImportKind.YOUTUBE_LIKES,
            ImportKind.YOUTUBE_PLAYLIST,
            ImportKind.WATCH_LATER,
        ),
    ),
    NEWPIPE(
        R.string.settings_backup_source_newpipe,
        R.string.settings_backup_source_newpipe,
        R.drawable.ic_newpipe,
        listOf(ImportKind.NEWPIPE_SUBSCRIPTIONS, ImportKind.NEWPIPE_HISTORY, ImportKind.NEWPIPE_PLAYLISTS),
    ),
    LIBRETUBE(
        R.string.settings_backup_source_libretube,
        R.string.settings_backup_source_libretube,
        R.drawable.ic_libretube,
        listOf(ImportKind.LIBRETUBE_SUBSCRIPTIONS, ImportKind.LIBRETUBE_PLAYLISTS),
    ),
    FREETUBE(
        R.string.settings_backup_source_freetube,
        R.string.settings_backup_source_freetube,
        null,
        listOf(ImportKind.FREETUBE_HISTORY),
    )
}
