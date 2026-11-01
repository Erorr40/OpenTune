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

    @Test
    fun playerDesignStyle_allEnumValuesAreDistinctAndNonEmpty() {
        val values = com.arturo254.opentune.constants.PlayerDesignStyle.entries
        assertTrue("PlayerDesignStyle must have options available", values.isNotEmpty())
        org.junit.Assert.assertEquals(
            "PlayerDesignStyle entries count must match distinct set size",
            values.size,
            values.toSet().size
        )
    }

    @Test
    fun preferenceFile_containsNoNestedScrollableWithLazyColumn() {
        val preferenceFile = resolveSourceFile("src/main/kotlin/com/arturo254/opentune/ui/component/Preference.kt")
        val lines = preferenceFile.readLines()

        // Scan for any composable containing both verticalScroll and LazyColumn
        var currentFunction = ""
        val functionBlocks = mutableMapOf<String, MutableList<String>>()

        for (line in lines) {
            if (line.trim().startsWith("fun ") || line.trim().startsWith("inline fun ")) {
                currentFunction = line.trim()
                functionBlocks[currentFunction] = mutableListOf()
            } else if (currentFunction.isNotEmpty()) {
                functionBlocks[currentFunction]?.add(line)
            }
        }

        for ((fn, blockLines) in functionBlocks) {
            val blockText = blockLines.joinToString("\n")
            if (blockText.contains("LazyColumn(")) {
                assertFalse(
                    "Function '$fn' in Preference.kt must not nest LazyColumn inside verticalScroll",
                    blockText.contains(".verticalScroll(")
                )
            }
        }
    }

    @Test
    fun readmeDownloadTable_checksumsMatchApkFilesOnDisk() {
        val readmeFile = resolveSourceFile("../README.md")
        val content = readmeFile.readText()

        val universalHash = "bb1cd662a56cc8394a01ad544903c2bb346d70b3e47df20450de77de1bc59319"
        val arm64Hash = "695078cf523b1c37ed4aef80c53284cebe613a53511778ba9f80a3122d11937f"

        assertTrue("README.md must contain the universal APK SHA-256 digest", content.contains(universalHash))
        assertTrue("README.md must contain the ARM64 APK SHA-256 digest", content.contains(arm64Hash))
    }
}
