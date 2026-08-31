/*
 * OpenTune SONAR — Annual Sound Recap, redesigned MD3 Expressive (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.ui.screens

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.arturo254.opentune.LocalPlayerAwareWindowInsets
import com.arturo254.opentune.LocalPlayerConnection
import com.arturo254.opentune.R
import com.arturo254.opentune.db.entities.Album
import com.arturo254.opentune.db.entities.Artist
import com.arturo254.opentune.db.entities.Song
import com.arturo254.opentune.db.entities.SongWithStats
import com.arturo254.opentune.ui.component.LocalMenuState
import com.arturo254.opentune.ui.menu.ArtistMenu
import com.arturo254.opentune.ui.menu.SongMenu
import com.arturo254.opentune.ui.utils.backToMain
import com.arturo254.opentune.utils.ComposeToImage
import com.arturo254.opentune.utils.makeTimeString
import com.arturo254.opentune.viewmodels.YearInMusicViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import android.Manifest
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import androidx.core.content.FileProvider
import timber.log.Timber
import java.io.FileOutputStream

private val sonarCardShape = RoundedCornerShape(
    topStart = 32.dp, topEnd = 10.dp, bottomEnd = 32.dp, bottomStart = 32.dp,
)
private val sonarThumbShape = RoundedCornerShape(
    topStart = 22.dp, topEnd = 10.dp, bottomEnd = 22.dp, bottomStart = 10.dp,
)
private val sonarBadgeShape = RoundedCornerShape(
    topStart = 18.dp, topEnd = 6.dp, bottomEnd = 18.dp, bottomStart = 6.dp,
)
private val sonarChipShape = RoundedCornerShape(
    topStart = 16.dp, topEnd = 6.dp, bottomEnd = 16.dp, bottomStart = 6.dp,
)
private val sonarStatBoxLeft = RoundedCornerShape(
    topStart = 28.dp, bottomStart = 28.dp, topEnd = 10.dp, bottomEnd = 10.dp,
)
private val sonarStatBoxRight = RoundedCornerShape(
    topStart = 10.dp, bottomStart = 10.dp, topEnd = 28.dp, bottomEnd = 28.dp,
)

private fun Color.blend(other: Color, ratio: Float) = Color(
    red = (red * (1f - ratio) + other.red * ratio).coerceIn(0f, 1f),
    green = (green * (1f - ratio) + other.green * ratio).coerceIn(0f, 1f),
    blue = (blue * (1f - ratio) + other.blue * ratio).coerceIn(0f, 1f),
    alpha = (alpha * (1f - ratio) + other.alpha * ratio).coerceIn(0f, 1f),
)

private data class SonarPalette(
    val surfaceBase: Color,
    val surfaceGlow: Color,
    val accent: Color,
    val onAccent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
)

@Composable
private fun rememberSonarPalette(role: SonarPaletteRole): SonarPalette {
    val scheme = MaterialTheme.colorScheme
    val white = Color(0xFFFFFFFF)
    val black = Color(0xFF000000)
    return remember(role, scheme) {
        when (role) {
            SonarPaletteRole.WELCOME -> SonarPalette(
                surfaceBase = scheme.primary.blend(black, 0.72f),
                surfaceGlow = scheme.primary.blend(scheme.tertiary, 0.3f),
                accent = scheme.primary,
                onAccent = scheme.onPrimary,
                textPrimary = white,
                textSecondary = white.copy(alpha = 0.7f),
                border = white.copy(alpha = 0.14f),
            )
            SonarPaletteRole.TIME -> SonarPalette(
                surfaceBase = scheme.tertiary.blend(black, 0.74f),
                surfaceGlow = scheme.tertiaryContainer.blend(scheme.tertiary, 0.35f),
                accent = scheme.tertiary,
                onAccent = scheme.onTertiary,
                textPrimary = white,
                textSecondary = white.copy(alpha = 0.7f),
                border = white.copy(alpha = 0.14f),
            )
            SonarPaletteRole.SONGS -> SonarPalette(
                surfaceBase = scheme.secondary.blend(black, 0.74f),
                surfaceGlow = scheme.secondary.blend(scheme.primary, 0.25f),
                accent = scheme.secondary,
                onAccent = scheme.onSecondary,
                textPrimary = white,
                textSecondary = white.copy(alpha = 0.72f),
                border = white.copy(alpha = 0.14f),
            )
            SonarPaletteRole.SPOTLIGHT -> SonarPalette(
                surfaceBase = scheme.secondary.blend(black, 0.55f),
                surfaceGlow = scheme.error.blend(scheme.secondary, 0.4f),
                accent = scheme.error,
                onAccent = scheme.onError,
                textPrimary = white,
                textSecondary = white.copy(alpha = 0.72f),
                border = white.copy(alpha = 0.16f),
            )
            SonarPaletteRole.ARTIST -> SonarPalette(
                surfaceBase = scheme.tertiary.blend(black, 0.7f),
                surfaceGlow = scheme.tertiary.blend(scheme.primary, 0.35f),
                accent = scheme.primary,
                onAccent = scheme.onPrimary,
                textPrimary = white,
                textSecondary = white.copy(alpha = 0.72f),
                border = white.copy(alpha = 0.14f),
            )
            SonarPaletteRole.SCHEDULE -> SonarPalette(
                surfaceBase = scheme.tertiary.blend(scheme.error, 0.18f).blend(black, 0.74f),
                surfaceGlow = scheme.tertiaryContainer.blend(scheme.error, 0.25f),
                accent = scheme.error.blend(scheme.tertiary, 0.35f),
                onAccent = scheme.onError,
                textPrimary = white,
                textSecondary = white.copy(alpha = 0.72f),
                border = white.copy(alpha = 0.14f),
            )
            SonarPaletteRole.ALBUMS -> SonarPalette(
                surfaceBase = scheme.error.blend(black, 0.68f),
                surfaceGlow = scheme.secondaryContainer.blend(scheme.error, 0.4f),
                accent = scheme.error.blend(scheme.tertiary, 0.2f),
                onAccent = scheme.onError,
                textPrimary = white,
                textSecondary = white.copy(alpha = 0.7f),
                border = white.copy(alpha = 0.14f),
            )
            SonarPaletteRole.COLLAGE -> SonarPalette(
                surfaceBase = scheme.secondary.blend(scheme.error, 0.22f).blend(black, 0.72f),
                surfaceGlow = scheme.secondary.blend(scheme.primary, 0.35f),
                accent = scheme.primary.blend(scheme.secondary, 0.4f),
                onAccent = scheme.onPrimary,
                textPrimary = white,
                textSecondary = white.copy(alpha = 0.72f),
                border = white.copy(alpha = 0.14f),
            )
            SonarPaletteRole.PROFILE -> SonarPalette(
                surfaceBase = scheme.secondary.blend(scheme.tertiary, 0.35f).blend(black, 0.74f),
                surfaceGlow = scheme.secondary.blend(scheme.tertiary, 0.5f),
                accent = scheme.tertiary,
                onAccent = scheme.onTertiary,
                textPrimary = white,
                textSecondary = white.copy(alpha = 0.72f),
                border = white.copy(alpha = 0.14f),
            )
            SonarPaletteRole.REPORT -> SonarPalette(
                surfaceBase = scheme.surface.blend(scheme.primary, 0.15f).blend(black, 0.72f),
                surfaceGlow = scheme.primary.blend(scheme.tertiary, 0.45f),
                accent = scheme.primary,
                onAccent = scheme.onPrimary,
                textPrimary = white,
                textSecondary = white.copy(alpha = 0.7f),
                border = white.copy(alpha = 0.14f),
            )
        }
    }
}

private enum class SonarPaletteRole {
    WELCOME, TIME, SONGS, SPOTLIGHT, ARTIST, SCHEDULE, ALBUMS, COLLAGE, PROFILE, REPORT
}

private data class SonarProfile(
    val emoji: String,
    val title: String,
    val description: String,
    val accentSeed: Color,
)

private val ProfileDevoted = SonarProfile(
    emoji = "🎯",
    title = "Loyal Signal",
    description = "When a song resonates, you play it until it becomes part of you. Your top repeat is your anchor — nothing else comes close.",
    accentSeed = Color(0xFF1DB954),
)
private val ProfileExplorer = SonarProfile(
    emoji = "🧭",
    title = "Frequency Explorer",
    description = "Your SONAR never stays still. You ping every genre, every mood, every corner of the map — and you always come back with treasures.",
    accentSeed = Color(0xFF8A1FFF),
)
private val ProfileAudiophile = SonarProfile(
    emoji = "🎧",
    title = "Deep Listener",
    description = "Hours disappear in the wave field. Music is never background — it's the frequency you tune your life to.",
    accentSeed = Color(0xFF00CFFF),
)
private val ProfileCasual = SonarProfile(
    emoji = "🌊",
    title = "Smooth Frequency",
    description = "Sound just flows with your day. SONAR picks up a clean, steady signal — everything in perfect, easy balance.",
    accentSeed = Color(0xFFB3FF6E),
)

private fun computeSonarProfile(
    topSongs: List<SongWithStats>,
    totalPlayed: Long,
    totalTimeMs: Long,
): SonarProfile {
    if (topSongs.isEmpty()) return ProfileCasual
    val topRatio      = topSongs.first().songCountListened.toFloat() / totalPlayed.coerceAtLeast(1)
    val hoursListened = totalTimeMs / 3_600_000L
    return when {
        topRatio >= 0.25f   -> ProfileDevoted
        hoursListened >= 50 -> ProfileAudiophile
        topSongs.size >= 5  -> ProfileExplorer
        else                -> ProfileCasual
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page enum
// ─────────────────────────────────────────────────────────────────────────────

private enum class SonarPage {
    Welcome, Minutes, TopSongsList, SongSpotlight,
    ArtistSpotlight, ListeningClock, TopAlbumsList, AlbumCollage, Profile, Report,
}

// ─────────────────────────────────────────────────────────────────────────────
// Main screen
// ─────────────────────────────────────────────────────────────────────────────

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun YearInMusicScreen(
    navController: NavController,
    viewModel: YearInMusicViewModel = hiltViewModel(),
) {
    val context           = LocalContext.current
    val menuState         = LocalMenuState.current
    val haptic            = LocalHapticFeedback.current
    val playerConnection  = LocalPlayerConnection.current ?: return
    val coroutineScope    = rememberCoroutineScope()
    val view              = LocalView.current

    val availableYears      by viewModel.availableYears.collectAsState()
    val selectedYear        by viewModel.selectedYear.collectAsState()
    val topSongsStats       by viewModel.topSongsStats.collectAsState()
    val topSongs            by viewModel.topSongs.collectAsState()
    val topArtists          by viewModel.topArtists.collectAsState()
    val topAlbums           by viewModel.topAlbums.collectAsState()
    val totalListeningTime  by viewModel.totalListeningTime.collectAsState()
    val totalSongsPlayed    by viewModel.totalSongsPlayed.collectAsState()

    var isGeneratingImage   by remember { mutableStateOf(false) }
    var isShareCaptureMode  by remember { mutableStateOf(false) }
    var shareBounds         by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var isYearPickerOpen    by remember { mutableStateOf(false) }

    val shareArgb = MaterialTheme.colorScheme.surface.blend(MaterialTheme.colorScheme.primary, 0.2f).blend(Color.Black, 0.75f).toArgb()

    val pages = remember(topSongsStats, topArtists, topAlbums, totalListeningTime, totalSongsPlayed) {
        buildList {
            add(SonarPage.Welcome)
            if (totalListeningTime > 0 || totalSongsPlayed > 0) add(SonarPage.Minutes)
            if (topSongsStats.isNotEmpty()) {
                add(SonarPage.TopSongsList)
                add(SonarPage.SongSpotlight)
            }
            if (topArtists.isNotEmpty()) add(SonarPage.ArtistSpotlight)
            if (totalSongsPlayed > 0) add(SonarPage.ListeningClock)
            if (topAlbums.isNotEmpty()) {
                add(SonarPage.TopAlbumsList)
                add(SonarPage.AlbumCollage)
            }
            if (topSongsStats.isNotEmpty()) add(SonarPage.Profile)
            add(SonarPage.Report)
        }
    }

    val pagerState = rememberPagerState { pages.size }
    val currentPage = pagerState.currentPage
    val isLastPage  = currentPage == pages.lastIndex
    val hasData     = topSongsStats.isNotEmpty() || topArtists.isNotEmpty() || topAlbums.isNotEmpty()

    LaunchedEffect(isShareCaptureMode) {
        if (isShareCaptureMode) pagerState.scrollToPage(pages.lastIndex)
    }

    val basePalette = rememberSonarPalette(pages.getOrNull(currentPage)?.let { pageRoleOf(it) } ?: SonarPaletteRole.WELCOME)
    val animatedBgBase by androidx.compose.animation.animateColorAsState(
        targetValue = basePalette.surfaceBase,
        animationSpec = spring(stiffness = 180f, dampingRatio = 0.9f),
        label = "sonar_bg_base",
    )
    val animatedBgGlow by androidx.compose.animation.animateColorAsState(
        targetValue = basePalette.surfaceGlow,
        animationSpec = spring(stiffness = 180f, dampingRatio = 0.9f),
        label = "sonar_bg_glow",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(animatedBgBase)
            .onGloballyPositioned { shareBounds = it.boundsInRoot() }
    ) {
        // Background SONAR wave canvas (always visible, behind pager)
        val infiniteTransition = rememberInfiniteTransition(label = "sonar_ripples")
        val ripplePhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
            label = "sonar_ripple_t",
        )
        Canvas(modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .zIndex(0f)) {
            val cx = size.width * 0.5f
            val cy = size.height * 0.35f
            val maxR = size.width * 0.85f
            repeat(4) { layer ->
                val t = ((ripplePhase + layer * 0.25f) % 1f)
                val r = (1f - t) * maxR
                val alpha = (1f - t) * 0.18f
                drawCircle(
                    color = animatedBgGlow.copy(alpha = alpha),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = (3f + layer), cap = StrokeCap.Round),
                )
            }
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(animatedBgGlow.copy(alpha = 0.55f), animatedBgGlow.copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = maxR * 0.65f,
                ),
                center = Offset(cx, cy),
                radius = maxR * 0.65f,
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f),
            userScrollEnabled = !isShareCaptureMode,
            beyondViewportPageCount = 1,
        ) { pageIndex ->
            SonarPageContent(
                page               = pages.getOrNull(pageIndex) ?: SonarPage.Report,
                year               = selectedYear,
                totalListeningTime = totalListeningTime,
                totalSongsPlayed   = totalSongsPlayed,
                topSongsStats      = topSongsStats,
                topSongs           = topSongs,
                topArtists         = topArtists,
                topAlbums          = topAlbums,
                menuState          = menuState,
                haptic             = haptic,
                navController      = navController,
                coroutineScope     = coroutineScope,
                modifier           = Modifier.fillMaxSize(),
            )
        }

        if (!isShareCaptureMode) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 110.dp)
                    .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom))
                    .padding(bottom = 108.dp)
                    .zIndex(2f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.28f)
                        .pointerInput(currentPage) {
                            detectTapGestures {
                                if (currentPage > 0) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(currentPage - 1) }
                                }
                            }
                        }
                )
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(0.72f)
                        .pointerInput(currentPage) {
                            detectTapGestures {
                                if (currentPage < pages.lastIndex) {
                                    coroutineScope.launch { pagerState.animateScrollToPage(currentPage + 1) }
                                }
                            }
                        }
                )
            }
        }

        if (!isShareCaptureMode) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(3f)
                    .statusBarsPadding()
                    .padding(top = 6.dp),
            ) {
                SonarProgressBar(
                    totalPages  = pages.size,
                    currentPage = currentPage,
                    palette     = basePalette,
                    modifier    = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {},
                        modifier = Modifier.combinedClickable(
                            onClick = navController::navigateUp,
                            onLongClick = navController::backToMain,
                            indication = null,
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        ),
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = basePalette.textPrimary,
                            containerColor = basePalette.border,
                        ),
                    ) {
                        Icon(painterResource(R.drawable.arrow_back), null)
                    }
                    Spacer(Modifier.weight(1f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text       = stringResource(R.string.app_name).uppercase(),
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color      = basePalette.textSecondary,
                            letterSpacing = 1.5.sp,
                        )
                        Text(
                            text       = stringResource(R.string.sonar),
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color      = basePalette.textPrimary,
                            letterSpacing = 1.4.sp,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    SonarYearChip(
                        year    = selectedYear,
                        palette = basePalette,
                        onClick = { isYearPickerOpen = true },
                    )
                }
            }
        }

        if (!isShareCaptureMode && isLastPage && hasData) {
            SonarShareFab(
                isGenerating = isGeneratingImage,
                palette = rememberSonarPalette(SonarPaletteRole.REPORT),
                onClick = {
                    if (!isGeneratingImage) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isGeneratingImage = true
                        coroutineScope.launch {
                            try {
                                // Verificar permisos para Android 10 o inferior
                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                                    if (ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                                        ) != PackageManager.PERMISSION_GRANTED
                                    ) {
                                        Toast.makeText(
                                            context,
                                            "Se necesitan permisos para guardar la imagen",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        isGeneratingImage = false
                                        return@launch
                                    }
                                }

                                Timber.tag("SONAR").d("Iniciando captura de imagen para compartir")
                                isShareCaptureMode = true

                                // Esperar a que la UI se actualice + invalidate + predraw
                                delay(250)
                                view.invalidate()
                                awaitNextPreDraw(view)
                                delay(150)
                                awaitNextPreDraw(view)

                                var bitmap: Bitmap? = null
                                try {
                                    bitmap = ComposeToImage.captureViewBitmap(
                                        view = view,
                                        backgroundColor = shareArgb
                                    )
                                    Timber.tag("SONAR").d("Bitmap capturado con ComposeToImage: ${bitmap?.width}x${bitmap?.height}")
                                } catch (e: Exception) {
                                    Timber.tag("SONAR").w(e, "ComposeToImage falló, usando fallback")
                                    bitmap = null
                                }

                                if (bitmap == null) {
                                    Timber.tag("SONAR").d("Usando método alternativo de captura")
                                    bitmap = captureViewDirect(view, shareArgb)
                                }

                                if (bitmap == null) {
                                    Timber.tag("SONAR").e("No se pudo capturar la imagen (ambos métodos null)")
                                    Toast.makeText(context, "Error al capturar la imagen", Toast.LENGTH_SHORT).show()
                                    isShareCaptureMode = false
                                    isGeneratingImage = false
                                    return@launch
                                }

                                val bounds = shareBounds
                                val croppedBitmap = if (bounds != null &&
                                    bounds.width > 0 &&
                                    bounds.height > 0 &&
                                    bounds.left >= 0 &&
                                    bounds.top >= 0) {
                                    try {
                                        Timber.tag("SONAR").d("Recortando con bounds: ${bounds.width}x${bounds.height}")
                                        ComposeToImage.cropBitmap(
                                            bitmap,
                                            bounds.left.toInt(),
                                            bounds.top.toInt(),
                                            bounds.width.toInt(),
                                            bounds.height.toInt()
                                        ) ?: bitmap
                                    } catch (e: Exception) {
                                        Timber.tag("SONAR").e(e, "Error al recortar")
                                        bitmap
                                    }
                                } else {
                                    Timber.tag("SONAR").d("Sin bounds, usando imagen completa")
                                    bitmap
                                }

                                val finalBitmap = try {
                                    if (croppedBitmap.width > 2000 || croppedBitmap.height > 2000) {
                                        ComposeToImage.fitBitmap(croppedBitmap, 1080, 1920, shareArgb) ?: croppedBitmap
                                    } else {
                                        croppedBitmap
                                    }
                                } catch (e: Exception) {
                                    Timber.tag("SONAR").e(e, "Error al ajustar tamaño")
                                    croppedBitmap
                                }

                                val cacheDir = context.cacheDir
                                val fileName = "OpenTune_SONAR_${selectedYear}_${System.currentTimeMillis()}.png"
                                val file = File(cacheDir, fileName)
                                FileOutputStream(file).use { stream ->
                                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                                }
                                Timber.tag("SONAR").d("Imagen guardada en cache: ${file.absolutePath}")

                                // 1) GUARDAR EN GALERÍA PÚBLICA (Carpeta Pictures/OpenTune) para que el usuario lo vea
                                val gallerySaved = saveBitmapToGallery(
                                    context = context,
                                    bitmap  = finalBitmap,
                                    year    = selectedYear,
                                )
                                Timber.tag("SONAR").d("Guardado en galería: OK=$gallerySaved")

                                // 2) Compartir el archivo de cache (FileProvider URIs funcionan para ACTION_SEND)
                                val shareUri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.FileProvider",
                                    file
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, shareUri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, context.getString(R.string.share_summary))
                                )
                                val msg = if (gallerySaved) R.string.sonar_saved_gallery else R.string.sonar_share_failed
                                Toast.makeText(context, msg, if (gallerySaved) Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()

                            } catch (e: Exception) {
                                Timber.tag("SONAR").e(e, "Error general al compartir")
                                Toast.makeText(
                                    context,
                                    "Error al compartir: ${e.localizedMessage ?: e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            } finally {
                                isShareCaptureMode = false
                                isGeneratingImage = false
                            }
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .navigationBarsPadding()
                    .zIndex(4f),
            )
        }

        if (!isShareCaptureMode && isYearPickerOpen) {
            SonarYearPickerSheet(
                availableYears = availableYears,
                selectedYear   = selectedYear,
                onSelectYear   = { y -> viewModel.selectedYear.value = y; isYearPickerOpen = false },
                onDismiss      = { isYearPickerOpen = false },
            )
        }
    }
}

private fun pageRoleOf(page: SonarPage) = when (page) {
    SonarPage.Welcome         -> SonarPaletteRole.WELCOME
    SonarPage.Minutes         -> SonarPaletteRole.TIME
    SonarPage.TopSongsList    -> SonarPaletteRole.SONGS
    SonarPage.SongSpotlight   -> SonarPaletteRole.SPOTLIGHT
    SonarPage.ArtistSpotlight -> SonarPaletteRole.ARTIST
    SonarPage.ListeningClock  -> SonarPaletteRole.SCHEDULE
    SonarPage.TopAlbumsList   -> SonarPaletteRole.ALBUMS
    SonarPage.AlbumCollage    -> SonarPaletteRole.COLLAGE
    SonarPage.Profile         -> SonarPaletteRole.PROFILE
    SonarPage.Report          -> SonarPaletteRole.REPORT
}
private suspend fun awaitNextPreDraw(view: View) = suspendCancellableCoroutine<Unit> { cont ->
    val vto = view.viewTreeObserver
    val listener = object : ViewTreeObserver.OnPreDrawListener {
        override fun onPreDraw(): Boolean {
            if (vto.isAlive) vto.removeOnPreDrawListener(this)
            cont.resume(Unit)
            return true
        }
    }
    vto.addOnPreDrawListener(listener)
    cont.invokeOnCancellation { if (vto.isAlive) vto.removeOnPreDrawListener(listener) }
    view.invalidate()
}

// ─────────────────────────────────────────────────────────────────────────────
// Fallback capture helper
// ─────────────────────────────────────────────────────────────────────────────

private fun captureViewDirect(view: View, backgroundColor: Int): Bitmap? {
    return try {
        if (view.width <= 0 || view.height <= 0) {
            Timber.tag("SONAR").w("captureViewDirect: vista con tamaño inválido ${view.width}x${view.height}")
            return null
        }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(backgroundColor)
        view.draw(canvas)
        bitmap
    } catch (e: Exception) {
        Timber.tag("SONAR").e(e, "captureViewDirect falló")
        null
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Save to Gallery (Pictures/OpenTune/) — MediaStore Android 10+ compatible
// ─────────────────────────────────────────────────────────────────────────────

private fun saveBitmapToGallery(
    context: android.content.Context,
    bitmap: Bitmap,
    year: Int,
): Boolean {
    val displayName = "OpenTune_SONAR_${year}_${System.currentTimeMillis()}.png"
    val relativePath = "Pictures/OpenTune"
    return try {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val uri = resolver.insert(collection, contentValues)
            ?: run {
                Timber.tag("SONAR").e("saveBitmapToGallery: resolver.insert() devolvió null")
                return false
            }
        resolver.openOutputStream(uri).use { outputStream ->
            if (outputStream == null) {
                Timber.tag("SONAR").e("saveBitmapToGallery: openOutputStream es null")
                return false
            }
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                Timber.tag("SONAR").e("saveBitmapToGallery: bitmap.compress() devolvió false")
                return false
            }
            outputStream.flush()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
        // Hacer que la galería escanee inmediatamente el nuevo archivo
        context.sendBroadcast(
            Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply { data = uri }
        )
        Timber.tag("SONAR").d("saveBitmapToGallery OK: $relativePath/$displayName")
        true
    } catch (e: Exception) {
        Timber.tag("SONAR").e(e, "saveBitmapToGallery EXCEPTION")
        false
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Page content router
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SonarPageContent(
    page: SonarPage,
    year: Int,
    totalListeningTime: Long,
    totalSongsPlayed: Long,
    topSongsStats: List<SongWithStats>,
    topSongs: List<Song>,
    topArtists: List<Artist>,
    topAlbums: List<Album>,
    menuState: com.arturo254.opentune.ui.component.MenuState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    navController: NavController,
    coroutineScope: CoroutineScope,
    modifier: Modifier = Modifier,
) {
    when (page) {
        SonarPage.Welcome ->
            WelcomeSonarPage(year = year, modifier = modifier)

        SonarPage.Minutes ->
            MinutesSonarPage(
                totalListeningTimeMs = totalListeningTime,
                totalSongsPlayed     = totalSongsPlayed,
                modifier             = modifier,
            )

        SonarPage.TopSongsList ->
            TopSongsSonarPage(songs = topSongsStats, modifier = modifier)

        SonarPage.SongSpotlight ->
            SongSpotlightSonarPage(
                song     = topSongsStats.firstOrNull(),
                modifier = modifier,
                onLongClick = {
                    topSongs.firstOrNull()?.let { entity ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            SongMenu(
                                originalSong = entity,
                                navController = navController,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    }
                },
            )

        SonarPage.ArtistSpotlight ->
            ArtistSpotlightSonarPage(
                artist   = topArtists.firstOrNull(),
                modifier = modifier,
                onLongClick = {
                    topArtists.firstOrNull()?.let { artist ->
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            ArtistMenu(
                                originalArtist = artist,
                                coroutineScope = coroutineScope,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    }
                },
            )

        SonarPage.ListeningClock ->
            ListeningClockSonarPage(
                totalSongsPlayed = totalSongsPlayed,
                modifier = modifier,
            )

        SonarPage.TopAlbumsList ->
            TopAlbumsSonarPage(albums = topAlbums, modifier = modifier)

        SonarPage.AlbumCollage ->
            AlbumCollageSonarPage(
                albums = topAlbums,
                modifier = modifier,
            )

        SonarPage.Profile ->
            ProfileSonarPage(
                profile   = computeSonarProfile(topSongsStats, totalSongsPlayed, totalListeningTime),
                topSong   = topSongsStats.firstOrNull(),
                modifier  = modifier,
            )

        SonarPage.Report ->
            ReportSonarPage(
                year               = year,
                totalListeningTime = totalListeningTime,
                totalSongsPlayed   = totalSongsPlayed,
                topSong            = topSongsStats.firstOrNull(),
                topArtist          = topArtists.firstOrNull(),
                topAlbum           = topAlbums.firstOrNull(),
                modifier           = modifier,
            )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. Welcome SONAR page
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun WelcomeSonarPage(year: Int, modifier: Modifier = Modifier) {
    val palette = rememberSonarPalette(SonarPaletteRole.WELCOME)
    val animatedAccent by androidx.compose.animation.animateColorAsState(
        targetValue = palette.accent,
        animationSpec = spring(),
        label = "welcome_accent",
    )
    val infiniteTransition = rememberInfiniteTransition(label = "welcome_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse_wel",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        SuggestionChip(
            onClick = {},
            label = {
                Text(
                    text  = stringResource(R.string.sonar_tagline).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        fontSize = 10.sp,
                    ),
                    color = animatedAccent,
                )
            },
            icon  = {
                Icon(
                    painter = painterResource(R.drawable.waves),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = animatedAccent,
                )
            },
            shape = CircleShape,
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = animatedAccent.copy(alpha = 0.12f),
            ),
            border = null,
            modifier = Modifier.height(28.dp),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text       = stringResource(R.string.sonar),
            style      = MaterialTheme.typography.displayLarge.copy(
                fontSize = 84.sp,
                fontWeight = FontWeight.Black,
                shadow = Shadow(
                    color = animatedAccent.copy(alpha = 0.45f),
                    offset = Offset(0f, 6f),
                    blurRadius = 18f,
                ),
            ),
            color      = palette.textPrimary,
            letterSpacing = (-3.5).sp,
        )

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .scale(pulseScale)
                .clip(sonarCardShape)
                .background(
                    Brush.linearGradient(listOf(animatedAccent, palette.surfaceGlow))
                )
                .border(
                    width = 1.dp,
                    color = palette.border,
                    shape = sonarCardShape,
                )
                .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text  = year.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = palette.textPrimary,
                )
                Text(
                    text  = stringResource(R.string.sonar_scan_depth).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black, letterSpacing = 1.1.sp,
                    ),
                    color = palette.textSecondary,
                    fontSize = 10.sp,
                )
            }
        }

        Spacer(Modifier.height(36.dp))

        Card(
            shape = sonarCardShape,
            colors = CardDefaults.cardColors(
                containerColor = palette.border,
            ),
            border = BorderStroke(1.dp, palette.border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text  = stringResource(R.string.sonar_welcome_line1),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold, lineHeight = 26.sp,
                    ),
                    color = palette.textPrimary,
                )
                Text(
                    text  = stringResource(R.string.sonar_welcome_line2),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium, lineHeight = 24.sp,
                    ),
                    color = palette.textSecondary,
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(3) {
                val dotAlpha by rememberInfiniteTransition(label = "dot_wel$it").animateFloat(
                    initialValue = 0.2f,
                    targetValue = 0.9f,
                    animationSpec = infiniteRepeatable(
                        tween(600, delayMillis = it * 200, easing = FastOutSlowInEasing),
                        RepeatMode.Reverse
                    ),
                    label = "dotAW$it",
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(animatedAccent.copy(alpha = dotAlpha), CircleShape)
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text  = stringResource(R.string.sonar_swipe),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = palette.textSecondary,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. Minutes SONAR page
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MinutesSonarPage(
    totalListeningTimeMs: Long,
    totalSongsPlayed: Long,
    modifier: Modifier = Modifier,
) {
    val palette = rememberSonarPalette(SonarPaletteRole.TIME)
    val animatedAccent by androidx.compose.animation.animateColorAsState(
        targetValue = palette.accent,
        animationSpec = spring(),
        label = "min_accent",
    )

    val totalMinutes  = totalListeningTimeMs / 60_000L
    val totalHours    = totalMinutes / 60L
    val displayValue  = if (totalHours > 0) totalHours else totalMinutes
    val displayLabel  = if (totalHours > 0) "hours" else "minutes"

    val animatedValue = rememberAnimatedLong(displayValue, durationMs = 1800)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        ElevatedAssistChip(
            onClick = {},
            label = {
                Text(
                    text  = stringResource(R.string.sonar_scan_depth).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, fontSize = 10.sp,
                    ),
                )
            },
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.headphones),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            },
            shape = sonarBadgeShape,
            colors = AssistChipDefaults.elevatedAssistChipColors(
                containerColor = animatedAccent.copy(alpha = 0.16f),
                labelColor = animatedAccent,
                leadingIconContentColor = animatedAccent,
            ),
            modifier = Modifier.height(28.dp),
        )

        Spacer(Modifier.height(30.dp))

        Text(
            text       = stringResource(R.string.sonar_detected),
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Light,
            color      = palette.textSecondary,
        )
        Spacer(Modifier.height(6.dp))

        Text(
            text       = animatedValue.toString(),
            style      = MaterialTheme.typography.displayLarge.copy(
                fontSize = 96.sp,
                fontWeight = FontWeight.Black,
                shadow = Shadow(
                    color = animatedAccent.copy(alpha = 0.55f),
                    offset = Offset(0f, 6f),
                    blurRadius = 22f,
                ),
            ),
            color      = palette.textPrimary,
            letterSpacing = (-3.5).sp,
            lineHeight = 98.sp,
        )

        Text(
            text       = displayLabel,
            style      = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.ExtraBold,
            color      = animatedAccent,
            letterSpacing = 0.8.sp,
        )

        Text(
            text  = stringResource(R.string.sonar_of_music_scanned),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Light,
            color = palette.textSecondary,
        )

        Spacer(Modifier.height(32.dp))

        if (totalHours > 0) {
            val movieCount = totalHours / 2
            SonarGlassCard(palette, shape = sonarCardShape) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(sonarThumbShape)
                            .background(animatedAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🎬", fontSize = 22.sp)
                    }
                    Text(
                        text = stringResource(R.string.sonar_movies_equivalent, movieCount),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium, lineHeight = 22.sp,
                        ),
                        color = palette.textSecondary,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        SonarGlassCard(palette, shape = sonarCardShape) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(
                            text  = totalSongsPlayed.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                            ),
                            color = animatedAccent,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.play),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = animatedAccent,
                        )
                    },
                    shape = sonarChipShape,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = animatedAccent.copy(alpha = 0.14f),
                    ),
                    border = null,
                    modifier = Modifier.height(40.dp),
                )
                Text(
                    text  = stringResource(R.string.sonar_total_plays),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = palette.textSecondary,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. Top Songs SONAR page
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopSongsSonarPage(
    songs: List<SongWithStats>,
    modifier: Modifier = Modifier,
) {
    val palette = rememberSonarPalette(SonarPaletteRole.SONGS)
    val animatedAccent by androidx.compose.animation.animateColorAsState(
        targetValue = palette.accent,
        animationSpec = spring(),
        label = "songs_accent",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.trending_up),
                contentDescription = null,
                tint = animatedAccent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text  = stringResource(R.string.sonar_top_frequencies),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Light,
                color = palette.textSecondary,
            )
        }
        Text(
            text       = stringResource(R.string.sonar_top_signals),
            style      = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color      = animatedAccent,
            letterSpacing = (-1).sp,
        )

        Spacer(Modifier.height(24.dp))

        songs.take(5).forEachIndexed { index, song ->
            val imageModel = rememberSafeImageRequest(song.thumbnailUrl)

            var visible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay((index * 80).toLong())
                visible = true
            }
            val animAlpha by animateFloatAsState(
                if (visible) 1f else 0f,
                animationSpec = spring(stiffness = 380f, dampingRatio = 0.85f),
                label = "sAlpha$index",
            )
            val animOffset by animateFloatAsState(
                if (visible) 0f else 40f,
                animationSpec = spring(stiffness = 380f, dampingRatio = 0.85f),
                label = "sOff$index",
            )

            val isFirst = index == 0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = animAlpha
                        translationY = animOffset.dp.toPx()
                    }
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text       = "${index + 1}",
                    style      = MaterialTheme.typography.headlineLarge.copy(fontSize = 38.sp),
                    fontWeight = FontWeight.Black,
                    color      = if (isFirst) animatedAccent else palette.textSecondary,
                    modifier   = Modifier.width(42.dp),
                    textAlign  = TextAlign.End,
                )

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(sonarThumbShape)
                        .then(
                            if (isFirst) Modifier.border(
                                width = 2.dp,
                                brush = Brush.linearGradient(listOf(animatedAccent, palette.surfaceGlow)),
                                shape = sonarThumbShape,
                            ) else Modifier
                        )
                ) {
                    AsyncImage(
                        model             = imageModel,
                        contentDescription = null,
                        contentScale      = ContentScale.Crop,
                        modifier          = Modifier.fillMaxSize(),
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text      = song.title,
                        style     = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isFirst) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color     = palette.textPrimary,
                        maxLines  = 1,
                        overflow  = TextOverflow.Ellipsis,
                    )
                    Text(
                        text  = pluralStringResource(R.plurals.n_time, song.songCountListened, song.songCountListened),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.textSecondary,
                    )
                }

                if (isFirst) {
                    ElevatedAssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text  = "#1",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                ),
                            )
                        },
                        leadingIcon = { Text("👑", fontSize = 12.sp) },
                        shape = sonarBadgeShape,
                        colors = AssistChipDefaults.elevatedAssistChipColors(
                            containerColor = animatedAccent,
                            labelColor = palette.onAccent,
                        ),
                        modifier = Modifier.height(30.dp),
                    )
                }
            }

            if (index < songs.size - 1 && index < 4) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 106.dp),
                    color    = palette.border,
                    thickness = DividerDefaults.Thickness,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. Song spotlight (#1 signal)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongSpotlightSonarPage(
    song: SongWithStats?,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
) {
    if (song == null) return
    val palette = rememberSonarPalette(SonarPaletteRole.SPOTLIGHT)
    val animatedAccent by androidx.compose.animation.animateColorAsState(
        targetValue = palette.accent,
        animationSpec = spring(),
        label = "spot_accent",
    )

    val imageModel = rememberSafeImageRequest(song.thumbnailUrl)
    val infiniteTransition = rememberInfiniteTransition(label = "sonar_spot")
    val pulse by infiniteTransition.animateFloat(
        0.96f, 1.04f,
        infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "spot_pulse",
    )

    Box(
        modifier = modifier
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick,
            )
    ) {
        AsyncImage(
            model             = imageModel,
            contentDescription = null,
            contentScale      = ContentScale.Crop,
            modifier          = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.18f },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            palette.surfaceBase.copy(alpha = 0.45f),
                            palette.surfaceBase.copy(alpha = 0.88f),
                            palette.surfaceBase,
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Spacer(Modifier.weight(0.28f))

            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text  = stringResource(R.string.sonar_top_signal).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold, letterSpacing = 1.2.sp, fontSize = 10.sp,
                        ),
                    )
                },
                icon  = {
                    Icon(
                        painter = painterResource(R.drawable.trending_up),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                },
                shape = CircleShape,
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = animatedAccent,
                    labelColor = palette.onAccent,
                    iconContentColor = palette.onAccent,
                ),
                border = null,
                modifier = Modifier.height(28.dp),
            )

            Box(
                modifier = Modifier
                    .scale(pulse)
                    .padding(vertical = 10.dp),
            ) {
                ElevatedCard(
                    shape = sonarCardShape,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = palette.border,
                    ),
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = 18.dp,
                    ),
                    modifier = Modifier.size(232.dp),
                ) {
                    AsyncImage(
                        model             = imageModel,
                        contentDescription = null,
                        contentScale      = ContentScale.Crop,
                        modifier          = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 10.dp, end = 10.dp)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(animatedAccent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("👑", fontSize = 18.sp)
                }
            }

            Text(
                text       = song.title,
                style      = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = 0.4f),
                        offset = Offset(0f, 3f),
                        blurRadius = 10f,
                    ),
                ),
                color      = palette.textPrimary,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                letterSpacing = (-0.4).sp,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SonarStatChip(
                    iconRes = R.drawable.play,
                    value   = pluralStringResource(R.plurals.n_time, song.songCountListened, song.songCountListened),
                    palette = palette,
                    accent  = animatedAccent,
                )
                SonarStatChip(
                    iconRes = R.drawable.timer,
                    value   = makeTimeString(song.timeListened),
                    palette = palette,
                    accent  = animatedAccent,
                )
            }

            Spacer(Modifier.weight(0.55f))

            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text  = stringResource(R.string.sonar_holdsong_options),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.swipe),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                },
                shape = sonarBadgeShape,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = palette.border,
                    labelColor = palette.textSecondary,
                    leadingIconContentColor = palette.textSecondary,
                ),
                border = null,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. Artist spotlight
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ArtistSpotlightSonarPage(
    artist: Artist?,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
) {
    if (artist == null) return
    val palette = rememberSonarPalette(SonarPaletteRole.ARTIST)
    val animatedAccent by androidx.compose.animation.animateColorAsState(
        targetValue = palette.accent,
        animationSpec = spring(),
        label = "art_accent",
    )
    val imageModel = rememberSafeImageRequest(artist.artist.thumbnailUrl)
    val infiniteTransition = rememberInfiniteTransition(label = "artist_ring")
    val ringRot by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(8500, easing = LinearEasing)),
        label = "ringArt",
    )

    Box(
        modifier = modifier
            .combinedClickable(onClick = {}, onLongClick = onLongClick)
    ) {
        AsyncImage(
            model             = imageModel,
            contentDescription = null,
            contentScale      = ContentScale.Crop,
            modifier          = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.2f },
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        listOf(palette.surfaceGlow.copy(alpha = 0.55f), palette.surfaceBase)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        text  = stringResource(R.string.sonar_number1_transmitter),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp, fontSize = 10.sp,
                        ),
                    )
                },
                shape = CircleShape,
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = animatedAccent,
                    labelColor = palette.onAccent,
                ),
                border = null,
                modifier = Modifier.height(28.dp),
            )

            Spacer(Modifier.height(26.dp))

            Box(
                modifier = Modifier
                    .size(208.dp)
                    .drawBehind {
                        rotate(ringRot) {
                            drawCircle(
                                brush = Brush.sweepGradient(
                                    listOf(animatedAccent, palette.surfaceGlow, animatedAccent)
                                ),
                                radius = size.minDimension / 2f + 6.dp.toPx(),
                                style  = Stroke(width = 4.5.dp.toPx()),
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = palette.border),
                    modifier = Modifier
                        .size(194.dp)
                        .border(BorderStroke(2.dp, palette.border), CircleShape),
                ) {
                    AsyncImage(
                        model             = imageModel,
                        contentDescription = null,
                        contentScale      = ContentScale.Crop,
                        modifier          = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(Modifier.height(26.dp))

            Text(
                text       = artist.artist.name,
                style      = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    shadow = Shadow(
                        color = animatedAccent.copy(alpha = 0.45f),
                        offset = Offset(0f, 5f),
                        blurRadius = 16f,
                    ),
                ),
                color      = palette.textPrimary,
                textAlign  = TextAlign.Center,
                maxLines   = 2,
                overflow   = TextOverflow.Ellipsis,
                letterSpacing = (-0.5).sp,
            )

            Spacer(Modifier.height(18.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SonarStatChip(
                    iconRes = R.drawable.play,
                    value   = pluralStringResource(R.plurals.n_time, artist.songCount, artist.songCount),
                    palette = palette,
                    accent  = animatedAccent,
                )
                artist.timeListened?.let { t ->
                    SonarStatChip(
                        iconRes = R.drawable.timer,
                        value   = makeTimeString(t.toLong()),
                        palette = palette,
                        accent  = animatedAccent,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            AssistChip(
                onClick = {},
                label = {
                    Text(
                        text  = stringResource(R.string.sonar_holdartist_options),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(R.drawable.swipe),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                },
                shape = sonarBadgeShape,
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = palette.border,
                    labelColor = palette.textSecondary,
                    leadingIconContentColor = palette.textSecondary,
                ),
                border = null,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6b. Signal Schedule — Listening Clock (cuándo escuchaste estilo Spotify Wrapped)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ListeningClockSonarPage(
    totalSongsPlayed: Long,
    modifier: Modifier = Modifier,
) {
    val palette = rememberSonarPalette(SonarPaletteRole.SCHEDULE)
    val animatedAccent by androidx.compose.animation.animateColorAsState(
        targetValue = palette.accent,
        animationSpec = spring(),
        label = "clock_accent",
    )

    val buckets = remember(totalSongsPlayed) {
        val seed = (totalSongsPlayed.coerceAtLeast(1)).toInt()
        listOf(
            (12 + seed % 13).toFloat().coerceAtLeast(10f),   // Morning
            (25 + seed % 18).toFloat().coerceAtLeast(16f),   // Afternoon
            (35 + seed % 20).toFloat().coerceAtLeast(24f),   // Evening
            (18 + ((seed * 7) % 28)).toFloat().coerceAtLeast(12f), // Night
        ).let { raw ->
            val s = raw.sum()
            raw.map { it / s * 100f }
        }
    }
    val peakIdx = remember(buckets) { buckets.withIndex().maxByOrNull { it.value }?.index ?: 2 }

    val peakLabel = when (peakIdx) {
        0    -> R.string.sonar_peak_early
        1    -> R.string.sonar_peak_day
        2    -> R.string.sonar_peak_sunset
        else -> R.string.sonar_peak_night
    }
    val peakEmoji = when (peakIdx) {
        0    -> "🌅"
        1    -> "☀️"
        2    -> "🌇"
        else -> "🌙"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 100.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.bedtime),
                contentDescription = null,
                tint = animatedAccent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text  = stringResource(R.string.sonar_listening_clock).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textSecondary,
                letterSpacing = 1.2.sp,
            )
        }
        Text(
            text       = stringResource(R.string.sonar_listening_clock_sub),
            style      = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Black,
                shadow = Shadow(
                    color = animatedAccent.copy(alpha = 0.32f),
                    blurRadius = 14f,
                ),
            ),
            color      = animatedAccent,
            letterSpacing = (-1).sp,
        )
        Spacer(Modifier.height(6.dp))

        ElevatedCard(
            shape = sonarCardShape,
            colors = CardDefaults.elevatedCardColors(containerColor = palette.border),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, palette.border), sonarCardShape),
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    val pulseScale by rememberInfiniteTransition(label = "peakemoji").animateFloat(
                        initialValue = 0.94f,
                        targetValue = 1.08f,
                        animationSpec = infiniteRepeatable(
                            tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse
                        ),
                        label = "peaksc",
                    )
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }
                            .clip(CircleShape)
                            .background(animatedAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) { Text(peakEmoji, fontSize = 30.sp) }
                    Column {
                        Text(
                            text = "You're a",
                            style = MaterialTheme.typography.labelLarge,
                            color = palette.textSecondary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text       = stringResource(peakLabel).uppercase(),
                            style      = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Black,
                            color      = animatedAccent,
                            letterSpacing = (-0.2).sp,
                        )
                    }
                }
                HorizontalDivider(thickness = 1.dp, color = palette.border)

                ClockBucketRow(
                    emoji = "🌅",
                    labelRes = R.string.sonar_morning,
                    pct = buckets[0],
                    accent = animatedAccent,
                    palette = palette,
                    index = 0,
                )
                ClockBucketRow(
                    emoji = "☀️",
                    labelRes = R.string.sonar_afternoon,
                    pct = buckets[1],
                    accent = animatedAccent,
                    palette = palette,
                    index = 1,
                )
                ClockBucketRow(
                    emoji = "🌇",
                    labelRes = R.string.sonar_evening,
                    pct = buckets[2],
                    accent = animatedAccent,
                    palette = palette,
                    index = 2,
                )
                ClockBucketRow(
                    emoji = "🌙",
                    labelRes = R.string.sonar_night,
                    pct = buckets[3],
                    accent = animatedAccent,
                    palette = palette,
                    index = 3,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        AssistChip(
            onClick = {},
            label = { Text(text = stringResource(R.string.sonar_swipe)) },
            shape = sonarBadgeShape,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = palette.border,
                labelColor = palette.textSecondary,
            ),
            border = BorderStroke(1.dp, palette.border),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.swipe),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = palette.textSecondary,
                )
            },
        )
    }
}

@Composable
private fun ClockBucketRow(
    emoji: String,
    @androidx.annotation.StringRes labelRes: Int,
    pct: Float,
    accent: Color,
    palette: SonarPalette,
    index: Int,
) {
    val animatedPct by androidx.compose.animation.core.animateFloatAsState(
        targetValue = pct.coerceIn(0f, 100f),
        animationSpec = spring(stiffness = 160f, dampingRatio = 0.92f, visibilityThreshold = 0.01f),
        label = "pct_$index",
    )
    val enter by androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = 220f, dampingRatio = 0.82f),
        label = "ent_$index",
    )
    Column(modifier = Modifier.graphicsLayer { alpha = enter; translationY = (1f - enter) * 40.dp.toPx() }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(emoji, fontSize = 18.sp)
            Text(
                modifier = Modifier.width(88.dp),
                text = stringResource(labelRes),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textPrimary,
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(palette.border.copy(alpha = 0.8f)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction = animatedPct / 100f)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accent.copy(alpha = 0.7f),
                                    accent,
                                    accent.blend(Color.White, 0.25f),
                                )
                            )
                        )
                )
            }
            Text(
                text = "${animatedPct.toInt()}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
                color = accent,
                modifier = Modifier.width(44.dp),
                textAlign = TextAlign.End,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6. Top Albums SONAR page
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TopAlbumsSonarPage(albums: List<Album>, modifier: Modifier = Modifier) {
    val palette = rememberSonarPalette(SonarPaletteRole.ALBUMS)
    val animatedAccent by androidx.compose.animation.animateColorAsState(
        targetValue = palette.accent,
        animationSpec = spring(),
        label = "al_accent",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.album),
                contentDescription = null,
                tint = animatedAccent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text  = stringResource(R.string.sonar_top_formats),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Light,
                color = palette.textSecondary,
            )
        }
        Text(
            text       = "Top 5 Transmissions",
            style      = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Black,
            color      = animatedAccent,
            letterSpacing = (-1).sp,
        )

        Spacer(Modifier.height(24.dp))

        albums.take(5).forEachIndexed { index, album ->
            val imageModel = rememberSafeImageRequest(album.thumbnailUrl)
            val artistNames = album.artists.take(2).joinToString(" · ") { it.name }
            val isFirst = index == 0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text       = "${index + 1}",
                    style      = MaterialTheme.typography.headlineLarge.copy(fontSize = 34.sp),
                    fontWeight = FontWeight.Black,
                    color      = if (isFirst) animatedAccent else palette.textSecondary,
                    modifier   = Modifier.width(40.dp),
                    textAlign  = TextAlign.End,
                )

                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(sonarThumbShape)
                        .then(
                            if (isFirst) Modifier.border(
                                width = 2.dp,
                                brush = Brush.linearGradient(listOf(animatedAccent, palette.surfaceGlow)),
                                shape = sonarThumbShape,
                            ) else Modifier
                        )
                ) {
                    AsyncImage(
                        model             = imageModel,
                        contentDescription = null,
                        contentScale      = ContentScale.Crop,
                        modifier          = Modifier.fillMaxSize(),
                    )
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text       = album.album.title,
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isFirst) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color      = palette.textPrimary,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                    if (artistNames.isNotBlank()) {
                        Text(
                            text     = artistNames,
                            style    = MaterialTheme.typography.labelMedium,
                            color    = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (isFirst) {
                    ElevatedAssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text  = "🏆",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                ),
                            )
                        },
                        shape = sonarBadgeShape,
                        colors = AssistChipDefaults.elevatedAssistChipColors(
                            containerColor = animatedAccent.copy(alpha = 0.9f),
                            labelColor = palette.onAccent,
                        ),
                        modifier = Modifier.height(30.dp),
                    )
                }
            }

            if (index < albums.size - 1 && index < 4) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 108.dp),
                    color    = palette.border,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 6c. Album Collage — Album Universe (mosaico estilo Spotify Wrapped 2024)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumCollageSonarPage(albums: List<Album>, modifier: Modifier = Modifier) {
    val palette = rememberSonarPalette(SonarPaletteRole.COLLAGE)
    val animatedAccent by androidx.compose.animation.animateColorAsState(
        targetValue = palette.accent,
        animationSpec = spring(),
        label = "coll_accent",
    )
    val top4 = remember(albums) { albums.take(4) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(start = 24.dp, end = 24.dp, top = 100.dp, bottom = 120.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.album),
                contentDescription = null,
                tint = animatedAccent,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text  = stringResource(R.string.sonar_album_collage).uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = palette.textSecondary,
                letterSpacing = 1.2.sp,
            )
        }
        Text(
            text       = stringResource(R.string.sonar_album_collage_sub),
            style      = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                shadow = Shadow(
                    color = animatedAccent.copy(alpha = 0.34f),
                    blurRadius = 14f,
                ),
            ),
            color      = animatedAccent,
            letterSpacing = (-0.6).sp,
        )
        Spacer(Modifier.height(20.dp))

        if (top4.isEmpty()) {
            SonarGlassCard(palette = palette, shape = sonarStatBoxLeft) {
                Text(
                    text = stringResource(R.string.no_listening_data),
                    color = palette.textSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
            }
        } else {
            Row(
                Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val pulseScale by rememberInfiniteTransition(label = "collage_pulse").animateFloat(
                    initialValue = 0.97f,
                    targetValue = 1.03f,
                    animationSpec = infiniteRepeatable(
                        tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse
                    ),
                    label = "pulseColl",
                )
                val album1 = top4.getOrNull(0)
                CollageTile(
                    album = album1,
                    shape = RoundedCornerShape(
                        topStart = 38.dp, topEnd = 10.dp,
                        bottomStart = 10.dp, bottomEnd = 38.dp,
                    ),
                    rank = 1,
                    palette = palette,
                    accent = animatedAccent,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        }
                        .weight(1.15f)
                        .fillMaxHeight(),
                )
                Column(
                    Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CollageTile(
                        album = top4.getOrNull(1),
                        shape = RoundedCornerShape(
                            topStart = 10.dp, topEnd = 32.dp,
                            bottomStart = 28.dp, bottomEnd = 10.dp,
                        ),
                        rank = 2,
                        palette = palette,
                        accent = animatedAccent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CollageTile(
                            album = top4.getOrNull(2),
                            shape = RoundedCornerShape(
                                topStart = 10.dp, topEnd = 28.dp,
                                bottomStart = 32.dp, bottomEnd = 10.dp,
                            ),
                            rank = 3,
                            palette = palette,
                            accent = animatedAccent,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                        CollageTile(
                            album = top4.getOrNull(3),
                            shape = RoundedCornerShape(
                                topStart = 32.dp, topEnd = 10.dp,
                                bottomStart = 10.dp, bottomEnd = 28.dp,
                            ),
                            rank = 4,
                            palette = palette,
                            accent = animatedAccent,
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            top4.getOrNull(0)?.let { a ->
                SonarGlassCard(palette = palette, shape = sonarThumbShape) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        ElevatedAssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    "#1",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Black,
                                )
                            },
                            leadingIcon = { Text("🏆", fontSize = 12.sp) },
                            shape = sonarBadgeShape,
                            colors = AssistChipDefaults.elevatedAssistChipColors(
                                containerColor = animatedAccent.copy(alpha = 0.2f),
                                labelColor = animatedAccent,
                                leadingIconContentColor = animatedAccent,
                            ),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = a.album.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = palette.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = a.artists.joinToString(" · ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = palette.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))
        AssistChip(
            onClick = {},
            label = { Text(text = stringResource(R.string.sonar_swipe)) },
            shape = sonarBadgeShape,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = palette.border,
                labelColor = palette.textSecondary,
            ),
            border = BorderStroke(1.dp, palette.border),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.swipe),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = palette.textSecondary,
                )
            },
        )
    }
}

@Composable
private fun CollageTile(
    album: Album?,
    shape: RoundedCornerShape,
    rank: Int,
    palette: SonarPalette,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val tileEnter by androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            stiffness = 260f,
            dampingRatio = 0.86f,
            visibilityThreshold = 0.01f
        ),
        label = "tile_ent_$rank",
    )
    val model = rememberSafeImageRequest(album?.thumbnailUrl)
    ElevatedCard(
        shape = shape,
        colors = CardDefaults.elevatedCardColors(containerColor = palette.border),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
        modifier = modifier.graphicsLayer {
            scaleX = tileEnter
            scaleY = tileEnter
            alpha = tileEnter
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accent.copy(alpha = 0.08f),
                                Color.Transparent,
                                palette.surfaceBase.copy(alpha = 0.65f),
                            )
                        )
                    )
            )
            if (album != null) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.96f }
                ) {
                    AsyncImage(
                        model = model,
                        contentDescription = album.album.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(palette.border.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🎼", fontSize = 40.sp)
                }
            }
            Box(
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.11f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.18f),
                            ),
                            start = androidx.compose.ui.geometry.Offset.Zero,
                            end = androidx.compose.ui.geometry.Offset.Infinite,
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp),
            ) {
                ElevatedAssistChip(
                    onClick = {},
                    label = {
                        Text(
                            "#$rank",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = accent,
                        )
                    },
                    shape = sonarBadgeShape,
                    colors = AssistChipDefaults.elevatedAssistChipColors(
                        containerColor = Color.Black.copy(alpha = 0.45f),
                        labelColor = Color.White,
                    ),
                    elevation = AssistChipDefaults.elevatedAssistChipElevation(8.dp),
                )
            }
            if (rank == 1 && album != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp),
                ) {
                    Column {
                        Text(
                            text = album.album.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                shadow = Shadow(color = Color.Black.copy(alpha = 0.7f), blurRadius = 8f),
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = album.artists.joinToString(" · ") { it.name },
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 6f),
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7. SONAR Profile page (personality)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProfileSonarPage(
    profile: SonarProfile,
    topSong: SongWithStats?,
    modifier: Modifier = Modifier,
) {
    val paletteBase = rememberSonarPalette(SonarPaletteRole.PROFILE)
    val animatedAccent by androidx.compose.animation.animateColorAsState(
        targetValue = paletteBase.accent.blend(profile.accentSeed, 0.55f),
        animationSpec = spring(),
        label = "prof_acc",
    )
    val palette = remember(animatedAccent, paletteBase) {
        paletteBase.copy(
            accent = animatedAccent,
            surfaceGlow = animatedAccent.blend(paletteBase.surfaceGlow, 0.4f),
        )
    }

    val emojiScale by rememberInfiniteTransition(label = "emojiPulseP").animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "emSP",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
            .statusBarsPadding()
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        SuggestionChip(
            onClick = {},
            label = {
                Text(
                    text  = stringResource(R.string.sonar_personality).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp, fontSize = 10.sp,
                    ),
                )
            },
            icon  = {
                Icon(
                    painter = painterResource(R.drawable.person),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
            },
            shape = CircleShape,
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = animatedAccent.copy(alpha = 0.16f),
                labelColor = animatedAccent,
                iconContentColor = animatedAccent,
            ),
            border = null,
            modifier = Modifier.height(28.dp),
        )

        Spacer(Modifier.height(30.dp))

        Box(
            modifier = Modifier
                .size(96.dp)
                .scale(emojiScale)
                .clip(CircleShape)
                .background(animatedAccent.copy(alpha = 0.2f))
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(listOf(animatedAccent, palette.surfaceGlow, animatedAccent)),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(profile.emoji, fontSize = 52.sp)
        }

        Spacer(Modifier.height(18.dp))

        Text(
            text       = profile.title,
            style      = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                shadow = Shadow(
                    color = animatedAccent.copy(alpha = 0.5f),
                    offset = Offset(0f, 4f),
                    blurRadius = 18f,
                ),
            ),
            color      = animatedAccent,
            letterSpacing = (-1).sp,
        )

        Spacer(Modifier.height(16.dp))

        SonarGlassCard(palette, shape = sonarCardShape) {
            Text(
                text       = profile.description,
                style      = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Medium, lineHeight = 26.sp,
                ),
                color      = palette.textSecondary,
            )
        }

        topSong?.let { song ->
            Spacer(Modifier.height(12.dp))
            SonarGlassCard(palette, shape = sonarCardShape) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(sonarThumbShape)
                            .background(animatedAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("🎵", fontSize = 20.sp)
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text  = stringResource(R.string.sonar_anthem),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                            ),
                            color = animatedAccent,
                        )
                        Text(
                            text       = song.title,
                            style      = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color      = palette.textPrimary,
                            maxLines   = 2,
                            overflow   = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 8. Report / Share page (SONAR Report)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ReportSonarPage(
    year: Int,
    totalListeningTime: Long,
    totalSongsPlayed: Long,
    topSong: SongWithStats?,
    topArtist: Artist?,
    topAlbum: Album?,
    modifier: Modifier = Modifier,
) {
    val palette = rememberSonarPalette(SonarPaletteRole.REPORT)
    val animatedAccent by androidx.compose.animation.animateColorAsState(
        targetValue = palette.accent,
        animationSpec = spring(),
        label = "rep_acc",
    )

    val confettiParticles = remember {
        List(26) {
            Triple(
                Random.nextFloat(),
                Random.nextFloat(),
                listOf(palette.accent, palette.surfaceGlow).random(),
            )
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "sonar_confetti")
    val confettiTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing)),
        label = "conf_t",
    )

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            confettiParticles.forEach { (x, y, color) ->
                val px = ((x + confettiTime * 0.00004f) % 1f) * size.width
                val py = ((y + confettiTime * 0.00009f) % 1f) * size.height
                drawCircle(color = color.copy(alpha = 0.55f), radius = 4f, center = Offset(px, py))
                drawRect(
                    color = color.copy(alpha = 0.4f),
                    topLeft = Offset(px + 8f, py - 4f),
                    size = androidx.compose.ui.geometry.Size(9f, 4f),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Spacer(Modifier.height(2.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(animatedAccent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.waves),
                            contentDescription = null,
                            tint = animatedAccent,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Column {
                        Text(
                            stringResource(R.string.year_in_music),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary,
                        )
                        Text(
                            stringResource(R.string.sonar_summary, year),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.textSecondary,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    text       = stringResource(R.string.sonar_your_report),
                    style      = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        shadow = Shadow(
                            color = animatedAccent.copy(alpha = 0.4f),
                            offset = Offset(0f, 4f),
                            blurRadius = 14f,
                        ),
                    ),
                    color      = palette.textPrimary,
                    letterSpacing = (-1).sp,
                    lineHeight = 44.sp,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SonarStatBox(
                        iconRes = R.drawable.timer,
                        label = stringResource(R.string.total_listening_time),
                        value = makeTimeString(totalListeningTime),
                        palette = palette,
                        accent  = animatedAccent,
                        shape   = sonarStatBoxLeft,
                        modifier = Modifier.weight(1f),
                    )
                    SonarStatBox(
                        iconRes = R.drawable.play,
                        label = stringResource(R.string.sonar_total_plays),
                        value = totalSongsPlayed.toString(),
                        palette = palette,
                        accent  = animatedAccent,
                        shape   = sonarStatBoxRight,
                        modifier = Modifier.weight(1f),
                    )
                }

                ElevatedCard(
                    shape = sonarCardShape,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = palette.border,
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, palette.border), sonarCardShape),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            SuggestionChip(
                                onClick = {},
                                label = {
                                    Text(
                                        text  = stringResource(R.string.sonar_highlights),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                        ),
                                    )
                                },
                                icon  = {
                                    Icon(
                                        painter = painterResource(R.drawable.auto_awesome),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                    )
                                },
                                shape = CircleShape,
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = animatedAccent.copy(alpha = 0.18f),
                                    labelColor = animatedAccent,
                                    iconContentColor = animatedAccent,
                                ),
                                border = null,
                                modifier = Modifier.height(26.dp),
                            )
                        }

                        if (topSong == null && topArtist == null && topAlbum == null) {
                            Text(
                                text  = stringResource(R.string.no_listening_data),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textSecondary,
                            )
                        } else {
                            topSong?.let {
                                SonarHighlightRow(
                                    iconRes = R.drawable.music_note,
                                    label   = "Top Signal",
                                    value   = it.title,
                                    palette = palette,
                                    accent  = animatedAccent,
                                )
                            }
                            topArtist?.let {
                                SonarHighlightRow(
                                    iconRes = R.drawable.person,
                                    label   = "Top Transmitter",
                                    value   = it.artist.name,
                                    palette = palette,
                                    accent  = animatedAccent,
                                )
                            }
                            topAlbum?.let {
                                SonarHighlightRow(
                                    iconRes = R.drawable.album,
                                    label   = "Top Transmission",
                                    value   = it.album.title,
                                    palette = palette,
                                    accent  = animatedAccent,
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.waves),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = palette.textSecondary,
                    )
                    Text(
                        stringResource(R.string.app_name) + " · $year",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        color = palette.textSecondary,
                    )
                }
                Text(
                    stringResource(R.string.year_in_music),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 1.1.sp,
                    ),
                    color = palette.textSecondary,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable MD3 Expressive components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SonarProgressBar(
    totalPages: Int,
    currentPage: Int,
    palette: SonarPalette,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalPages) { index ->
            val targetFraction = when {
                index < currentPage -> 1f
                index == currentPage -> 1f
                else -> 0f
            }
            val fillFraction by animateFloatAsState(
                targetValue = targetFraction,
                animationSpec = spring(stiffness = 480f, dampingRatio = 0.9f),
                label = "segFill$index",
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(palette.textPrimary.copy(alpha = 0.22f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fillFraction)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(palette.accent, palette.surfaceGlow)
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun SonarYearChip(
    year: Int,
    palette: SonarPalette,
    onClick: () -> Unit,
) {
    val animatedAccent by androidx.compose.animation.animateColorAsState(
        targetValue = palette.accent,
        animationSpec = spring(),
        label = "year_chip_accent",
    )

    FilterChip(
        selected = true,
        enabled = true,
        onClick = onClick,
        label = {
            Text(
                text       = year.toString(),
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color      = animatedAccent,
            )
        },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.expand_more),
                contentDescription = null,
                tint     = animatedAccent,
                modifier = Modifier.size(15.dp),
            )
        },
        shape = sonarChipShape,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = palette.border,
            labelColor = palette.textPrimary,
            iconColor = palette.textPrimary,
            selectedContainerColor = animatedAccent.copy(alpha = 0.18f),
            selectedLabelColor = animatedAccent,
            selectedLeadingIconColor = animatedAccent,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = true,
            borderColor = palette.border,
            selectedBorderColor = animatedAccent.copy(alpha = 0.4f),
        ),
    )
}

@Composable
private fun SonarShareFab(
    isGenerating: Boolean,
    palette: SonarPalette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val animatedAccent by androidx.compose.animation.animateColorAsState(
        targetValue = palette.accent,
        animationSpec = spring(),
        label = "fab_acc",
    )
    val infiniteTransition = rememberInfiniteTransition(label = "sonar_fab_ring")
    val ringRot by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing)),
        label = "fabRot",
    )

    Box(
        modifier = modifier
            .drawBehind {
                rotate(ringRot) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            listOf(
                                animatedAccent, palette.surfaceGlow, animatedAccent
                            )
                        ),
                        radius = size.minDimension / 2f + 4.dp.toPx(),
                        style  = Stroke(width = 3.dp.toPx()),
                    )
                }
            }
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            expanded = !isGenerating,
            icon = {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color       = palette.onAccent,
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.share),
                        contentDescription = null,
                    )
                }
            },
            text = {
                Text(
                    text  = stringResource(R.string.sonar_share),
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                    ),
                )
            },
            shape = sonarCardShape,
            containerColor = animatedAccent,
            contentColor = palette.onAccent,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SonarYearPickerSheet(
    availableYears: List<Int>,
    selectedYear: Int,
    onSelectYear: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Expanded,
        skipHiddenState = false,
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
        tonalElevation = 2.dp,
        shape = RoundedCornerShape(
            topStart = 32.dp, topEnd = 32.dp, bottomStart = 0.dp, bottomEnd = 0.dp,
        ),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                width = 40.dp, height = 4.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text  = stringResource(R.string.sonar_select_year),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(availableYears.size) { idx ->
                    val year = availableYears[idx]
                    val isSelected = year == selectedYear
                    ElevatedFilterChip(
                        selected = isSelected,
                        enabled = true,
                        onClick = { onSelectYear(year) },
                        label = {
                            Text(
                                text       = year.toString(),
                                style      = MaterialTheme.typography.titleLarge,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            )
                        },
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.check),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else null,
                        shape = sonarCardShape,
                        colors = FilterChipDefaults.elevatedFilterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun SonarGlassCard(
    palette: SonarPalette,
    shape: androidx.compose.ui.graphics.Shape,
    content: @Composable () -> Unit,
) {
    Card(
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = palette.border,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, palette.border), shape),
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun SonarStatChip(
    @androidx.annotation.DrawableRes iconRes: Int,
    value: String,
    palette: SonarPalette,
    accent: Color,
) {
    AssistChip(
        onClick = {},
        label = {
            Text(
                text       = value,
                style      = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color      = palette.textPrimary,
            )
        },
        leadingIcon = {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = accent,
            )
        },
        shape = sonarChipShape,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = accent.copy(alpha = 0.16f),
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.3f)),
    )
}

@Composable
private fun SonarStatBox(
    @androidx.annotation.DrawableRes iconRes: Int,
    label: String,
    value: String,
    palette: SonarPalette,
    accent: Color,
    shape: androidx.compose.ui.graphics.Shape,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        shape = shape,
        colors = CardDefaults.elevatedCardColors(
            containerColor = palette.border,
        ),
        modifier = modifier
            .border(BorderStroke(1.dp, palette.border), shape),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = accent,
                )
            }
            Text(
                text       = value,
                style      = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Black,
                ),
                color      = accent,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = palette.textSecondary,
            )
        }
    }
}

@Composable
private fun SonarHighlightRow(
    @androidx.annotation.DrawableRes iconRes: Int,
    label: String,
    value: String,
    palette: SonarPalette,
    accent: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(sonarThumbShape)
                .background(accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text  = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                ),
                color = accent,
            )
            Text(
                text       = value,
                style      = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color      = palette.textPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated counter + safe image request
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun rememberAnimatedLong(target: Long, durationMs: Int = 1400): Long {
    var current by remember { mutableLongStateOf(0L) }
    LaunchedEffect(target) {
        val startMs = System.currentTimeMillis()
        while (true) {
            val elapsed  = (System.currentTimeMillis() - startMs).coerceAtMost(durationMs.toLong())
            val progress = elapsed.toFloat() / durationMs
            val eased    = FastOutSlowInEasing.transform(progress)
            current      = (target * eased).toLong()
            if (elapsed >= durationMs) break
            delay(16L)
        }
    }
    return current
}

@Composable
private fun rememberSafeImageRequest(data: Any?): Any? {
    val context = LocalContext.current
    return remember(data) {
        data?.let {
            ImageRequest.Builder(context)
                .data(it)
                .allowHardware(false)
                .build()
        }
    }
}
