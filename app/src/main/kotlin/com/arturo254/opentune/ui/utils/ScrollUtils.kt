/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.ui.utils

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    var isScrollingUp by remember(this) { mutableStateOf(true) }

    LaunchedEffect(this) {
        snapshotFlow { firstVisibleItemIndex to firstVisibleItemScrollOffset }
            .collect { (currentIndex, currentOffset) ->
                if (previousIndex != currentIndex) {
                    isScrollingUp = previousIndex > currentIndex
                    previousIndex = currentIndex
                    previousScrollOffset = currentOffset
                } else if (previousScrollOffset != currentOffset) {
                    isScrollingUp = previousScrollOffset > currentOffset
                    previousScrollOffset = currentOffset
                }
            }
    }
    return isScrollingUp
}

@Composable
fun LazyGridState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    var isScrollingUp by remember(this) { mutableStateOf(true) }

    LaunchedEffect(this) {
        snapshotFlow { firstVisibleItemIndex to firstVisibleItemScrollOffset }
            .collect { (currentIndex, currentOffset) ->
                if (previousIndex != currentIndex) {
                    isScrollingUp = previousIndex > currentIndex
                    previousIndex = currentIndex
                    previousScrollOffset = currentOffset
                } else if (previousScrollOffset != currentOffset) {
                    isScrollingUp = previousScrollOffset > currentOffset
                    previousScrollOffset = currentOffset
                }
            }
    }
    return isScrollingUp
}

@Composable
fun ScrollState.isScrollingUp(): Boolean {
    var previousScrollOffset by remember(this) { mutableIntStateOf(value) }
    var isScrollingUp by remember(this) { mutableStateOf(true) }

    LaunchedEffect(this) {
        snapshotFlow { value }
            .collect { currentValue ->
                if (previousScrollOffset != currentValue) {
                    isScrollingUp = previousScrollOffset > currentValue
                    previousScrollOffset = currentValue
                }
            }
    }
    return isScrollingUp
}
