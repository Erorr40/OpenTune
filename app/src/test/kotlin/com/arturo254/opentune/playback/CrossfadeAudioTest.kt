package com.arturo254.opentune.playback

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossfadeAudioTest {

    // ── 1. Equal-Power Mathematical Identity Tests ────────────────────────────

    @Test
    fun equalPowerMathematicalIdentity_acrossNormalizedProgress() {
        val epsilon = 1e-4f

        for (step in 0..100) {
            val t = step / 100.0f
            val (outgoing, incoming) = CrossfadeAudio.calculateEqualPowerVolumes(t)

            // Verify individual volume factors are within valid acoustic limits [0, 1]
            assertTrue("Outgoing factor $outgoing at t=$t must be in [0, 1]", outgoing in 0f..1f)
            assertTrue("Incoming factor $incoming at t=$t must be in [0, 1]", incoming in 0f..1f)

            // Equal-power identity: cos²(t * π/2) + sin²(t * π/2) = 1.0 (constant acoustic power, 0 dB dip)
            val totalPower = (outgoing * outgoing) + (incoming * incoming)
            assertEquals(
                "Acoustic power at t=$t must equal 1.0 (0 dB dip)",
                1.0f,
                totalPower,
                epsilon,
            )
        }
    }

    @Test
    fun equalPowerMathematicalIdentity_boundaryValues() {
        val epsilon = 1e-5f

        // t = 0.0: Outgoing at 100%, incoming at 0%
        val (out0, in0) = CrossfadeAudio.calculateEqualPowerVolumes(0.0f)
        assertEquals(1.0f, out0, epsilon)
        assertEquals(0.0f, in0, epsilon)

        // t = 0.5: Midpoint equal power (both at sqrt(2)/2 ≈ 0.7071)
        val (outMid, inMid) = CrossfadeAudio.calculateEqualPowerVolumes(0.5f)
        val expectedMid = (Math.sqrt(2.0) / 2.0).toFloat()
        assertEquals(expectedMid, outMid, epsilon)
        assertEquals(expectedMid, inMid, epsilon)
        assertEquals(1.0f, (outMid * outMid) + (inMid * inMid), epsilon)

        // t = 1.0: Outgoing at 0%, incoming at 100%
        val (out1, in1) = CrossfadeAudio.calculateEqualPowerVolumes(1.0f)
        assertEquals(0.0f, out1, epsilon)
        assertEquals(1.0f, in1, epsilon)
    }

    @Test
    fun equalPowerMathematicalIdentity_clampsOutOfBoundsProgress() {
        val epsilon = 1e-5f

        // Negative t clamps to 0.0
        val (outNeg, inNeg) = CrossfadeAudio.calculateEqualPowerVolumes(-0.5f)
        assertEquals(1.0f, outNeg, epsilon)
        assertEquals(0.0f, inNeg, epsilon)

        // Excess t (> 1.0) clamps to 1.0
        val (outExcess, inExcess) = CrossfadeAudio.calculateEqualPowerVolumes(1.5f)
        assertEquals(0.0f, outExcess, epsilon)
        assertEquals(1.0f, inExcess, epsilon)
    }

    // ── 2. Duration Clamping Tests (0..12 Seconds) ────────────────────────────

    @Test
    fun durationClamping_limitsIntSecondsToZeroToTwelve() {
        // Lower bound clamping
        assertEquals(0, CrossfadeAudio.clampCrossfadeDuration(-10))
        assertEquals(0, CrossfadeAudio.clampCrossfadeDuration(-1))
        assertEquals(0, CrossfadeAudio.clampCrossfadeDuration(0))

        // In-range values
        assertEquals(1, CrossfadeAudio.clampCrossfadeDuration(1))
        assertEquals(2, CrossfadeAudio.clampCrossfadeDuration(2))
        assertEquals(6, CrossfadeAudio.clampCrossfadeDuration(6))
        assertEquals(8, CrossfadeAudio.clampCrossfadeDuration(8))
        assertEquals(10, CrossfadeAudio.clampCrossfadeDuration(10))
        assertEquals(12, CrossfadeAudio.clampCrossfadeDuration(12))

        // Upper bound clamping
        assertEquals(12, CrossfadeAudio.clampCrossfadeDuration(13))
        assertEquals(12, CrossfadeAudio.clampCrossfadeDuration(20))
        assertEquals(12, CrossfadeAudio.clampCrossfadeDuration(100))
    }

    @Test
    fun durationClamping_limitsFloatSecondsToZeroToTwelve() {
        val epsilon = 1e-5f

        assertEquals(0f, CrossfadeAudio.clampCrossfadeDuration(-5.0f), epsilon)
        assertEquals(0f, CrossfadeAudio.clampCrossfadeDuration(0f), epsilon)
        assertEquals(4.5f, CrossfadeAudio.clampCrossfadeDuration(4.5f), epsilon)
        assertEquals(12f, CrossfadeAudio.clampCrossfadeDuration(12f), epsilon)
        assertEquals(12f, CrossfadeAudio.clampCrossfadeDuration(15.5f), epsilon)
    }

    // ── 3. Short Track Duration Adaptation Tests ──────────────────────────────

    @Test
    fun shortTrackDurationAdaptation_capsCrossfadeToHalfDuration() {
        // Track shorter than configured fade:
        // Configured: 8000ms (8s), Track length: 4000ms (4s)
        // Must be capped to min(8000, 4000 / 2) = 2000ms
        val durationShort = CrossfadeAudio.calculateActiveCrossfadeDuration(
            configuredFadeMs = 8000,
            remainingMs = 4000L,
            trackDurationMs = 4000L,
        )
        assertEquals(2000, durationShort)

        // Track shorter than 2x fade:
        // Configured: 6000ms (6s), Track length: 8000ms (8s)
        // Must be capped to min(6000, 8000 / 2) = 4000ms
        val durationMid = CrossfadeAudio.calculateActiveCrossfadeDuration(
            configuredFadeMs = 6000,
            remainingMs = 6000L,
            trackDurationMs = 8000L,
        )
        assertEquals(4000, durationMid)

        // Incoming track is short:
        // Configured: 10000ms (10s), Outgoing: 120000ms (2m), Incoming: 6000ms (6s)
        // Must be capped to min(10000, 120000/2, 6000/2) = 3000ms
        val durationIncomingShort = CrossfadeAudio.calculateActiveCrossfadeDuration(
            configuredFadeMs = 10000,
            remainingMs = 10000L,
            trackDurationMs = 120000L,
            incomingDurationMs = 6000L,
        )
        assertEquals(3000, durationIncomingShort)

        // Normal full-length tracks (e.g. 3 minutes = 180000ms):
        // Configured: 6000ms -> full 6000ms used
        val durationNormal = CrossfadeAudio.calculateActiveCrossfadeDuration(
            configuredFadeMs = 6000,
            remainingMs = 6000L,
            trackDurationMs = 180000L,
            incomingDurationMs = 240000L,
        )
        assertEquals(6000, durationNormal)
    }

    @Test
    fun shortTrackDurationAdaptation_boundsAndEdgeCases() {
        // Configured crossfade disabled (0ms or negative)
        assertEquals(
            0,
            CrossfadeAudio.calculateActiveCrossfadeDuration(
                configuredFadeMs = 0,
                remainingMs = 5000L,
                trackDurationMs = 180000L,
            ),
        )
        assertEquals(
            0,
            CrossfadeAudio.calculateActiveCrossfadeDuration(
                configuredFadeMs = -1000,
                remainingMs = 5000L,
                trackDurationMs = 180000L,
            ),
        )

        // Remaining time is less than configured crossfade
        // Configured: 6000ms, Remaining: 2500ms -> should cap to remainingMs = 2500ms
        val durationRemaining = CrossfadeAudio.calculateActiveCrossfadeDuration(
            configuredFadeMs = 6000,
            remainingMs = 2500L,
            trackDurationMs = 180000L,
        )
        assertEquals(2500, durationRemaining)

        // Unknown / unset track duration (C.TIME_UNSET) -> fallback to configured fade
        val durationUnset = CrossfadeAudio.calculateActiveCrossfadeDuration(
            configuredFadeMs = 5000,
            remainingMs = 5000L,
            trackDurationMs = C.TIME_UNSET,
            incomingDurationMs = C.TIME_UNSET,
        )
        assertEquals(5000, durationUnset)
    }

    // ── 4. Handoff Parameter Tests ────────────────────────────────────────────

    @Test
    fun handoffVolumeParameters_rampsSmoothlyAcrossDuration() {
        val epsilon = 1e-5f
        val handoffDuration = 450

        // At start (elapsed = 0ms): Primary player muted (0.0), overlap player full (1.0)
        val (mainStart, overlapStart) = CrossfadeAudio.calculateHandoffVolumes(0L, handoffDuration)
        assertEquals(0.0f, mainStart, epsilon)
        assertEquals(1.0f, overlapStart, epsilon)

        // At midpoint (elapsed = 225ms): Both at 50%
        val (mainMid, overlapMid) = CrossfadeAudio.calculateHandoffVolumes(225L, handoffDuration)
        assertEquals(0.5f, mainMid, epsilon)
        assertEquals(0.5f, overlapMid, epsilon)

        // At end (elapsed = 450ms): Primary player full (1.0), overlap player muted (0.0)
        val (mainEnd, overlapEnd) = CrossfadeAudio.calculateHandoffVolumes(450L, handoffDuration)
        assertEquals(1.0f, mainEnd, epsilon)
        assertEquals(0.0f, overlapEnd, epsilon)

        // Beyond end (elapsed = 600ms): Clamped cleanly at 1.0 / 0.0
        val (mainOver, overlapOver) = CrossfadeAudio.calculateHandoffVolumes(600L, handoffDuration)
        assertEquals(1.0f, mainOver, epsilon)
        assertEquals(0.0f, overlapOver, epsilon)

        // Negative elapsed: Clamped cleanly at 0.0 / 1.0
        val (mainNeg, overlapNeg) = CrossfadeAudio.calculateHandoffVolumes(-50L, handoffDuration)
        assertEquals(0.0f, mainNeg, epsilon)
        assertEquals(1.0f, overlapNeg, epsilon)

        // Linear complementary sum across whole handoff range: main + overlap == 1.0
        for (elapsed in 0..handoffDuration step 25) {
            val (main, overlap) = CrossfadeAudio.calculateHandoffVolumes(elapsed.toLong(), handoffDuration)
            assertEquals("Handoff sum at elapsed=$elapsed must be 1.0", 1.0f, main + overlap, epsilon)
        }
    }

    // ── 5. Gapless Transition Evaluation Tests ────────────────────────────────

    @Test
    fun gaplessAlbumTransition_respectsUserSettingAndMetadata() {
        val albumItemA = MediaItem.Builder()
            .setMediaId("song_1")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setAlbumTitle("Abbey Road")
                    .build()
            )
            .build()

        val albumItemB = MediaItem.Builder()
            .setMediaId("song_2")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setAlbumTitle("Abbey Road")
                    .build()
            )
            .build()

        val differentAlbumItem = MediaItem.Builder()
            .setMediaId("song_3")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setAlbumTitle("Dark Side of the Moon")
                    .build()
            )
            .build()

        // When user disabled gapless album bypass (isGaplessAlbumEnabled = false):
        // Crossfade should NOT be bypassed even between tracks of the same album!
        assertFalse(
            "When user gapless setting is off, transitions between album tracks must NOT bypass crossfade",
            CrossfadeAudio.isGaplessAlbumTransition(albumItemA, albumItemB, isGaplessAlbumEnabled = false),
        )

        // When user enabled gapless album bypass (isGaplessAlbumEnabled = true):
        // Same album tracks should bypass crossfade (return true)
        assertTrue(
            "When user gapless setting is on, transitions between same-album tracks MUST bypass crossfade",
            CrossfadeAudio.isGaplessAlbumTransition(albumItemA, albumItemB, isGaplessAlbumEnabled = true),
        )

        // Different album tracks should NOT bypass crossfade even if gapless setting is on
        assertFalse(
            "Tracks from different albums must NOT bypass crossfade",
            CrossfadeAudio.isGaplessAlbumTransition(albumItemA, differentAlbumItem, isGaplessAlbumEnabled = true),
        )
    }

    // ── 6. Spotify-Style Automix & Preset Tests ──────────────────────────────

    @Test
    fun automixDurationResolution_providesSensibleDefaults() {
        fun resolveEffectiveCrossfadeMs(configuredSeconds: Int, automixEnabled: Boolean): Int {
            val effectiveSec = if (automixEnabled && configuredSeconds == 0) 5 else configuredSeconds
            return effectiveSec * 1000
        }

        // When Automix is on but crossfade is 0s, defaults to 5000ms
        assertEquals(5000, resolveEffectiveCrossfadeMs(0, automixEnabled = true))

        // When Automix is on and user selected 8s, honors user 8000ms
        assertEquals(8000, resolveEffectiveCrossfadeMs(8, automixEnabled = true))

        // When Automix is off, 0s yields 0ms (disabled)
        assertEquals(0, resolveEffectiveCrossfadeMs(0, automixEnabled = false))

        // When Automix is off, 6s yields 6000ms
        assertEquals(6000, resolveEffectiveCrossfadeMs(6, automixEnabled = false))
    }

    @Test
    fun spotifyPresets_allFallWithinValidDurationBounds() {
        val presets = listOf(0, 3, 6, 9, 12)
        for (preset in presets) {
            val clamped = CrossfadeAudio.clampCrossfadeDuration(preset)
            assertEquals("Preset $preset must equal clamped duration", preset, clamped)
            assertTrue("Preset $preset must be in [0, 12]", clamped in 0..12)
        }
    }
}
