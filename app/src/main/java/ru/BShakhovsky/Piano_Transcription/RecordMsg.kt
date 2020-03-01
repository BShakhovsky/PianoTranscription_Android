@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription

import android.content.Context
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import androidx.appcompat.app.AlertDialog

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

import kotlin.math.roundToInt

class RecordMsg(private val recDlg: AlertDialog, record: MediaRecorder, context: Context) :
    Runnable {

    private var schedule: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private val startTime = SystemClock.uptimeMillis()
    private val fmtMsg = record.run {
        with(context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                DebugMode.assertState(
                    (activeRecordingConfiguration != null)
                            and (activeRecordingConfiguration?.format != null)
                )
                activeRecordingConfiguration?.format?.run {
                    context.getString(
                        R.string.recFormat,
                        with(sampleRate / 1_000f) {
                            when (this) {
                                toInt().toFloat() -> "%d".format(this.toInt())
                                else -> "%.1f".format(this)
                            }
                        },
                        getString(
                            when {
                                channelCount == 1 -> R.string.mono
                                channelCount > 1 -> R.string.stereo
                                else -> {
                                    DebugMode.assertState(false)
                                    R.string.invalidChannels
                                }
                            }
                        ), @Suppress("Reformat") when (encoding) {
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
                            AudioFormat.ENCODING_INVALID -> {
                                DebugMode.assertState(false)
                                getString(R.string.invalid)
                            }
                            else -> {
                                DebugMode.assertState(false)
                                getString(R.string.invalid)
                            }
                        }
                    )
                } ?: ""
            } else getString(R.string.tapStop)
        }
    }

    override fun run() {
        (SystemClock.uptimeMillis() - startTime).also { milSec ->
            recDlg.setMessage(
                "$fmtMsg\n\nTime: ${milSec / 60_000} min : ${
                ((milSec % 60_000) / 1_000f).roundToInt()} sec"
            )
        }
        schedule.schedule(this, 500, TimeUnit.MILLISECONDS)
    }

    fun start(): Unit = schedule.execute(this)
    fun stop(): Unit = schedule.shutdown()
}
