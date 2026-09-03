/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.arturo254.opentune.R
import com.arturo254.opentune.widget.PlayerWidgetActions.openAppIntent

class OpenTuneCompactWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = OpenTuneCompactWidget()
}

class OpenTuneCompactWidget : GlanceAppWidget() {
    override val stateDefinition = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val uiPrefs = readWidgetUiPrefs(context)
        provideContent {
            val prefs = currentState<Preferences>()
            val state = PlayerWidgetState.fromPreferences(prefs)
            GlanceTheme(colors = OpenTuneWidgetColors) {
                CompactWidgetContent(state = state, uiPrefs = uiPrefs)
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@Composable
private fun CompactWidgetContent(state: PlayerWidgetState, uiPrefs: WidgetUiPrefs) {
    val context = LocalContext.current
    val title = state.title.ifBlank { context.getString(R.string.app_name) }
    val artist = state.artist.ifBlank { "OpenTune" }

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(uiPrefs.cornerRadius)
            .clickable(actionStartActivity(openAppIntent(context))),
    ) {
        WidgetBackground(state = state, mode = uiPrefs.backgroundMode)

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(Color.Black.copy(alpha = uiPrefs.scrimOpacity))),
        ) {}

        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ArtworkBox(bitmap = state.artworkBitmap)
            Spacer(modifier = GlanceModifier.width(10.dp))
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = title,
                    maxLines = 1,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Text(
                    text = artist,
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(Color.White.copy(alpha = 0.78f))),
                )
            }
            Spacer(modifier = GlanceModifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                ControlButton(
                    icon = R.drawable.skip_previous,
                    contentDescription = "Previous",
                    enabled = state.hasPrevious,
                    action = actionRunCallback<PreviousWidgetAction>(),
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                PlayPauseButton(isPlaying = state.isPlaying)
                Spacer(modifier = GlanceModifier.width(8.dp))
                ControlButton(
                    icon = R.drawable.skip_next,
                    contentDescription = "Next",
                    enabled = state.hasNext,
                    action = actionRunCallback<NextWidgetAction>(),
                )
            }
        }
    }
}
