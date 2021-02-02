package ru.bshakhovsky.piano_transcription.web

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.webkit.WebView

import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider

import com.google.android.material.snackbar.Snackbar

import ru.bshakhovsky.piano_transcription.R.layout.activity_web
import ru.bshakhovsky.piano_transcription.R.string
import ru.bshakhovsky.piano_transcription.databinding.ActivityWebBinding

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage

class WebActivity : AppCompatActivity() {

    enum class Permission(val id: Int) { STORAGE(40), STORAGE_SETTINGS(41) }

    private lateinit var web: WebView
    private lateinit var model: WebModel

    override fun onCreate(savedInstanceState: Bundle?): Unit =
        super.onCreate(savedInstanceState).also {
            with(DataBindingUtil.setContentView<ActivityWebBinding>(this, activity_web)) {
                web = contentWeb.web
                model = ViewModelProvider(this@WebActivity).get(WebModel::class.java)
                    .apply { initialize(lifecycle, this@WebActivity, web) }
                webModel = model

                with(web) {
                    @SuppressLint("SetJavaScriptEnabled")
                    settings.javaScriptEnabled = true

                    savedInstanceState?.let { web.restoreState(it) } ?: with(intent.extras) {
                        (if (this == null) "https://youtu.be"
                        else get(Intent.EXTRA_TEXT).toString().also {
                            if (!intent.hasExtra(Intent.EXTRA_SUBJECT)) Snackbar
                                .make(web, getString(string.notUrl, it), Snackbar.LENGTH_LONG)
                                .show()
                        }).also {
                            webViewClient = WebClient(lifecycle, contentWeb.webText, it)
                            loadUrl(it)
                        }
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

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ): Unit = super.onRequestPermissionsResult(requestCode, permissions, grantResults).also {
        when (requestCode) {
            Permission.STORAGE.id -> if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) model.request() else settings()

            else -> DebugMode.assertArgument(false)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Unit =
        super.onActivityResult(requestCode, resultCode, data).also {
            when (requestCode) {
                Permission.STORAGE_SETTINGS.id -> {
                    DebugMode.assertState(resultCode != RESULT_OK)
                    if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PackageManager.PERMISSION_GRANTED
                    ) model.request() else settings()
                }
                else -> DebugMode.assertArgument(false)
            }
        }

    private fun settings() = Snackbar.make(web, string.grantStorage, Snackbar.LENGTH_LONG)
        .setAction(string.settings) {
            startActivityForResult(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")
                ), Permission.STORAGE_SETTINGS.id
            )
            InfoMessage.toast(applicationContext, string.grantStorage)
        }.show()
}