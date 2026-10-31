/*
 * Copyright: Pushkar (2026) under GPLv3
 *
 * This file is part of Chorplay (https://github.com/chorus-mu/Chorplay).
 *
 * This recommendation algorithm (ChorplayNeuroEngine) is the intellectual property
 * of the Chorplay project. Any use of this code in other projects must
 * explicitly credit "Chorplay Android Client" and link back to the original repository.
 */

package io.github.pushkar.chorplay.data.recommendation

import kotlinx.serialization.json.JsonElement

/** Everything a rank pass reads besides its candidates, as a Home snapshot stores it. */
internal data class NeuroDiagnosticState(
    val brain: JsonElement,
    val categories: NeuroCategoryStore.CategoryIndex,
    val coWatch: NeuroCoWatchStore.CoWatchIndex,
)
