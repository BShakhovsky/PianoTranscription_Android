package ru.bshakhovsky.piano_transcription.web

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar

import kotlinx.android.synthetic.main.content_web.web
import kotlinx.android.synthetic.main.content_web.webText

import ru.bshakhovsky.piano_transcription.R.layout.activity_web
import ru.bshakhovsky.piano_transcription.R.string.notUrl
import ru.bshakhovsky.piano_transcription.databinding.ActivityWebBinding

class WebActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebBinding
    private lateinit var model: WebModel

    override fun onCreate(savedInstanceState: Bundle?): Unit =
        super.onCreate(savedInstanceState).also {
            binding = DataBindingUtil.setContentView(this, activity_web)
            model = ViewModelProvider(this).get(WebModel::class.java)
                .apply { initialize(lifecycle, this@WebActivity, web) }
            binding.webModel = model

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
                        if (!intent.hasExtra(Intent.EXTRA_SUBJECT))
                            Snackbar.make(web, getString(notUrl, it), Snackbar.LENGTH_LONG).show()
                    }).also {
                        webViewClient = WebClient(lifecycle, webText, it)
                        loadUrl(it)
                    }
                }
            }
        }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        web.restoreState(savedInstanceState)
    }

    override fun onBackPressed(): Unit =
        with(web) { if (canGoBack()) goBack() else super.onBackPressed() }
}