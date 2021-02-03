package ru.bshakhovsky.piano_transcription.utils

import android.content.Context
import android.view.Gravity
import android.widget.Toast

import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog

import ru.bshakhovsky.piano_transcription.R.drawable.info
import ru.bshakhovsky.piano_transcription.R.string

object InfoMessage {

    fun toast(context: Context?, msgStr: String): Unit =
        Toast.makeText(context, msgStr, Toast.LENGTH_LONG)
            .apply { setGravity(Gravity.CENTER, 0, 0) }.show()

    fun toast(context: Context?, @StringRes msgId: Int): Unit? = DebugMode
        .assertState(context != null).let { context?.run { toast(this, getString(msgId)) } }


    fun dialog(
        context: Context?, @StringRes titleId: Int, msgStr: String,
        @StringRes okId: Int = string.ok, twoButtons: Boolean = false,
        cancelAction: (() -> Unit) = {}, okAction: (() -> Unit) = {}
    ): AlertDialog? = DebugMode.assertState(context != null).let {
        context?.let {
            AlertDialog.Builder(it).setTitle(titleId).setMessage(msgStr).setIcon(info)
                .setPositiveButton(okId) { _, _ -> okAction() }.setCancelable(false).apply {
                    if (twoButtons) setNegativeButton(string.cancel) { _, _ -> cancelAction() }
                }.show()
        }
    }

    fun dialog(
        context: Context?, @StringRes titleId: Int, @StringRes msgId: Int,
        @StringRes okId: Int = string.ok, twoButtons: Boolean = false,
        cancelAction: (() -> Unit) = {}, okAction: (() -> Unit) = {}
    ): AlertDialog? = DebugMode.assertState(context != null).let {
        context?.run {
            dialog(this, titleId, getString(msgId), okId, twoButtons, cancelAction, okAction)
        }
    }
}