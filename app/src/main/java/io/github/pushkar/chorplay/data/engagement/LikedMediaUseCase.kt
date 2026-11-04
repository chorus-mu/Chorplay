package io.github.pushkar.chorplay.data.engagement

import io.github.pushkar.chorplay.data.local.LikedVideoInfo
import io.github.pushkar.chorplay.data.local.LikedVideosRepository
import javax.inject.Inject

class LikedMediaUseCase
    @Inject
    constructor(
        private val likes: LikedVideosRepository,
    ) {
        suspend fun unlike(videoIds: Collection<String>): List<LikedVideoInfo> = likes.takeLikes(videoIds)

        suspend fun restore(taken: List<LikedVideoInfo>) {
            likes.restoreLikes(taken)
        }
    }
