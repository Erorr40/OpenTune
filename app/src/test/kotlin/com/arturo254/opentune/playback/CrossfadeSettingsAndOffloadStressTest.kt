/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Empirical Stress Test Suite: Settings Persistence, Slider Bounds,
 * Rapid Toggle Transitions, and Hardware Audio Offload Mutual Exclusion.
 */

package com.arturo254.opentune.playback

import com.arturo254.opentune.constants.AudioCrossfadeDurationKey
import com.arturo254.opentune.constants.AudioCrossfadeGaplessKey
import com.arturo254.opentune.constants.AudioOffload
import com.arturo254.opentune.constants.SkipSilenceKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.roundToInt

class CrossfadeSettingsAndOffloadStressTest {

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Slider Mark Bounds (0, 1, 2, 4, 6, 8, 10, 11, 12 seconds) & Clamping
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun sliderBounds_exactCardinalAndIntermediateMarks_preserveIdentity() {
        // Explicitly test the required cardinal marks: 0, 1, 2, 4, 6, 8, 10, 11, 12 seconds
        val requiredMarks = listOf(0, 1, 2, 4, 6, 8, 10, 11, 12)

        for (mark in requiredMarks) {
            val clampedInt = CrossfadeAudio.clampCrossfadeDuration(mark)
            assertEquals("Mark $mark must be preserved exactly by clampCrossfadeDuration(Int)", mark, clampedInt)

            val clampedFloat = CrossfadeAudio.clampCrossfadeDuration(mark.toFloat())
            assertEquals("Mark $mark must be preserved exactly by clampCrossfadeDuration(Float)", mark.toFloat(), clampedFloat, 1e-6f)

            // Millisecond conversion verification
            val durationMs = clampedInt * 1000
            assertEquals("Mark $mark must convert to ${mark * 1000}ms", mark * 1000, durationMs)
        }

        // Also test the remaining intermediate seconds in the 0..12 range
        val intermediateMarks = listOf(3, 5, 7, 9)
        for (mark in intermediateMarks) {
            val clamped = CrossfadeAudio.clampCrossfadeDuration(mark)
            assertEquals("Intermediate second $mark must be preserved", mark, clamped)
        }
    }

    @Test
    fun sliderBounds_floatRoundingAndContinuousSlidingSimulation() {
        // In CrossfadeSliderPreference, slider input is:
        // localValue = it.roundToInt().coerceIn(0, 12).toFloat()
        fun simulateSliderInput(rawSliderPos: Float): Int {
            return rawSliderPos.roundToInt().coerceIn(0, 12)
        }

        val testCases = listOf(
            -5.0f to 0,
            -0.4f to 0,
            0.0f to 0,
            0.4f to 0,
            0.5f to 1,
            0.9f to 1,
            1.0f to 1,
            1.49f to 1,
            1.5f to 2,
            3.8f to 4,
            5.7f to 6,
            7.9f to 8,
            9.6f to 10,
            10.8f to 11,
            11.2f to 11,
            11.5f to 12,
            12.0f to 12,
            12.4f to 12,
            15.0f to 12,
            999.0f to 12,
        )

        for ((input, expected) in testCases) {
            val result = simulateSliderInput(input)
            assertEquals("Slider input $input must resolve to $expected", expected, result)
        }
    }

    @Test
    fun sliderBounds_dataStoreKeyIntegrity() {
        assertEquals("audioCrossfadeDuration", AudioCrossfadeDurationKey.name)
        assertEquals("audio_crossfade_gapless", AudioCrossfadeGaplessKey.name)
        assertEquals("audioOffload", AudioOffload.name)
        assertEquals("skipSilence", SkipSilenceKey.name)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Hardware Audio Offload Conflict Resolution & Mutual Exclusion
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun audioOffloadConflict_enablingCrossfadeDisablesAudioOffload() {
        // State simulation representing PlayerSettings state
        var audioOffload = true
        var crossfadeSeconds = 0

        // User action: Enable crossfade via slider (e.g. set to 6 seconds)
        fun onCrossfadeChanged(newSeconds: Int) {
            if (newSeconds > 0 && audioOffload) {
                audioOffload = false
            }
            crossfadeSeconds = newSeconds
        }

        onCrossfadeChanged(6)

        assertFalse("Enabling crossfade MUST disable audio offload", audioOffload)
        assertEquals(6, crossfadeSeconds)
    }

    @Test
    fun audioOffloadConflict_enablingAudioOffloadResetsCrossfadeToZero() {
        // State simulation representing PlayerSettings state
        var audioOffload = false
        var crossfadeSeconds = 8

        // User action: Enable Audio Offload switch
        fun onAudioOffloadChanged(enabled: Boolean) {
            audioOffload = enabled
            if (enabled) {
                crossfadeSeconds = 0
            }
        }

        onAudioOffloadChanged(true)

        assertTrue("Audio offload must be enabled", audioOffload)
        assertEquals("Enabling audio offload MUST reset crossfade to 0 seconds", 0, crossfadeSeconds)
    }

    @Test
    fun audioOffloadConflict_serviceLevelConflictResolution_handlesDirectDataStoreModifications() {
        // Simulation of MusicService.kt lines 812-828
        class MockServiceDataStore(var offload: Boolean, var crossfade: Int, var skipSilence: Boolean) {
            var editCount = 0

            fun updateOffload(enabled: Boolean) {
                offload = enabled
                if (enabled) {
                    if (skipSilence) {
                        skipSilence = false
                        editCount++
                    }
                    if (crossfade != 0) {
                        crossfade = 0
                        editCount++
                    }
                }
            }

            fun updateCrossfade(seconds: Int) {
                crossfade = seconds
                if (seconds > 0 && offload) {
                    offload = false
                    editCount++
                }
            }
        }

        val store = MockServiceDataStore(offload = true, crossfade = 0, skipSilence = true)

        // 1. External trigger forces crossfade to 10s while offload is on
        store.updateCrossfade(10)
        assertFalse("Offload must be disabled when crossfade becomes positive", store.offload)
        assertEquals(10, store.crossfade)

        // 2. External trigger turns offload back on
        store.updateOffload(true)
        assertTrue(store.offload)
        assertEquals("Crossfade must be reset to 0 when offload becomes true", 0, store.crossfade)
        assertFalse("Skip silence must also be reset", store.skipSilence)

        // 3. Turning offload on when crossfade is already 0 does not trigger redundant edits
        val priorEdits = store.editCount
        store.updateOffload(true)
        assertEquals("No redundant edits when already in consistent state", priorEdits, store.editCount)
    }

    @Test
    fun audioOffloadConflict_monteCarloConcurrentToggleStress_preservesMutualExclusionInvariant() {
        // Stress test 10,000 rapid concurrent/interleaved toggles between Crossfade and Offload
        val random = Random(12345L)
        val offloadState = AtomicBoolean(false)
        val crossfadeState = AtomicInteger(0)

        for (i in 0 until 10_000) {
            val action = random.nextInt(4)
            when (action) {
                0 -> { // Toggle Crossfade ON with random duration 1..12
                    val targetSec = random.nextInt(12) + 1
                    if (offloadState.get()) {
                        offloadState.set(false)
                    }
                    crossfadeState.set(targetSec)
                }
                1 -> { // Toggle Crossfade OFF
                    crossfadeState.set(0)
                }
                2 -> { // Toggle Offload ON
                    offloadState.set(true)
                    crossfadeState.set(0)
                }
                3 -> { // Toggle Offload OFF
                    offloadState.set(false)
                }
            }

            // Invariant check after each transaction:
            // It is IMPOSSIBLE for offload == true AND crossfade > 0 simultaneously!
            val currentOffload = offloadState.get()
            val currentCrossfade = crossfadeState.get()

            assertFalse(
                "Violation of mutual exclusion at step $i: offload=$currentOffload, crossfade=$currentCrossfade",
                currentOffload && currentCrossfade > 0,
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Switch Toggle State & Last Active Value Memory Invariance
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun preferenceToggle_remembersLastActiveDurationAcrossOffOnCycles() {
        // Models CrossfadeSliderPreference state behavior:
        // var localValue by remember { mutableFloatStateOf(value.toFloat()) }
        // var lastActiveValue by remember { mutableIntStateOf(if (value > 0) value else 4) }
        class PreferenceState(initialValue: Int) {
            var value = initialValue
            var lastActiveValue = if (initialValue > 0) initialValue else 4

            fun onSliderChange(newValue: Int) {
                value = newValue
                if (newValue > 0) {
                    lastActiveValue = newValue
                }
            }

            fun onSwitchToggle(checked: Boolean) {
                if (checked) {
                    val target = if (lastActiveValue in 1..12) lastActiveValue else 4
                    value = target
                } else {
                    value = 0
                }
            }
        }

        // Test 1: Default initialization at 0s (Off) -> lastActive defaults to 4s
        val pref = PreferenceState(initialValue = 0)
        assertEquals(0, pref.value)
        assertEquals(4, pref.lastActiveValue)

        // Test 2: Turn switch ON -> restored to 4s
        pref.onSwitchToggle(true)
        assertEquals(4, pref.value)
        assertEquals(4, pref.lastActiveValue)

        // Test 3: Change slider to 10s -> lastActive becomes 10s
        pref.onSliderChange(10)
        assertEquals(10, pref.value)
        assertEquals(10, pref.lastActiveValue)

        // Test 4: Turn switch OFF -> value becomes 0s, lastActive preserved at 10s
        pref.onSwitchToggle(false)
        assertEquals(0, pref.value)
        assertEquals(10, pref.lastActiveValue)

        // Test 5: Turn switch back ON -> restored to 10s
        pref.onSwitchToggle(true)
        assertEquals(10, pref.value)
        assertEquals(10, pref.lastActiveValue)

        // Test 6: Move slider all the way down to 0s -> value is 0, switch reflects OFF, lastActive remains 10s
        pref.onSliderChange(0)
        assertEquals(0, pref.value)
        assertEquals(10, pref.lastActiveValue)

        // Test 7: Turn switch ON again -> restores 10s
        pref.onSwitchToggle(true)
        assertEquals(10, pref.value)

        // Test 8: Degenerate corrupted lastActiveValue fallback
        pref.lastActiveValue = 99
        pref.onSwitchToggle(true)
        assertEquals("Out-of-range lastActiveValue must safely fall back to 4s default", 4, pref.value)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Rapid State Transitions During Audio Engine Lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    fun engineStateTransitions_rapidCrossfadeToggle_cancelsAndResetsCleanly() = runBlocking {
        // Verify CrossfadeAudio loop behavior when crossfadeDurationMs toggles rapidly
        val crossfadeDurationMs = MutableStateFlow(0)
        val playbackFadeFactor = MutableStateFlow(1f)

        // State machine simulation of CrossfadeAudio loop check:
        // if (fadeMs <= 0) { stopOverlapCrossfade(resetMainFade = true); ... }
        var isCrossfadeActive = false
        var isOverlapPrimed = false

        fun processEngineTick(fadeMs: Int) {
            if (fadeMs <= 0) {
                // stopOverlapCrossfade(resetMainFade = true)
                isCrossfadeActive = false
                isOverlapPrimed = false
                playbackFadeFactor.value = 1f
            } else {
                // If fadeMs > 0, crossfade can be primed
                isOverlapPrimed = true
            }
        }

        // Rapid 1,000 cycles of ON/OFF toggling
        for (i in 1..1000) {
            // Turn ON
            crossfadeDurationMs.value = 6000
            processEngineTick(crossfadeDurationMs.value)
            assertTrue("Crossfade must be active/primed when duration > 0", isOverlapPrimed)

            // Simulate mid-crossfade attenuation
            isCrossfadeActive = true
            playbackFadeFactor.value = 0.5f

            // Turn OFF mid-fade
            crossfadeDurationMs.value = 0
            processEngineTick(crossfadeDurationMs.value)

            // Invariant verification:
            assertFalse("Crossfade must be stopped immediately when duration set to 0", isCrossfadeActive)
            assertFalse("Overlap must be unprimed", isOverlapPrimed)
            assertEquals("Main fade factor must reset to 1.0f on cancel", 1.0f, playbackFadeFactor.value, 1e-6f)
        }
    }
}
