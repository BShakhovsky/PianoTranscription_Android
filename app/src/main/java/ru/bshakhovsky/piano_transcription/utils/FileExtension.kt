package ru.bshakhovsky.piano_transcription.utils

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile

import ru.bshakhovsky.piano_transcription.R.string

object FileExtension {

    fun addExtension(context: Context?, uri: Uri?, ext: String): Uri? =
        DebugMode.assertState(context != null).let {
            context?.run {
                DebugMode.assertArgument(uri != null)
                uri?.let { u ->
                    DocumentFile.fromSingleUri(this, u)?.name.let { name ->
                        DebugMode.assertState(name != null)
                        name?.let { n ->
                            DebugMode.assertState(contentResolver != null)
                            contentResolver?.let { resolver ->
                                if (name.endsWith(".$ext")) uri else try {
                                    DocumentsContract.renameDocument(resolver, uri, "$n.$ext")
                                } catch (e: IllegalStateException) {
                                    e.localizedMessage.let {
                                        DebugMode.assertState(!it.isNullOrEmpty())
                                        it?.run {
                                            DebugMode.assertState(
                                                startsWith("File already exists: ")
                                                        and endsWith(".$ext")
                                            )
                                        }
                                        InfoMessage.dialog(
                                            this, string.error, getString(string.existsTryAgain, it)
                                        )
                                    }
                                    null
                                }
                            }
                        }
                    }
                }
            }
        }
}