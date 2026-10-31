package io.github.pushkar.chorplay.innertube.models.response

import io.github.pushkar.chorplay.innertube.models.NavigationEndpoint
import kotlinx.serialization.Serializable

@Serializable
data class ResolveUrlResponse(
    val endpoint: NavigationEndpoint? = null,
)
