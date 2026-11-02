/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.ui.utils

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
class AppBarScrollBehaviorStressTest {

    @Test
    fun heightOffset_stressTest_neverBecomesPositiveUnderAnyRandomSequence() {
        val limit = -200f
        val state = TopAppBarState(
            initialHeightOffsetLimit = limit,
            initialHeightOffset = 0f,
            initialContentOffset = 0f,
        )
        val behavior = AppBarScrollBehavior(
            state = state,
            snapAnimationSpec = null,
            flingAnimationSpec = null,
            canScroll = { true },
        )

        val rng = Random(seed = 42)

        // 10,000 randomized scroll events simulating flings, jerks, overscrolls
        for (i in 0 until 10000) {
            val deltaY = rng.nextDouble(-1000.0, 1000.0).toFloat()
            val availableY = if (deltaY > 0) rng.nextDouble(0.0, 500.0).toFloat() else 0f

            behavior.nestedScrollConnection.onPostScroll(
                consumed = Offset(0f, deltaY),
                available = Offset(0f, availableY),
                source = if (i % 2 == 0) NestedScrollSource.UserInput else NestedScrollSource.SideEffect,
            )

            assertTrue(
                "Invariant violated at iteration $i: heightOffset (${state.heightOffset}) must be <= 0f",
                state.heightOffset <= 0f,
            )
            assertTrue(
                "Invariant violated at iteration $i: heightOffset (${state.heightOffset}) must be >= limit ($limit)",
                state.heightOffset >= limit,
            )
        }
    }

    @Test
    fun heightOffset_extremeLargeFlingDeltas_clampedWithoutOverflow() {
        val limit = -150f
        val state = TopAppBarState(
            initialHeightOffsetLimit = limit,
            initialHeightOffset = 0f,
            initialContentOffset = 0f,
        )
        val behavior = AppBarScrollBehavior(
            state = state,
            snapAnimationSpec = null,
            flingAnimationSpec = null,
            canScroll = { true },
        )

        // Massive downward fling (positive consumed.y)
        behavior.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, 1_000_000f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput,
        )
        assertEquals(0f, state.heightOffset, 0.0001f)

        // Massive upward fling (negative consumed.y)
        behavior.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, -1_000_000f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput,
        )
        assertEquals(limit, state.heightOffset, 0.0001f)

        // Immediate massive downward rebound
        behavior.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, 2_000_000f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput,
        )
        assertEquals(0f, state.heightOffset, 0.0001f)
    }

    @Test
    fun heightOffset_subpixelMicroDeltas_neverDriftPositive() {
        val limit = -100f
        val state = TopAppBarState(
            initialHeightOffsetLimit = limit,
            initialHeightOffset = 0f,
            initialContentOffset = 0f,
        )
        val behavior = AppBarScrollBehavior(
            state = state,
            snapAnimationSpec = null,
            flingAnimationSpec = null,
            canScroll = { true },
        )

        // 1000 micro-scrolls down
        for (i in 0 until 1000) {
            behavior.nestedScrollConnection.onPostScroll(
                consumed = Offset(0f, 0.0001f),
                available = Offset.Zero,
                source = NestedScrollSource.UserInput,
            )
            assertEquals("Micro-drift positive at $i", 0f, state.heightOffset, 0.0f)
        }
    }

    @Test
    fun heightOffset_zeroHeightLimit_remainsZero() {
        val state = TopAppBarState(
            initialHeightOffsetLimit = 0f,
            initialHeightOffset = 0f,
            initialContentOffset = 0f,
        )
        val behavior = AppBarScrollBehavior(
            state = state,
            snapAnimationSpec = null,
            flingAnimationSpec = null,
            canScroll = { true },
        )

        behavior.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, 100f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput,
        )
        assertEquals(0f, state.heightOffset, 0.0f)

        behavior.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, -100f),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput,
        )
        assertEquals(0f, state.heightOffset, 0.0f)
    }

    @Test
    fun resetHeightOffset_restoresNegativeOffsetCleanly() = runBlocking {
        val clock = object : MonotonicFrameClock {
            private var time = 0L
            override suspend fun <R> withFrameNanos(onFrame: (frameTimeNanos: Long) -> R): R {
                time += 16_000_000L
                return onFrame(time)
            }
        }
        withContext(clock) {
            val state = TopAppBarState(
                initialHeightOffsetLimit = -200f,
                initialHeightOffset = -150f,
                initialContentOffset = 150f,
            )

            state.resetHeightOffset()

            assertEquals(0f, state.heightOffset, 0.001f)
            assertTrue(state.heightOffset <= 0f)
        }
    }

    @Test
    fun containerOffsetConversion_adversarialBoundaryCheck() {
        // Test IntOffset conversion logic from MainActivity.kt:1345:
        // y = currentScrollBehavior.state.heightOffset.toInt().coerceAtMost(0)
        val testOffsets = listOf(
            -500.0f to -500,
            -150.7f to -150,
            -1.0f to -1,
            -0.9f to 0,
            -0.1f to 0,
            0.0f to 0,
            0.1f to 0,
            0.9f to 0,
            1.0f to 0,
            50.5f to 0,
            Float.MAX_VALUE to 0,
        )

        for ((heightOffset, expectedIntY) in testOffsets) {
            val intY = heightOffset.toInt().coerceAtMost(0)
            assertEquals(
                "Offset $heightOffset converted to $intY, expected <= 0 and == $expectedIntY",
                expectedIntY,
                intY,
            )
            assertTrue("Int offset must never be positive", intY <= 0)
        }
    }
}
