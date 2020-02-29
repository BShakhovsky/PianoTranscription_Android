@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar

import kotlinx.android.synthetic.main.dialog_add.mediaFile
import kotlinx.android.synthetic.main.dialog_add.midiFile
import kotlinx.android.synthetic.main.dialog_add.record
import kotlinx.android.synthetic.main.dialog_add.surf

class AddDialog : DialogFragment(), View.OnClickListener {

    enum class RequestCode(val id: Int) { OPEN_MEDIA(10), OPEN_MIDI(11), WRITE_WAV(12) }
    enum class Permission(val id: Int) { RECORD(20), RECORD_SETTINGS(21) }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        DebugMode.assertState(context != null)
        with(Dialog((context ?: return super.onCreateDialog(savedInstanceState)))) {
            setContentView(R.layout.dialog_add)

            DebugMode.assertState(window != null)
            window?.run {
                setGravity(Gravity.BOTTOM or Gravity.END)
                setBackgroundDrawableResource(android.R.color.transparent)
            }

            arrayOf(surf, mediaFile, midiFile, record).forEach {
                it.setOnClickListener(this@AddDialog)
            }

            return this
        }
    }

    override fun onClick(view: View?) {
        DebugMode.assertArgument(view != null)
        DebugMode.assertState((dialog != null) and (activity != null))
        when (view?.id) {
            R.id.surf -> dialog?.dismiss()
            R.id.mediaFile -> {
                with(Intent(Intent.ACTION_GET_CONTENT)) {
                    type = "*/*"
                    // Don't show list of contacts or timezones:
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/*"))
                    activity?.startActivityForResult(this, RequestCode.OPEN_MEDIA.id)
                }
                dialog?.dismiss()
            }
            R.id.midiFile -> {
                with(Intent(Intent.ACTION_GET_CONTENT)) {
                    type = "audio/midi"
                    // Don't show list of contacts or timezones:
                    addCategory(Intent.CATEGORY_OPENABLE)
                    activity?.startActivityForResult(this, RequestCode.OPEN_MIDI.id)
                }
                dialog?.dismiss()
            }
            R.id.record ->
                if (activity?.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED
                ) requestPermissions(
                    arrayOf(Manifest.permission.RECORD_AUDIO), Permission.RECORD.id
                ) else writeWav()

            else -> DebugMode.assertArgument(false)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Permission.RECORD.id ->
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) writeWav()
                else settings()
            else -> DebugMode.assertArgument(false)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Permission.RECORD_SETTINGS.id -> {
                // if (resultCode != FragmentActivity.RESULT_OK) for some reason it is never equal
                DebugMode.assertState(activity != null)
                if (activity?.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) writeWav()
                else settings()
            }
            RequestCode.WRITE_WAV.id -> {
                DebugMode.assertState((context != null) and (dialog != null) and (activity != null))
                context?.let { c ->
                    if (resultCode != FragmentActivity.RESULT_OK) {
                        MainActivity.msgDialog(
                            c, R.string.warning, R.string.notSaved
                        )
                        dialog?.dismiss()
                    } else {
                        DebugMode.assertArgument(data != null)
                        DebugMode.assertState(data?.data != null)
                        data?.data?.let { uri ->
                            activity?.contentResolver?.openOutputStream(uri)?.let { outStream ->
                                with(Record(c)) {
                                    MainActivity.msgDialog(
                                        c, R.string.recording, getString(
                                            R.string.recFormat, (sampleRate / 1_000f).run {
                                                when (this) {
                                                    toInt().toFloat() -> "%d".format(this.toInt())
                                                    else -> "%.1f".format(this)
                                                }
                                            },
                                            getString(
                                                when (nChannels.toInt()) {
                                                    1 -> R.string.mono
                                                    2 -> R.string.stereo
                                                    else -> {
                                                        DebugMode.assertState(false)
                                                        R.string.error
                                                    }
                                                }
                                            ), nBits
                                        ), R.string.stop
                                    ) {
                                        outStream.use {
                                            finish(outStream)
                                            dialog?.dismiss()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> DebugMode.assertArgument(false)
        }
    }

    private fun settings() {
        DebugMode.assertState((dialog != null) and (dialog?.record != null))
        dialog?.record?.let {
            Snackbar.make(it, R.string.grantRec, Snackbar.LENGTH_LONG)
                .setAction(R.string.settings) {
                    DebugMode.assertState(context != null)
                    context?.run {
                        startActivityForResult(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:$packageName")
                            ), Permission.RECORD_SETTINGS.id
                        )
                    }
                    Toast.makeText(context, R.string.grantRec, Toast.LENGTH_LONG).show()
                }.show()
        }
    }

    private fun writeWav() {
        with(Intent(Intent.ACTION_CREATE_DOCUMENT)) {
            type = "audio/x-wav"
            addCategory(Intent.CATEGORY_OPENABLE) // don't show list of contacts or timezones
            putExtra(
                Intent.EXTRA_TITLE,
                "${getString(R.string.record)} ${Calendar.getInstance().time}.wav"
            )
            startActivityForResult(this, RequestCode.WRITE_WAV.id)
            Toast.makeText(context, R.string.save, Toast.LENGTH_LONG).show()
        }
    }
}