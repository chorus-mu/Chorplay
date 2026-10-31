package io.github.pushkar.chorplay.data.backup

import android.net.Uri
import androidx.annotation.StringRes
import io.github.pushkar.chorplay.R
import java.time.Instant

private const val JSON = "application/json"
private const val ZIP = "application/zip"
private const val BINARY = "application/octet-stream"
private const val ANY = "*/*"
private val CsvTypes = arrayOf("text/comma-separated-values", "text/csv", "text/plain")
private val ZipTypes = arrayOf(ZIP, BINARY, ANY)

enum class ExportKind(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val mimeType: String,
    private val fileStem: String,
    private val extension: String,
    private val timestamped: Boolean = true,
) {
    MASTER(R.string.master_backup_title, R.string.master_backup_subtitle, ZIP, "chorplay_master_backup", "zip"),
    APP_DATA(R.string.export_app_data_title, R.string.export_app_data_desc, JSON, "chorplay_backup", "json"),
    ENGINE(R.string.export_engine_data, R.string.export_engine_data_subtitle, JSON, "chorplay_engine", "json"),
    NEWPIPE_SUBSCRIPTIONS(
        R.string.export_newpipe_subs_title,
        R.string.export_newpipe_subs_desc,
        JSON,
        "newpipe_subscriptions",
        "json"
    ),
    WATCH_HISTORY(
        R.string.export_watch_history_title,
        R.string.export_watch_history_desc,
        JSON,
        "chorplay-watch-history",
        "json",
        timestamped = false,
    ),
    ;

    fun fileName(now: Instant = Instant.now()): String =
        if (timestamped) "${fileStem}_${now.toEpochMilli()}.$extension" else "$fileStem.$extension"

    fun start(
        coordinator: BackupCoordinator,
        uri: Uri,
    ) = when (this) {
        MASTER -> coordinator.exportMaster(uri)
        APP_DATA -> coordinator.exportAppData(uri)
        ENGINE -> coordinator.exportEngine(uri)
        NEWPIPE_SUBSCRIPTIONS -> coordinator.exportNewPipeSubscriptions(uri)
        WATCH_HISTORY -> coordinator.exportWatchHistory(uri)
    }
}

enum class ImportKind(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    val mimeTypes: Array<String>,
    val takesSeveralFiles: Boolean = false,
) {
    CHORPLAY_BACKUP(R.string.import_chorplay_backup_item_title, R.string.import_chorplay_backup_desc, arrayOf(JSON)),
    MASTER(R.string.import_master_backup_title, R.string.import_master_backup_desc, arrayOf(ZIP, BINARY)),
    ENGINE(R.string.import_engine_data, R.string.import_engine_data_desc, arrayOf(JSON)),
    TAKEOUT(R.string.import_yt_takeout_all, R.string.import_yt_takeout_all_desc, ZipTypes, takesSeveralFiles = true),
    YOUTUBE_SUBSCRIPTIONS(R.string.import_from_youtube, R.string.import_from_youtube_desc, CsvTypes),
    YOUTUBE_HISTORY(
        R.string.import_yt_watch_history,
        R.string.import_yt_watch_history_desc,
        arrayOf("text/html", "text/plain", ANY)
    ),
    YOUTUBE_LIKES(R.string.import_yt_likes, R.string.import_yt_likes_desc, arrayOf(JSON, ZIP, BINARY, ANY)),
    YOUTUBE_PLAYLIST(R.string.import_yt_playlist, R.string.import_yt_playlist_desc, CsvTypes),
    WATCH_LATER(R.string.import_yt_watch_later, R.string.import_yt_watch_later_desc, CsvTypes),
    NEWPIPE_SUBSCRIPTIONS(R.string.import_from_newpipe, R.string.import_from_newpipe_desc, arrayOf(JSON)),
    NEWPIPE_HISTORY(
        R.string.import_newpipe_history,
        R.string.import_newpipe_history_desc,
        arrayOf(ZIP, BINARY, "application/x-sqlite3", ANY),
    ),
    NEWPIPE_PLAYLISTS(R.string.import_newpipe_playlists, R.string.import_newpipe_playlists_desc, ZipTypes),
    LIBRETUBE_SUBSCRIPTIONS(R.string.import_from_libretube, R.string.import_from_libretube_desc, arrayOf(JSON)),
    LIBRETUBE_PLAYLISTS(R.string.import_libretube_playlists, R.string.import_libretube_playlists_desc, arrayOf(JSON)),
    FREETUBE_HISTORY(
        R.string.import_freetube_history,
        R.string.import_freetube_history_desc,
        arrayOf(JSON, "text/plain", BINARY, ANY)
    ),
    ;

    fun start(
        coordinator: BackupCoordinator,
        uris: List<Uri>,
    ): Boolean {
        if (uris.isEmpty()) return false
        if (this == TAKEOUT) return coordinator.importYouTubeTakeout(uris)
        return start(coordinator, uris.first())
    }

    private fun start(
        coordinator: BackupCoordinator,
        uri: Uri,
    ) = when (this) {
        CHORPLAY_BACKUP -> coordinator.importChorplayBackup(uri)
        MASTER -> coordinator.importMaster(uri)
        ENGINE -> coordinator.importEngine(uri)
        TAKEOUT -> coordinator.importYouTubeTakeout(listOf(uri))
        YOUTUBE_SUBSCRIPTIONS -> coordinator.importYouTube(uri)
        YOUTUBE_HISTORY -> coordinator.importYouTubeWatchHistory(uri)
        YOUTUBE_LIKES -> coordinator.importYouTubeLikes(uri)
        YOUTUBE_PLAYLIST -> coordinator.importYouTubePlaylist(uri)
        WATCH_LATER -> coordinator.importYouTubePlaylist(uri, forceWatchLater = true)
        NEWPIPE_SUBSCRIPTIONS -> coordinator.importNewPipe(uri)
        NEWPIPE_HISTORY -> coordinator.importNewPipeWatchHistory(uri)
        NEWPIPE_PLAYLISTS -> coordinator.importNewPipePlaylists(uri)
        LIBRETUBE_SUBSCRIPTIONS -> coordinator.importLibreTube(uri)
        LIBRETUBE_PLAYLISTS -> coordinator.importLibreTubePlaylists(uri)
        FREETUBE_HISTORY -> coordinator.importFreeTubeWatchHistory(uri)
    }
}
