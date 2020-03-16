@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.Utils

import android.content.Context
import androidx.appcompat.app.AlertDialog
import ru.BShakhovsky.Piano_Transcription.R

object MessageDialog {

    fun show(
        context: Context?, titleId: Int, msgStr: String,
        okId: Int = R.string.ok, okAction: (() -> Unit) = {}
    ): AlertDialog? {
        DebugMode.assertState(context != null)
        return context?.let {
            AlertDialog.Builder(it).setTitle(titleId).setMessage(msgStr).setIcon(R.drawable.info)
                .setPositiveButton(okId) { _, _ -> okAction() }.setCancelable(false).show()
        }
    }

    fun show(
        context: Context?, titleId: Int, msgId: Int,
        okId: Int = R.string.ok, okAction: (() -> Unit) = {}
    ): AlertDialog? {
        DebugMode.assertState(context != null)
        return context?.run { show(this, titleId, getString(msgId), okId, okAction) }
    }
}