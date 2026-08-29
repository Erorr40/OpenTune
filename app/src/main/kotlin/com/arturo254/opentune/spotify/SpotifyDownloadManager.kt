/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.spotify

import android.content.Context
import android.widget.Toast
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.arturo254.opentune.R
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.models.MediaMetadata
import com.arturo254.opentune.playback.DownloadUtil
import com.arturo254.opentune.playback.ExoDownloadService
import com.arturo254.opentune.spotify.models.SpotifyTrack
import com.arturo254.opentune.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class PlaylistDownloadProgress(
    val playlistId: String,
    val isDownloading: Boolean = false,
    val totalCount: Int = 0,
    val processedCount: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
)

@Singleton
class SpotifyDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: MusicDatabase,
    private val downloadUtil: DownloadUtil,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadJobs = ConcurrentHashMap<String, Job>()
    private val _downloadStates = MutableStateFlow<Map<String, PlaylistDownloadProgress>>(emptyMap())
    val downloadStates: StateFlow<Map<String, PlaylistDownloadProgress>> = _downloadStates.asStateFlow()

    fun isPlaylistDownloading(playlistId: String): Boolean {
        return _downloadStates.value[playlistId]?.isDownloading == true
    }

    fun getProgress(playlistId: String): PlaylistDownloadProgress? {
        return _downloadStates.value[playlistId]
    }

    fun downloadAllTracks(
        playlistId: String,
        tracks: List<SpotifyTrack>,
    ) {
        if (tracks.isEmpty() || isPlaylistDownloading(playlistId)) return

        downloadJobs[playlistId]?.cancel()
        val job = scope.launch {
            _downloadStates.update { map ->
                map + (playlistId to PlaylistDownloadProgress(
                    playlistId = playlistId,
                    isDownloading = true,
                    totalCount = tracks.size,
                    processedCount = 0,
                    successCount = 0,
                    failedCount = 0,
                ))
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    context.getString(R.string.downloading),
                    Toast.LENGTH_SHORT
                ).show()
            }

            val semaphore = Semaphore(4)
            var successCount = 0
            var failedCount = 0
            var processedCount = 0

            tracks.forEach { track ->
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
                        downloadUtil.syncOfflineAssets(metadata.id)
                        successCount++
                    } else {
                        failedCount++
                    }
                } catch (e: Exception) {
                    reportException(e)
                    failedCount++
                }

                processedCount++
                val curProcessed = processedCount
                val curSuccess = successCount
                val curFailed = failedCount
                _downloadStates.update { map ->
                    val cur = map[playlistId]
                    if (cur != null) {
                        map + (playlistId to cur.copy(
                            processedCount = curProcessed,
                            successCount = curSuccess,
                            failedCount = curFailed,
                        ))
                    } else map
                }
            }

            _downloadStates.update { map ->
                val cur = map[playlistId]
                if (cur != null) {
                    map + (playlistId to cur.copy(
                        isDownloading = false,
                        processedCount = processedCount,
                        successCount = successCount,
                        failedCount = failedCount,
                    ))
                } else map
            }

            withContext(Dispatchers.Main) {
                val message = "Downloaded $successCount songs" + if (failedCount > 0) " ($failedCount failed)" else ""
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
        downloadJobs[playlistId] = job
    }

    fun cancelDownloads(playlistId: String) {
        downloadJobs[playlistId]?.cancel()
        downloadJobs.remove(playlistId)
        _downloadStates.update { map ->
            val cur = map[playlistId]
            if (cur != null) {
                map + (playlistId to cur.copy(isDownloading = false))
            } else map
        }
    }

    fun downloadSingleTrack(
        track: SpotifyTrack,
        onComplete: ((MediaMetadata?) -> Unit)? = null,
    ) {
        scope.launch {
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
                    downloadUtil.syncOfflineAssets(resolved.id)
                }
                resolved
            }.getOrNull()

            withContext(Dispatchers.Main) {
                onComplete?.invoke(metadata)
            }
        }
    }
}
