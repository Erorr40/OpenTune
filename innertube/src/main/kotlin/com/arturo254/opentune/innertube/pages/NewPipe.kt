/*
 * OpenTune Project Original (2026)
 * Arturo254 (github.com/Arturo254)
 * Licensed Under GPL-3.0 | see git history for contributors
 */

package com.arturo254.opentune.innertube

import com.arturo254.opentune.innertube.PlaybackAuthState
import com.arturo254.opentune.innertube.models.YouTubeClient
import com.arturo254.opentune.innertube.models.response.PlayerResponse
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ParsingException
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.net.Proxy
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

private class NewPipeDownloaderImpl(proxy: Proxy?) : Downloader() {

    private val client = OkHttpClient.Builder()
        .proxy(proxy)
        .retryOnConnectionFailure(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = okhttp3.Request.Builder()
            .method(httpMethod, dataToSend?.toRequestBody())
            .url(url)

        var hasUserAgent = false
        headers.forEach { (headerName, headerValueList) ->
            if (headerName.equals("User-Agent", ignoreCase = true) && headerValueList.isNotEmpty()) {
                hasUserAgent = true
            }
            if (headerValueList.size > 1) {
                requestBuilder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    requestBuilder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                requestBuilder.header(headerName, headerValueList[0])
            }
        }

        if (!hasUserAgent) {
            requestBuilder.header("User-Agent", YouTubeClient.USER_AGENT_WEB)
        }

        val response = client.newCall(requestBuilder.build()).execute()

        if (response.code == 429) {
            response.close()

            throw ReCaptchaException("reCaptcha Challenge requested", url)
        }

        val responseBodyToReturn = response.body.string()

        val latestUrl = response.request.url.toString()
        return Response(response.code, response.message, response.headers.toMultimap(), responseBodyToReturn, latestUrl)
    }

}

object NewPipeUtils {

    private val logger: Logger = Logger.getLogger("NewPipeUtils")

    init {
        NewPipe.init(NewPipeDownloaderImpl(YouTube.proxy))
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> = runCatching {
        0
    }

    fun getStreamUrl(
        format: PlayerResponse.StreamingData.Format,
        videoId: String,
        client: YouTubeClient? = null,
        authState: PlaybackAuthState = YouTube.currentPlaybackAuthState(),
    ): Result<String> =
        runCatching {
            val directUrl = format.url
            if (directUrl != null) {
                logger.fine(
                    "getStreamUrl direct | videoId=$videoId, itag=${format.itag}, " +
                        "mimeType=${format.mimeType}, hasNParam=${
                            directUrl.toHttpUrlOrNull()?.queryParameter("n")?.isNotBlank() == true
                        }"
                )
                return@runCatching YouTube.appendGvsPoToken(
                    url = directUrl,
                    client = client,
                    authState = authState,
                )
            }

            val cipherString = format.signatureCipher ?: format.cipher
            if (cipherString == null) {
                logger.warning("getStreamUrl | videoId=$videoId, itag=${format.itag} -> no url/cipher")
                throw ParsingException("Could not find format url")
            }

            logger.warning(
                "getStreamUrl cipher bypass | videoId=$videoId, itag=${format.itag}, " +
                    "mimeType=${format.mimeType} -> Skipping NewPipe JS deobfuscation (incompatible signatures " +
                    "from v0.25.2). Returning failure to trigger next client fallback."
            )
            throw ParsingException(
                "Ciphered streams require NewPipe JS deobfuscation which is unavailable for this " +
                    "extractor version; use fallback client with direct URLs instead."
            )
        }

}
