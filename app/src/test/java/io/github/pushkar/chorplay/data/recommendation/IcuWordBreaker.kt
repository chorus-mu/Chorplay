/*
 * Copyright: Pushkar (2026) under GPLv3
 *
 * This file is part of Chorplay (https://github.com/chorus-mu/Chorplay).
 */

package io.github.pushkar.chorplay.data.recommendation

import com.ibm.icu.text.BreakIterator
import java.util.Locale

/** ICU4J dictionary breaker; the JDK breaker does not split Han runs. */
internal object IcuWordBreaker : WordBreaker {
    override fun split(chunk: String): List<String> {
        val breaks = BreakIterator.getWordInstance(Locale.ROOT)
        breaks.setText(chunk)
        val tokens = ArrayList<String>()
        var start = breaks.first()
        var end = breaks.next()
        while (end != BreakIterator.DONE) {
            tokens += chunk.substring(start, end)
            start = end
            end = breaks.next()
        }
        return tokens
    }
}
