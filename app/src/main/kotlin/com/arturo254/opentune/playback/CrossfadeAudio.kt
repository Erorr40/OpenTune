/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 *
 * Crossfade improvements ported from ArchiveTune (github.com/koiverse):
 *  - Equal-power volume curve (sin/cos) — elimina el "dip" perceptual en el punto medio
 *  - Gapless album skip — no hace crossfade entre pistas del mismo álbum
 *  - Buffer check antes de iniciar — evita arranque con rebuffering
 */

package com.arturo254.opentune.playback

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.arturo254.opentune.constants.AudioCrossfadeGaplessKey
import com.arturo254.opentune.constants.AudioAutomixKey
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.extensions.metadata
import com.arturo254.opentune.utils.PreferenceStore
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

internal class CrossfadeAudio(
    private val player: ExoPlayer,
    private val database: MusicDatabase,
    private val crossfadeDurationMs: MutableStateFlow<Int>,
    private val playbackFadeFactor: MutableStateFlow<Float>,
    private val playerVolume: MutableStateFlow<Float>,
    private val audioFocusVolumeFactor: MutableStateFlow<Float>,
    private val audioNormalizationEnabled: MutableStateFlow<Boolean>,
    private val maxSafeGainFactor: Float,
    private val overlapPlayerFactory: () -> ExoPlayer,
    private val onCrossfadeStart: (MediaItem) -> Unit = {},
    private val isGaplessAlbumEnabled: () -> Boolean = {
        (PreferenceStore.get(AudioCrossfadeGaplessKey) ?: false) ||
                (PreferenceStore.get(AudioAutomixKey) ?: false)
    },
) {
    // ── Estado del loop ───────────────────────────────────────────────────────

    private var loopJob: Job? = null
    private var isPaused: Boolean = false
    private var pauseStartElapsedMs: Long = 0L

    // ── Estado del overlap player ─────────────────────────────────────────────

    private var overlapPlayer: ExoPlayer? = null
    private var overlapPrimedIndex: Int = C.INDEX_UNSET
    private var overlapPrimedMediaId: String? = null
    private var crossfadeActive = false
    private var crossfadeTargetIndex: Int = C.INDEX_UNSET
    private var crossfadeTargetMediaId: String? = null
    private var crossfadeStartElapsedMs: Long = 0L
    private var crossfadeActiveDurationMs: Int = 0
    private var overlapNormalizeFactor: Float = 1f

    // ── Estado del handoff ────────────────────────────────────────────────────

    private var handoffActive = false
    private var handoffStartElapsedMs: Long = 0L
    private var handoffDurationMs: Int = 0
    private var handoffTargetPositionMs: Long = 0L
    private var handoffLastSyncSeekElapsedMs: Long = 0L
    private var handoffSeekIssued = false
    private var handoffRampStarted = false

    // Constantes de handoff
    private val handoffReseekMinIntervalMs = 180L
    private val handoffDriftCorrectionThresholdMs = 220L
    private val handoffRampStartDriftToleranceMs = 120L
    private val handoffTimeoutMs = 5000L

    // ── Buffer mínimo antes de arrancar el crossfade ──────────────────────────

    /** Cuánto buffer (ms) necesita el overlap player antes de permitir beginOverlapCrossfade.
     *  Se calcula como fadeMs + 2s, acotado entre 3s y 10s. */
    private fun requiredStartBufferMs(fadeMs: Int): Long =
        (fadeMs.toLong() + 2_000L).coerceIn(3_000L, 10_000L)

    // ── API pública ───────────────────────────────────────────────────────────

    fun isCrossfading(): Boolean = crossfadeActive

    fun start(scope: CoroutineScope) {
        if (loopJob?.isActive == true) return
        loopJob = scope.launch { runLoop() }
    }

    fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        handleMediaItemTransition(mediaItem, reason)
    }

    fun onPlaybackStateChanged(@Player.State playbackState: Int) {
        if (playbackState == Player.STATE_IDLE || playbackState == Player.STATE_ENDED) {
            stop(resetMainFade = true)
        }
    }

    fun stop(resetMainFade: Boolean) {
        stopOverlapCrossfade(resetMainFade = resetMainFade)
    }

    fun release() {
        loopJob?.cancel()
        loopJob = null
        stopOverlapCrossfade(resetMainFade = true)
        runCatching { overlapPlayer?.release() }
        overlapPlayer = null
    }

    // ── Loop principal ────────────────────────────────────────────────────────

    private suspend fun runLoop() {
        while (kotlin.coroutines.coroutineContext.isActive) {
            val fadeMs = crossfadeDurationMs.value

            if (fadeMs <= 0) {
                stopOverlapCrossfade(resetMainFade = true)
                delay(250)
                continue
            }

            if (!player.playWhenReady) {
                if (crossfadeActive || handoffActive) {
                    if (!isPaused) {
                        isPaused = true
                        pauseStartElapsedMs = android.os.SystemClock.elapsedRealtime()
                        overlapPlayer?.playWhenReady = false
                    }
                } else {
                    stopOverlapCrossfade(resetMainFade = true)
                }
                delay(150)
                continue
            } else if (isPaused) {
                isPaused = false
                val pauseDuration =
                    (android.os.SystemClock.elapsedRealtime() - pauseStartElapsedMs).coerceAtLeast(0L)
                if (crossfadeActive) {
                    crossfadeStartElapsedMs += pauseDuration
                }
                if (handoffActive) {
                    handoffStartElapsedMs += pauseDuration
                }
                overlapPlayer?.let { overlap ->
                    overlap.playWhenReady = true
                    overlap.play()
                }
            }

            // Durante el handoff solo actualizar volúmenes
            if (handoffActive) {
                updateVolumes()
                delay(50)
                continue
            }

            if (!crossfadeActive && (player.playbackState != Player.STATE_READY || !player.isPlaying)) {
                stopOverlapCrossfade(resetMainFade = true)
                delay(150)
                continue
            }

            val durationMs = player.duration
            val positionMs = player.currentPosition.coerceAtLeast(0L)
            val nextIndex = player.nextMediaItemIndex

            val effectiveFadeMs = calculateActiveCrossfadeDuration(
                configuredFadeMs = fadeMs,
                remainingMs = fadeMs.toLong(),
                trackDurationMs = durationMs,
                incomingDurationMs = overlapPlayer?.duration ?: C.TIME_UNSET,
            )

            // No crossfade en repeat-one
            if (player.repeatMode == Player.REPEAT_MODE_ONE) {
                stopOverlapCrossfade(resetMainFade = true)
                delay(150)
                continue
            }

            if (!crossfadeActive && (nextIndex == C.INDEX_UNSET || durationMs <= 0 || durationMs == C.TIME_UNSET)) {
                stopOverlapCrossfade(resetMainFade = true)
                delay(150)
                continue
            }

            // ── Skip gapless para álbumes ─────────────────────────────────────
            // Si la transición es entre pistas del mismo álbum y el usuario activó
            // el modo gapless para álbumes, lo omitimos para respetar el gapless.
            if (!crossfadeActive && nextIndex != C.INDEX_UNSET) {
                val currentItem =
                    runCatching { player.getMediaItemAt(player.currentMediaItemIndex) }.getOrNull()
                val nextItem = runCatching { player.getMediaItemAt(nextIndex) }.getOrNull()
                if (currentItem != null && nextItem != null && isGaplessAlbumTransition(
                        currentItem,
                        nextItem
                    )
                ) {
                    unprimeOverlap()
                    delay(150)
                    continue
                }
            }

            if (crossfadeActive) {
                val targetId = crossfadeTargetMediaId
                val currentId = player.currentMediaItem?.mediaId
                val onTarget = !targetId.isNullOrBlank() && targetId == currentId

                if (!onTarget && (nextIndex == C.INDEX_UNSET || durationMs <= 0 || durationMs == C.TIME_UNSET)) {
                    stopOverlapCrossfade(resetMainFade = true)
                    delay(150)
                    continue
                }

                val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
                val tooFarFromEnd = !onTarget && remainingMs > effectiveFadeMs.toLong() + 2000L
                val nextChanged =
                    !onTarget && crossfadeTargetIndex != C.INDEX_UNSET && nextIndex != crossfadeTargetIndex
                if (tooFarFromEnd || nextChanged) {
                    stopOverlapCrossfade(resetMainFade = true)
                    delay(100)
                    continue
                }

                updateVolumes()
                delay(50)
                continue
            }

            val remainingMs = (durationMs - positionMs).coerceAtLeast(0L)
            val preloadWindowMs = effectiveFadeMs.toLong() + 1200L

            if (remainingMs in 1L..preloadWindowMs) {
                primeOverlapForNext(nextIndex)
            } else {
                unprimeOverlap()
            }

            // ── Iniciar crossfade cuando quede tiempo suficiente ──────────────
            if (overlapPrimedIndex == nextIndex && remainingMs in 1L..effectiveFadeMs.toLong()) {
                // Verificar buffer mínimo ANTES de comenzar — evita arrancar con rebuffering
                val overlap = overlapPlayer
                if (overlap != null && hasEnoughBuffer(overlap, requiredStartBufferMs(effectiveFadeMs))) {
                    beginOverlapCrossfade(fadeMs = effectiveFadeMs, remainingMs = remainingMs)
                }
                // Si no hay buffer suficiente, el próximo tick lo intentará de nuevo
                delay(50)
                continue
            }

            if (playbackFadeFactor.value != 1f) playbackFadeFactor.value = 1f
            delay(100)
        }
    }

    // ── Helpers de buffer ─────────────────────────────────────────────────────

    /**
     * Verifica si [targetPlayer] tiene al menos [minMs] de audio bufferizado,
     * o si el track es tan corto que está prácticamente completo en buffer.
     */
    private fun hasEnoughBuffer(targetPlayer: ExoPlayer, minMs: Long): Boolean {
        if (minMs <= 0L) return true
        if (targetPlayer.playbackState != Player.STATE_READY) return false

        val duration = targetPlayer.duration
        val buffered = targetPlayer.totalBufferedDuration.coerceAtLeast(0L)
        if (buffered >= minMs) return true

        // Track corto: aceptar si ya está casi completamente bufferizado
        return duration != C.TIME_UNSET &&
                targetPlayer.bufferedPosition >= duration - 150L
    }

    // ── Detección de transición gapless ───────────────────────────────────────

    /**
     * Devuelve true si [current] y [target] pertenecen al mismo álbum,
     * indicando que la transición debe ser gapless (sin crossfade).
     */
    internal fun isGaplessAlbumTransition(current: MediaItem, target: MediaItem): Boolean =
        Companion.isGaplessAlbumTransition(current, target, isGaplessAlbumEnabled())

    // ── Gestión del overlap player ────────────────────────────────────────────

    private suspend fun primeOverlapForNext(nextIndex: Int) {
        val nextItem = runCatching { player.getMediaItemAt(nextIndex) }.getOrNull() ?: return
        val nextMediaId = nextItem.mediaId

        if (overlapPrimedIndex == nextIndex && overlapPrimedMediaId == nextMediaId) return

        stopOverlapCrossfade(resetMainFade = false)

        val overlap = ensureOverlapPlayer()
        overlap.clearMediaItems()
        overlap.setMediaItem(nextItem)
        overlap.prepare()
        overlap.playWhenReady = false
        overlap.volume = 0f

        overlapNormalizeFactor = fetchNormalizeFactorForMediaId(nextMediaId)
        overlapPrimedIndex = nextIndex
        overlapPrimedMediaId = nextMediaId
    }

    private fun unprimeOverlap() {
        if (crossfadeActive) return
        if (overlapPrimedIndex == C.INDEX_UNSET && overlapPrimedMediaId == null) return
        stopOverlapCrossfade(resetMainFade = false)
    }

    private fun beginOverlapCrossfade(fadeMs: Int, remainingMs: Long) {
        val overlap = overlapPlayer ?: return

        val targetIndex = overlapPrimedIndex
        if (targetIndex != C.INDEX_UNSET && targetIndex < player.mediaItemCount) {
            onCrossfadeStart(player.getMediaItemAt(targetIndex))
        }

        crossfadeActive = true
        crossfadeStartElapsedMs = android.os.SystemClock.elapsedRealtime()
        crossfadeActiveDurationMs = calculateActiveCrossfadeDuration(
            configuredFadeMs = fadeMs,
            remainingMs = remainingMs,
            trackDurationMs = player.duration,
            incomingDurationMs = overlap.duration,
        )
        crossfadeTargetIndex = overlapPrimedIndex
        crossfadeTargetMediaId = overlapPrimedMediaId

        overlap.seekTo(0L)
        overlap.playWhenReady = true
        overlap.play()
    }

    // ── Actualización de volúmenes (equal-power) ──────────────────────────────

    /**
     * Aplica la curva de crossfade equal-power usando sin/cos.
     *
     * Con una curva lineal la suma de potencias cae ~3 dB en t=0.5.
     * Con sin/cos se mantiene constante (sin²θ + cos²θ = 1),
     * eliminando el "dip" perceptual en el centro del fundido.
     *
     * @param t          Progreso normalizado [0..1]
     * @param baseVolume Volumen base del player principal
     * @param outgoing   Player que está saliendo (volumen decrece: cos)
     * @param incoming   Player que está entrando  (volumen crece:  sin)
     */
    private fun applyEqualPowerVolumes(
        t: Float,
        baseVolume: Float,
        outgoing: ExoPlayer,
        incoming: ExoPlayer,
    ) {
        val (outgoingFactor, incomingFactor) = calculateEqualPowerVolumes(t)
        outgoing.volume = (baseVolume * outgoingFactor).coerceIn(0f, maxSafeGainFactor)
        incoming.volume = (baseVolume * incomingFactor).coerceIn(0f, maxSafeGainFactor)
    }

    private fun updateVolumes() {
        val overlap = overlapPlayer ?: run {
            stopOverlapCrossfade(resetMainFade = true)
            return
        }

        val baseOverlapVolume =
            (playerVolume.value * overlapNormalizeFactor * audioFocusVolumeFactor.value)
                .coerceIn(0f, 1f)

        // ── Fase de handoff ───────────────────────────────────────────────────
        if (handoffActive) {
            val nowElapsedMs = android.os.SystemClock.elapsedRealtime()

            val overlapDead =
                overlap.playbackState == Player.STATE_IDLE || overlap.playbackState == Player.STATE_ENDED
            val handoffElapsed = nowElapsedMs - handoffStartElapsedMs
            val handoffTimedOut = handoffElapsed >= handoffTimeoutMs

            if (overlapDead || handoffTimedOut) {
                completeHandoffFromOverlap()
                return
            }

            val overlapPositionMs = overlap.currentPosition.coerceAtLeast(0L)
            val mainPositionMs = player.currentPosition.coerceAtLeast(0L)
            val positionDriftMs = mainPositionMs - overlapPositionMs

            val shouldResyncMainToOverlap =
                !handoffSeekIssued || (
                        abs(positionDriftMs) > handoffDriftCorrectionThresholdMs &&
                                nowElapsedMs - handoffLastSyncSeekElapsedMs >= handoffReseekMinIntervalMs
                        )

            if (shouldResyncMainToOverlap) {
                handoffSeekIssued = true
                handoffTargetPositionMs = overlapPositionMs
                val currentIndex = player.currentMediaItemIndex
                if (currentIndex != C.INDEX_UNSET) {
                    player.seekTo(currentIndex, handoffTargetPositionMs)
                    handoffLastSyncSeekElapsedMs = nowElapsedMs
                }
                playbackFadeFactor.value = 0f
                overlap.volume = baseOverlapVolume
                return
            }

            if (!handoffRampStarted) {
                val bufferedMs = player.totalBufferedDuration.coerceAtLeast(0L)
                val mainStable =
                    player.playbackState == Player.STATE_READY &&
                            player.isPlaying &&
                            bufferedMs >= 1200L &&
                            abs(positionDriftMs) <= handoffRampStartDriftToleranceMs

                if (!mainStable) {
                    playbackFadeFactor.value = 0f
                    overlap.volume = baseOverlapVolume
                    return
                }

                handoffRampStarted = true
                handoffStartElapsedMs = nowElapsedMs
            }

            val denom = handoffDurationMs.toLong().coerceAtLeast(1L)
            val elapsed = (nowElapsedMs - handoffStartElapsedMs).coerceAtLeast(0L)
            val (mainFactor, overlapFactor) = calculateHandoffVolumes(elapsed, handoffDurationMs)

            // Handoff: ramp lineal corto (450 ms) — suficiente para que no se note
            playbackFadeFactor.value = mainFactor
            overlap.volume = (baseOverlapVolume * overlapFactor).coerceIn(0f, 1f)

            val t = (elapsed.toFloat() / denom.toFloat()).coerceIn(0f, 1f)
            if (t >= 1f) completeHandoffFromOverlap()
            return
        }

        // ── Fase de crossfade activo (equal-power) ────────────────────────────
        val denom = crossfadeActiveDurationMs.toLong().coerceAtLeast(1L)
        val elapsed = (android.os.SystemClock.elapsedRealtime() - crossfadeStartElapsedMs).coerceAtLeast(0L)
        val t = (elapsed.toFloat() / denom.toFloat()).coerceIn(0f, 1f)

        // Calculamos el factor del player principal a través del flujo playbackFadeFactor
        // para que el volumen final del player principal sea:
        //   player.volume = playerVolume * normalizeFactor * audioFocusFactor * playbackFadeFactor
        //                 = baseMainVolume * cos(t * π/2)
        //
        // Necesitamos que playbackFadeFactor = cos(t * π/2), ya que los demás factores
        // se combinan en el collect de MusicService.
        val (outgoingFactor, incomingFactor) = calculateEqualPowerVolumes(t)
        playbackFadeFactor.value = outgoingFactor

        // El overlap no pasa por playbackFadeFactor, así que aplicamos el volumen completo
        // directamente con la curva seno.
        overlap.volume =
            (baseOverlapVolume * incomingFactor).coerceIn(0f, maxSafeGainFactor)
    }

    // ── Transición de MediaItem ───────────────────────────────────────────────

    private fun handleMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
        if (!crossfadeActive) return

        val targetId = crossfadeTargetMediaId
        val newId = mediaItem?.mediaId

        if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO &&
            !targetId.isNullOrBlank() && targetId == newId
        ) {
            beginHandoffFromOverlap()
            return
        }

        stopOverlapCrossfade(resetMainFade = true)
    }

    // ── Handoff: traspaso del overlap al player principal ─────────────────────

    private fun beginHandoffFromOverlap() {
        val overlap = overlapPlayer ?: run {
            stopOverlapCrossfade(resetMainFade = true)
            return
        }

        val overlapPositionMs = overlap.currentPosition.coerceAtLeast(0L)

        handoffActive = true
        handoffSeekIssued = false
        handoffRampStarted = false
        handoffTargetPositionMs = overlapPositionMs
        handoffStartElapsedMs = android.os.SystemClock.elapsedRealtime()
        handoffLastSyncSeekElapsedMs = 0L
        handoffDurationMs = 450
        playbackFadeFactor.value = 0f
    }

    private fun completeHandoffFromOverlap() {
        val overlap = overlapPlayer ?: run {
            stopOverlapCrossfade(resetMainFade = true)
            return
        }

        runCatching {
            overlap.volume = 0f
            overlap.stop()
            overlap.clearMediaItems()
        }

        handoffActive = false
        handoffStartElapsedMs = 0L
        handoffDurationMs = 0
        handoffTargetPositionMs = 0L
        handoffLastSyncSeekElapsedMs = 0L
        handoffSeekIssued = false
        handoffRampStarted = false
        isPaused = false
        pauseStartElapsedMs = 0L

        crossfadeActive = false
        crossfadeTargetIndex = C.INDEX_UNSET
        crossfadeTargetMediaId = null
        crossfadeActiveDurationMs = 0
        overlapNormalizeFactor = 1f
        overlapPrimedIndex = C.INDEX_UNSET
        overlapPrimedMediaId = null

        playbackFadeFactor.value = 1f
    }

    // ── Stop / reset ──────────────────────────────────────────────────────────

    private fun stopOverlapCrossfade(resetMainFade: Boolean) {
        crossfadeActive = false
        crossfadeTargetIndex = C.INDEX_UNSET
        crossfadeTargetMediaId = null
        crossfadeActiveDurationMs = 0
        overlapNormalizeFactor = 1f
        overlapPrimedIndex = C.INDEX_UNSET
        overlapPrimedMediaId = null
        handoffActive = false
        handoffStartElapsedMs = 0L
        handoffDurationMs = 0
        handoffTargetPositionMs = 0L
        handoffLastSyncSeekElapsedMs = 0L
        handoffSeekIssued = false
        handoffRampStarted = false
        isPaused = false
        pauseStartElapsedMs = 0L

        overlapPlayer?.let { overlap ->
            runCatching {
                overlap.volume = 0f
                overlap.stop()
                overlap.clearMediaItems()
            }
        }

        if (resetMainFade) {
            playbackFadeFactor.value = 1f
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun ensureOverlapPlayer(): ExoPlayer {
        val existing = overlapPlayer
        if (existing != null) return existing
        return overlapPlayerFactory().also { overlapPlayer = it }
    }

    private suspend fun fetchNormalizeFactorForMediaId(mediaId: String): Float {
        if (!audioNormalizationEnabled.value) return 1f

        val format = withContext(Dispatchers.IO) {
            database.format(mediaId).first()
        }

        val loudness = format?.loudnessDb ?: format?.perceptualLoudnessDb ?: return 1f
        var factor = 10f.pow((-loudness.toFloat()) / 20f)
        if (factor > 1f) factor = min(factor, maxSafeGainFactor)
        return factor
    }

    companion object {
        const val MIN_DURATION_SECONDS = 0
        const val MAX_DURATION_SECONDS = 12

        /**
         * Clamps a duration in seconds to the supported range [0, 12].
         */
        fun clampCrossfadeDuration(seconds: Int): Int =
            seconds.coerceIn(MIN_DURATION_SECONDS, MAX_DURATION_SECONDS)

        fun clampCrossfadeDuration(seconds: Float): Float =
            seconds.coerceIn(MIN_DURATION_SECONDS.toFloat(), MAX_DURATION_SECONDS.toFloat())

        /**
         * Computes the equal-power volume attenuation factors for outgoing and incoming tracks.
         *
         * Outgoing follows cos(t * π / 2).
         * Incoming follows sin(t * π / 2).
         *
         * Mathematical property: cos²(θ) + sin²(θ) = 1.0 (constant acoustic power, 0 dB dip).
         *
         * @param t Progress in [0.0, 1.0]
         * @return Pair(outgoingVolumeFactor, incomingVolumeFactor)
         */
        fun calculateEqualPowerVolumes(t: Float): Pair<Float, Float> {
            val clamped = t.coerceIn(0f, 1f)
            val radians = clamped.toDouble() * (PI / 2.0)
            val outgoing = cos(radians).toFloat().coerceIn(0f, 1f)
            val incoming = sin(radians).toFloat().coerceIn(0f, 1f)
            return Pair(outgoing, incoming)
        }

        /**
         * Calculates the active crossfade duration in milliseconds, capping it for short tracks.
         *
         * For tracks shorter than the configured duration (or shorter than 2 * configured duration),
         * the duration is capped to min(configuredFadeMs, trackDurationMs / 2).
         */
        fun calculateActiveCrossfadeDuration(
            configuredFadeMs: Int,
            remainingMs: Long,
            trackDurationMs: Long = C.TIME_UNSET,
            incomingDurationMs: Long = C.TIME_UNSET,
        ): Int {
            if (configuredFadeMs <= 0) return 0
            var maxAllowed = configuredFadeMs.toLong()
            if (trackDurationMs > 0L) {
                maxAllowed = min(maxAllowed, trackDurationMs / 2)
            }
            if (incomingDurationMs > 0L) {
                maxAllowed = min(maxAllowed, incomingDurationMs / 2)
            }
            val effectiveRemaining = if (remainingMs > 0L) remainingMs else maxAllowed
            val finalDuration = min(maxAllowed, effectiveRemaining)
            return finalDuration.toInt().coerceAtLeast(1)
        }

        /**
         * Calculates linear ramp factors during the 450ms handoff phase.
         * Primary player fades in (0 -> 1), overlap player fades out (1 -> 0).
         */
        fun calculateHandoffVolumes(elapsedMs: Long, handoffDurationMs: Int = 450): Pair<Float, Float> {
            val denom = handoffDurationMs.coerceAtLeast(1)
            val t = (elapsedMs.toFloat() / denom.toFloat()).coerceIn(0f, 1f)
            val mainFactor = t
            val overlapFactor = 1f - t
            return Pair(mainFactor, overlapFactor)
        }

        /**
         * Checks if the transition between current and target media items represents
         * a transition within the same album and gapless album mode is enabled.
         */
        fun isGaplessAlbumTransition(
            current: MediaItem,
            target: MediaItem,
            isGaplessAlbumEnabled: Boolean,
        ): Boolean {
            if (!isGaplessAlbumEnabled) return false

            val albumA = current.metadata?.album?.id?.takeIf { it.isNotBlank() }
                ?: current.metadata?.album?.title?.takeIf { it.isNotBlank() }
                ?: current.mediaMetadata.albumTitle?.toString()?.takeIf { it.isNotBlank() }

            val albumB = target.metadata?.album?.id?.takeIf { it.isNotBlank() }
                ?: target.metadata?.album?.title?.takeIf { it.isNotBlank() }
                ?: target.mediaMetadata.albumTitle?.toString()?.takeIf { it.isNotBlank() }

            return albumA != null && albumA == albumB
        }
    }
}