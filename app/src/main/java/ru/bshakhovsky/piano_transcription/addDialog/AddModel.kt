package ru.bshakhovsky.piano_transcription.addDialog

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build

import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDialogFragment

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
import ru.bshakhovsky.piano_transcription.utils.MicSource
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

import java.io.FileDescriptor
import java.io.FileNotFoundException
import java.io.IOException

class AddModel : ViewModel(), LifecycleObserver {

    private lateinit var fragment: WeakPtr<AppCompatDialogFragment>

    private lateinit var getMidi: WeakPtr<ActivityResultLauncher<Intent>>
    private lateinit var getMedia: WeakPtr<ActivityResultLauncher<Intent>>
    private lateinit var recPermission: MicPermission

    private var recFile: Uri? = null
    private var recorder: MediaRecorder? = null
    private var recDlg: AlertDialog? = null
    private var recMsg: RecordMsg? = null

    fun initialize(
        lifecycle: Lifecycle, f: AppCompatDialogFragment,
        midi: WeakPtr<ActivityResultLauncher<Intent>>,
        media: WeakPtr<ActivityResultLauncher<Intent>>,
        recPerm: MicPermission
    ) {
        lifecycle.addObserver(this)
        fragment = WeakPtr(lifecycle, f)

        getMidi = midi
        getMedia = media
        recPermission = recPerm
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun stopRecording() {
        with(fragment.get()) {
            recorder?.run {
                try {
                    stop()
                } catch (e: RuntimeException) { /* https://developer.android.com/reference/android
                                                    /media/MediaRecorder.html#stop()
                    Intentionally thrown to the application if no valid audio data has been received
                    (e.g. emulator does not have audio source) */
                    InfoMessage.dialog(
                        activity, string.micError,
                        getString(string.prepError, getString(string.noMic))
                    )
                }
                release()
            }
            recorder = null

            DebugMode.assertState(if (recMsg == null) recDlg == null else recDlg != null)
            recMsg = null

            if (recDlg != null) {
                DebugMode.assertState(dialog != null)
                dialog?.dismiss()
            }
            recDlg?.dismiss()
            recDlg = null

            recFile?.let { (activity as MainActivity).openMedia(it) }
        }
    }

    fun media(): Unit = getMedia.get().launch(Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "*/*"
        addCategory(Intent.CATEGORY_OPENABLE) // don't show list of contacts or timezones
        putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*", "video/*"))
    }).also {
        with(fragment.get()) {
            DebugMode.assertState(dialog != null)
            dialog?.dismiss()
        }
    }

    fun midi(): Unit = getMidi.get().launch(Intent(Intent.ACTION_GET_CONTENT).apply {
        type = "audio/midi"
        addCategory(Intent.CATEGORY_OPENABLE) // don't show list of contacts or timezones
    }).also {
        with(fragment.get()) {
            DebugMode.assertState(dialog != null)
            dialog?.dismiss()
        }
    }

    fun record(): Unit = recPermission.requestPermission()

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
        DebugMode.assertState(context != null)
        context?.run {
            when ((getSystemService(Context.AUDIO_SERVICE) as AudioManager).mode) {
                AudioManager.MODE_NORMAL -> {
                }
                AudioManager.MODE_RINGTONE -> {
                    InfoMessage.dialog(this, string.micError, string.phoneRinging)
                    return
                }
                AudioManager.MODE_IN_CALL -> {
                    InfoMessage.dialog(this, string.micError, string.phoneCall)
                    return
                }
                AudioManager.MODE_IN_COMMUNICATION -> {
                    InfoMessage.dialog(this, string.micError, string.voIpCall)
                    return
                }
                AudioManager.MODE_CALL_SCREENING -> { // Call is connected and audio is accessible
                } // to call screening applications but other audio use cases are still possible
                else -> DebugMode.assertState(false)
            }
        }
        recorder = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(requireContext()) else @Suppress("DEPRECATION") MediaRecorder()).apply {

            /* TODO: Samsung Galaxy S10, java.lang.RuntimeException
                at android.media.MediaRecorder.setAudioSource */
            setAudioSource(MicSource.micSource)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(outFile)

            fun notStarted(e: Exception, @StringRes msgId: Int) {
                InfoMessage.dialog(context, string.micError, getString(msgId, e.localizedMessage))
                reset()
                release()
                recFile = null
            }

            try {
                prepare()
            } catch (e: IOException) {
                notStarted(e, string.prepError)
                return
            }
            try {
                start()
            } catch (e: IllegalStateException) {
                notStarted(e, string.micInUse)
                return
            }

            DebugMode.assertState((recDlg == null) and (recMsg == null) and (recFile != null))
            recMsg = RecordMsg(lifecycle,
                InfoMessage.dialog(context, string.recording, "", string.stop)
                { dialog?.dismiss() }.apply { recDlg = this }, this, context
            ).apply { start() }
        }
    }
}