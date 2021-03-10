package ru.bshakhovsky.piano_transcription.utils

import android.app.Activity
import android.content.Intent

import com.google.android.play.core.review.ReviewManagerFactory

import ru.bshakhovsky.piano_transcription.R.string

object ShareReview {

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

    fun review(activity: Activity): Unit =
        with(ReviewManagerFactory.create(activity.applicationContext)) {
            requestReviewFlow().addOnCompleteListener {
                with(it) { if (isSuccessful) launchReviewFlow(activity, result) }
            }
        }
}