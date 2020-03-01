@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription

import android.Manifest
import android.app.Dialog

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

import android.icu.util.Calendar
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.google.android.material.snackbar.Snackbar

import kotlinx.android.synthetic.main.dialog_add.mediaFile
import kotlinx.android.synthetic.main.dialog_add.midiFile
import kotlinx.android.synthetic.main.dialog_add.record
import kotlinx.android.synthetic.main.dialog_add.surf

import ru.BShakhovsky.Piano_Transcription.Web.WebActivity

import java.io.FileDescriptor
import java.io.IOException

class AddDialog : DialogFragment(), View.OnClickListener {

    enum class RequestCode(val id: Int) { OPEN_MEDIA(10), OPEN_MIDI(11), WRITE_3GP(12) }
    enum class Permission(val id: Int) { RECORD(20), RECORD_SETTINGS(21) }

    private var record: MediaRecorder? = null
    private var recDlg: AlertDialog? = null
    private var recMsg: RecordMsg? = null

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
            R.id.surf -> {
                startActivity(Intent(activity, WebActivity::class.java))
                dialog?.dismiss()
            }
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
            RequestCode.WRITE_3GP.id ->
                if (resultCode != FragmentActivity.RESULT_OK) {
                    DebugMode.assertState((context != null) and (dialog != null))
                    context?.let { c ->
                        MainActivity.msgDialog(c, R.string.warning, R.string.notSaved)
                    }
                    dialog?.dismiss()
                } else {
                    DebugMode.assertArgument(data != null)
                    startRec(data?.data)
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
            type = "audio/3gp"
            addCategory(Intent.CATEGORY_OPENABLE) // don't show list of contacts or timezones
            putExtra(
                Intent.EXTRA_TITLE,
                "${getString(R.string.record)} ${Calendar.getInstance().time}.3gp"
            )
            startActivityForResult(this, RequestCode.WRITE_3GP.id)
            Toast.makeText(context, R.string.save, Toast.LENGTH_LONG).show()
        }
    }

    private fun add3gpExt(resolver: ContentResolver, uri: Uri, name: String): Uri? {
        if (name.endsWith(".3gp")) return uri
        try {
            return DocumentsContract.renameDocument(resolver, uri, "$name.3gp")
        } catch (e: IllegalStateException) {
            DebugMode.assertState(!e.localizedMessage.isNullOrEmpty())
            e.localizedMessage?.run {
                DebugMode.assertState(
                    startsWith("File already exists: ") and endsWith(".3gp")
                )
            }
            context?.let {
                MainActivity.msgDialog(
                    it, R.string.micError, getString(R.string.exists, e.localizedMessage)
                )
            }
        }
        return null
    }

    private fun startRec(uri: Uri?) {
        DebugMode.assertArgument(uri != null)
        uri?.let { u ->
            DebugMode.assertState(context != null)
            context?.let { c ->
                DocumentFile.fromSingleUri(c, u)?.name.let { name ->
                    DebugMode.assertState(name != null)
                    name?.let { n ->
                        DebugMode.assertState(
                            (activity != null) and (activity?.contentResolver != null)
                        )
                        activity?.contentResolver?.let { resolver ->
                            add3gpExt(resolver, u, n)?.let { newUri ->
                                // DebugMode.assertState(newUri != null) // null if file exists
                                // if (newUri == null) return
                                resolver.openFileDescriptor(newUri, "w").use { outFile ->
                                    DebugMode.assertState(outFile != null)
                                    startRecNonNull(outFile?.fileDescriptor ?: return, c)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startRecNonNull(outFile: FileDescriptor, c: Context) {
        record = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outFile)

            try {
                prepare()
            } catch (e: IOException) {
                MainActivity.msgDialog(
                    c, R.string.micError, getString(R.string.prepError, e.localizedMessage)
                )
                release()
                return
            }
            start()

            DebugMode.assertState((recDlg == null) and (recMsg == null))
            recMsg = RecordMsg(
                MainActivity.msgDialog(c, R.string.recording, "", R.string.stop) {
                    dialog?.dismiss()
                }.apply { recDlg = this }, this, c
            ).apply { start() }
        }
    }

    override fun onStop() {
        super.onStop()
        record?.run {
            stop()
            release()
        }
        record = null

        DebugMode.assertState(if (recMsg == null) recDlg == null else recDlg != null)
        recMsg?.stop()
        recMsg = null

        if (recDlg != null) {
            DebugMode.assertState(dialog != null)
            dialog?.dismiss()
        }
        recDlg?.dismiss()
        recDlg = null
    }
}