/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.betterlyrics

import org.junit.Assert.assertEquals
import org.junit.Test

class TTMLParserTest {

    @Test
    fun parseBasicTTML() {
        val ttml =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body>
                <div>
                  <p begin="00:00:01.000" end="00:00:05.000">
                    <span begin="00:00:01.000" end="00:00:02.000">mi</span>
                    <span begin="00:00:02.000" end="00:00:03.000">ne,</span>
                  </p>
                </div>
              </body>
            </tt>
            """.trimIndent()

        val lines = TTMLParser.parseTTML(ttml)
        assertEquals(1, lines.size)
        assertEquals("mine,", lines[0].text.trim())
        assertEquals(2, lines[0].words.size)
        assertEquals("mi", lines[0].words[0].text)
        assertEquals("ne,", lines[0].words[1].text)
    }

    @Test
    fun parseNestedDivsDoesNotDuplicate() {
        val ttml =
            """
            <?xml version="1.0" encoding="utf-8"?>
            <tt xmlns="http://www.w3.org/ns/ttml">
              <body>
                <div>
                  <div>
                    <p begin="00:00:01.000" end="00:00:03.000">Hello world</p>
                  </div>
                </div>
              </body>
            </tt>
            """.trimIndent()

        val lines = TTMLParser.parseTTML(ttml)
        assertEquals(1, lines.size)
        assertEquals("Hello world", lines[0].text.trim())
    }
}
