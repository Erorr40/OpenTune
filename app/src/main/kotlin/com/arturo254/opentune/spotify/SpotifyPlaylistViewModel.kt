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
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import javax.inject.Inject

@HiltViewModel
class SpotifyPlaylistViewModel
@Inject
constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SpotifyLibraryRepository,
) : ViewModel() {
    private val playlistId: String = savedStateHandle.get<String>("playlistId").orEmpty()

    private val _uiState = MutableStateFlow(SpotifyPlaylistUiState(isLoading = true))
    val uiState: StateFlow<SpotifyPlaylistUiState> = _uiState.asStateFlow()

    private var downloadJob: Job? = null

    init {
        reload()
    }

    fun reload() {
        if (playlistId.isBlank()) {
            _uiState.value = SpotifyPlaylistUiState(errorMessage = "Missing Spotify playlist")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val playlist = repository.playlist(playlistId)
                val tracks = repository.playlistTracks(playlistId)
                _uiState.value =
                    SpotifyPlaylistUiState(
                        playlist = playlist,
                        tracks = tracks,
                        isLoading = false,
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

    fun downloadAllTracks(
        context: Context,
        database: MusicDatabase,
        onProgress: ((current: Int, total: Int) -> Unit)? = null,
        onComplete: ((successCount: Int, failedCount: Int) -> Unit)? = null,
    ) {
        val currentTracks = _uiState.value.tracks
        if (currentTracks.isEmpty() || _uiState.value.isDownloading) return

        downloadJob?.cancel()
        downloadJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    isDownloading = true,
                    downloadedCount = 0,
                    totalDownloadCount = currentTracks.size,
                )
            }

            val semaphore = Semaphore(3)
            var successCount = 0
            var failedCount = 0
            var processedCount = 0

            currentTracks.forEach { track ->
                try {
                    val metadata = semaphore.withPermit {
                        SpotifyPlaybackResolver.resolveToMetadata(track)
                    }

                    if (metadata != null) {
                        database.transaction {
                            insert(metadata)
                        }

                        val downloadRequest =
                            DownloadRequest.Builder(metadata.id, metadata.id.toUri())
                                .setCustomCacheKey(metadata.id)
                                .setData(metadata.title.toByteArray())
                                .build()

                        DownloadService.sendAddDownload(
                            context,
                            ExoDownloadService::class.java,
                            downloadRequest,
                            false,
                        )
                        successCount++
                    } else {
                        failedCount++
                    }
                } catch (e: Exception) {
                    reportException(e)
                    failedCount++
                }

                processedCount++
                val currentProcessed = processedCount
                _uiState.update {
                    it.copy(downloadedCount = currentProcessed)
                }
                withContext(Dispatchers.Main) {
                    onProgress?.invoke(currentProcessed, currentTracks.size)
                }
            }

            _uiState.update {
                it.copy(
                    isDownloading = false,
                    downloadedCount = successCount,
                )
            }
            withContext(Dispatchers.Main) {
                onComplete?.invoke(successCount, failedCount)
            }
        }
    }

    fun downloadSingleTrack(
        context: Context,
        database: MusicDatabase,
        track: SpotifyTrack,
        onComplete: ((MediaMetadata?) -> Unit)? = null,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val metadata = runCatching {
                val resolved = SpotifyPlaybackResolver.resolveToMetadata(track)
                if (resolved != null) {
                    database.transaction {
                        insert(resolved)
                    }
                    val downloadRequest =
                        DownloadRequest.Builder(resolved.id, resolved.id.toUri())
                            .setCustomCacheKey(resolved.id)
                            .setData(resolved.title.toByteArray())
                            .build()

                    DownloadService.sendAddDownload(
                        context,
                        ExoDownloadService::class.java,
                        downloadRequest,
                        false,
                    )
                }
                resolved
            }.getOrNull()

            withContext(Dispatchers.Main) {
                onComplete?.invoke(metadata)
            }
        }
    }

    fun cancelDownloads() {
        downloadJob?.cancel()
        _uiState.update { it.copy(isDownloading = false) }
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
