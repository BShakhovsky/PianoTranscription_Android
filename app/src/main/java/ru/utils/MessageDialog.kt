package ru.bshakhovsky.piano_transcription.utils

import android.content.Context
import androidx.appcompat.app.AlertDialog

import ru.bshakhovsky.piano_transcription.R.drawable.info
import ru.bshakhovsky.piano_transcription.R.string.ok

object MessageDialog {

    fun show(
        context: Context?, titleId: Int, msgStr: String,
        okId: Int = ok, okAction: (() -> Unit) = {}
    ): AlertDialog? {
        DebugMode.assertState(context != null)
        return context?.let {
            AlertDialog.Builder(it).setTitle(titleId).setMessage(msgStr).setIcon(info)
                .setPositiveButton(okId) { _, _ -> okAction() }.setCancelable(false).show()
        }
    }

    fun show(
        context: Context?, titleId: Int, msgId: Int,
        okId: Int = ok, okAction: (() -> Unit) = {}
    ): AlertDialog? {
        DebugMode.assertState(context != null)
        return context?.run { show(this, titleId, getString(msgId), okId, okAction) }
    }
}