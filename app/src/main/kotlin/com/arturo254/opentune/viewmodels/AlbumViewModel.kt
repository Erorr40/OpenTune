/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.AlbumItem
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.utils.reportException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

sealed interface AlbumUiState {
    data object Loading : AlbumUiState

    data object Content : AlbumUiState

    data object Empty : AlbumUiState

    data class Error(
        val isNotFound: Boolean = false,
    ) : AlbumUiState
}

private sealed interface FetchState {
    data object Pending : FetchState
    data object Success : FetchState
    data class Failed(val isNotFound: Boolean = false) : FetchState
}

@HiltViewModel
class AlbumViewModel
@Inject
constructor(
    private val database: MusicDatabase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    val albumId = savedStateHandle.get<String>("albumId")!!
    val playlistId = MutableStateFlow("")
    val albumWithSongs =
        database
            .albumWithSongs(albumId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    var otherVersions = MutableStateFlow<List<AlbumItem>>(emptyList())

    private val _fetchState = MutableStateFlow<FetchState>(FetchState.Pending)

    val uiState: StateFlow<AlbumUiState> =
        combine(albumWithSongs, _fetchState) { data, fetch ->
            val songsCount = data?.songs?.size ?: -1
            val albumExists = data != null
            val hasSongs = data?.songs?.isNotEmpty() == true
            val result = when {
                hasSongs -> AlbumUiState.Content
                fetch is FetchState.Pending -> AlbumUiState.Loading
                fetch is FetchState.Success -> if (hasSongs) AlbumUiState.Content else AlbumUiState.Empty
                fetch is FetchState.Failed -> if (hasSongs) AlbumUiState.Content else AlbumUiState.Error(fetch.isNotFound)
                else -> AlbumUiState.Loading
            }
            Timber.d(
                "AlbumViewModel: uiState update | albumId=$albumId | " +
                    "albumExists=$albumExists | songsCount=$songsCount | " +
                    "fetch=$fetch | => result=$result"
            )
            result
        }.stateIn(viewModelScope, SharingStarted.Eagerly, AlbumUiState.Loading)

    init {
        Timber.d("AlbumViewModel: init for albumId=$albumId")
        retry()
    }

    fun retry() {
        viewModelScope.launch {
            Timber.d("AlbumViewModel: retry() called for albumId=$albumId")
            _fetchState.value = FetchState.Pending
            val album = database.album(albumId).first()
            Timber.d("AlbumViewModel: local album exists=${album != null} for albumId=$albumId")

            YouTube
                .album(albumId)
                .onSuccess { albumPage ->
                    Timber.d(
                        "AlbumViewModel: YouTube fetch SUCCESS for albumId=$albumId | " +
                            "title='${albumPage.album.title}' | " +
                            "songsFetched=${albumPage.songs.size} | " +
                            "otherVersions=${albumPage.otherVersions.size}"
                    )
                    playlistId.value = albumPage.album.playlistId
                    otherVersions.value = albumPage.otherVersions

                    database.withTransaction {
                        if (album == null) {
                            Timber.d("AlbumViewModel: inserting NEW album into DB: $albumId")
                            insert(albumPage)
                        } else {
                            Timber.d("AlbumViewModel: UPDATING existing album in DB: $albumId")
                            update(album.album, albumPage, album.artists)
                        }
                    }

                    val dbAlbumAfter = database.albumWithSongs(albumId).first()
                    Timber.d(
                        "AlbumViewModel: DB after transaction | albumId=$albumId | " +
                            "songsInDB=${dbAlbumAfter?.songs?.size ?: "null"}"
                    )
                    _fetchState.value = FetchState.Success
                }.onFailure { error ->
                    Timber.e(
                        error,
                        "AlbumViewModel: YouTube fetch FAILED for albumId=$albumId | " +
                            "message=${error.message}"
                    )
                    reportException(error)
                    val isNotFound = error.message?.contains("NOT_FOUND") == true
                    if (isNotFound) {
                        database.query {
                            album?.album?.let(::delete)
                        }
                    }
                    _fetchState.value = FetchState.Failed(isNotFound = isNotFound)
                }
        }
    }
}
