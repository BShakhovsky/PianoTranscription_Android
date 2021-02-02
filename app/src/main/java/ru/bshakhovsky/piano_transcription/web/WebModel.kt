package ru.bshakhovsky.piano_transcription.web

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.net.Uri
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

    private lateinit var activity: WeakPtr<Activity>
    private lateinit var web: WeakPtr<WebView>
    private lateinit var downloader: DownloadsReceiver

    fun initialize(lifecycle: Lifecycle, a: Activity, w: WebView) {
        activity = WeakPtr(lifecycle, a)
        web = WeakPtr(lifecycle, w)
        downloader = DownloadsReceiver(lifecycle, a)
    }

    fun home(): Unit = activity.get().finish()
    fun back(): Unit = with(web.get()) { if (canGoBack()) goBack() }
    fun forward(): Unit = with(web.get()) { if (canGoForward()) goForward() }
    fun request(): Unit = with(activity.get()) {
        Manifest.permission.WRITE_EXTERNAL_STORAGE.let {
            if (checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(arrayOf(it), WebActivity.Permission.STORAGE.id) else {
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
                        if (id == null)
                            Snackbar.make(web.get(), string.noLink, Snackbar.LENGTH_LONG).show()
                        else "https://www.youtube.com/get_video_info?video_id=$id"
                            .httpGet().response { _, response, result ->
                                when (result) {
                                    is Result.Failure -> {
                                        Snackbar.make(
                                            web.get(), string.linkFailed, Snackbar.LENGTH_LONG
                                        ).show()
                                        return@response
                                    }
                                    is Result.Success -> bestLink(response)
                                    else -> DebugMode.assertState(false)
                                }
                            }
                    }
                }
            }
        }
    }

    // TODO: Increase number of attempts
    private fun bestLink(response: Response) = IntArray(2).forEach { _ ->
        try {
            var playerResponse = ""
            for (p in response.toString().split('&'))
                p.split('=').also { if (it[0] == "player_response") playerResponse = it[1] }
            with(
                Gson().fromJson(
                    URLDecoder.decode(playerResponse, "UTF-8"),
                    YouTubeFormat.PlayerResponse::class.java
                )
            ) {
                when {
                    this == null -> Snackbar
                        .make(web.get(), string.copyrightProtected, Snackbar.LENGTH_LONG).show()
                    // playabilityStatus.status == "UNPLAYABLE" ->
                    streamingData == null -> Snackbar
                        .make(web.get(), string.copyrightProtected, Snackbar.LENGTH_LONG).show()
                    else -> {
                        var maxITag = 0
                        var bestUrl: Uri? = null
                        var fileName: String? = null
                        streamingData.adaptiveFormats.forEach {
                            with(it) {
                                if (mimeType.startsWith("audio")) {
                                    DebugMode.assertState(mimeType in YouTubeFormat.audios.keys)
                                    if (itag >= maxITag) {
                                        maxITag = itag
                                        bestUrl = Uri.parse(url)
                                        fileName = "${videoDetails.title}.${
                                            YouTubeFormat.audios[mimeType]
                                        }"
                                    }
                                }
                            }
                        }
                        bestUrl?.let {
                            DebugMode.assertState(fileName != null)
                            downloader.addDownload(it, fileName!!)
                            return
                        } ?: run {
                            DebugMode.assertState(fileName == null)
                            Snackbar
                                .make(web.get(), string.noAudioStream, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            }
        } catch (e: JsonSyntaxException) {
            Snackbar.make(web.get(), string.notJson, Snackbar.LENGTH_LONG).show()
        }
    }
}