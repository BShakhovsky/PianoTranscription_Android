package ru.bshakhovsky.piano_transcription.addDialog

import android.content.Context
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.appcompat.app.AlertDialog

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

import ru.bshakhovsky.piano_transcription.R.string

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.MinSec
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class RecordMsg(
    lifecycle: Lifecycle, dlg: AlertDialog?, recorder: MediaRecorder, context: Context?
) : Runnable, LifecycleObserver {

    private val recDlg = WeakPtr(lifecycle, dlg)

    private var schedule: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    private val startTime = SystemClock.uptimeMillis()
    private val fmtMsg = recorder.run {
        DebugMode.assertState(context != null)
        context?.run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                DebugMode.assertState(
                    (activeRecordingConfiguration != null)
                            and (activeRecordingConfiguration?.format != null)
                )
                activeRecordingConfiguration?.format?.run {
                    getString(string.recFormat, with(sampleRate / 1_000f) {
                        when (this) {
                            toInt().toFloat() -> "%d".format(toInt())
                            else -> "%.1f".format(this)
                        }
                    }, getString(when {
                        channelCount == 1 -> string.mono
                        channelCount > 1 -> string.stereo
                        else -> string.invalidChannels.also { DebugMode.assertState(false) }
                    }), @Suppress("Reformat") when (encoding) {
                        AudioFormat.ENCODING_AAC_ELD        -> "AAC ELD"
                        AudioFormat.ENCODING_AAC_HE_V1      -> "AAC HE V1"
                        AudioFormat.ENCODING_AAC_HE_V2      -> "AAC HE V2"
                        AudioFormat.ENCODING_AAC_LC         -> "AAC LC"
                        AudioFormat.ENCODING_AAC_XHE        -> "AAC XHE"
                        AudioFormat.ENCODING_AC3            -> "AC3"
                        AudioFormat.ENCODING_AC4            -> "AC4"
                        AudioFormat.ENCODING_DEFAULT        -> "Default"
                        AudioFormat.ENCODING_DOLBY_MAT      -> "Dolby Mat"
                        AudioFormat.ENCODING_DOLBY_TRUEHD   -> "Dolby True HD"
                        AudioFormat.ENCODING_DTS            -> "DTS"
                        AudioFormat.ENCODING_DTS_HD         -> "DTS hd"
                        AudioFormat.ENCODING_E_AC3          -> "E AC3"
                        AudioFormat.ENCODING_E_AC3_JOC      -> "E AC3 JOC"
                        AudioFormat.ENCODING_IEC61937       -> "IEC61937"
                        AudioFormat.ENCODING_MP3            -> "MP3"
                        AudioFormat.ENCODING_PCM_16BIT      -> "PCM 16 bit"
                        AudioFormat.ENCODING_PCM_8BIT       -> "PCM 8 bit"
                        AudioFormat.ENCODING_PCM_FLOAT      -> "PCM Float"
                        AudioFormat.ENCODING_INVALID    -> getString(string.invalid)
                            .also { DebugMode.assertState(false) }
                        else                            -> getString(string.invalid)
                            .also { DebugMode.assertState(false) }
                    })
                } ?: ""
            } else getString(string.tapStop)
        }
    }

    init {
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun stopThread() = schedule.shutdown()

    /* TODO: Emulator 2.7 Q VGA API 24
        ScheduledExecutorService called just once, then msg = "0 min : 1 sec" forever
        However, 3GP-recording duration is correct */
    override fun run(): Unit = (SystemClock.uptimeMillis() - startTime).let { milSec ->
        DebugMode.assertState(recDlg.get() != null)
        recDlg.get()?.setMessage(
            "$fmtMsg\n\nTime: ${MinSec.minutes(milSec)} min : ${MinSec.seconds(milSec)} sec"
        )
    }

    fun start(): Unit = run { schedule.scheduleWithFixedDelay(this, 0, 500, TimeUnit.MILLISECONDS) }
}