/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Unit & Architecture Verification Test Suite: Expressive List & Preference Constraints
 */

package com.arturo254.opentune.e2e

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExpressiveListPreferenceTest {

    private fun resolveSourceFile(relativePath: String): File {
        val candidates = listOf(
            File(relativePath),
            File("app", relativePath),
            File("../app", relativePath),
            File("D:/MyWork/OpenTune", relativePath),
            File("D:/MyWork/OpenTune/app", relativePath),
        )
        return candidates.firstOrNull { it.exists() }
            ?: error("Source file not found: $relativePath")
    }

    @Test
    fun expressiveListBottomSheet_doesNotNestLazyColumnInsideVerticalScroll() {
        val preferenceFile = resolveSourceFile("src/main/kotlin/com/arturo254/opentune/ui/component/Preference.kt")
        val content = preferenceFile.readText()

        // Extract ExpressiveListBottomSheet block
        val bottomSheetStart = content.indexOf("fun <T> ExpressiveListBottomSheet(")
        assertTrue("ExpressiveListBottomSheet function must exist in Preference.kt", bottomSheetStart != -1)

        val bottomSheetBlock = content.substring(
            bottomSheetStart,
            content.indexOf("fun ExpressiveTextFieldBottomSheet(", bottomSheetStart).let {
                if (it != -1) it else content.length
            }
        )

        // Verify that inside ExpressiveListBottomSheet there is NO .verticalScroll(
        assertFalse(
            "ExpressiveListBottomSheet must NOT contain .verticalScroll(...) which causes infinite constraint crash with LazyColumn",
            bottomSheetBlock.contains(".verticalScroll(")
        )

        // Verify that ExpressiveListBottomSheet uses LazyColumn directly
        assertTrue(
            "ExpressiveListBottomSheet must use LazyColumn directly for efficient and safe scrolling",
            bottomSheetBlock.contains("LazyColumn(")
        )

        // Verify that ExpressiveListItem is used for rendering items
        assertTrue(
            "ExpressiveListBottomSheet must use ExpressiveListItem",
            bottomSheetBlock.contains("ExpressiveListItem(")
        )
    }

    @Test
    fun expressiveList_delegatesToExpressiveListItem() {
        val preferenceFile = resolveSourceFile("src/main/kotlin/com/arturo254/opentune/ui/component/Preference.kt")
        val content = preferenceFile.readText()

        val listStart = content.indexOf("fun <T> ExpressiveList(")
        assertTrue("ExpressiveList function must exist", listStart != -1)

        val itemStart = content.indexOf("fun <T> ExpressiveListItem(")
        assertTrue("ExpressiveListItem function must exist", itemStart != -1)

        val listBlock = content.substring(listStart, itemStart)
        assertTrue(
            "ExpressiveList must delegate item rendering to ExpressiveListItem",
            listBlock.contains("ExpressiveListItem(")
        )
    }

    @Test
    fun mainActivityTopBar_respectsEnableLiquidGlass() {
        val mainActivityFile = resolveSourceFile("src/main/kotlin/com/arturo254/opentune/MainActivity.kt")
        val content = mainActivityFile.readText()

        // Verify shouldShowBlurBackground includes enableLiquidGlass
        assertTrue(
            "MainActivity must show blur background when enableLiquidGlass is true",
            content.contains("shouldUseFloatingTopBar || enableLiquidGlass")
        )

        // Verify TopAppBar colors use Color.Transparent when enableLiquidGlass is true
        assertTrue(
            "TopAppBar colors must be transparent when enableLiquidGlass is true",
            content.contains("enableLiquidGlass || shouldUseFloatingTopBar")
        )
    }
}
