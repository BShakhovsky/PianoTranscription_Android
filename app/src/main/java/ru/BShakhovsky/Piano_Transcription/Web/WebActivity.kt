package ru.bshakhovsky.piano_transcription.web

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

import kotlinx.android.synthetic.main.activity_web.download
import kotlinx.android.synthetic.main.activity_web.goBack
import kotlinx.android.synthetic.main.activity_web.goForward
import kotlinx.android.synthetic.main.activity_web.goHome

import kotlinx.android.synthetic.main.content_web.web
import kotlinx.android.synthetic.main.content_web.webText

import ru.bshakhovsky.piano_transcription.R.layout.activity_web
import ru.bshakhovsky.piano_transcription.R // id
import ru.bshakhovsky.piano_transcription.R.string

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import java.net.URLDecoder

@Suppress("SpellCheckingInspection")
// https://github.com/KomeijiKaede/AndroidYoutubeDL
// /blob/experimental/app/src/main/java/net/teamfruit/androidyoutubedl/utils/Extractor.kt

class WebActivity : AppCompatActivity(), View.OnClickListener {

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(activity_web)

        with(web) {
            @SuppressLint("SetJavaScriptEnabled")
            settings.javaScriptEnabled = true

            savedInstanceState?.let {
                web.restoreState(it)
                return@with
            }

            with(intent.extras) {
                (if (this == null) "https://youtu.be"
                else get(Intent.EXTRA_TEXT).toString().also {
                    if (!intent.hasExtra(Intent.EXTRA_SUBJECT)) Snackbar.make(
                        web, getString(string.notUrl, it), Snackbar.LENGTH_LONG
                    ).show()
                }).also {
                    webViewClient = WebClient(it, webText)
                    loadUrl(it)
                }
            }
        }

        arrayOf(goHome, goBack, goForward, download).forEach { it.setOnClickListener(this) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        web.restoreState(savedInstanceState)
    }

    override fun onClick(view: View?): Unit = with(web) {
        DebugMode.assertArgument(view != null)
        when (view?.id) {
            R.id.goHome -> super.onBackPressed()
            R.id.goBack -> if (canGoBack()) goBack()
            R.id.goForward -> if (canGoForward()) goForward()
            R.id.download -> tryDownload()
            else -> DebugMode.assertState(false)
        }
    }

    override fun onBackPressed(): Unit =
        with(web) { if (canGoBack()) goBack() else super.onBackPressed() }

    private fun tryDownload() {
        DebugMode.assertState(web.url != null)
        web.url?.let { url ->
            @Suppress("RegExpAnonymousGroup") when {
                Regex("""^https?://(www\.)?(m\.)?youtube\.com""").containsMatchIn(url) ->
                    Regex("""v=[\w\-]{11}""").find(url)?.value?.substring(2)
                @Suppress("SpellCheckingInspection")
                Regex("""^https?://(www\.)?(m\.)?youtu.be/""").containsMatchIn(url) ->
                    @Suppress("SpellCheckingInspection")
                    Regex("""youtu.be/[\w\-]{11}""").find(url)?.value?.substring(9)
                else -> null
            }.also { id ->
                if (id == null) Snackbar.make(web, string.noLink, Snackbar.LENGTH_LONG).show()
                else "https://www.youtube.com/get_video_info?video_id=$id"
                    .httpGet().response { _, response, result ->
                        when (result) {
                            is Result.Failure -> {
                                Snackbar.make(web, string.linkFailed, Snackbar.LENGTH_LONG).show()
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
                Snackbar.make(web, string.copyrightProtected, Snackbar.LENGTH_LONG).show()
            // parsedJson.playabilityStatus.status == "UNPLAYABLE" ->
            parsedJson.streamingData == null ->
                Snackbar.make(web, string.copyrightProtected, Snackbar.LENGTH_LONG).show()
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
                    web, bestQualityAudioFormat?.url
                        ?: getString(string.noAudioStream), Snackbar.LENGTH_LONG
                ).show()
            }
        }
    } catch (e: JsonSyntaxException) {
        Snackbar.make(web, string.notJson, Snackbar.LENGTH_LONG).show()
    }
}