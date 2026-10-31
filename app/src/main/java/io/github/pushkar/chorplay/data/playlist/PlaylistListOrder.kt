package io.github.pushkar.chorplay.data.playlist

import io.github.pushkar.chorplay.data.model.PlaylistInfo
import java.text.Collator

enum class PlaylistListOrder(
    val storageValue: String,
) {
    NEWEST("newest"),
    OLDEST("oldest"),
    NAME("name"),
    CUSTOM("custom"),
    ;

    companion object {
        fun fromStorageValue(value: String?): PlaylistListOrder =
            entries.firstOrNull { it.storageValue == value } ?: NEWEST
    }
}

fun List<PlaylistInfo>.sortedFor(order: PlaylistListOrder): List<PlaylistInfo> =
    when (order) {
        PlaylistListOrder.NEWEST -> {
            sortedByDescending { it.createdAt }
        }

        PlaylistListOrder.OLDEST -> {
            sortedBy { it.createdAt }
        }

        PlaylistListOrder.NAME -> {
            val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
            sortedWith(compareBy(collator) { it.name })
        }

        PlaylistListOrder.CUSTOM -> {
            sortedWith(compareBy<PlaylistInfo> { it.position }.thenByDescending { it.createdAt })
        }
    }
