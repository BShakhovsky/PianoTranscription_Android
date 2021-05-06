package ru.bshakhovsky.piano_transcription.utils

import android.app.Activity
import android.content.Intent

import ru.bshakhovsky.piano_transcription.R.string

object Share {

    fun share(activity: Activity): Unit = with(activity) {
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${getString(string.app_name)} for Android")
            putExtra(
                Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=$packageName"
            )
            DebugMode.assertState(resolveActivity(packageManager) != null)
            InfoMessage.toast(applicationContext, string.guideShare)
        }, null))
    }
}