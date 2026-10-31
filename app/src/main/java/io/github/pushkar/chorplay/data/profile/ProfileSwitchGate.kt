package io.github.pushkar.chorplay.data.profile

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ProfileSwitchGate {
    private val switching = MutableStateFlow<String?>(null)

    val target: StateFlow<String?> = switching.asStateFlow()

    val isSwitching: Boolean get() = switching.value != null

    internal fun close(target: String) {
        switching.value = target
    }

    internal fun reopen() {
        switching.value = null
    }
}
