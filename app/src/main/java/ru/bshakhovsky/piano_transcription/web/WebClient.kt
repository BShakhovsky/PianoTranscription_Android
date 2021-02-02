package ru.bshakhovsky.piano_transcription.web

import android.view.View

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

import android.widget.TextView
import androidx.lifecycle.Lifecycle

import ru.bshakhovsky.piano_transcription.R.string.aboutBlank

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

class WebClient(lifecycle: Lifecycle, wt: TextView, private var requestedUrl: String) :
    WebViewClient() {

    private val webText = WeakPtr(lifecycle, wt)

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean =
        super.shouldOverrideUrlLoading(view, request).also {
            DebugMode.assertArgument((view != null) and (request != null))
            request?.url.toString().also {
                requestedUrl = it
                view?.loadUrl(it)
            }
        }

    override fun onPageFinished(view: WebView?, url: String?): Unit =
        super.onPageFinished(view, url).also {
            DebugMode.assertArgument((view != null) and (url != null))
            if (url == "about:blank") with(webText.get()) {
                visibility = View.VISIBLE
                text = context.getString(aboutBlank, requestedUrl)
            }
        }
}