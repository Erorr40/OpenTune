package com.arturo254.opentune.ui.component

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.arturo254.opentune.ui.theme.FrostedGlassTokens
import com.arturo254.opentune.ui.theme.LocalFrostedGlass
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import kotlin.math.sign
import com.kyant.backdrop.backdrops.layerBackdrop as nativeBackdrop

typealias PlatformBackdrop = LayerBackdrop

val LocalBackdrop = staticCompositionLocalOf<PlatformBackdrop?> { null }

@Composable
fun rememberBackdrop(): PlatformBackdrop = rememberLayerBackdrop {
    drawRect(Color.Black)
    drawContent()
}

fun Modifier.layerBackdrop(backdrop: PlatformBackdrop): Modifier = this.nativeBackdrop(backdrop)

fun Modifier.drawBackdropCustomShape(
    backdrop: PlatformBackdrop,
    layer: GraphicsLayer? = null,
    luminanceAnimation: Float = 0.3f,
    shape: Shape,
    disableBlur: Boolean = false,
    fallbackColor: Color = Color(0xFF121212),
    surfaceAlpha: Float = 0.82f,
    borderStroke: BorderStroke? = BorderStroke(
        1.dp,
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.05f)
            )
        )
    ),
): Modifier {
    val base = this.drawBackdrop(
        backdrop = backdrop,
        effects = {
            val l = (luminanceAnimation * 2f - 1f).let { sign(it) * it * it }
            vibrancy()
            colorControls(
                brightness =
                    if (l > 0f) {
                        lerp(0.1f, 0.5f, l)
                    } else {
                        lerp(0.1f, -0.2f, -l)
                    },
                contrast =
                    if (l > 0f) {
                        lerp(1f, 0f, l)
                    } else {
                        1f
                    },
                saturation = 1.5f,
            )
            blur(
                if (l > 0f) {
                    lerp(8f.dp.toPx(), 16f.dp.toPx(), l)
                } else {
                    lerp(8f.dp.toPx(), 2f.dp.toPx(), -l)
                },
            )
            if (shape is CornerBasedShape) {
                runCatching {
                    lens(24f.dp.toPx(), size.minDimension / 2f, true)
                }
            }
        },
        onDrawBackdrop = { drawBackdrop ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !disableBlur) {
                drawBackdrop()
            }
        },
        shape = { shape },
        onDrawSurface = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !disableBlur) {
                drawRect(Color.Black.copy(alpha = 0.18f))
            } else {
                // Graceful fallback for Android 11 and below or when blur is disabled
                drawRect(fallbackColor.copy(alpha = surfaceAlpha))
            }
        }
    )
    return if (borderStroke != null) {
        base.border(borderStroke, shape)
    } else {
        base
    }
}

@Composable
fun Modifier.frostedGlassSurface(
    shape: Shape = RectangleShape,
    luminanceAnimation: Float = 0.3f,
    customFallbackColor: Color? = null,
): Modifier {
    val frostedGlass = LocalFrostedGlass.current
    if (!frostedGlass.isEnabled) return this

    val backdrop = LocalBackdrop.current
    val fallback = customFallbackColor ?: frostedGlass.fallbackSurfaceColor

    return if (frostedGlass.isBlurSupported && backdrop != null) {
        this.drawBackdropCustomShape(
            backdrop = backdrop,
            luminanceAnimation = luminanceAnimation,
            shape = shape,
            disableBlur = !frostedGlass.isBlurSupported,
            fallbackColor = fallback,
            surfaceAlpha = frostedGlass.surfaceAlpha,
            borderStroke = frostedGlass.borderStroke,
        )
    } else {
        this
            .clip(shape)
            .background(fallback.copy(alpha = frostedGlass.surfaceAlpha))
            .border(frostedGlass.borderStroke, shape)
    }
}
