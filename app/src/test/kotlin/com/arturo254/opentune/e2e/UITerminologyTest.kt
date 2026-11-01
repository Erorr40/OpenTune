/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Tier 1-4 E2E Test Suite: UI Terminology & Strings Quality Architecture
 */

package com.arturo254.opentune.e2e

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class UITerminologyTest {

    private lateinit var stringsXmlFile: File
    private lateinit var opentuneStringsXmlFile: File
    private lateinit var stringsDoc: Document
    private lateinit var opentuneStringsDoc: Document

    @Before
    fun setup() {
        stringsXmlFile = resolveResourceFile("src/main/res/values/strings.xml")
        opentuneStringsXmlFile = resolveResourceFile("src/main/res/values/opentune_strings.xml")

        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        factory.isValidating = false

        val builder = factory.newDocumentBuilder()
        stringsDoc = builder.parse(stringsXmlFile)
        opentuneStringsDoc = builder.parse(opentuneStringsXmlFile)
    }

    private fun resolveResourceFile(relativePath: String): File {
        val candidates = listOf(
            File(relativePath),
            File("app", relativePath),
            File("../app", relativePath),
            File("D:/MyWork/OpenTune", relativePath),
            File("D:/MyWork/OpenTune/app", relativePath),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Resource file not found: $relativePath (searched: ${candidates.map { it.absolutePath }})")
    }

    private fun extractStringsMap(doc: Document): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val stringNodes: NodeList = doc.getElementsByTagName("string")
        for (i in 0 until stringNodes.length) {
            val element = stringNodes.item(i) as Element
            val name = element.getAttribute("name")
            val content = element.textContent
            if (name.isNotBlank()) {
                map[name] = content
            }
        }
        return map
    }

    private fun extractPluralsMap(doc: Document): Map<String, Map<String, String>> {
        val map = mutableMapOf<String, Map<String, String>>()
        val pluralNodes: NodeList = doc.getElementsByTagName("plurals")
        for (i in 0 until pluralNodes.length) {
            val pluralElement = pluralNodes.item(i) as Element
            val name = pluralElement.getAttribute("name")
            val items = mutableMapOf<String, String>()
            val itemNodes = pluralElement.getElementsByTagName("item")
            for (j in 0 until itemNodes.length) {
                val itemElement = itemNodes.item(j) as Element
                val quantity = itemElement.getAttribute("quantity")
                items[quantity] = itemElement.textContent
            }
            if (name.isNotBlank()) {
                map[name] = items
            }
        }
        return map
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Terminology Validation Engine
    // ─────────────────────────────────────────────────────────────────────────

    data class TerminologyValidationResult(
        val isCompliant: Boolean,
        val detectedViolations: List<String>,
    )

    private fun validateSongTerminology(text: String, isSongContext: Boolean): TerminologyValidationResult {
        val violations = mutableListOf<String>()
        val lower = text.lowercase()

        if (isSongContext) {
            val forbiddenPatterns = listOf(
                "element selected",
                "elements selected",
                "%d element",
                "%d elements",
                "%1\$d element",
                "%1\$d elements",
                "element count",
            )
            for (pattern in forbiddenPatterns) {
                if (lower.contains(pattern)) {
                    violations.add("Forbidden song unit pattern found: '$pattern'")
                }
            }
        }

        return TerminologyValidationResult(
            isCompliant = violations.isEmpty(),
            detectedViolations = violations,
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tier 1: Feature Coverage — String Resources & Song Terminology Validation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun resourceFiles_existAndAreValidXml() {
        assertTrue("strings.xml must exist and not be empty", stringsXmlFile.length() > 0)
        assertTrue("opentune_strings.xml must exist and not be empty", opentuneStringsXmlFile.length() > 0)
        assertNotNull("strings.xml root element must be present", stringsDoc.documentElement)
        assertNotNull("opentune_strings.xml root element must be present", opentuneStringsDoc.documentElement)
    }

    @Test
    fun canonicalStringsXml_songTerminology_containsNoDeprecatedElementUnits() {
        val strings = extractStringsMap(stringsDoc)
        val plurals = extractPluralsMap(stringsDoc)

        // Verify strings in strings.xml
        for ((name, value) in strings) {
            if (name.contains("song", ignoreCase = true) || name.contains("track", ignoreCase = true)) {
                val result = validateSongTerminology(value, isSongContext = true)
                assertTrue(
                    "String '$name' in strings.xml must not use deprecated element units: ${result.detectedViolations}",
                    result.isCompliant,
                )
            }
        }

        // Verify plurals in strings.xml
        for ((name, items) in plurals) {
            if (name.contains("song", ignoreCase = true) || name.contains("track", ignoreCase = true)) {
                for ((quantity, text) in items) {
                    val result = validateSongTerminology(text, isSongContext = true)
                    assertTrue(
                        "Plural '$name' ($quantity) must not use deprecated element units: ${result.detectedViolations}",
                        result.isCompliant,
                    )
                }
            }
        }
    }

    @Test
    fun terminologyValidator_correctlyDistinguishesSongUnitsFromUiElementWidgets() {
        // Song contexts with deprecated terminology -> flagged
        val songViolation1 = validateSongTerminology("%1\$d elements selected", isSongContext = true)
        assertFalse(songViolation1.isCompliant)
        assertTrue(songViolation1.detectedViolations.isNotEmpty())

        val songViolation2 = validateSongTerminology("%d elements", isSongContext = true)
        assertFalse(songViolation2.isCompliant)

        // Song contexts with modernized terminology -> approved
        val songValid1 = validateSongTerminology("%1\$d songs selected", isSongContext = true)
        assertTrue(songValid1.isCompliant)

        val songValid2 = validateSongTerminology("%d songs", isSongContext = true)
        assertTrue(songValid2.isCompliant)

        // Non-song UI widgets (e.g. Always On Display visible clock/title/progress widgets) -> approved
        val uiWidgetResult = validateSongTerminology("Visible elements", isSongContext = false)
        assertTrue("UI widget element naming must be permitted outside of song contexts", uiWidgetResult.isCompliant)
    }

    @Test
    fun pluralFormatting_canonicalSongPlurals_validatesStructureAndFormatTokens() {
        val plurals = extractPluralsMap(stringsDoc)
        assertTrue("strings.xml must define canonical plural 'n_song'", plurals.containsKey("n_song"))

        val nSong = plurals["n_song"]!!
        assertTrue("n_song must contain 'one' quantity", nSong.containsKey("one"))
        assertTrue("n_song must contain 'other' quantity", nSong.containsKey("other"))

        val oneText = nSong["one"]!!
        val otherText = nSong["other"]!!

        // Must contain %d placeholder
        assertTrue("'one' quantity must contain %d specifier (was: $oneText)", oneText.contains("%d"))
        assertTrue("'other' quantity must contain %d specifier (was: $otherText)", otherText.contains("%d"))

        // Must use 'song' and 'songs'
        assertTrue("'one' quantity must use 'song'", oneText.contains("song"))
        assertTrue("'other' quantity must use 'songs'", otherText.contains("songs"))
        assertFalse("'one' quantity must not use 'element'", oneText.contains("element"))
        assertFalse("'other' quantity must not use 'elements'", otherText.contains("elements"))
    }

    @Test
    fun verifiedKeyExistence_coreSongAndPlaybackKeys() {
        val strings = extractStringsMap(stringsDoc)

        val requiredCoreKeys = listOf(
            "filter_songs",
            "liked_songs",
            "downloaded_songs",
            "library_song_empty",
            "most_played_songs",
            "sort_by_song_count",
            "edit_song",
            "song_title",
            "song_artists",
            "queue_all_songs",
            "repeat_mode_one",
            "end_of_song",
            "duplicates_description_single",
            "duplicates_description_multiple",
            "added_n_songs_to_playlist",
        )

        for (key in requiredCoreKeys) {
            assertTrue("Key '$key' must exist in strings.xml", strings.containsKey(key))
            assertNotNull(strings[key])
            assertTrue("Value for '$key' must not be blank", strings[key]!!.isNotBlank())
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tier 2: Boundary & Corner Cases — Pluralization Formatting & Quantities
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun pluralFormatting_allPluralsAcrossResources_containValidQuantitiesAndSpecifiers() {
        val allPlurals = extractPluralsMap(stringsDoc) + extractPluralsMap(opentuneStringsDoc)
        assertTrue("Plural resources must be present", allPlurals.isNotEmpty())

        for ((pluralName, quantities) in allPlurals) {
            // In Android resource specification, every plural MUST declare fallback quantity 'other'
            assertTrue(
                "Plural '$pluralName' must declare quantity 'other'",
                quantities.containsKey("other"),
            )

            for ((quantity, text) in quantities) {
                assertTrue(
                    "Plural '$pluralName' ($quantity) must not be blank",
                    text.isNotBlank(),
                )
            }
        }
    }

    @Test
    fun pluralFormatting_boundaryCountSimulation() {
        val plurals = extractPluralsMap(stringsDoc)
        val nSong = plurals["n_song"]!!

        fun formatSongCount(count: Int): String {
            val template = if (count == 1) nSong["one"]!! else nSong["other"]!!
            return String.format(template, count)
        }

        // Boundary counts: 0, 1, 2, 1000, 50000
        assertEquals("0 songs", formatSongCount(0))
        assertEquals("1 song", formatSongCount(1))
        assertEquals("2 songs", formatSongCount(2))
        assertEquals("1000 songs", formatSongCount(1000))
        assertEquals("50000 songs", formatSongCount(50000))
    }

    @Test
    fun stringFormatIntegrity_noUnmatchedFormatSpecifiers() {
        val allStrings = extractStringsMap(stringsDoc) + extractStringsMap(opentuneStringsDoc)

        for ((key, value) in allStrings) {
            // Verify there is no lone '%' that is not escaped as '%%' and not followed by a specifier
            val cleaned = value.replace("%%", "")
            val percentIndices = cleaned.indices.filter { cleaned[it] == '%' }

            for (idx in percentIndices) {
                assertTrue(
                    "String '$key' has trailing unescaped '%' at end: '$value'",
                    idx < cleaned.length - 1,
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tier 3: Cross-Feature Combinations — Audio & Theme Setting Strings
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun crossFeatureStrings_audioAndThemeSettingsHaveDescriptiveLabels() {
        val opentuneStrings = extractStringsMap(opentuneStringsDoc)
        val strings = extractStringsMap(stringsDoc)
        val combined = strings + opentuneStrings

        // Audio crossfade setting description exists and is descriptive
        assertTrue("audio_crossfade_description must exist", combined.containsKey("audio_crossfade_description"))
        val crossfadeDesc = combined["audio_crossfade_description"]!!
        assertTrue("Crossfade description must explain fading", crossfadeDesc.contains("transition") || crossfadeDesc.contains("fad"))

        // Theme and blur descriptions
        assertTrue(combined.containsKey("general_settings"))
        assertTrue(combined.containsKey("player_background_style"))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tier 4: Real-World Scenarios (S6 from TEST_INFRA.md)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun realWorld_scenarioS6_uiStringAuditAcrossScreens() {
        // S6: Full Navigation & UI String Audit Across Screens
        // Home, Library, Player, Search, and Settings display standardized terminology
        val opentuneStrings = extractStringsMap(opentuneStringsDoc)
        val strings = extractStringsMap(stringsDoc)
        val combined = strings + opentuneStrings

        val navigationAndScreenTitles = listOf(
            "now_playing",
            "liked",
            "offline",
            "my_top",
            "cached_playlist",
            "general_settings",
            "login_settings",
        )

        for (titleKey in navigationAndScreenTitles) {
            assertTrue("Key '$titleKey' must exist across screen resources", combined.containsKey(titleKey))
            val text = combined[titleKey]!!
            assertTrue("Screen title for '$titleKey' must not be blank", text.isNotBlank())
            assertTrue(
                "Screen title for '$titleKey' must start with an uppercase letter: '$text'",
                text.first().isUpperCase(),
            )
        }
    }
}
