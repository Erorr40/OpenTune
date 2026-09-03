/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.arturo254.opentune.playback

import android.content.Context
import android.media.MediaCodecList
import android.net.ConnectivityManager
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import com.arturo254.opentune.innertube.YouTube
import com.arturo254.opentune.innertube.models.YouTubeClient
import com.arturo254.opentune.constants.AudioQuality
import com.arturo254.opentune.constants.AudioQualityKey
import com.arturo254.opentune.constants.PlayerStreamClient
import com.arturo254.opentune.constants.PlayerStreamClientKey
import com.arturo254.opentune.db.MusicDatabase
import com.arturo254.opentune.db.entities.FormatEntity
import com.arturo254.opentune.db.entities.SongEntity
import com.arturo254.opentune.di.DownloadCache
import com.arturo254.opentune.di.PlayerCache
import com.arturo254.opentune.utils.YTPlayerUtils
import com.arturo254.opentune.utils.StreamClientUtils
import com.arturo254.opentune.utils.enumPreference
import com.arturo254.opentune.constants.NetworkMeteredKey
import com.arturo254.opentune.utils.dataStore
import com.arturo254.opentune.utils.get
import com.arturo254.opentune.models.toMediaMetadata
import androidx.media3.exoplayer.offline.DownloadService
import com.arturo254.opentune.constants.AutoDownloadArtworkKey
import com.arturo254.opentune.constants.AutoDownloadLyricsKey
import com.arturo254.opentune.constants.AutoDownloadOnLikeKey
import com.arturo254.opentune.db.entities.LyricsEntity
import com.arturo254.opentune.lyrics.LyricsHelper
import com.arturo254.opentune.utils.reportException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadUtil
@Inject
constructor(
    @ApplicationContext val context: Context,
    val database: MusicDatabase,
    val databaseProvider: DatabaseProvider,
    @DownloadCache val downloadCache: Cache,
    @PlayerCache val playerCache: Cache,
    val lyricsHelper: LyricsHelper,
) {
    private val connectivityManager = context.getSystemService<ConnectivityManager>()!!
    private val audioQuality by enumPreference(context, AudioQualityKey, AudioQuality.AUTO)
    private val preferredStreamClient by enumPreference(context, PlayerStreamClientKey, PlayerStreamClient.ANDROID_VR)
    private val songUrlCache = ConcurrentHashMap<String, Pair<String, Long>>()
    private val avoidStreamCodecs: Set<String> by lazy {
        if (deviceSupportsMimeType("audio/opus")) emptySet() else setOf("opus")
    }
    private val mediaOkHttpClient: OkHttpClient by lazy {
        val dispatcher = Dispatcher().apply {
            maxRequests = 64
            maxRequestsPerHost = 24
        }
        val pool = ConnectionPool(16, 5, TimeUnit.MINUTES)

        OkHttpClient
            .Builder()
            .proxy(YouTube.streamProxy)
            .dispatcher(dispatcher)
            .connectionPool(pool)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request()
                val host = request.url.host
                val isYouTubeMediaHost =
                    host.endsWith("googlevideo.com") ||
                        host.endsWith("googleusercontent.com") ||
                        host.endsWith("youtube.com") ||
                        host.endsWith("youtube-nocookie.com") ||
                        host.endsWith("ytimg.com")

                if (!isYouTubeMediaHost) return@addInterceptor chain.proceed(request)

                val clientParam = request.url.queryParameter("c")?.trim().orEmpty()

                val userAgent = StreamClientUtils.resolveUserAgent(clientParam)
                val originReferer = StreamClientUtils.resolveOriginReferer(clientParam)

                val builder = request.newBuilder().header("User-Agent", userAgent)
                originReferer.origin?.let { builder.header("Origin", it) }
                originReferer.referer?.let { builder.header("Referer", it) }

                chain.proceed(builder.build())
            }.build()
    }

    val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())
    private val downloadRetryCount = ConcurrentHashMap<String, Int>()
    private val MAX_DOWNLOAD_RETRIES = 4

    private val dataSourceFactory =
        ResolvingDataSource.Factory(
            CacheDataSource
                .Factory()
                .setCache(playerCache)
                .setUpstreamDataSourceFactory(
                    OkHttpDataSource.Factory(
                        mediaOkHttpClient,
                    ),
                ),
        ) { dataSpec ->
            val mediaId = dataSpec.key ?: error("No media id")
            val length = if (dataSpec.length >= 0) dataSpec.length else 1
            if (playerCache.isCached(mediaId, dataSpec.position, length)) {
                return@Factory dataSpec
            }
            songUrlCache[mediaId]?.takeIf { it.second > System.currentTimeMillis() + 60_000L }?.let {
                return@Factory dataSpec.withUri(it.first.toUri())
            }
            var playbackDataResult: Result<YTPlayerUtils.PlaybackData>? = null
            for (attempt in 0..2) {
                playbackDataResult = runBlocking(Dispatchers.IO) {
                    val networkMeteredPref = context.dataStore.get(NetworkMeteredKey, true)
                    YTPlayerUtils.playerResponseForPlayback(
                        mediaId,
                        audioQuality = audioQuality,
                        preferredStreamClient = preferredStreamClient,
                        connectivityManager = connectivityManager,
                        networkMetered = networkMeteredPref,
                        avoidCodecs = avoidStreamCodecs,
                    )
                }
                if (playbackDataResult.isSuccess) break
                if (attempt < 2) {
                    Thread.sleep(600L * (attempt + 1))
                }
            }
            val playbackData = playbackDataResult?.getOrThrow() ?: error("No playback data for $mediaId")
            val format = playbackData.format

            database.query {
                upsert(
                    FormatEntity(
                        id = mediaId,
                        itag = format.itag,
                        mimeType = format.mimeType.split(";")[0],
                        codecs = format.mimeType.split("codecs=").getOrNull(1)?.removeSurrounding("\"").orEmpty(),
                        bitrate = format.bitrate,
                        sampleRate = format.audioSampleRate,
                        contentLength = format.contentLength ?: 0L,
                        loudnessDb = playbackData.audioConfig?.loudnessDb,
                        perceptualLoudnessDb = playbackData.audioConfig?.perceptualLoudnessDb,
                        playbackUrl = playbackData.playbackTracking?.videostatsPlaybackUrl?.baseUrl
                    ),
                )

                val now = LocalDateTime.now()
                val existing = getSongByIdBlocking(mediaId)?.song

                val updatedSong = if (existing != null) {
                    if (existing.dateDownload == null) existing.copy(dateDownload = now) else existing
                } else {
                    SongEntity(
                        id = mediaId,
                        title = playbackData.videoDetails?.title ?: "Unknown",
                        duration = playbackData.videoDetails?.lengthSeconds?.toIntOrNull() ?: 0,
                        thumbnailUrl = playbackData.videoDetails?.thumbnail?.thumbnails?.lastOrNull()?.url,
                        dateDownload = now
                    )
                }

                upsert(updatedSong)
            }

            val streamUrl = playbackData.streamUrl

            songUrlCache[mediaId] = streamUrl to (System.currentTimeMillis() + (playbackData.streamExpiresInSeconds * 1000L))
            dataSpec.withUri(streamUrl.toUri())
        }

    val downloadNotificationHelper =
        DownloadNotificationHelper(context, ExoDownloadService.CHANNEL_ID)

    val downloadManager: DownloadManager =
        DownloadManager(
            context,
            databaseProvider,
            downloadCache,
            dataSourceFactory,
            Executor(Runnable::run)
        ).apply {
            maxParallelDownloads = 6
            addListener(
                object : DownloadManager.Listener {
                    override fun onDownloadChanged(
                        downloadManager: DownloadManager,
                        download: Download,
                        finalException: Exception?,
                    ) {
                        downloads.update { map ->
                            map.toMutableMap().apply {
                                set(download.request.id, download)
                            }
                        }
                        when (download.state) {
                            Download.STATE_COMPLETED -> {
                                downloadRetryCount.remove(download.request.id)
                                syncOfflineAssets(download.request.id)
                            }
                            Download.STATE_DOWNLOADING -> {
                                syncOfflineAssets(download.request.id)
                            }
                            Download.STATE_FAILED -> {
                                songUrlCache.remove(download.request.id)
                                CoroutineScope(Dispatchers.IO).launch {
                                    val retries = downloadRetryCount.getOrDefault(download.request.id, 0)
                                    val delayMs = ((retries + 1) * 2000L).coerceAtMost(30_000L)
                                    downloadRetryCount[download.request.id] = retries + 1
                                    delay(delayMs)
                                    try {
                                        DownloadService.sendAddDownload(
                                            context,
                                            ExoDownloadService::class.java,
                                            download.request,
                                            false,
                                        )
                                    } catch (e: Exception) {
                                        reportException(e)
                                    }
                                }
                            }
                            Download.STATE_REMOVING -> {
                                downloadRetryCount.remove(download.request.id)
                                songUrlCache.remove(download.request.id)
                            }
                        }
                    }

                    override fun onDownloadRemoved(
                        downloadManager: DownloadManager,
                        download: Download,
                    ) {
                        downloads.update { map ->
                            map.toMutableMap().apply {
                                remove(download.request.id)
                            }
                        }
                        downloadRetryCount.remove(download.request.id)
                        songUrlCache.remove(download.request.id)
                    }
                }
            )
        }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val result = mutableMapOf<String, Download>()
            downloadManager.downloadIndex.getDownloads().use { cursor ->
                while (cursor.moveToNext()) {
                    result[cursor.download.request.id] = cursor.download
                }
            }
            downloads.value = result
            retryFailedDownloads()
            syncLikedSongsDownloads()

            // Periodic auto-retry worker for any failed downloads
            while (isActive) {
                delay(30_000L)
                val failed = downloads.value.values.filter { it.state == Download.STATE_FAILED }
                if (failed.isNotEmpty()) {
                    failed.forEach { dl ->
                        try {
                            songUrlCache.remove(dl.request.id)
                            DownloadService.sendAddDownload(
                                context,
                                ExoDownloadService::class.java,
                                dl.request,
                                false,
                            )
                        } catch (e: Exception) {
                            reportException(e)
                        }
                    }
                }
            }
        }
    }

    fun retryFailedDownloads() {
        CoroutineScope(Dispatchers.IO).launch {
            val failed = downloads.value.values.filter { it.state == Download.STATE_FAILED }
            failed.forEach { dl ->
                try {
                    songUrlCache.remove(dl.request.id)
                    DownloadService.sendAddDownload(
                        context,
                        ExoDownloadService::class.java,
                        dl.request,
                        false,
                    )
                } catch (e: Exception) {
                    reportException(e)
                }
            }
        }
    }

    fun syncLikedSongsDownloads() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val autoDownloadOnLike = context.dataStore.data.map { it[AutoDownloadOnLikeKey] ?: true }.first()
                if (!autoDownloadOnLike) return@launch

                val likedSongs = database.likedSongs(com.arturo254.opentune.constants.SongSortType.CREATE_DATE, true).firstOrNull() ?: emptyList()
                val currentDownloads = downloads.value

                likedSongs.forEach { song ->
                    val dl = currentDownloads[song.id]
                    if (dl == null || (dl.state != Download.STATE_COMPLETED && dl.state != Download.STATE_DOWNLOADING && dl.state != Download.STATE_QUEUED)) {
                        autoDownloadSong(song.id, song.song.title)
                    }
                }
            } catch (e: Exception) {
                reportException(e)
            }
        }
    }

    fun getDownload(songId: String): Flow<Download?> = downloads.map { it[songId] }

    fun syncOfflineAssets(songId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val song = database.song(songId).firstOrNull()
                if (song != null) {
                    val autoLyrics = context.dataStore.data.map { it[AutoDownloadLyricsKey] ?: true }.first()
                    if (autoLyrics) {
                        val existingLyrics = database.lyrics(songId).firstOrNull()
                        if (existingLyrics == null) {
                            try {
                                val mediaMetadata = song.toMediaMetadata()
                                val lyrics = lyricsHelper.getLyrics(mediaMetadata)
                                if (lyrics.isNotBlank() && lyrics != LyricsEntity.LYRICS_NOT_FOUND) {
                                    database.query {
                                        upsert(LyricsEntity(id = songId, lyrics = lyrics))
                                    }
                                }
                            } catch (e: Exception) {
                                reportException(e)
                            }
                        }
                    }

                    val autoArtwork = context.dataStore.data.map { it[AutoDownloadArtworkKey] ?: true }.first()
                    if (autoArtwork) {
                        song.song.thumbnailUrl?.let { url ->
                            try {
                                val request = coil3.request.ImageRequest.Builder(context)
                                    .data(url)
                                    .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                                    .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                                    .build()
                                coil3.SingletonImageLoader.get(context).enqueue(request)
                            } catch (_: Exception) {}
                        }
                        song.artists.forEach { artist ->
                            artist.thumbnailUrl?.let { url ->
                                try {
                                    val request = coil3.request.ImageRequest.Builder(context)
                                        .data(url)
                                        .memoryCachePolicy(coil3.request.CachePolicy.ENABLED)
                                        .diskCachePolicy(coil3.request.CachePolicy.ENABLED)
                                        .build()
                                    coil3.SingletonImageLoader.get(context).enqueue(request)
                                } catch (_: Exception) {}
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                reportException(e)
            }
        }
    }

    fun autoDownloadSong(songId: String, title: String) {
        val downloadRequest =
            androidx.media3.exoplayer.offline.DownloadRequest.Builder(songId, songId.toUri())
                .setCustomCacheKey(songId)
                .setData(title.toByteArray())
                .build()
        androidx.media3.exoplayer.offline.DownloadService.sendAddDownload(
            context,
            ExoDownloadService::class.java,
            downloadRequest,
            false,
        )
        syncOfflineAssets(songId)
    }

    private fun deviceSupportsMimeType(mimeType: String): Boolean {
        return runCatching {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            codecList.codecInfos.any { info ->
                !info.isEncoder && info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }
            }
        }.getOrDefault(false)
    }
}
