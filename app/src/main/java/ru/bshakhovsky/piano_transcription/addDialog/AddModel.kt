package ru.bshakhovsky.piano_transcription.addDialog

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.media.MediaRecorder
import android.net.Uri
import android.provider.DocumentsContract
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.DialogFragment

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ViewModel

import ru.bshakhovsky.piano_transcription.R.string

import ru.bshakhovsky.piano_transcription.spectrum.SpectrumActivity
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.MessageDialog
import ru.bshakhovsky.piano_transcription.utils.WeakPtr
import ru.bshakhovsky.piano_transcription.web.WebActivity

import java.io.FileDescriptor
import java.io.FileNotFoundException
import java.io.IOException

class AddModel : ViewModel(), LifecycleObserver {

    private lateinit var fragment: WeakPtr<DialogFragment>

    private var recFile: Uri? = null
    private var recorder: MediaRecorder? = null
    private var recDlg: AlertDialog? = null
    private var recMsg: RecordMsg? = null

    fun initialize(lifecycle: Lifecycle, f: DialogFragment) {
        lifecycle.addObserver(this)
        fragment = WeakPtr(lifecycle, f)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun stopRecording() {
        recorder?.run {
            stop()
            release()
        }
        recorder = null

        DebugMode.assertState(if (recMsg == null) recDlg == null else recDlg != null)
        recMsg = null

        with(fragment.get()) {
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

    fun surf(): Unit = with(fragment.get()) {
        DebugMode.assertState((dialog != null) and (activity != null))
        activity?.startActivity(Intent(activity, WebActivity::class.java))
        dialog?.dismiss()
    }

    fun media(): Unit = with(fragment.get()) {
        DebugMode.assertState((dialog != null) and (activity != null))
        activity?.startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE) // don't show list of contacts or timezones
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/*"))
        }, AddDialog.RequestCode.OPEN_MEDIA.id)
        dialog?.dismiss()
    }

    fun midi(): Unit = with(fragment.get()) {
        DebugMode.assertState((dialog != null) and (activity != null))
        activity?.startActivityForResult(
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "audio/midi"
                addCategory(Intent.CATEGORY_OPENABLE) // don't show list of contacts or timezones
            }, AddDialog.RequestCode.OPEN_MIDI.id
        )
        dialog?.dismiss()
    }

    fun record(): Unit = with(fragment.get()) {
        DebugMode.assertState(activity != null)
        if (activity?.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(
            arrayOf(Manifest.permission.RECORD_AUDIO), AddDialog.Permission.RECORD.id
        ) else writeWav()
    }

    fun writeWav(): Unit = with(fragment.get()) {
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "audio/3gp"
            addCategory(Intent.CATEGORY_OPENABLE) // don't show list of contacts or timezones
            putExtra(
                Intent.EXTRA_TITLE, "${getString(string.record)} ${Calendar.getInstance().time}.3gp"
            )
            Toast.makeText(context, string.save, Toast.LENGTH_LONG)
                .apply { setGravity(Gravity.CENTER, 0, 0) }.show()
        }, AddDialog.RequestCode.WRITE_3GP.id)
    }

    private fun add3gpExt(resolver: ContentResolver, uri: Uri, name: String) =
        if (name.endsWith(".3gp")) uri else try {
            DocumentsContract.renameDocument(resolver, uri, "$name.3gp")
        } catch (e: IllegalStateException) {
            e.localizedMessage.let {
                DebugMode.assertState(!it.isNullOrEmpty())
                it?.run {
                    DebugMode.assertState(startsWith("File already exists: ") and endsWith(".3gp"))
                }
                with(fragment.get()) {
                    MessageDialog.show(context, string.micError, getString(string.exists, it))
                }
            }
            null
        }

    fun startRec(uri: Uri?): Unit = with(fragment.get()) {
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
                                        context, string.noFile, "${e.localizedMessage ?: e}\n\n$uri"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startRecNonNull(outFile: FileDescriptor) = with(fragment.get()) {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outFile)

            try {
                prepare()
            } catch (e: IOException) {
                MessageDialog.show(
                    context, string.micError, getString(string.prepError, e.localizedMessage)
                )
                release()
                recFile = null
                return
            }
            start()

            DebugMode.assertState((recDlg == null) and (recMsg == null) and (recFile != null))
            recMsg = RecordMsg(lifecycle, context,
                MessageDialog.show(context, string.recording, "", string.stop)
                { dialog?.dismiss() }.apply { recDlg = this }, this
            ).apply { start() }
        }
    }
}