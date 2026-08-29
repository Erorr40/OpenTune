/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.kugou

import org.junit.Assert.assertEquals
import org.junit.Test

class KuGouTest {

    @Test
    fun generateKeywordNormalizesTitleAndArtist() {
        val keyword = KuGou.generateKeyword("Song Title (feat. Artist)", "Artist One & Artist Two")
        assertEquals("Song Title ", keyword.title)
        assertEquals("Artist One、Artist Two", keyword.artist)
    }

    @Test
    fun generateKeywordWithRemixAndClean() {
        val keyword = KuGou.generateKeyword("Hello (Official Music Video)", "Singer")
        assertEquals("Hello ", keyword.title)
        assertEquals("Singer", keyword.artist)
    }
}
