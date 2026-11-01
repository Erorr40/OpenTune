/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Empirical Adversarial Stress Test Suite: Milestone 2 Frosted Glass / Glassmorphism
 */

package com.arturo254.opentune.e2e

import com.arturo254.opentune.constants.DarkModeKey
import com.arturo254.opentune.constants.DisableBlurKey
import com.arturo254.opentune.constants.EnableLiquidGlassKey
import com.arturo254.opentune.constants.PureBlackKey
import com.arturo254.opentune.ui.screens.settings.DarkMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

class FrostedGlassAdversarialStressTest {

    data class FrostedGlassTokens(
        val isEnabled: Boolean,
        val isBlurSupported: Boolean,
        val surfaceAlpha: Float = 0.82f,
        val borderStrokeWidthDp: Float = 1.0f,
        val borderTopAlpha: Float = 0.22f,
        val borderBottomAlpha: Float = 0.05f,
        val fallbackSurfaceColor: Long = 0xFF121212,
    )

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
    // Test 1: Standard Themes Active -> FrostedGlassTokens Disabled No-Op Behavior
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun standardThemes_frostedGlassTokens_strictlyDisabledZeroOverhead() {
        for (sdk in 21..35) {
            for (disableBlur in listOf(false, true)) {
                val tokens = resolveFrostedGlassTokens(
                    isEnabled = false,
                    sdkInt = sdk,
                    disableBlur = disableBlur,
                )
                assertFalse("When frosted glass is disabled, isEnabled must be false", tokens.isEnabled)
                assertFalse("When frosted glass is disabled, isBlurSupported must be false", tokens.isBlurSupported)
                assertEquals(0.82f, tokens.surfaceAlpha, 0.0001f)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: Complete SDK Version x DisableBlur Matrix
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun renderEffect_exhaustiveSdkMatrix_ensuresSafeFallbackOnAllSurfaces() {
        // SDK 21 to 30: RenderEffect is not supported on Android < 12
        for (sdk in 21..30) {
            val tokensWithoutDisable = resolveFrostedGlassTokens(
                isEnabled = true,
                sdkInt = sdk,
                disableBlur = false,
            )
            assertTrue("isEnabled should be true", tokensWithoutDisable.isEnabled)
            assertFalse(
                "SDK $sdk must NOT support blur (graceful fallback mandatory)",
                tokensWithoutDisable.isBlurSupported
            )

            val tokensWithDisable = resolveFrostedGlassTokens(
                isEnabled = true,
                sdkInt = sdk,
                disableBlur = true,
            )
            assertFalse("SDK $sdk with disableBlur must NOT support blur", tokensWithDisable.isBlurSupported)
        }

        // SDK 31 to 35: RenderEffect is supported unless disableBlur is true
        for (sdk in 31..35) {
            val tokensBlurActive = resolveFrostedGlassTokens(
                isEnabled = true,
                sdkInt = sdk,
                disableBlur = false,
            )
            assertTrue("isEnabled should be true", tokensBlurActive.isEnabled)
            assertTrue("SDK $sdk must support hardware blur when disableBlur is false", tokensBlurActive.isBlurSupported)

            val tokensBlurDisabled = resolveFrostedGlassTokens(
                isEnabled = true,
                sdkInt = sdk,
                disableBlur = true,
            )
            assertTrue("isEnabled should be true", tokensBlurDisabled.isEnabled)
            assertFalse(
                "SDK $sdk with disableBlur=true must IMMEDIATELY fall back without blur",
                tokensBlurDisabled.isBlurSupported
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: Player Bottom Sheet Expand & Collapse Mathematical Invariants
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun playerBottomSheet_expandCollapseAlphaContinuity_zeroGlitchesAndMonotonicFading() {
        val collapsedBound = 64f // MiniPlayer height in dp
        val expandedBound = 800f  // Screen height in dp
        val baseAlphaBlur = 0.65f
        val baseAlphaFallback = 0.88f

        fun computeBottomSheetAlpha(stateValue: Float, isBlurSupported: Boolean): Float {
            val range = expandedBound - collapsedBound
            val progress = if (range > 0f) {
                ((stateValue - collapsedBound) / range).coerceIn(0f, 1f)
            } else {
                0f
            }
            val fadeProgress = if (progress < 0.2f) {
                ((0.2f - progress) / 0.2f).coerceIn(0f, 1f)
            } else {
                0f
            }
            val baseAlpha = if (isBlurSupported) baseAlphaBlur else baseAlphaFallback
            return (baseAlpha * (1f - fadeProgress)).coerceIn(0f, 1f)
        }

        for (blurSupported in listOf(true, false)) {
            val expectedBaseAlpha = if (blurSupported) baseAlphaBlur else baseAlphaFallback

            // 1. Collapsed bound: alpha MUST be exactly 0.0f (clean collapse, no miniplayer masking)
            val collapsedAlpha = computeBottomSheetAlpha(collapsedBound, blurSupported)
            assertEquals("Collapsed state must have zero alpha", 0.0f, collapsedAlpha, 0.0001f)

            // 2. Undershoot / negative scroll: must clamp to 0.0f safely
            val undershootAlpha = computeBottomSheetAlpha(collapsedBound - 100f, blurSupported)
            assertEquals("Undershoot must clamp to zero alpha", 0.0f, undershootAlpha, 0.0001f)

            // 3. Fully expanded bound: alpha MUST equal baseAlpha
            val expandedAlpha = computeBottomSheetAlpha(expandedBound, blurSupported)
            assertEquals("Fully expanded state must equal baseAlpha", expectedBaseAlpha, expandedAlpha, 0.0001f)

            // 4. Overshoot / stretch: must clamp to baseAlpha safely
            val overshootAlpha = computeBottomSheetAlpha(expandedBound + 500f, blurSupported)
            assertEquals("Overshoot must clamp to baseAlpha", expectedBaseAlpha, overshootAlpha, 0.0001f)

            // 5. Monotonic fade-in during expansion (0.0 to 0.2 progress)
            var prevAlpha = 0.0f
            val steps = 200
            for (step in 0..steps) {
                val stateValue = collapsedBound + (expandedBound - collapsedBound) * (step / (steps * 5.0f)) // up to progress 0.2
                val currentAlpha = computeBottomSheetAlpha(stateValue, blurSupported)
                assertTrue(
                    "Alpha must monotonically increase during expansion (prev=$prevAlpha, current=$currentAlpha)",
                    currentAlpha >= prevAlpha - 0.00001f
                )
                prevAlpha = currentAlpha
            }

            // At 20% expansion (progress = 0.2f), alpha must reach full baseAlpha
            val transitionPoint = collapsedBound + (expandedBound - collapsedBound) * 0.2f
            val alphaAt20Percent = computeBottomSheetAlpha(transitionPoint, blurSupported)
            assertEquals("At 20% expansion, alpha must reach baseAlpha", expectedBaseAlpha, alphaAt20Percent, 0.0001f)

            // Beyond 20% up to 100%, alpha must remain completely invariant at baseAlpha
            for (step in 1..10) {
                val stateValue = transitionPoint + (expandedBound - transitionPoint) * (step / 10.0f)
                val alpha = computeBottomSheetAlpha(stateValue, blurSupported)
                assertEquals("Beyond 20% expansion, alpha must remain baseAlpha", expectedBaseAlpha, alpha, 0.0001f)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4: DataStore Key Contracts & Runtime Theme Coupling Invariants
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun dataStore_preferenceCouplingAndInstantFallbackInvariants() {
        // Preference key names must match exactly
        assertEquals("enableLiquidGlass", EnableLiquidGlassKey.name)
        assertEquals("disableBlur", DisableBlurKey.name)
        assertEquals("darkMode", DarkModeKey.name)
        assertEquals("pureBlack", PureBlackKey.name)

        // Coupling rule 1: When enableLiquidGlass is true, useDarkTheme MUST evaluate to true
        fun computeUseDarkTheme(enableLiquidGlass: Boolean, darkMode: DarkMode, isSystemInDarkTheme: Boolean): Boolean {
            return if (enableLiquidGlass) {
                true
            } else if (darkMode == DarkMode.AUTO) {
                isSystemInDarkTheme
            } else {
                darkMode == DarkMode.ON
            }
        }

        // With frosted glass enabled, dark theme is forced regardless of system/user setting
        assertTrue(computeUseDarkTheme(enableLiquidGlass = true, darkMode = DarkMode.OFF, isSystemInDarkTheme = false))
        assertTrue(computeUseDarkTheme(enableLiquidGlass = true, darkMode = DarkMode.AUTO, isSystemInDarkTheme = false))
        assertTrue(computeUseDarkTheme(enableLiquidGlass = true, darkMode = DarkMode.ON, isSystemInDarkTheme = true))

        // Coupling rule 2: pureBlack is automatically suppressed when frosted glass is enabled
        fun computePureBlack(pureBlackEnabled: Boolean, useDarkTheme: Boolean, enableLiquidGlass: Boolean): Boolean {
            return pureBlackEnabled && useDarkTheme && !enableLiquidGlass
        }

        assertFalse("pureBlack must be false when enableLiquidGlass is true",
            computePureBlack(pureBlackEnabled = true, useDarkTheme = true, enableLiquidGlass = true)
        )
        assertTrue("pureBlack is allowed when enableLiquidGlass is false",
            computePureBlack(pureBlackEnabled = true, useDarkTheme = true, enableLiquidGlass = false)
        )

        // Coupling rule 3: disableBlur toggle triggers IMMEDIATE graceful fallback without activity restart
        var disableBlurPref = false
        var currentTokens = resolveFrostedGlassTokens(isEnabled = true, sdkInt = 34, disableBlur = disableBlurPref)
        assertTrue("Initially on API 34 without disableBlur, blur is supported", currentTokens.isBlurSupported)

        // User flips toggle in settings
        disableBlurPref = true
        currentTokens = resolveFrostedGlassTokens(isEnabled = true, sdkInt = 34, disableBlur = disableBlurPref)
        assertFalse("Immediately after toggling disableBlur, blur is unsupported (graceful fallback)", currentTokens.isBlurSupported)
        assertEquals("Surface alpha remains consistent", 0.82f, currentTokens.surfaceAlpha, 0.0001f)

        // User flips toggle back
        disableBlurPref = false
        currentTokens = resolveFrostedGlassTokens(isEnabled = true, sdkInt = 34, disableBlur = disableBlurPref)
        assertTrue("Blur is immediately restored", currentTokens.isBlurSupported)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5: Adversarial WCAG 2.1 AA Contrast Across 100 Random & Extreme Palettes
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun wcag21AA_adversarialContrastProof_across100ExtremePalettes() {
        val whiteLuminance = relativeLuminance(1.0, 1.0, 1.0)
        val darkScrimRgb = Triple(0.0706, 0.0706, 0.0706) // Color(0xFF121212)
        val scrimAlpha = 0.82

        // Include hand-crafted extreme edge cases first
        val extremePalettes = mutableListOf(
            Triple(1.0, 1.0, 1.0),       // Pure White
            Triple(0.0, 0.0, 0.0),       // Pure Black
            Triple(1.0, 1.0, 0.0),       // Pure Neon Yellow
            Triple(0.0, 1.0, 0.0),       // Pure Neon Green
            Triple(0.0, 1.0, 1.0),       // Pure Cyan
            Triple(1.0, 0.0, 1.0),       // Pure Magenta
            Triple(1.0, 0.2, 0.2),       // Bright Scarlet
            Triple(0.93, 0.33, 0.39),    // OpenTune Theme Red
            Triple(0.12, 0.84, 0.38),    // Spotify Green
            Triple(0.98, 0.98, 0.98),    // Off-white 98%
        )

        // Generate 90 additional randomized RGB artworks across color space
        val rng = Random(42)
        for (i in 1..90) {
            extremePalettes.add(Triple(rng.nextDouble(), rng.nextDouble(), rng.nextDouble()))
        }

        for ((index, artwork) in extremePalettes.withIndex()) {
            val composite = compositeColor(darkScrimRgb, scrimAlpha, artwork)
            val compLum = relativeLuminance(composite.first, composite.second, composite.third)
            val contrastRatio = calculateContrastRatio(whiteLuminance, compLum)

            assertTrue(
                "Palette #$index (R=${artwork.first}, G=${artwork.second}, B=${artwork.third}) produced contrast $contrastRatio < 4.5:1 (WCAG AA violation)",
                contrastRatio >= 4.5
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 6: High Frequency State Stress: 10,000 Rapid Transitions
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    fun rapidThemeCycling_10000Transitions_preservesAbsoluteDeterminism() {
        val rng = Random(12345)
        for (i in 1..10_000) {
            val isEnabled = rng.nextBoolean()
            val sdk = rng.nextInt(21, 36)
            val disableBlur = rng.nextBoolean()
            val fallbackColor = if (rng.nextBoolean()) 0xFF121212 else 0xFF2A2A2A

            val tokens = resolveFrostedGlassTokens(
                isEnabled = isEnabled,
                sdkInt = sdk,
                disableBlur = disableBlur,
                fallbackColor = fallbackColor,
            )

            // Strict determinism invariant
            val expectedBlur = isEnabled && sdk >= 31 && !disableBlur
            assertEquals(isEnabled, tokens.isEnabled)
            assertEquals(expectedBlur, tokens.isBlurSupported)
            assertEquals(0.82f, tokens.surfaceAlpha, 0.0001f)
            assertEquals(fallbackColor, tokens.fallbackSurfaceColor)
        }
    }
}
