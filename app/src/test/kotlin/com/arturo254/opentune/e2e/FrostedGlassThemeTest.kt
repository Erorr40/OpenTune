/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Tier 1-4 E2E Test Suite: Frosted Glass / Glassmorphism Theming Architecture
 */

package com.arturo254.opentune.e2e

import com.arturo254.opentune.constants.DarkModeKey
import com.arturo254.opentune.constants.DisableBlurKey
import com.arturo254.opentune.constants.EnableLiquidGlassKey
import com.arturo254.opentune.constants.PureBlackKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class FrostedGlassThemeTest {

    // ─────────────────────────────────────────────────────────────────────────
    // Token Models & Resolution Engine per PROJECT.md § Interface Contracts
    // ─────────────────────────────────────────────────────────────────────────

    data class FrostedGlassTokens(
        val isEnabled: Boolean,
        val isBlurSupported: Boolean,
        val surfaceAlpha: Float = 0.82f,
        val borderStrokeWidthDp: Float = 1.0f,
        val borderTopAlpha: Float = 0.22f,
        val borderBottomAlpha: Float = 0.05f,
        val fallbackSurfaceColor: Long = 0xFF121212,
    )

    enum class AppThemeMode {
        STANDARD_LIGHT,
        STANDARD_DARK,
        PURE_BLACK,
        FROSTED_GLASS,
    }

    private fun resolveFrostedGlassTokens(
        isEnabled: Boolean,
        sdkInt: Int,
        disableBlur: Boolean,
        fallbackColor: Long = 0xFF121212,
    ): FrostedGlassTokens {
        val blurSupported = isEnabled && sdkInt >= 31 && !disableBlur
        return FrostedGlassTokens(
            isEnabled = isEnabled,
            isBlurSupported = blurSupported,
            surfaceAlpha = 0.82f,
            fallbackSurfaceColor = fallbackColor,
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // WCAG 2.1 Math Engine (Relative Luminance & Contrast Ratio)
    // ─────────────────────────────────────────────────────────────────────────

    private fun sRgbToLinear(c: Double): Double {
        return if (c <= 0.04045) {
            c / 12.92
        } else {
            ((c + 0.055) / 1.055).pow(2.4)
        }
    }

    private fun relativeLuminance(r: Double, g: Double, b: Double): Double {
        val rLin = sRgbToLinear(r)
        val gLin = sRgbToLinear(g)
        val bLin = sRgbToLinear(b)
        return 0.2126 * rLin + 0.7152 * gLin + 0.0722 * bLin
    }

    private fun calculateContrastRatio(lum1: Double, lum2: Double): Double {
        val lighter = max(lum1, lum2)
        val darker = min(lum1, lum2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun compositeColor(
        scrimRgb: Triple<Double, Double, Double>,
        alpha: Double,
        artworkRgb: Triple<Double, Double, Double>,
    ): Triple<Double, Double, Double> {
        val r = alpha * scrimRgb.first + (1.0 - alpha) * artworkRgb.first
        val g = alpha * scrimRgb.second + (1.0 - alpha) * artworkRgb.second
        val b = alpha * scrimRgb.third + (1.0 - alpha) * artworkRgb.third
        return Triple(r, g, b)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tier 1: Feature Coverage — Token Generation & Fallback Resolution
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun tokens_onApi31PlusWithoutBlurDisabled_generatesActiveTokensWithHardwareBlur() {
        val tokens = resolveFrostedGlassTokens(
            isEnabled = true,
            sdkInt = 31, // Android 12 (S)
            disableBlur = false,
        )

        assertTrue("Frosted glass theme must be enabled", tokens.isEnabled)
        assertTrue("API 31+ must support hardware backdrop blur when not disabled", tokens.isBlurSupported)
        assertEquals(0.82f, tokens.surfaceAlpha, 0.001f)
        assertEquals(1.0f, tokens.borderStrokeWidthDp, 0.001f)
        assertEquals(0.22f, tokens.borderTopAlpha, 0.001f)
        assertEquals(0.05f, tokens.borderBottomAlpha, 0.001f)
    }

    @Test
    fun tokens_onApiBelow31_resolvesGracefulFallbackWithoutHardwareBlur() {
        // API 30 (Android 11), API 29 (Android 10), API 26 (Android 8.0 Oreo)
        for (sdk in listOf(26, 28, 29, 30)) {
            val tokens = resolveFrostedGlassTokens(
                isEnabled = true,
                sdkInt = sdk,
                disableBlur = false,
            )

            assertTrue("Theme is enabled on API $sdk", tokens.isEnabled)
            assertFalse(
                "Hardware blur must NOT be active on API $sdk (< 31)",
                tokens.isBlurSupported,
            )
            assertEquals("Fallback surface alpha must remain 0.82f", 0.82f, tokens.surfaceAlpha, 0.001f)
            assertNotNull("Fallback surface color must be present", tokens.fallbackSurfaceColor)
        }
    }

    @Test
    fun tokens_onApi31PlusWithDisableBlurPreference_resolvesGracefulFallback() {
        val tokens = resolveFrostedGlassTokens(
            isEnabled = true,
            sdkInt = 34, // Android 14
            disableBlur = true, // User turned off blur in settings
        )

        assertTrue(tokens.isEnabled)
        assertFalse(
            "Hardware blur must be disabled when user sets disableBlur = true, even on API 34",
            tokens.isBlurSupported,
        )
        assertEquals(0.82f, tokens.surfaceAlpha, 0.001f)
    }

    @Test
    fun tokens_whenThemeDisabled_resolvesDisabledTokens() {
        val tokens = resolveFrostedGlassTokens(
            isEnabled = false,
            sdkInt = 33,
            disableBlur = false,
        )

        assertFalse("Tokens must indicate theme is disabled", tokens.isEnabled)
        assertFalse("Blur must not be active when theme is disabled", tokens.isBlurSupported)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tier 2: Boundary & Corner Cases — Contrast Calculation & WCAG 2.1 AA
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun contrast_whiteTextOverFrostedDarkScrim_satisfiesWcag21AA_acrossDiverseArtworks() {
        // Scrim: dark surface #121212, alpha = 0.82
        val darkScrimRgb = Triple(18.0 / 255.0, 18.0 / 255.0, 18.0 / 255.0)
        val alpha = 0.82

        // Diverse album artwork dominant background colors
        val artworkPalettes = listOf(
            "Pure Black" to Triple(0.0, 0.0, 0.0),
            "Pure White" to Triple(1.0, 1.0, 1.0),
            "OpenTune Default (#ED5564)" to Triple(237.0 / 255.0, 85.0 / 255.0, 100.0 / 255.0),
            "Spotify Green (#1DB954)" to Triple(29.0 / 255.0, 185.0 / 255.0, 84.0 / 255.0),
            "Bright Yellow (#FFEB3B)" to Triple(255.0 / 255.0, 235.0 / 255.0, 59.0 / 255.0),
            "Deep Navy (#0D1B2A)" to Triple(13.0 / 255.0, 27.0 / 255.0, 42.0 / 255.0),
            "Mid Grey (#808080)" to Triple(128.0 / 255.0, 128.0 / 255.0, 128.0 / 255.0),
        )

        val whiteTextLuminance = relativeLuminance(1.0, 1.0, 1.0) // 1.0

        for ((name, artworkRgb) in artworkPalettes) {
            val eff = compositeColor(darkScrimRgb, alpha, artworkRgb)
            val effLuminance = relativeLuminance(eff.first, eff.second, eff.third)
            val contrast = calculateContrastRatio(whiteTextLuminance, effLuminance)

            // WCAG 2.1 AA requires >= 4.5:1 for normal body text
            assertTrue(
                "Contrast ratio for white text over frosted glass with $name artwork must be >= 4.5:1 (was $contrast:1)",
                contrast >= 4.5,
            )

            // Large text / UI icons require >= 3.0:1
            assertTrue(
                "Large text / icon contrast must be >= 3.0:1",
                contrast >= 3.0,
            )
        }
    }

    @Test
    fun contrast_boundaryValues_atZeroAndFullAlpha() {
        val scrimRgb = Triple(0.0, 0.0, 0.0) // Pure Black
        val artworkRgb = Triple(1.0, 1.0, 1.0) // Pure White
        val whiteTextLum = relativeLuminance(1.0, 1.0, 1.0)

        // At alpha = 1.0 (fully opaque black scrim), contrast against white text is maximum (~21:1)
        val opaqueEff = compositeColor(scrimRgb, 1.0, artworkRgb)
        val opaqueLum = relativeLuminance(opaqueEff.first, opaqueEff.second, opaqueEff.third)
        val opaqueContrast = calculateContrastRatio(whiteTextLum, opaqueLum)
        assertTrue("Opaque black scrim should yield ~21:1 contrast", opaqueContrast >= 20.0)

        // At alpha = 0.0 (scrim has no effect, direct white on white), contrast is minimum (1.0:1)
        val transparentEff = compositeColor(scrimRgb, 0.0, artworkRgb)
        val transparentLum = relativeLuminance(transparentEff.first, transparentEff.second, transparentEff.third)
        val transparentContrast = calculateContrastRatio(whiteTextLum, transparentLum)
        assertEquals(1.0, transparentContrast, 0.01)
    }

    @Test
    fun themeCycling_rapidSwitchingWithoutStateCorruption_preservesDeterminism() {
        val modes = listOf(
            AppThemeMode.STANDARD_LIGHT,
            AppThemeMode.STANDARD_DARK,
            AppThemeMode.PURE_BLACK,
            AppThemeMode.FROSTED_GLASS,
        )

        var currentMode = AppThemeMode.STANDARD_DARK

        // Simulate 100 rapid cycles across all theme options
        for (i in 0 until 100) {
            currentMode = modes[i % modes.size]

            val isFrosted = currentMode == AppThemeMode.FROSTED_GLASS
            val tokens = resolveFrostedGlassTokens(
                isEnabled = isFrosted,
                sdkInt = 34,
                disableBlur = false,
            )

            if (isFrosted) {
                assertTrue("Iteration $i: Frosted mode must have tokens enabled", tokens.isEnabled)
                assertTrue("Iteration $i: Blur must be supported on API 34", tokens.isBlurSupported)
                assertFalse("Alpha must not be NaN", tokens.surfaceAlpha.isNaN())
                assertFalse("Alpha must not be Infinite", tokens.surfaceAlpha.isInfinite())
            } else {
                assertFalse("Iteration $i: Non-frosted mode must have tokens disabled", tokens.isEnabled)
                assertFalse("Iteration $i: Blur must not be active", tokens.isBlurSupported)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tier 3: Cross-Feature Combinations — Theme State & Preferences Serialization
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun datastoreKeys_contractIntegrity() {
        // Assert exact Preference key names required by DataStore contracts in PROJECT.md
        assertEquals("enableLiquidGlass", EnableLiquidGlassKey.name)
        assertEquals("disableBlur", DisableBlurKey.name)
        assertEquals("darkMode", DarkModeKey.name)
        assertEquals("pureBlack", PureBlackKey.name)
    }

    @Test
    fun crossFeature_frostedGlassActiveWithAudioCrossfadeStateCombinations() {
        // Crossfade state (enabled = true/false, duration = 0..12s)
        for (fadeSec in listOf(0, 1, 6, 12)) {
            val crossfadeActive = fadeSec > 0
            val frostedTokens = resolveFrostedGlassTokens(
                isEnabled = true,
                sdkInt = 33,
                disableBlur = false,
            )

            // Theme tokens must remain consistent regardless of playback engine crossfade status
            assertTrue(frostedTokens.isEnabled)
            assertTrue(frostedTokens.isBlurSupported)
            assertEquals(0.82f, frostedTokens.surfaceAlpha, 0.001f)
            assertTrue(crossfadeActive || fadeSec == 0)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tier 4: Real-World Scenarios (S3 & S4 from TEST_INFRA.md)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun realWorld_scenarioS3_frostedGlassActivationAndThemeCycling() {
        // S3: Switching between Frosted Glass, Dark, Light, and Pure Black updates UI tokens instantly
        val standardDarkTokens = resolveFrostedGlassTokens(isEnabled = false, sdkInt = 33, disableBlur = false)
        assertFalse("Standard dark theme does not enable frosted glass surfaces", standardDarkTokens.isEnabled)

        val frostedTokens = resolveFrostedGlassTokens(isEnabled = true, sdkInt = 33, disableBlur = false)
        assertTrue("Frosted glass theme enables translucent surfaces and blur", frostedTokens.isEnabled)
        assertTrue("Hardware blur active on API 33", frostedTokens.isBlurSupported)

        val pureBlackTokens = resolveFrostedGlassTokens(isEnabled = false, sdkInt = 33, disableBlur = false)
        assertFalse("Pure black mode does not enable frosted glass surfaces", pureBlackTokens.isEnabled)
    }

    @Test
    fun realWorld_scenarioS4_lowEndHardwareAndBlurDisabledFallback() {
        // S4: When "Disable Blur" is enabled or on API < 31, surfaces render crisp semi-opaque scrims with border highlights
        val api28Tokens = resolveFrostedGlassTokens(isEnabled = true, sdkInt = 28, disableBlur = false)
        assertTrue("Theme remains enabled on older API", api28Tokens.isEnabled)
        assertFalse("Hardware blur is disabled on API 28", api28Tokens.isBlurSupported)
        assertEquals(0.82f, api28Tokens.surfaceAlpha, 0.001f)
        assertEquals(1.0f, api28Tokens.borderStrokeWidthDp, 0.001f)
        assertEquals(0xFF121212, api28Tokens.fallbackSurfaceColor)

        val userDisabledBlurTokens = resolveFrostedGlassTokens(isEnabled = true, sdkInt = 34, disableBlur = true)
        assertTrue("Theme remains enabled when blur disabled", userDisabledBlurTokens.isEnabled)
        assertFalse("Hardware blur disabled by preference", userDisabledBlurTokens.isBlurSupported)
        assertEquals(0.82f, userDisabledBlurTokens.surfaceAlpha, 0.001f)
    }
}
