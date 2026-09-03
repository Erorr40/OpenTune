/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.R
import com.arturo254.opentune.ui.component.GridPosition
import com.arturo254.opentune.ui.component.NavigationTitle
import com.arturo254.opentune.ui.component.shimmer.ShimmerHost
import com.arturo254.opentune.ui.component.shimmer.TextPlaceholder
import com.arturo254.opentune.viewmodels.MoodAndGenresViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoodAndGenresScreen(
    navController: NavController,
    viewModel: MoodAndGenresViewModel = hiltViewModel(),
) {
    val moodAndGenres by viewModel.moodAndGenres.collectAsState()
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val topPadding = with(density) { windowInsets.getTop(this).toDp() }
    val bottomPadding = with(density) { windowInsets.getBottom(this).toDp() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsState()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            gridState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 180.dp),
        state = gridState,
        contentPadding = PaddingValues(
            start = 8.dp,
            top = topPadding,
            end = 8.dp,
            bottom = bottomPadding + 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            NavigationTitle(
                title = stringResource(R.string.mood_and_genres),
                modifier = Modifier.animateItem(),
            )
        }

        if (moodAndGenres == null) {
            itemsIndexed(Array(12) { it }.toList()) { idx, _ ->
                ShimmerHost {
                    val position = when {
                        idx % 2 == 0 -> GridPosition.LEFT
                        else -> GridPosition.RIGHT
                    }
                    TextPlaceholder(
                        height = MoodAndGenresButtonHeight,
                        shape = expressiveGridShape(position),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        } else {
            itemsIndexed(
                items = moodAndGenres.orEmpty(),
                key = { _, item -> "${item.title}:${item.endpoint.browseId}:${item.endpoint.params}" },
            ) { index, item ->
                val total = moodAndGenres?.size ?: 0
                val position = when {
                    total == 1 -> GridPosition.SINGLE
                    index % 2 == 0 -> if (index == total - 1) GridPosition.SINGLE else GridPosition.LEFT
                    else -> GridPosition.RIGHT
                }
                AnimatedVisibility(
                    visible = true,
                    enter = scaleIn(
                        animationSpec = tween(380, delayMillis = (index % 6) * 30),
                        initialScale = 0.88f,
                    ),
                    exit = scaleOut(animationSpec = tween(160)),
                ) {
                    MoodAndGenresButton(
                        title = item.title,
                        stripeColor = item.stripeColor,
                        gridPosition = position,
                        onClick = {
                            navController.navigate("youtube_browse/${item.endpoint.browseId}?params=${item.endpoint.params}")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(
                                fadeInSpec = spring(stiffness = 380f, dampingRatio = 0.85f),
                                placementSpec = spring(stiffness = 380f, dampingRatio = 0.85f),
                            ),
                    )
                }
            }
        }
    }
}

private fun expressiveGridShape(position: GridPosition) = when (position) {
    GridPosition.LEFT -> RoundedCornerShape(
        topStart = 32.dp,
        bottomStart = 32.dp,
        topEnd = 10.dp,
        bottomEnd = 10.dp,
    )

    GridPosition.RIGHT -> RoundedCornerShape(
        topStart = 10.dp,
        bottomStart = 10.dp,
        topEnd = 32.dp,
        bottomEnd = 32.dp,
    )

    GridPosition.SINGLE -> RoundedCornerShape(32.dp)
}

private fun expressiveAccentBadgeShape() = RoundedCornerShape(
    topStart = 18.dp,
    topEnd = 6.dp,
    bottomEnd = 18.dp,
    bottomStart = 6.dp,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MoodAndGenresButton(
    title: String,
    stripeColor: Long,
    onClick: () -> Unit,
    gridPosition: GridPosition = GridPosition.SINGLE,
    modifier: Modifier = Modifier,
) {
    val base = Color(stripeColor)
    val lightVariant = remember(base) {
        Color(
            red = (base.red + 0.28f).coerceIn(0f, 1f),
            green = (base.green + 0.22f).coerceIn(0f, 1f),
            blue = (base.blue + 0.18f).coerceIn(0f, 1f),
        )
    }
    val darkVariant = remember(base) {
        Color(
            red = (base.red * 0.55f).coerceIn(0f, 1f),
            green = (base.green * 0.55f).coerceIn(0f, 1f),
            blue = (base.blue * 0.55f).coerceIn(0f, 1f),
        )
    }
    val animatedBase by animateColorAsState(
        targetValue = base,
        animationSpec = spring(),
        label = "mood_base_$title",
    )
    val animatedLight by animateColorAsState(
        targetValue = lightVariant,
        animationSpec = spring(),
        label = "mood_light_$title",
    )
    val animatedDark by animateColorAsState(
        targetValue = darkVariant,
        animationSpec = spring(),
        label = "mood_dark_$title",
    )

    val gradient = remember(animatedBase, animatedLight, animatedDark) {
        Brush.linearGradient(
            colors = listOf(animatedLight, animatedBase, animatedDark),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )
    }
    val glossBrush = remember {
        Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0.22f), Color.Transparent),
            start = Offset(-1f, -1f),
            end = Offset(1f, 0.3f),
        )
    }

    val cardShape = expressiveGridShape(gridPosition)
    val badgeShape = expressiveAccentBadgeShape()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = 550f, dampingRatio = 0.75f),
        label = "mood_press_$title",
    )

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent,
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 0.5.dp,
        ),
        modifier = modifier
            .height(MoodAndGenresButtonHeight)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(glossBrush),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 12.dp, start = 12.dp)
                    .clip(badgeShape)
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.music_note),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.explore).take(3).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp,
                        ),
                        color = Color.White,
                        fontSize = 9.sp,
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.1.sp,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.4f),
                            offset = Offset(0f, 2f),
                            blurRadius = 6f,
                        ),
                        lineHeight = 22.sp,
                    ),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 14.dp, end = 14.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.chevron_right),
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

val MoodAndGenresButtonHeight = 108.dp
