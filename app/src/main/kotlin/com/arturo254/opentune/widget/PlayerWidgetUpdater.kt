/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import timber.log.Timber
import java.io.ByteArrayOutputStream

object PlayerWidgetUpdater {
    @Volatile private var lastArtworkBitmap: Bitmap? = null
    @Volatile private var lastArtworkBytes: ByteArray? = null

    @Volatile private var lastBlurBitmap: Bitmap? = null
    @Volatile private var lastBlurBytes: ByteArray? = null

    suspend fun update(
        context: Context,
        state: PlayerWidgetState,
    ) {
        runCatching {
            val manager = GlanceAppWidgetManager(context)
            val widgetClasses = listOf(
                OpenTunePlayerWidget::class.java to OpenTunePlayerWidget(),
                OpenTuneCompactWidget::class.java to OpenTuneCompactWidget(),
                OpenTuneVinylWidget::class.java to OpenTuneVinylWidget(),
                OpenTuneLargeWidget::class.java to OpenTuneLargeWidget(),
            )

            val activeWidgets = widgetClasses.mapNotNull { (clazz, widget) ->
                val glanceIds = manager.getGlanceIds(clazz)
                if (glanceIds.isNotEmpty()) (widget to glanceIds) else null
            }
            if (activeWidgets.isEmpty()) return@runCatching

            val artworkBytes = state.artworkBitmap?.let { bmp ->
                if (bmp == lastArtworkBitmap && lastArtworkBytes != null) {
                    lastArtworkBytes
                } else {
                    val bytes = bitmapToByteArray(bmp)
                    lastArtworkBitmap = bmp
                    lastArtworkBytes = bytes
                    bytes
                }
            }

            val blurBytes = state.backgroundBlurBitmap?.let { bmp ->
                if (bmp == lastBlurBitmap && lastBlurBytes != null) {
                    lastBlurBytes
                } else {
                    val bytes = bitmapToByteArray(bmp)
                    lastBlurBitmap = bmp
                    lastBlurBytes = bytes
                    bytes
                }
            }

            activeWidgets.forEach { (widget, glanceIds) ->
                glanceIds.forEach { glanceId ->
                    updateAppWidgetState(
                        context,
                        PreferencesGlanceStateDefinition,
                        glanceId
                    ) { preferences ->
                        preferences.toMutablePreferences().apply {
                            this[PlayerWidgetStateKeys.Title] = state.title
                            this[PlayerWidgetStateKeys.Artist] = state.artist
                            state.thumbnailUrl?.let {
                                this[PlayerWidgetStateKeys.ThumbnailUrl] = it
                            } ?: remove(PlayerWidgetStateKeys.ThumbnailUrl)
                            this[PlayerWidgetStateKeys.IsPlaying] = state.isPlaying
                            this[PlayerWidgetStateKeys.HasPrevious] = state.hasPrevious
                            this[PlayerWidgetStateKeys.HasNext] = state.hasNext
                            this[PlayerWidgetStateKeys.DurationMs] = state.durationMs
                            this[PlayerWidgetStateKeys.PositionMs] = state.positionMs

                            artworkBytes?.let {
                                this[PlayerWidgetStateKeys.ArtworkBytes] = it
                            } ?: remove(PlayerWidgetStateKeys.ArtworkBytes)

                            blurBytes?.let {
                                this[PlayerWidgetStateKeys.BackgroundBlurBytes] = it
                            } ?: remove(PlayerWidgetStateKeys.BackgroundBlurBytes)

                            state.dominantColor?.let {
                                this[PlayerWidgetStateKeys.DominantColor] = it
                            } ?: remove(PlayerWidgetStateKeys.DominantColor)
                        }
                    }
                    widget.update(context, glanceId)
                }
            }
        }.onFailure {
            Timber.tag("PlayerWidgetUpdater").w(it, "Unable to update OpenTune player widget")
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        return stream.toByteArray()
    }
}