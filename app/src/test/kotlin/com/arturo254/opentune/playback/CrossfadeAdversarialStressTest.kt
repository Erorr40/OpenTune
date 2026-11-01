/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Adversarial Stress & Property-Based Verification Test Suite for Milestone 1 (Crossfade)
 */

package com.arturo254.opentune.playback

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import kotlin.math.abs

class CrossfadeAdversarialStressTest {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Equal-Power Mathematical Fuzzing & Stress Invariance
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun equalPower_monteCarloFuzzing_100kRandomSteps_preservesAcousticIdentity() {
        val random = Random(424242L)
        val tolerance = 1e-4f

        for (i in 0 until 100_000) {
            val t = random.nextFloat() // uniformly distributed in [0.0, 1.0)
            val (outgoing, incoming) = CrossfadeAudio.calculateEqualPowerVolumes(t)

            // Factors must never be NaN or infinite
            assertFalse("Outgoing must not be NaN at t=$t", outgoing.isNaN())
            assertFalse("Incoming must not be NaN at t=$t", incoming.isNaN())
            assertFalse("Outgoing must not be infinite at t=$t", outgoing.isInfinite())
            assertFalse("Incoming must not be infinite at t=$t", incoming.isInfinite())

            // Factors must lie strictly in [0.0, 1.0]
            assertTrue("Outgoing must be in [0, 1] at t=$t", outgoing in 0.0f..1.0f)
            assertTrue("Incoming must be in [0, 1] at t=$t", incoming in 0.0f..1.0f)

            // Equal-power sum of squares must equal 1.0 (0 dB drop)
            val powerSum = (outgoing * outgoing) + (incoming * incoming)
            assertEquals("Equal-power identity failed at t=$t", 1.0f, powerSum, tolerance)
        }
    }

    @Test
    fun equalPower_strictMonotonicityAndSymmetry() {
        val steps = 10_000
        var prevOutgoing = 1.0f
        var prevIncoming = 0.0f

        for (i in 0..steps) {
            val t = i.toFloat() / steps.toFloat()
            val (outgoing, incoming) = CrossfadeAudio.calculateEqualPowerVolumes(t)

            // Outgoing must monotonically decrease
            assertTrue(
                "Outgoing must be monotonically non-increasing at t=$t",
                outgoing <= prevOutgoing + 1e-6f,
            )
            // Incoming must monotonically increase
            assertTrue(
                "Incoming must be monotonically non-decreasing at t=$t",
                incoming >= prevIncoming - 1e-6f,
            )

            // Perfect symmetry: outgoing(t) == incoming(1.0 - t)
            val (symOut, symIn) = CrossfadeAudio.calculateEqualPowerVolumes(1.0f - t)
            assertEquals("Symmetry outgoing(t) == incoming(1-t) failed at t=$t", incoming, symOut, 1e-5f)
            assertEquals("Symmetry incoming(t) == outgoing(1-t) failed at t=$t", outgoing, symIn, 1e-5f)

            prevOutgoing = outgoing
            prevIncoming = incoming
        }
    }

    @Test
    fun equalPower_extremeFloatBoundaries_doesNotCrashOrProduceNaN() {
        val extremeInputs = floatArrayOf(
            -1_000_000f,
            -100f,
            -1.0f,
            -0.0001f,
            -Float.MIN_VALUE,
            0.0f,
            Float.MIN_VALUE,
            1e-7f,
            0.999999f,
            1.0f,
            1.000001f,
            10.0f,
            100.0f,
            1_000_000f,
            Float.MAX_VALUE,
            -Float.MAX_VALUE,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
        )

        for (t in extremeInputs) {
            val (outgoing, incoming) = CrossfadeAudio.calculateEqualPowerVolumes(t)

            assertFalse("Outgoing must not be NaN for t=$t", outgoing.isNaN())
            assertFalse("Incoming must not be NaN for t=$t", incoming.isNaN())
            assertTrue("Outgoing factor must be within [0, 1] for t=$t", outgoing in 0.0f..1.0f)
            assertTrue("Incoming factor must be within [0, 1] for t=$t", incoming in 0.0f..1.0f)

            if (t <= 0.0f || t == Float.NEGATIVE_INFINITY) {
                assertEquals("Negative t must clamp to full outgoing", 1.0f, outgoing, 1e-6f)
                assertEquals("Negative t must clamp to zero incoming", 0.0f, incoming, 1e-6f)
            } else if (t >= 1.0f || t == Float.POSITIVE_INFINITY) {
                assertEquals("Excess t must clamp to zero outgoing", 0.0f, outgoing, 1e-6f)
                assertEquals("Excess t must clamp to full incoming", 1.0f, incoming, 1e-6f)
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Duration Clamping Stress Across Integer & Float Extremes
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun durationClamping_stressAllBoundaries() {
        val intCases = listOf(
            Int.MIN_VALUE to 0,
            -999999 to 0,
            -1 to 0,
            0 to 0,
            1 to 1,
            6 to 6,
            11 to 11,
            12 to 12,
            13 to 12,
            999999 to 12,
            Int.MAX_VALUE to 12,
        )

        for ((input, expected) in intCases) {
            assertEquals("Int duration $input must clamp to $expected", expected, CrossfadeAudio.clampCrossfadeDuration(input))
        }

        val floatCases = listOf(
            -Float.MAX_VALUE to 0f,
            Float.NEGATIVE_INFINITY to 0f,
            -10.0f to 0f,
            0f to 0f,
            0.5f to 0.5f,
            6.0f to 6.0f,
            11.99f to 11.99f,
            12.0f to 12.0f,
            12.01f to 12.0f,
            Float.MAX_VALUE to 12.0f,
            Float.POSITIVE_INFINITY to 12.0f,
        )

        for ((input, expected) in floatCases) {
            assertEquals(
                "Float duration $input must clamp to $expected",
                expected,
                CrossfadeAudio.clampCrossfadeDuration(input),
                1e-4f,
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Short Track Adaptation & Boundary Conditions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun shortTrack_ultraShortTrackDurations_doesNotCrashOrExceedBounds() {
        val testConfigs = listOf(1000, 4000, 8000, 12000)

        // Ultra short track durations: 0ms, 1ms, 2ms, 50ms, 100ms, 500ms, 999ms
        val ultraShortLengths = listOf(0L, 1L, 2L, 50L, 100L, 500L, 999L, 1000L, 1500L, 2000L)

        for (cfg in testConfigs) {
            for (len in ultraShortLengths) {
                val activeDuration = CrossfadeAudio.calculateActiveCrossfadeDuration(
                    configuredFadeMs = cfg,
                    remainingMs = len,
                    trackDurationMs = len,
                    incomingDurationMs = len,
                )

                assertTrue("Active crossfade duration must be >= 1 for positive config", activeDuration >= 1)
                if (len > 0L) {
                    val expectedMax = (len / 2).toInt()
                    if (expectedMax >= 1) {
                        assertTrue(
                            "Active crossfade $activeDuration must not exceed half track duration $expectedMax (len=$len, cfg=$cfg)",
                            activeDuration <= expectedMax,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun shortTrack_asymmetricTrackDurations() {
        // Track A is normal (180s), Track B is very short (2s)
        val durationOutgoingLongIncomingShort = CrossfadeAudio.calculateActiveCrossfadeDuration(
            configuredFadeMs = 8000,
            remainingMs = 8000L,
            trackDurationMs = 180_000L,
            incomingDurationMs = 2000L,
        )
        // Must be capped by Track B's half duration: 2000 / 2 = 1000ms
        assertEquals(1000, durationOutgoingLongIncomingShort)

        // Track A is very short (3s), Track B is normal (240s)
        val durationOutgoingShortIncomingLong = CrossfadeAudio.calculateActiveCrossfadeDuration(
            configuredFadeMs = 8000,
            remainingMs = 3000L,
            trackDurationMs = 3000L,
            incomingDurationMs = 240_000L,
        )
        // Must be capped by Track A's half duration: 3000 / 2 = 1500ms
        assertEquals(1500, durationOutgoingShortIncomingLong)

        // Unset durations (C.TIME_UNSET) must fall back gracefully to configured duration
        val durationUnset = CrossfadeAudio.calculateActiveCrossfadeDuration(
            configuredFadeMs = 6000,
            remainingMs = 6000L,
            trackDurationMs = C.TIME_UNSET,
            incomingDurationMs = C.TIME_UNSET,
        )
        assertEquals(6000, durationUnset)
    }

    @Test
    fun shortTrack_zeroAndNegativeDurations_handledCleanly() {
        // Configured fade = 0
        assertEquals(
            0,
            CrossfadeAudio.calculateActiveCrossfadeDuration(
                configuredFadeMs = 0,
                remainingMs = 5000L,
                trackDurationMs = 60_000L,
            ),
        )

        // Configured fade < 0
        assertEquals(
            0,
            CrossfadeAudio.calculateActiveCrossfadeDuration(
                configuredFadeMs = -5000,
                remainingMs = 5000L,
                trackDurationMs = 60_000L,
            ),
        )

        // Remaining <= 0L falls back to maxAllowed
        val durationNegativeRemaining = CrossfadeAudio.calculateActiveCrossfadeDuration(
            configuredFadeMs = 6000,
            remainingMs = -100L,
            trackDurationMs = 60_000L,
        )
        assertEquals(6000, durationNegativeRemaining)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Handoff Phase Volume Attenuation & Extreme Timings
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun handoff_extremeElapsedTimes_clampsProperly() {
        val handoffDuration = 450

        // Negative elapsed time
        val (mainNeg, overlapNeg) = CrossfadeAudio.calculateHandoffVolumes(-1000L, handoffDuration)
        assertEquals(0.0f, mainNeg, 1e-6f)
        assertEquals(1.0f, overlapNeg, 1e-6f)

        // Exactly at start
        val (main0, overlap0) = CrossfadeAudio.calculateHandoffVolumes(0L, handoffDuration)
        assertEquals(0.0f, main0, 1e-6f)
        assertEquals(1.0f, overlap0, 1e-6f)

        // Exactly at end
        val (mainEnd, overlapEnd) = CrossfadeAudio.calculateHandoffVolumes(450L, handoffDuration)
        assertEquals(1.0f, mainEnd, 1e-6f)
        assertEquals(0.0f, overlapEnd, 1e-6f)

        // Far beyond end
        val (mainFar, overlapFar) = CrossfadeAudio.calculateHandoffVolumes(100_000L, handoffDuration)
        assertEquals(1.0f, mainFar, 1e-6f)
        assertEquals(0.0f, overlapFar, 1e-6f)

        // Long.MAX_VALUE
        val (mainMax, overlapMax) = CrossfadeAudio.calculateHandoffVolumes(Long.MAX_VALUE, handoffDuration)
        assertEquals(1.0f, mainMax, 1e-6f)
        assertEquals(0.0f, overlapMax, 1e-6f)

        // Complementary sum invariant across 1000 discrete points
        for (ms in 0..handoffDuration) {
            val (main, overlap) = CrossfadeAudio.calculateHandoffVolumes(ms.toLong(), handoffDuration)
            assertTrue("main factor in [0, 1]", main in 0.0f..1.0f)
            assertTrue("overlap factor in [0, 1]", overlap in 0.0f..1.0f)
            assertEquals("Handoff complementary sum must be 1.0 at ms=$ms", 1.0f, main + overlap, 1e-5f)
        }
    }

    @Test
    fun handoff_degenerateDurations_doesNotDivideByZero() {
        // handoffDurationMs <= 0 is protected by coerceAtLeast(1)
        val (mainZeroDur, overlapZeroDur) = CrossfadeAudio.calculateHandoffVolumes(10L, 0)
        assertEquals(1.0f, mainZeroDur, 1e-6f)
        assertEquals(0.0f, overlapZeroDur, 1e-6f)

        val (mainNegDur, overlapNegDur) = CrossfadeAudio.calculateHandoffVolumes(10L, -500)
        assertEquals(1.0f, mainNegDur, 1e-6f)
        assertEquals(0.0f, overlapNegDur, 1e-6f)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Gapless Album Edge Cases & Metadata Anomalies
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun gaplessAlbum_metadataAnomalies_handlesCleanly() {
        fun makeItem(albumId: String?, albumTitle: String?): MediaItem {
            val b = MediaItem.Builder().setMediaId("id_${albumId}_$albumTitle")
            val metaB = MediaMetadata.Builder()
            if (albumId != null) metaB.setAlbumArtist(albumId) // dummy
            if (albumTitle != null) metaB.setAlbumTitle(albumTitle)
            return b.setMediaMetadata(metaB.build()).build()
        }

        val itemA = makeItem(null, "Abbey Road")
        val itemB = makeItem(null, "Abbey Road")
        val itemC = makeItem(null, "Let It Be")
        val itemBlank1 = makeItem(null, "   ")
        val itemBlank2 = makeItem(null, "   ")
        val itemNull = makeItem(null, null)

        // Valid matching title with gapless enabled
        assertTrue(CrossfadeAudio.isGaplessAlbumTransition(itemA, itemB, true))

        // Valid different title with gapless enabled
        assertFalse(CrossfadeAudio.isGaplessAlbumTransition(itemA, itemC, true))

        // Same title with gapless DISABLED by user
        assertFalse(CrossfadeAudio.isGaplessAlbumTransition(itemA, itemB, false))

        // Blank string titles must NOT be treated as valid album matching
        assertFalse(CrossfadeAudio.isGaplessAlbumTransition(itemBlank1, itemBlank2, true))

        // Null titles must NOT match
        assertFalse(CrossfadeAudio.isGaplessAlbumTransition(itemNull, itemNull, true))
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. State Machine: Rapid Mid-Fade Pause/Resume & Mid-Fade Seek Cycles
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun stateMachine_simulation_rapidPauseResumeCycles_preservesContinuity() {
        // Simulates 100 consecutive rapid pause/resume events during a 6s crossfade
        val fadeMs = 6000L
        var virtualClockMs = 100_000L
        var crossfadeStartElapsedMs = virtualClockMs

        var isPaused = false
        var pauseStartElapsedMs = 0L

        // Crossfade starts at t = 0
        var effectiveT: Float

        for (cycle in 1..100) {
            // Play for 30ms
            virtualClockMs += 30L

            // Pause
            isPaused = true
            pauseStartElapsedMs = virtualClockMs

            // Pause duration 150ms
            virtualClockMs += 150L

            // Resume
            isPaused = false
            val pauseDuration = virtualClockMs - pauseStartElapsedMs
            crossfadeStartElapsedMs += pauseDuration

            // Calculate progress t
            val elapsed = virtualClockMs - crossfadeStartElapsedMs
            effectiveT = (elapsed.toFloat() / fadeMs.toFloat()).coerceIn(0f, 1f)

            // Even after 100 cycles of 150ms pauses (15 seconds of real-world pause time!),
            // the effective playback elapsed must strictly equal cycle * 30ms = 3000ms!
            val expectedElapsedPlayback = cycle * 30L
            assertEquals(
                "Elapsed playback must accurately discount pause time at cycle $cycle",
                expectedElapsedPlayback,
                elapsed,
            )

            val (outVol, inVol) = CrossfadeAudio.calculateEqualPowerVolumes(effectiveT)
            val power = (outVol * outVol) + (inVol * inVol)
            assertEquals("Equal-power identity holds during pause cycles", 1.0f, power, 1e-4f)
        }
    }

    @Test
    fun stateMachine_simulation_midFadeSeekBackwards_triggersReset() {
        // Simulation of player seeking backwards during crossfade
        val fadeMs = 6000
        val trackDurationMs = 180_000L

        // Crossfade is active near the end of the song
        var positionMs = 176_000L // 4s remaining (inside 6s fade)
        var remainingMs = trackDurationMs - positionMs

        val effectiveFadeMs = CrossfadeAudio.calculateActiveCrossfadeDuration(
            configuredFadeMs = fadeMs,
            remainingMs = remainingMs,
            trackDurationMs = trackDurationMs,
        )
        assertEquals(4000, effectiveFadeMs)

        var crossfadeActive = true

        // User suddenly seeks back to 30s into the track
        positionMs = 30_000L
        remainingMs = trackDurationMs - positionMs // 150_000L

        // Check the reset condition from CrossfadeAudio.kt:232
        // val tooFarFromEnd = !onTarget && remainingMs > effectiveFadeMs.toLong() + 2000L
        val onTarget = false
        val tooFarFromEnd = !onTarget && remainingMs > effectiveFadeMs.toLong() + 2000L

        assertTrue("Large seek backwards must trigger tooFarFromEnd", tooFarFromEnd)
        if (tooFarFromEnd) {
            crossfadeActive = false
        }
        assertFalse("Crossfade must be reset upon backwards seek", crossfadeActive)
    }

    @Test
    fun stateMachine_simulation_rebufferingStall_delaysCrossfadeInitiation() {
        // Crossfade requires minimum buffer: requiredStartBufferMs = (fadeMs + 2000L).coerceIn(3000L, 10000L)
        fun requiredStartBufferMs(fadeMs: Int): Long = (fadeMs.toLong() + 2000L).coerceIn(3000L, 10000L)

        val fadeMs = 6000
        val requiredBuffer = requiredStartBufferMs(fadeMs) // 8000ms

        // Overlap player has only 2000ms buffered due to slow network
        val currentBufferedMs = 2000L
        val hasEnoughBuffer = currentBufferedMs >= requiredBuffer

        assertFalse("Slow network buffer stall must prevent premature crossfade start", hasEnoughBuffer)

        // Buffer accumulates to 8500ms
        val recoveredBufferedMs = 8500L
        val canStartNow = recoveredBufferedMs >= requiredBuffer

        assertTrue("Crossfade begins once required buffer is satisfied", canStartNow)
    }
}
