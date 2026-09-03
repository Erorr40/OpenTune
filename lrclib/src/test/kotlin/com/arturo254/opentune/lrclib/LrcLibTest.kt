/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.lrclib

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class LrcLibTest {

    @Test
    fun parseSentencesWithMetadataAndStandardTimestamps() {
        val lrc = """
            [ti:Test Title]
            [ar:Test Artist]
            [al:Test Album]
            [00:12.34]First line of lyrics
            [01:05.678]Second line with 3 digit ms
            [02:00.00]Third line of lyrics
        """.trimIndent()

        val lyrics = LrcLib.Lyrics(lrc)
        val sentences = lyrics.sentences
        assertNotNull(sentences)
        assertEquals("", sentences?.get(0L))
        assertEquals("First line of lyrics", sentences?.get(12340L))
        assertEquals("Second line with 3 digit ms", sentences?.get(65678L))
        assertEquals("Third line of lyrics", sentences?.get(120000L))
    }
}
