/*
 * ArchiveTune (2026)
 * © Rukamori — github.com/rukamori
 * GPL-3.0 License | Contributors: see git history
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.arturo254.opentune.spotify

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.playback.ExoDownloadService
import com.arturo254.opentune.spotify.models.SpotifyPlaylist
import com.arturo254.opentune.spotify.models.SpotifyTrack
import com.arturo254.opentune.utils.reportException
import javax.inject.Inject

@HiltViewModel
class SpotifyPlaylistViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SpotifyLibraryRepository,
    private val downloadManager: SpotifyDownloadManager,
) : ViewModel() {
    val playlistId: String = savedStateHandle.get<String>("playlistId").orEmpty()

    private val _uiState = MutableStateFlow(SpotifyPlaylistUiState(isLoading = true))
    val uiState: StateFlow<SpotifyPlaylistUiState> = _uiState.asStateFlow()

    init {
        reload()
        viewModelScope.launch {
            downloadManager.downloadStates.collect { states ->
                val progress = states[playlistId]
                if (progress != null) {
                    _uiState.update {
                        it.copy(
                            isDownloading = progress.isDownloading,
                            downloadedCount = progress.processedCount,
                            totalDownloadCount = progress.totalCount,
                        )
                    }
                }
            }
        }
    }

    fun reload() {
        if (playlistId.isBlank()) {
            _uiState.value = SpotifyPlaylistUiState(errorMessage = "Missing Spotify playlist")
            return
        }

        viewModelScope.launch {
            _uiState.value = SpotifyPlaylistUiState(isLoading = true)
            try {
                val playlist = repository.playlist(playlistId)
                val tracks = repository.playlistTracks(playlistId)
                _uiState.value =
                    SpotifyPlaylistUiState(
                        isLoading = false,
                        playlist = playlist,
                        tracks = tracks,
                    )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                reportException(error)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message,
                    )
                }
            }
        }
    }

    fun downloadAllTracks() {
        val currentTracks = _uiState.value.tracks
        if (currentTracks.isEmpty()) return
        downloadManager.downloadAllTracks(playlistId, currentTracks)
    }

    fun downloadSingleTrack(
        track: SpotifyTrack,
        onComplete: ((MediaMetadata?) -> Unit)? = null,
    ) {
        downloadManager.downloadSingleTrack(track, onComplete)
    }

    fun cancelDownloads() {
        downloadManager.cancelDownloads(playlistId)
    }
}

@Immutable
data class SpotifyPlaylistUiState(
    val playlist: SpotifyPlaylist? = null,
    val tracks: List<SpotifyTrack> = emptyList(),
    val isLoading: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadedCount: Int = 0,
    val totalDownloadCount: Int = 0,
    val errorMessage: String? = null,
)
