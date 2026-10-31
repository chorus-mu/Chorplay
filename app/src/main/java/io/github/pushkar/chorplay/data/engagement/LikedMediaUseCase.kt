package io.github.pushkar.chorplay.data.engagement

import io.github.pushkar.chorplay.data.local.LikedVideoInfo
import io.github.pushkar.chorplay.data.local.LikedVideosRepository
import javax.inject.Inject

/**
 * Likes and unlikes from the Liked videos page.
 */
class LikedMediaUseCase
    @Inject
    constructor(
        private val likes: LikedVideosRepository,
    ) {
        /** Unlikes [videoIds]; the result is what [restore] needs to undo it. */
        suspend fun unlike(videoIds: Collection<String>): List<LikedVideoInfo> = likes.takeLikes(videoIds)

        suspend fun restore(taken: List<LikedVideoInfo>) {
            likes.restoreLikes(taken)
        }
    }
