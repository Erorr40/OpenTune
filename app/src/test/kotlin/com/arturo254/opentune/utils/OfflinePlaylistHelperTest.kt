/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.utils

import com.arturo254.opentune.db.entities.Playlist
import com.arturo254.opentune.db.entities.PlaylistEntity
import com.arturo254.opentune.db.entities.PlaylistSongEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflinePlaylistHelperTest {

    private fun makePlaylist(
        id: String,
        name: String,
        isEditable: Boolean = false,
        browseId: String? = "VL$id",
        songCount: Int = 0,
        remoteSongCount: Int? = 10,
    ) = Playlist(
        playlist = PlaylistEntity(
            id = id,
            name = name,
            browseId = browseId,
            isEditable = isEditable,
            remoteSongCount = remoteSongCount,
        ),
        songCount = songCount,
        songThumbnails = emptyList(),
    )

    @Test
    fun resolveDownloadedPlaylistsByIds_filtersToPlaylistsWithCompletedDownloads() {
        val p1 = makePlaylist("pl_1", "Roadtrip", songCount = 3)
        val p2 = makePlaylist("pl_2", "Online Only", songCount = 5)
        val p3 = makePlaylist("pl_3", "Focus Mix", songCount = 2)

        val entries = listOf(
            PlaylistSongEntry("pl_1", "s1"),
            PlaylistSongEntry("pl_1", "s2"),
            PlaylistSongEntry("pl_1", "s2"), // duplicate guard
            PlaylistSongEntry("pl_1", "s3"),
            PlaylistSongEntry("pl_2", "s9"),
            PlaylistSongEntry("pl_3", "s4"),
        )

        val result = OfflinePlaylistHelper.resolveDownloadedPlaylistsByIds(
            playlists = listOf(p1, p2, p3),
            playlistSongEntries = entries,
            completedSongIds = setOf("s1", "s2", "s4"),
        )

        assertEquals(2, result.size)
        assertEquals("pl_1", result[0].playlist.id)
        assertEquals(2, result[0].downloadedSongCount)
        assertEquals("pl_3", result[1].playlist.id)
        assertEquals(1, result[1].downloadedSongCount)
    }

    @Test
    fun resolveDownloadedPlaylistsByIds_returnsEmptyWhenNoCompletedDownloads() {
        val result = OfflinePlaylistHelper.resolveDownloadedPlaylistsByIds(
            playlists = listOf(makePlaylist("pl_1", "Empty")),
            playlistSongEntries = listOf(PlaylistSongEntry("pl_1", "s1")),
            completedSongIds = emptySet(),
        )
        assertTrue(result.isEmpty())
    }

    @Test
    fun shouldOpenLocally_forcesLocalRouteWhenOffline() {
        val remoteOnly = makePlaylist("pl_remote", "Remote", isEditable = false, songCount = 0, remoteSongCount = 25)
        val cachedRemote = makePlaylist("pl_cached", "Cached", isEditable = false, songCount = 10, remoteSongCount = 25)

        assertFalse(OfflinePlaylistHelper.shouldOpenLocally(isOffline = false, playlist = remoteOnly))
        assertTrue(OfflinePlaylistHelper.shouldOpenLocally(isOffline = true, playlist = remoteOnly))
        assertTrue(OfflinePlaylistHelper.shouldOpenLocally(isOffline = false, playlist = cachedRemote))
    }
}
