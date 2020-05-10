@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription

import android.Manifest
import android.app.Dialog
import android.content.ContentResolver
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

import ru.BShakhovsky.Piano_Transcription.Spectrum.SpectrumActivity
import ru.BShakhovsky.Piano_Transcription.Utils.DebugMode
import ru.BShakhovsky.Piano_Transcription.Utils.MessageDialog
import ru.BShakhovsky.Piano_Transcription.Web.WebActivity
import java.io.FileDescriptor
import java.io.FileNotFoundException
import java.io.IOException

class AddDialog : DialogFragment(), View.OnClickListener {

    enum class RequestCode(val id: Int) { OPEN_MEDIA(10), OPEN_MIDI(11), WRITE_3GP(12) }
    enum class Permission(val id: Int) { RECORD(20), RECORD_SETTINGS(21) }

    private var recFile: Uri? = null
    private var record: MediaRecorder? = null
    private var recDlg: AlertDialog? = null
    private var recMsg: RecordMsg? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        DebugMode.assertState(context != null)
        return Dialog((context ?: return super.onCreateDialog(savedInstanceState))).apply {
            setContentView(R.layout.dialog_add)

            DebugMode.assertState(window != null)
            window?.run {
                setGravity(Gravity.BOTTOM or Gravity.END)
                setBackgroundDrawableResource(android.R.color.transparent)
            }

            arrayOf(surf, mediaFile, midiFile, record).forEach {
                it.setOnClickListener(this@AddDialog)
            }
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
                activity?.startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    // Don't show list of contacts or timezones:
                    addCategory(Intent.CATEGORY_OPENABLE)
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/*"))
                }, RequestCode.OPEN_MEDIA.id)
                dialog?.dismiss()
            }
            R.id.midiFile -> {
                activity?.startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "audio/midi"
                    // Don't show list of contacts or timezones:
                    addCategory(Intent.CATEGORY_OPENABLE)
                }, RequestCode.OPEN_MIDI.id)
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
                    MessageDialog.show(context, R.string.warning, R.string.notSaved)
                    DebugMode.assertState(dialog != null)
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

    private fun writeWav() = startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
        type = "audio/3gp"
        addCategory(Intent.CATEGORY_OPENABLE) // don't show list of contacts or timezones
        putExtra(
            Intent.EXTRA_TITLE, "${getString(R.string.record)} ${Calendar.getInstance().time}.3gp"
        )
        Toast.makeText(context, R.string.save, Toast.LENGTH_LONG).show()
    }, RequestCode.WRITE_3GP.id)

    private fun add3gpExt(resolver: ContentResolver, uri: Uri, name: String) =
        if (name.endsWith(".3gp")) uri else try {
            DocumentsContract.renameDocument(resolver, uri, "$name.3gp")
        } catch (e: IllegalStateException) {
            e.localizedMessage.let {
                DebugMode.assertState(!it.isNullOrEmpty())
                it?.run {
                    DebugMode.assertState(startsWith("File already exists: ") and endsWith(".3gp"))
                }
                MessageDialog.show(context, R.string.micError, getString(R.string.exists, it))
            }
            null
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
                                try {
                                    resolver.openFileDescriptor(newUri, "w").use { outFile ->
                                        DebugMode.assertState(outFile != null)
                                        recFile = newUri
                                        startRecNonNull(outFile?.fileDescriptor ?: return)
                                    }
                                } catch (e: FileNotFoundException) {
                                    MessageDialog.show(
                                        context, R.string.noFile,
                                        "${e.localizedMessage ?: e}\n\n$uri"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startRecNonNull(outFile: FileDescriptor) {
        record = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outFile)

            try {
                prepare()
            } catch (e: IOException) {
                MessageDialog.show(
                    context, R.string.micError, getString(R.string.prepError, e.localizedMessage)
                )
                release()
                recFile = null
                return
            }
            start()

            DebugMode.assertState((recDlg == null) and (recMsg == null) and (recFile != null))
            recMsg = RecordMsg(
                MessageDialog.show(context, R.string.recording, "", R.string.stop)
                { dialog?.dismiss() }.apply { recDlg = this }, context, this
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

        recFile?.let {
            startActivity(Intent(context, SpectrumActivity::class.java)
                .apply { putExtra("Uri", it) })
        }
    }
}