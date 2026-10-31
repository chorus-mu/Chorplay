package io.github.pushkar.chorplay.data.video.downloader.request

import com.google.common.truth.Truth.assertThat
import io.github.pushkar.chorplay.data.local.entity.DownloadEntity
import io.github.pushkar.chorplay.data.local.entity.DownloadFileType
import io.github.pushkar.chorplay.data.local.entity.DownloadItemEntity
import io.github.pushkar.chorplay.data.local.entity.DownloadWithItems
import io.github.pushkar.chorplay.data.model.Video
import io.github.pushkar.chorplay.data.video.downloader.tags.DownloadKind
import io.github.pushkar.chorplay.data.video.downloader.work.LegacyDownloadRequest
import org.junit.Test

class DownloadRequestTest {
    private val video =
        Video(
            id = "abc",
            title = "A video",
            channelName = "A channel",
            channelId = "UC1",
            thumbnailUrl = "https://i.ytimg.com/vi/abc/hqdefault.jpg",
            duration = 90,
            viewCount = 10,
            likeCount = 2,
            uploadDate = "",
        )

    @Test
    fun `a request survives being stored with its row`() {
        val request = video.toDownloadRequest(targetHeight = 1080, videoCodec = "vp9", videoItag = 248, audioItag = 140)

        assertThat(DownloadRequest.decode(request.encode())).isEqualTo(request)
        assertThat(DownloadRequest.decode("not json")).isNull()
        assertThat(DownloadRequest.decode(null)).isNull()
    }

    @Test
    fun `a video request carries what the card knew and a watch url`() {
        val tags = video.toDownloadRequest().tags

        assertThat(tags.kind).isEqualTo(DownloadKind.VIDEO)
        assertThat(tags.channelId).isEqualTo("UC1")
        assertThat(tags.sourceUrl).isEqualTo("https://www.youtube.com/watch?v=abc")
        assertThat(
            video
                .copy(isShort = true)
                .toDownloadRequest()
                .tags.sourceUrl,
        ).isEqualTo("https://www.youtube.com/shorts/abc")
    }

    @Test
    fun `a row from before requests were stored asks for the quality its label named`() {
        val row =
            DownloadWithItems(
                DownloadEntity(videoId = "old", title = "Old", uploader = "Someone"),
                listOf(
                    DownloadItemEntity(
                        videoId = "old",
                        fileType = DownloadFileType.VIDEO,
                        fileName = "a",
                        filePath = "/a",
                        quality = "VP9 1080p",
                    ),
                ),
            )

        val request = LegacyDownloadRequest.from(row)

        assertThat(request.targetHeight).isEqualTo(1080)
        assertThat(request.videoCodec).isEqualTo("vp9")
        assertThat(request.wantsAudioOnly).isFalse()
        assertThat(request.tags.channelName).isEqualTo("Someone")
    }

}
