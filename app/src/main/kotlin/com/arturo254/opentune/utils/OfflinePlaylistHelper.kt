/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.utils

import androidx.compose.runtime.Immutable
import androidx.media3.exoplayer.offline.Download
import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.db.entities.PlaylistSongEntry

@Immutable
data class DownloadedPlaylistItem(
    val playlist: Playlist,
    val downloadedSongCount: Int,
)

object OfflinePlaylistHelper {

    fun resolveDownloadedPlaylists(
        playlists: List<Playlist>,
        playlistSongEntries: List<PlaylistSongEntry>,
        downloads: Map<String, Download>,
    ): List<DownloadedPlaylistItem> {
        val completedSongIds = downloads.entries
            .asSequence()
            .filter { it.value.state == Download.STATE_COMPLETED }
            .map { it.key }
            .toHashSet()
        return resolveDownloadedPlaylistsByIds(playlists, playlistSongEntries, completedSongIds)
    }

    fun resolveDownloadedPlaylistsByIds(
        playlists: List<Playlist>,
        playlistSongEntries: List<PlaylistSongEntry>,
        completedSongIds: Set<String>,
    ): List<DownloadedPlaylistItem> {
        if (playlists.isEmpty() || playlistSongEntries.isEmpty() || completedSongIds.isEmpty()) {
            return emptyList()
        }

        val downloadedCountsByPlaylist = HashMap<String, MutableSet<String>>()
        for (entry in playlistSongEntries) {
            if (entry.songId in completedSongIds) {
                downloadedCountsByPlaylist
                    .getOrPut(entry.playlistId) { HashSet() }
                    .add(entry.songId)
            }
        }

        return playlists
            .asSequence()
            .mapNotNull { playlist ->
                val count = downloadedCountsByPlaylist[playlist.id]?.size ?: 0
                if (count > 0) {
                    DownloadedPlaylistItem(
                        playlist = playlist,
                        downloadedSongCount = count,
                    )
                } else {
                    null
                }
            }
            .sortedByDescending { it.downloadedSongCount }
            .toList()
    }

    fun shouldOpenLocally(isOffline: Boolean, playlist: Playlist): Boolean {
        if (isOffline) return true
        return playlist.playlist.isEditable ||
            playlist.songCount > 0 ||
            playlist.playlist.remoteSongCount == 0
    }
}
