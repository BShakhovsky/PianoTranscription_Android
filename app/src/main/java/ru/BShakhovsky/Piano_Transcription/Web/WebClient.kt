@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.Web

import android.view.View

import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

import android.widget.TextView

import ru.BShakhovsky.Piano_Transcription.Utils.DebugMode
import ru.BShakhovsky.Piano_Transcription.R

class WebClient(private var requestedUrl: String, private var webText: TextView) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        DebugMode.assertArgument((view != null) and (request != null))
        request?.url.toString().also {
            requestedUrl = it
            view?.loadUrl(it)
        }
        return super.shouldOverrideUrlLoading(view, request)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        DebugMode.assertArgument((view != null) and (url != null))
        if (url == "about:blank") with(webText) {
            visibility = View.VISIBLE
            text = context.getString(R.string.aboutBlank, requestedUrl)
        }
    }
}