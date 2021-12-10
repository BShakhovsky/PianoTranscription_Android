package ru.bshakhovsky.piano_transcription.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract

import androidx.annotation.CheckResult
import androidx.documentfile.provider.DocumentFile

import ru.bshakhovsky.piano_transcription.R.string

object FileName {

    @CheckResult
    fun getName(context: Context, uri: Uri?): String = with(context) {
        @Suppress("SpellCheckingInspection") // java.lang.NullPointerException for Itel itel A14
        uri?.let { DocumentFile.fromSingleUri(this, it)?.name } ?: getString(string.app_name)
    }

    @CheckResult
    fun addExtension(context: Context?, uri: Uri?, ext: String): Uri? =
        DebugMode.assertState(context != null).let {
            context?.run {
                getName(this, uri).let { name ->
                    when {
                        name.endsWith(".$ext") -> uri
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> try {
                            DocumentsContract.renameDocument(contentResolver!!, uri!!, "$name.$ext")
                        } catch (e: IllegalStateException) {
                            e.localizedMessage.let {
                                DebugMode.assertState(!it.isNullOrEmpty())
                                it?.run {
                                    DebugMode.assertState(
                                        startsWith("File already exists: ") and endsWith(".$ext")
                                    )
                                }
                                InfoMessage.dialog(
                                    this, string.error, getString(string.existsTryAgain, it)
                                )
                            }
                            null
                        }
                        else -> uri
                    }
                }
            }
        }
}