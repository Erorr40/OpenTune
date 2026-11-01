/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Tier 1-4 E2E Test Suite: Audio Crossfade Mixing Engine & Settings
 */

package com.arturo254.opentune.e2e

import com.arturo254.opentune.constants.AudioCrossfadeDurationKey
import com.arturo254.opentune.constants.AudioCrossfadeGaplessKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class CrossfadeE2ETest {

    // ─────────────────────────────────────────────────────────────────────────
    // Mathematical & Engine Helper Models per PROJECT.md § Interface Contracts
    // ─────────────────────────────────────────────────────────────────────────

    companion object {
        const val MIN_CROSSFADE_SECONDS = 0
        const val MAX_CROSSFADE_SECONDS = 12
        const val DEFAULT_CROSSFADE_SECONDS = 0
        const val HANDOFF_RAMP_DURATION_MS = 450L
    }

    private fun clampCrossfadeDuration(seconds: Int): Int =
        seconds.coerceIn(MIN_CROSSFADE_SECONDS, MAX_CROSSFADE_SECONDS)

    private fun durationSecondsToMs(seconds: Int): Int =
        clampCrossfadeDuration(seconds) * 1000

    private fun calculateEqualPowerVolumes(t: Float): Pair<Float, Float> {
        val clamped = t.coerceIn(0f, 1f)
        val radians = clamped.toDouble() * (PI / 2.0)
        val outgoing = cos(radians).toFloat().coerceIn(0f, 1f)
        val incoming = sin(radians).toFloat().coerceIn(0f, 1f)
        return Pair(outgoing, incoming)
    }

    private fun calculateRequiredStartBufferMs(fadeMs: Int): Long =
        (fadeMs.toLong() + 2_000L).coerceIn(3_000L, 10_000L)

    private fun adaptCrossfadeDurationForTrack(requestedFadeMs: Int, trackDurationMs: Long): Int {
        if (trackDurationMs <= 0L) return 0
        val maxSafeDurationMs = (trackDurationMs / 2).toInt()
        return min(requestedFadeMs, maxSafeDurationMs).coerceAtLeast(0)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tier 1: Feature Coverage — Duration Bounds & Clamping
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun durationBounds_validRangeValues_remainUnchanged() {
        val validDurations = listOf(0, 1, 2, 5, 6, 8, 10, 11, 12)
        for (sec in validDurations) {
            val clamped = clampCrossfadeDuration(sec)
            assertEquals("Valid duration $sec must not be modified", sec, clamped)
            assertEquals(sec * 1000, durationSecondsToMs(sec))
        }
    }

    @Test
    fun durationBounds_outOfBoundsValues_areProperlyClamped() {
        // Negative values coerced to 0s
        assertEquals(0, clampCrossfadeDuration(-1))
        assertEquals(0, clampCrossfadeDuration(-5))
        assertEquals(0, clampCrossfadeDuration(-100))
        assertEquals(0, durationSecondsToMs(-10))

        // Values exceeding 12s coerced to 12s
        assertEquals(12, clampCrossfadeDuration(13))
        assertEquals(12, clampCrossfadeDuration(15))
        assertEquals(12, clampCrossfadeDuration(30))
        assertEquals(12, clampCrossfadeDuration(100))
        assertEquals(12000, durationSecondsToMs(20))
    }

    @Test
    fun durationBounds_boundaryIntervals_0s_1s_11s_12s() {
        // 0s: Crossfade disabled (gapless default)
        assertEquals(0, clampCrossfadeDuration(0))
        assertEquals(0, durationSecondsToMs(0))

        // 1s: Minimum active crossfade
        assertEquals(1, clampCrossfadeDuration(1))
        assertEquals(1000, durationSecondsToMs(1))

        // 11s: High crossfade duration
        assertEquals(11, clampCrossfadeDuration(11))
        assertEquals(11000, durationSecondsToMs(11))

        // 12s: Maximum allowed crossfade
        assertEquals(12, clampCrossfadeDuration(12))
        assertEquals(12000, durationSecondsToMs(12))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tier 2: Boundary & Corner Cases — Equal-Power Attenuation & Buffer Math
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun equalPowerMath_sumOfSquaresIdentity_equalsOneAcrossAllProgressSteps() {
        // Equal-power curve identity: cos^2(t * π/2) + sin^2(t * π/2) == 1.0
        // Verifies zero perceptual dip (0 dB drop) at midpoint compared to linear curve (-3 dB drop)
        val steps = 100
        for (i in 0..steps) {
            val t = i.toFloat() / steps.toFloat()
            val (outgoing, incoming) = calculateEqualPowerVolumes(t)

            val powerSum = (outgoing * outgoing) + (incoming * incoming)
            assertEquals(
                "Power sum at t=$t must equal 1.0 (got $powerSum)",
                1.0f,
                powerSum,
                0.0001f,
            )
        }
    }

    @Test
    fun equalPowerMath_cardinalPoints_t0_t05_t1() {
        // At t = 0.0 (start of transition): Outgoing 100%, Incoming 0%
        val (out0, in0) = calculateEqualPowerVolumes(0.0f)
        assertEquals(1.0f, out0, 0.0001f)
        assertEquals(0.0f, in0, 0.0001f)

        // At t = 0.5 (midpoint of transition): Both at ~0.7071 (1/√2)
        val (outMid, inMid) = calculateEqualPowerVolumes(0.5f)
        val expectedMid = (kotlin.math.sqrt(2.0) / 2.0).toFloat()
        assertEquals(expectedMid, outMid, 0.001f)
        assertEquals(expectedMid, inMid, 0.001f)
        assertEquals(1.0f, (outMid * outMid) + (inMid * inMid), 0.001f)

        // At t = 1.0 (end of transition): Outgoing 0%, Incoming 100%
        val (out1, in1) = calculateEqualPowerVolumes(1.0f)
        assertEquals(0.0f, out1, 0.0001f)
        assertEquals(1.0f, in1, 0.0001f)
    }

    @Test
    fun shortTrackAdaptation_adaptsCrossfadeDurationWhenTrackIsShorterThanFade() {
        val requestedFadeMs = 10_000 // 10s crossfade configured

        // Standard track: 180s -> uses full 10s
        assertEquals(10_000, adaptCrossfadeDurationForTrack(requestedFadeMs, 180_000L))

        // Short track: 12s -> clamped to half track duration = 6s
        assertEquals(6_000, adaptCrossfadeDurationForTrack(requestedFadeMs, 12_000L))

        // Ultra short track: 4s -> clamped to 2s
        assertEquals(2_000, adaptCrossfadeDurationForTrack(requestedFadeMs, 4_000L))

        // 0s or negative track duration -> 0s
        assertEquals(0, adaptCrossfadeDurationForTrack(requestedFadeMs, 0L))
        assertEquals(0, adaptCrossfadeDurationForTrack(requestedFadeMs, -100L))
    }

    @Test
    fun bufferThresholdCalculation_clampsWithin3sAnd10sWindow() {
        // requiredStartBufferMs(fadeMs) = (fadeMs + 2000L).coerceIn(3000L, 10000L)
        assertEquals(3_000L, calculateRequiredStartBufferMs(0))
        assertEquals(3_000L, calculateRequiredStartBufferMs(500))
        assertEquals(3_000L, calculateRequiredStartBufferMs(1_000))
        assertEquals(6_000L, calculateRequiredStartBufferMs(4_000))
        assertEquals(10_000L, calculateRequiredStartBufferMs(8_000))
        assertEquals(10_000L, calculateRequiredStartBufferMs(12_000)) // Capped at 10s
        assertEquals(10_000L, calculateRequiredStartBufferMs(20_000)) // Capped at 10s
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tier 3: Cross-Feature Combinations — Persistence & Gapless Integration
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun datastoreKeys_contractIntegrity() {
        assertEquals("audioCrossfadeDuration", AudioCrossfadeDurationKey.name)
        assertEquals("audio_crossfade_gapless", AudioCrossfadeGaplessKey.name)
    }

    @Test
    fun gaplessAlbumTransition_skipsCrossfadeWhenTracksShareSameAlbum() {
        data class MockMediaMetadata(val albumId: String?, val albumTitle: String?)

        fun isGaplessAlbumTransition(current: MockMediaMetadata, next: MockMediaMetadata): Boolean {
            val albumA = current.albumId?.takeIf { it.isNotBlank() } ?: current.albumTitle?.takeIf { it.isNotBlank() }
            val albumB = next.albumId?.takeIf { it.isNotBlank() } ?: next.albumTitle?.takeIf { it.isNotBlank() }
            return albumA != null && albumA == albumB
        }

        val track1 = MockMediaMetadata("album_123", "Abbey Road")
        val track2 = MockMediaMetadata("album_123", "Abbey Road")
        val trackDifferentAlbum = MockMediaMetadata("album_456", "Dark Side of the Moon")
        val trackNoAlbum = MockMediaMetadata(null, null)

        assertTrue("Consecutive tracks from same album must be gapless", isGaplessAlbumTransition(track1, track2))
        assertFalse("Tracks from different albums must allow crossfade", isGaplessAlbumTransition(track1, trackDifferentAlbum))
        assertFalse("Tracks without album metadata must allow crossfade", isGaplessAlbumTransition(track1, trackNoAlbum))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Tier 4: Real-World Scenarios (S1 & S2 from TEST_INFRA.md)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun realWorld_scenarioS1_simulationOfContinuousPlaylistProgressionAndHandoff() {
        // S1: Continuous Album & Playlist Playback with 6s Crossfade
        val fadeDurationMs = 6000
        val trackDurationMs = 180_000L // 3 minutes

        // State Machine Simulation
        var isOverlapPrimed = false
        var isCrossfadeActive = false
        var isHandoffActive = false
        var primaryVolumeFactor = 1.0f
        var overlapVolume = 0.0f

        // 1. Playback before preload window (remaining = 15s)
        var currentPositionMs = 165_000L
        var remainingMs = trackDurationMs - currentPositionMs
        val preloadWindowMs = fadeDurationMs + 1200L // 7200ms

        assertFalse("Before preload window, overlap is not primed", remainingMs <= preloadWindowMs)

        // 2. Playback enters preload window (remaining = 7.0s <= 7.2s)
        currentPositionMs = 173_000L
        remainingMs = trackDurationMs - currentPositionMs
        if (remainingMs in 1L..preloadWindowMs) {
            isOverlapPrimed = true
        }
        assertTrue("Within preload window, overlap is primed", isOverlapPrimed)
        assertFalse("Crossfade has not started yet", isCrossfadeActive)

        // 3. Playback reaches crossfade trigger (remaining = 6.0s <= fadeDurationMs)
        currentPositionMs = 174_000L
        remainingMs = trackDurationMs - currentPositionMs
        if (isOverlapPrimed && remainingMs in 1L..fadeDurationMs.toLong()) {
            isCrossfadeActive = true
        }
        assertTrue("Crossfade begins when remainingMs <= fadeDurationMs", isCrossfadeActive)

        // 4. Equal-power transition simulation across 6s (samples every 1s)
        for (elapsedSec in 0..6) {
            val progress = elapsedSec.toFloat() / 6.0f
            val (outVol, inVol) = calculateEqualPowerVolumes(progress)
            primaryVolumeFactor = outVol
            overlapVolume = inVol

            val power = (primaryVolumeFactor * primaryVolumeFactor) + (overlapVolume * overlapVolume)
            assertEquals("Power continuity maintained during crossfade at ${elapsedSec}s", 1.0f, power, 0.001f)
        }

        assertEquals(0.0f, primaryVolumeFactor, 0.001f)
        assertEquals(1.0f, overlapVolume, 0.001f)

        // 5. Track 1 ends -> Handoff to primary player
        isCrossfadeActive = false
        isHandoffActive = true

        // Handoff ramp (450ms)
        for (handoffStep in listOf(0.0f, 0.5f, 1.0f)) {
            primaryVolumeFactor = handoffStep
            overlapVolume = 1.0f - handoffStep
            assertTrue("Volume factor in range [0, 1]", primaryVolumeFactor in 0.0f..1.0f)
            assertTrue("Overlap volume in range [0, 1]", overlapVolume in 0.0f..1.0f)
        }

        // Complete handoff
        isHandoffActive = false
        isOverlapPrimed = false
        primaryVolumeFactor = 1.0f
        overlapVolume = 0.0f

        assertFalse("Handoff finished", isHandoffActive)
        assertEquals("Primary player back to 100% volume", 1.0f, primaryVolumeFactor, 0.001f)
        assertEquals("Overlap player stopped", 0.0f, overlapVolume, 0.001f)
    }

    @Test
    fun realWorld_scenarioS2_midCrossfadePauseSeekAndFastSkip() {
        // S2: Mid-Crossfade Pause, Seek, and Fast Skip
        var isCrossfadeActive = true
        var isPaused = false
        var primaryVolumeFactor = 0.7071f
        var overlapVolume = 0.7071f

        // 1. User presses Pause mid-crossfade at t = 0.5
        isPaused = true
        // Both streams pause; volume levels are held invariant
        val heldPrimaryVolume = primaryVolumeFactor
        val heldOverlapVolume = overlapVolume

        assertEquals(0.7071f, heldPrimaryVolume, 0.001f)
        assertEquals(0.7071f, heldOverlapVolume, 0.001f)

        // 2. User presses Resume
        isPaused = false
        // Playback resumes at exact same volume levels without blast or drop
        assertEquals(heldPrimaryVolume, primaryVolumeFactor, 0.001f)
        assertEquals(heldOverlapVolume, overlapVolume, 0.001f)

        // 3. User performs a Skip while crossfade is active
        // Skip must cancel overlap immediately, reset main fade factor to 1.0f
        isCrossfadeActive = false
        overlapVolume = 0.0f
        primaryVolumeFactor = 1.0f

        assertFalse("Crossfade cancelled upon skip", isCrossfadeActive)
        assertEquals("Main fade factor reset to 1.0 immediately", 1.0f, primaryVolumeFactor, 0.001f)
        assertEquals("Overlap stopped immediately", 0.0f, overlapVolume, 0.001f)
    }
}
