/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.HardwareRenderer
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.hardware.HardwareBuffer
import android.media.ImageReader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.byteArrayPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.media3.common.Player
import androidx.palette.graphics.Palette
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.arturo254.opentune.extensions.currentMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

data class PlayerWidgetState(
    val title: String = "",
    val artist: String = "",
    val thumbnailUrl: String? = null,
    val artworkBitmap: Bitmap? = null,
    val backgroundBlurBitmap: Bitmap? = null,
    val dominantColor: Int? = null,
    val isPlaying: Boolean = false,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val durationMs: Long = 0L,
    val positionMs: Long = 0L,
) {
    val hasMedia: Boolean
        get() = title.isNotBlank() || artist.isNotBlank()

    // Progreso de la canción (0.0 a 1.0)
    val progress: Float
        get() = if (durationMs > 0) {
            (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
        } else 0f

    // Tiempo formateado para mostrar
    val positionText: String
        get() = formatTime(positionMs)

    val durationText: String
        get() = formatTime(durationMs)

    private fun formatTime(ms: Long): String {
        if (ms <= 0) return "0:00"
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return if (minutes >= 60) {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            String.format("%d:%02d:%02d", hours, remainingMinutes, seconds)
        } else {
            String.format("%d:%02d", minutes, seconds)
        }
    }

    companion object {
        private data class CachedArtwork(
            val url: String,
            val artworkBitmap: Bitmap?,
            val blurBitmap: Bitmap?,
            val dominantColor: Int?,
        )

        @Volatile private var cachedArtwork: CachedArtwork? = null

        suspend fun fromPlayer(player: Player, context: Context): PlayerWidgetState {
            val metadata = player.currentMetadata
            val thumbUrl = metadata?.thumbnailUrl

            var artworkBitmap: Bitmap? = null
            var blurBitmap: Bitmap? = null
            var dominantColor: Int? = null

            if (!thumbUrl.isNullOrEmpty()) {
                val cached = cachedArtwork
                if (cached != null && cached.url == thumbUrl && cached.artworkBitmap != null) {
                    artworkBitmap = cached.artworkBitmap
                    blurBitmap = cached.blurBitmap
                    dominantColor = cached.dominantColor
                } else {
                    artworkBitmap = loadArtworkFromUrl(context, thumbUrl)
                    blurBitmap = artworkBitmap?.let { computeBackgroundBlur(it) }
                    dominantColor = artworkBitmap?.let { computeDominantColor(it) }
                    if (artworkBitmap != null) {
                        cachedArtwork = CachedArtwork(thumbUrl, artworkBitmap, blurBitmap, dominantColor)
                    }
                }
            }

            return PlayerWidgetState(
                title = metadata?.title.orEmpty(),
                artist = metadata?.artists?.joinToString(", ") { it.name }.orEmpty(),
                thumbnailUrl = metadata?.thumbnailUrl,
                artworkBitmap = artworkBitmap,
                backgroundBlurBitmap = blurBitmap,
                dominantColor = dominantColor,
                isPlaying = player.isPlaying,
                hasPrevious = player.hasPreviousMediaItem(),
                hasNext = player.hasNextMediaItem(),
                durationMs = player.duration.takeIf { it > 0 } ?: 0L,
                positionMs = player.currentPosition.coerceAtLeast(0L),
            )
        }

        /**
         * Genera un fondo difuminado a partir del artwork usando RenderEffect (API 31+)
         * con fallback robusto por software (StackBlur) en APIs anteriores o ante fallos.
         */
        private suspend fun computeBackgroundBlur(source: Bitmap): Bitmap? {
            return withContext(Dispatchers.Default) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        blurBitmapApi31(source, radius = 28f, downscaleTo = 64)
                            ?: fastBlur(source, radius = 16, downscaleTo = 64)
                    } else {
                        fastBlur(source, radius = 16, downscaleTo = 64)
                    }
                } catch (e: Exception) {
                    Timber.tag("PlayerWidgetState")
                        .w(e, "Background blur failed, falling back to dominant color")
                    null
                }
            }
        }

        private fun fastBlur(source: Bitmap, radius: Int = 16, downscaleTo: Int = 64): Bitmap? {
            val ratio = source.width.toFloat() / source.height.toFloat()
            val width = if (ratio >= 1f) downscaleTo else (downscaleTo * ratio).toInt().coerceAtLeast(1)
            val height = if (ratio >= 1f) (downscaleTo / ratio).toInt().coerceAtLeast(1) else downscaleTo
            val small = Bitmap.createScaledBitmap(source, width, height, true)
            return try {
                stackBlur(small, radius.coerceAtLeast(1))
            } catch (_: Exception) {
                small
            }
        }

        private fun stackBlur(src: Bitmap, radius: Int): Bitmap {
            val w = src.width
            val h = src.height
            val pix = IntArray(w * h)
            src.getPixels(pix, 0, w, 0, 0, w, h)

            val wm = w - 1
            val hm = h - 1
            val wh = w * h
            val div = radius + radius + 1

            val r = IntArray(wh)
            val g = IntArray(wh)
            val b = IntArray(wh)
            var rsum: Int
            var gsum: Int
            var bsum: Int
            var p: Int
            var yp: Int
            var yi: Int
            var yw: Int
            val vmin = IntArray(maxOf(w, h))

            var divsum = (div + 1) shr 1
            divsum *= divsum
            val dv = IntArray(256 * divsum)
            for (j in 0 until 256 * divsum) {
                dv[j] = j / divsum
            }

            yw = 0
            yi = 0

            val stack = Array(div) { IntArray(3) }
            var stackpointer: Int
            var stackstart: Int
            var sir: IntArray
            var rbs: Int
            val r1 = radius + 1
            var routsum: Int
            var goutsum: Int
            var boutsum: Int
            var rinsum: Int
            var ginsum: Int
            var binsum: Int

            for (y in 0 until h) {
                rinsum = 0
                ginsum = 0
                binsum = 0
                routsum = 0
                goutsum = 0
                boutsum = 0
                rsum = 0
                gsum = 0
                bsum = 0
                for (i in -radius..radius) {
                    p = pix[yi + minOf(wm, maxOf(i, 0))]
                    sir = stack[i + radius]
                    sir[0] = (p and 0xff0000) shr 16
                    sir[1] = (p and 0x00ff00) shr 8
                    sir[2] = p and 0x0000ff
                    rbs = r1 - kotlin.math.abs(i)
                    rsum += sir[0] * rbs
                    gsum += sir[1] * rbs
                    bsum += sir[2] * rbs
                    if (i > 0) {
                        rinsum += sir[0]
                        ginsum += sir[1]
                        binsum += sir[2]
                    } else {
                        routsum += sir[0]
                        goutsum += sir[1]
                        boutsum += sir[2]
                    }
                }
                stackpointer = radius

                for (x in 0 until w) {
                    r[yi] = dv[rsum]
                    g[yi] = dv[gsum]
                    b[yi] = dv[bsum]

                    rsum -= routsum
                    gsum -= goutsum
                    bsum -= boutsum

                    stackstart = stackpointer - radius + div
                    sir = stack[stackstart % div]

                    routsum -= sir[0]
                    goutsum -= sir[1]
                    boutsum -= sir[2]

                    if (y == 0) {
                        vmin[x] = minOf(x + radius + 1, wm)
                    }
                    p = pix[yw + vmin[x]]

                    sir[0] = (p and 0xff0000) shr 16
                    sir[1] = (p and 0x00ff00) shr 8
                    sir[2] = p and 0x0000ff

                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]

                    rsum += rinsum
                    gsum += ginsum
                    bsum += binsum

                    stackpointer = (stackpointer + 1) % div
                    sir = stack[stackpointer % div]

                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]

                    rinsum -= sir[0]
                    ginsum -= sir[1]
                    binsum -= sir[2]

                    yi++
                }
                yw += w
            }
            for (x in 0 until w) {
                rinsum = 0
                ginsum = 0
                binsum = 0
                routsum = 0
                goutsum = 0
                boutsum = 0
                rsum = 0
                gsum = 0
                bsum = 0
                yp = -radius * w
                for (i in -radius..radius) {
                    yi = maxOf(0, yp) + x
                    sir = stack[i + radius]
                    sir[0] = r[yi]
                    sir[1] = g[yi]
                    sir[2] = b[yi]
                    rbs = r1 - kotlin.math.abs(i)
                    rsum += r[yi] * rbs
                    gsum += g[yi] * rbs
                    bsum += b[yi] * rbs
                    if (i > 0) {
                        rinsum += sir[0]
                        ginsum += sir[1]
                        binsum += sir[2]
                    } else {
                        routsum += sir[0]
                        goutsum += sir[1]
                        boutsum += sir[2]
                    }
                    if (i < hm) {
                        yp += w
                    }
                }
                yi = x
                stackpointer = radius
                for (y in 0 until h) {
                    pix[yi] = (-0x1000000 and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                    rsum -= routsum
                    gsum -= goutsum
                    bsum -= boutsum

                    stackstart = stackpointer - radius + div
                    sir = stack[stackstart % div]

                    routsum -= sir[0]
                    goutsum -= sir[1]
                    boutsum -= sir[2]

                    if (x == 0) {
                        vmin[y] = minOf(y + r1, hm) * w
                    }
                    p = x + vmin[y]

                    sir[0] = r[p]
                    sir[1] = g[p]
                    sir[2] = b[p]

                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]

                    rsum += rinsum
                    gsum += ginsum
                    bsum += binsum

                    stackpointer = (stackpointer + 1) % div
                    sir = stack[stackpointer]

                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]

                    rinsum -= sir[0]
                    ginsum -= sir[1]
                    binsum -= sir[2]

                    yi += w
                }
            }
            val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            result.setPixels(pix, 0, w, 0, 0, w, h)
            return result
        }

        @RequiresApi(Build.VERSION_CODES.S)
        private fun blurBitmapApi31(source: Bitmap, radius: Float, downscaleTo: Int): Bitmap? {
            val ratio = source.width.toFloat() / source.height.toFloat()
            val width =
                if (ratio >= 1f) downscaleTo else (downscaleTo * ratio).toInt().coerceAtLeast(1)
            val height =
                if (ratio >= 1f) (downscaleTo / ratio).toInt().coerceAtLeast(1) else downscaleTo
            val small = Bitmap.createScaledBitmap(source, width, height, true)

            var reader: ImageReader? = null
            return try {
                reader = ImageReader.newInstance(
                    small.width,
                    small.height,
                    android.graphics.PixelFormat.RGBA_8888,
                    1,
                    HardwareBuffer.USAGE_GPU_SAMPLED_IMAGE or HardwareBuffer.USAGE_GPU_COLOR_OUTPUT,
                )

                val renderNode = RenderNode("widgetBlur")
                renderNode.setPosition(0, 0, small.width, small.height)
                renderNode.setRenderEffect(
                    RenderEffect.createBlurEffect(radius, radius, Shader.TileMode.CLAMP),
                )

                val canvas = renderNode.beginRecording()
                canvas.drawBitmap(small, 0f, 0f, null)
                renderNode.endRecording()

                val hardwareRenderer = HardwareRenderer()
                hardwareRenderer.setSurface(reader.surface)
                hardwareRenderer.setContentRoot(renderNode)
                hardwareRenderer.createRenderRequest()
                    .setWaitForPresent(true)
                    .syncAndDraw()

                val image = reader.acquireNextImage()
                    ?: return null
                val hardwareBuffer = image.hardwareBuffer
                    ?: return null.also { image.close() }

                val hardwareBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, null)
                hardwareBuffer.close()
                image.close()
                hardwareRenderer.destroy()

                val softwareCopy = hardwareBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                hardwareBitmap?.recycle()
                softwareCopy
            } catch (_: Exception) {
                null
            } finally {
                reader?.close()
                if (small != source) small.recycle()
            }
        }

        /** Extrae el color dominante/vibrante del artwork como fallback liviano del blur. */
        private suspend fun computeDominantColor(bitmap: Bitmap): Int? =
            withContext(Dispatchers.Default) {
                try {
                    val palette = Palette.from(bitmap).generate()
                    palette.vibrantSwatch?.rgb
                        ?: palette.dominantSwatch?.rgb
                        ?: palette.mutedSwatch?.rgb
                } catch (e: Exception) {
                    Timber.tag("PlayerWidgetState").w(e, "Dominant color extraction failed")
                    null
                }
            }

        private suspend fun loadArtworkFromUrl(context: Context, url: String): Bitmap? {
            if (url.isBlank()) return null

            return withContext(Dispatchers.IO) {
                try {
                    val imageLoader = context.imageLoader
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .size(200, 200)
                        .allowHardware(false)
                        .build()

                    val result = imageLoader.execute(request)
                    when (result) {
                        is SuccessResult -> result.image.toBitmap()
                        else -> null
                    }
                } catch (e: Exception) {
                    Timber.tag("PlayerWidgetState").e(e, "Failed to load artwork from URL: $url")
                    null
                }
            }
        }

        fun fromPreferences(preferences: Preferences): PlayerWidgetState {
            val bytes = preferences[PlayerWidgetStateKeys.ArtworkBytes]
            val bitmap = bytes?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }

            val blurBytes = preferences[PlayerWidgetStateKeys.BackgroundBlurBytes]
            val blurBitmap = blurBytes?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }

            return PlayerWidgetState(
                title = preferences[PlayerWidgetStateKeys.Title].orEmpty(),
                artist = preferences[PlayerWidgetStateKeys.Artist].orEmpty(),
                thumbnailUrl = preferences[PlayerWidgetStateKeys.ThumbnailUrl],
                artworkBitmap = bitmap,
                backgroundBlurBitmap = blurBitmap,
                dominantColor = preferences[PlayerWidgetStateKeys.DominantColor],
                isPlaying = preferences[PlayerWidgetStateKeys.IsPlaying] ?: false,
                hasPrevious = preferences[PlayerWidgetStateKeys.HasPrevious] ?: false,
                hasNext = preferences[PlayerWidgetStateKeys.HasNext] ?: false,
                durationMs = preferences[PlayerWidgetStateKeys.DurationMs] ?: 0L,
                positionMs = preferences[PlayerWidgetStateKeys.PositionMs] ?: 0L,
            )
        }
    }
}

object PlayerWidgetStateKeys {
    val Title = stringPreferencesKey("title")
    val Artist = stringPreferencesKey("artist")
    val ThumbnailUrl = stringPreferencesKey("thumbnail_url")
    val IsPlaying = booleanPreferencesKey("is_playing")
    val HasPrevious = booleanPreferencesKey("has_previous")
    val HasNext = booleanPreferencesKey("has_next")
    val DurationMs = longPreferencesKey("duration_ms")
    val PositionMs = longPreferencesKey("position_ms")
    val ArtworkBytes = byteArrayPreferencesKey("artwork_bytes")
    val BackgroundBlurBytes = byteArrayPreferencesKey("background_blur_bytes")
    val DominantColor = intPreferencesKey("dominant_color")
}