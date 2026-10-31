package io.github.pushkar.chorplay.innertube.models.body

import io.github.pushkar.chorplay.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class ResolveUrlBody(
    val context: Context,
    val url: String,
)
