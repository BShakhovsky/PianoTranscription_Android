package ru.bshakhovsky.piano_transcription.web

import android.app.Activity
import android.webkit.WebView

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel

import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result

import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

import ru.bshakhovsky.piano_transcription.R.string

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

import java.net.URLDecoder

class WebModel : ViewModel() {

    @Suppress("SpellCheckingInspection")
    // https://github.com/KomeijiKaede/AndroidYoutubeDL
    // /blob/experimental/app/src/main/java/net/teamfruit/androidyoutubedl/utils/Extractor.kt

    data class AdaptiveFormat(
        val url: String, val mimeType: String, @Suppress("SpellCheckingInspection") val itag: Int,
        val averageBitrate: Int, val audioSampleRate: String, val contentsLength: String
    )

    data class Thumbnail(val url: String, val width: String, val height: String)
    data class Thumbnails(val thumbnails: List<Thumbnail>)

    data class StreamingData(val adaptiveFormats: List<AdaptiveFormat>)
    data class PlayabilityStatus(val status: String)
    data class VideoDetails(val title: String, val author: String, val thumbnail: Thumbnails)

    data class PlayerResponse(
        val streamingData: StreamingData?,
        val playabilityStatus: PlayabilityStatus, val videoDetails: VideoDetails
    )

    private lateinit var activity: WeakPtr<Activity>
    private lateinit var web: WeakPtr<WebView>

    fun initialize(lifecycle: Lifecycle, a: Activity, w: WebView) {
        activity = WeakPtr(lifecycle, a)
        web = WeakPtr(lifecycle, w)
    }

    fun home(): Unit = activity.get().finish()
    fun back(): Unit = with(web.get()) { if (canGoBack()) goBack() }
    fun forward(): Unit = with(web.get()) { if (canGoForward()) goForward() }
    fun request() {
        DebugMode.assertState(web.get().url != null)
        web.get().url?.let { url ->
            @Suppress("RegExpAnonymousGroup") when {
                Regex("""^https?://(www\.)?(m\.)?youtube\.com""").containsMatchIn(url) ->
                    Regex("""v=[\w\-]{11}""").find(url)?.value?.substring(2)
                @Suppress("SpellCheckingInspection")
                Regex("""^https?://(www\.)?(m\.)?youtu.be/""").containsMatchIn(url) ->
                    @Suppress("SpellCheckingInspection")
                    Regex("""youtu.be/[\w\-]{11}""").find(url)?.value?.substring(9)
                else -> null
            }.also { id ->
                if (id == null) Snackbar.make(web.get(), string.noLink, Snackbar.LENGTH_LONG).show()
                else "https://www.youtube.com/get_video_info?video_id=$id"
                    .httpGet().response { _, response, result ->
                        when (result) {
                            is Result.Failure -> {
                                Snackbar.make(
                                    web.get(), string.linkFailed, Snackbar.LENGTH_LONG
                                ).show()
                                return@response
                            }
                            is Result.Success -> startDownload(response)
                            else -> DebugMode.assertState(false)
                        }
                    }
            }
        }
    }

    private fun startDownload(response: Response) = try {
        var playerResponse = ""
        for (p in response.toString().split('&'))
            p.split('=').also { if (it[0] == "player_response") playerResponse = it[1] }
        val parsedJson =
            Gson().fromJson(URLDecoder.decode(playerResponse, "UTF-8"), PlayerResponse::class.java)
        when {
            parsedJson == null ->
                Snackbar.make(web.get(), string.copyrightProtected, Snackbar.LENGTH_LONG).show()
            // parsedJson.playabilityStatus.status == "UNPLAYABLE" ->
            parsedJson.streamingData == null ->
                Snackbar.make(web.get(), string.copyrightProtected, Snackbar.LENGTH_LONG).show()
            else -> {
                var maxITag = 0
                var bestQualityAudioFormat: AdaptiveFormat? = null
                for (adaptiveFormat in parsedJson.streamingData.adaptiveFormats)
                    if ((adaptiveFormat.mimeType.startsWith("audio"))
                        and (adaptiveFormat.itag >= maxITag)
                    ) {
                        maxITag = adaptiveFormat.itag
                        bestQualityAudioFormat = adaptiveFormat
                    }

                Snackbar.make(
                    // Uri.parse(bestQualityAudioFormat.url).toString()
                    web.get(), bestQualityAudioFormat?.url
                        ?: activity.get().getString(string.noAudioStream), Snackbar.LENGTH_LONG
                ).show()
            }
        }
    } catch (e: JsonSyntaxException) {
        Snackbar.make(web.get(), string.notJson, Snackbar.LENGTH_LONG).show()
    }
}