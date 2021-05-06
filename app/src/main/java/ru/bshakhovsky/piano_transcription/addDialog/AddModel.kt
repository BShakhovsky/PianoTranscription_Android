package ru.bshakhovsky.piano_transcription.addDialog

import android.content.Intent
import android.icu.util.Calendar
import android.media.MediaRecorder
import android.net.Uri
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ViewModel

import ru.bshakhovsky.piano_transcription.R.string

import ru.bshakhovsky.piano_transcription.main.MainActivity
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.FileName
import ru.bshakhovsky.piano_transcription.utils.InfoMessage
import ru.bshakhovsky.piano_transcription.utils.MicPermission
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

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

            recFile?.let { (activity as MainActivity).openMedia(it) }
        }
    }

    fun media(): Unit = with(fragment.get()) {
        startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE) // don't show list of contacts or timezones
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/*"))
        }, AddDialog.RequestCode.OPEN_MEDIA.id)
        DebugMode.assertState(dialog != null)
        dialog?.dismiss()
    }

    fun midi(): Unit = with(fragment.get()) {
        startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "audio/midi"
            addCategory(Intent.CATEGORY_OPENABLE) // don't show list of contacts or timezones
        }, AddDialog.RequestCode.OPEN_MIDI.id)
        DebugMode.assertState(dialog != null)
        dialog?.dismiss()
    }

    fun record(): Unit? = MicPermission.requestPermission(
        MicPermission.RecPermission.RECORD.id, fragment.get().activity, fragment.get()
    ) { writeWav() }

    fun writeWav(): Unit = with(fragment.get()) {
        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "audio/3gp"
            addCategory(Intent.CATEGORY_OPENABLE) // don't show list of contacts or timezones
            putExtra(
                Intent.EXTRA_TITLE, "${getString(string.record)} ${Calendar.getInstance().time}.3gp"
            )
            InfoMessage.toast(context, getString(string.save, getString(string.record)))
        }, AddDialog.RequestCode.WRITE_3GP.id)
    }

    fun startRec(uri: Uri?): Unit = with(fragment.get()) {
        FileName.addExtension(context, uri, "3gp")?.let { newUri -> // null if file exists
            try {
                DebugMode.assertState((activity != null) and (activity?.contentResolver != null))
                activity?.contentResolver?.openFileDescriptor(newUri, "w").use { outFile ->
                    DebugMode.assertState(outFile != null)
                    recFile = newUri
                    startRecNonNull(outFile?.fileDescriptor ?: return)
                }
            } catch (e: FileNotFoundException) {
                InfoMessage.dialog(context, string.noFile, "${e.localizedMessage ?: e}\n\n$uri")
            }
        }
    }

    private fun startRecNonNull(outFile: FileDescriptor) = with(fragment.get()) {
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outFile)

            try {
                prepare()
            } catch (e: IOException) {
                InfoMessage.dialog(
                    context, string.micError, getString(string.prepError, e.localizedMessage)
                )
                release()
                recFile = null
                return
            }
            start()

            DebugMode.assertState((recDlg == null) and (recMsg == null) and (recFile != null))
            recMsg = RecordMsg(lifecycle,
                InfoMessage.dialog(context, string.recording, "", string.stop)
                { dialog?.dismiss() }.apply { recDlg = this }, this, context
            ).apply { start() }
        }
    }
}