package ru.bshakhovsky.piano_transcription.utils

import android.app.Activity
import android.content.Context

import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.tasks.Task

object Review {

    fun review(context: Context, activity: Activity): Task<ReviewInfo> =
        with(ReviewManagerFactory.create(context)) {
            requestReviewFlow().addOnCompleteListener {
                with(it) { if (isSuccessful) launchReviewFlow(activity, result) }
            }
        }
}