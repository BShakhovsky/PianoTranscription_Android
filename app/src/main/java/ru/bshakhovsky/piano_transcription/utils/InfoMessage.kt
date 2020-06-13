package ru.bshakhovsky.piano_transcription.utils

import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

import ru.bshakhovsky.piano_transcription.R.drawable.info
import ru.bshakhovsky.piano_transcription.R.string.ok

object InfoMessage {

    fun toast(context: Context?, msgStr: String): Unit =
        Toast.makeText(context, msgStr, Toast.LENGTH_LONG)
            .apply { setGravity(Gravity.CENTER, 0, 0) }.show()

    fun toast(context: Context?, msgId: Int): Unit? = DebugMode.assertState(context != null)
        .let { context?.run { toast(this, getString(msgId)) } }


    fun dialog(
        context: Context?, titleId: Int, msgStr: String,
        okId: Int = ok, okAction: (() -> Unit) = {}
    ): AlertDialog? = DebugMode.assertState(context != null).let {
        context?.let {
            AlertDialog.Builder(it).setTitle(titleId).setMessage(msgStr).setIcon(info)
                .setPositiveButton(okId) { _, _ -> okAction() }.setCancelable(false).show()
        }
    }

    fun dialog(
        context: Context?, titleId: Int, msgId: Int,
        okId: Int = ok, okAction: (() -> Unit) = {}
    ): AlertDialog? = DebugMode.assertState(context != null)
        .let { context?.run { dialog(this, titleId, getString(msgId), okId, okAction) } }
}