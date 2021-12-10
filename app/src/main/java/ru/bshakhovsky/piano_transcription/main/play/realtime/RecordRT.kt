package ru.bshakhovsky.piano_transcription.main.play.realtime

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.pm.PackageManager

import android.media.AudioFormat
import android.media.AudioRecord

import android.os.Looper

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent

import ru.bshakhovsky.piano_transcription.R.string

import ru.bshakhovsky.piano_transcription.media.background.DecodeRoutine
import ru.bshakhovsky.piano_transcription.media.utils.TfLiteModel
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage
import ru.bshakhovsky.piano_transcription.utils.MicSource

import java.lang.IllegalStateException

class RecordRT(application: Application) : RealTime(application) {

    private var record: AudioRecord? = null

    private var byteBuf = byteArrayOf()
    private var shortBuf = shortArrayOf()
    var totalBuf: FloatArray? = null
        private set

    fun initRecord(lifecycle: Lifecycle, activity: Activity): Unit =
        super.initialize(lifecycle, activity)

    @MainThread
    fun recStart(): Boolean {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "Realtime recording should be started from MainActivity UI-thread"
        )
        with(activity.get()) {
            DebugMode.assertState(record == null, "Realtime recording started twice")
            if (record == null)
                for (format in arrayOf(
                    AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_8BIT
                )) {
                    val bufSize = AudioRecord.getMinBufferSize(
                        DecodeRoutine.sampleRate, AudioFormat.CHANNEL_IN_MONO, format
                    )
                    if (!arrayOf(AudioRecord.ERROR, AudioRecord.ERROR_BAD_VALUE)
                            .contains(bufSize)
                    ) {
                        if (ActivityCompat.checkSelfPermission(
                                applicationContext, Manifest.permission.RECORD_AUDIO
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            DebugMode.assertState(false)
                            InfoMessage.dialog(this, string.micError, string.grantRec)
                            recStop()
                            return false
                        }
                        record = AudioRecord(
                            MicSource.micSource, DecodeRoutine.sampleRate,
                            AudioFormat.CHANNEL_IN_MONO, format, bufSize
                        )
                        when (format) {
                            AudioFormat.ENCODING_PCM_8BIT -> byteBuf = ByteArray(bufSize)
                            AudioFormat.ENCODING_PCM_16BIT -> shortBuf = ShortArray(bufSize / 2)
                        }
                        break
                    }
                }

            if ((record == null) or (record?.state != AudioRecord.STATE_INITIALIZED)
                or (byteBuf.isEmpty() and shortBuf.isEmpty())
            ) {
                InfoMessage.dialog(
                    this, string.micError, getString(string.prepError, getString(string.noMic))
                )
                recStop()
                return false
            }

            DebugMode.assertState( // Somehow it fails sometimes
                totalBuf == null,
                "Previous recording should have been cleared before recording again"
            )
            totalBuf = FloatArray(TfLiteModel.inNumSamples)
            DebugMode.assertState(record != null, "Should have already returned from this function")
            record?.run {
                try {
                    startRecording()
                } catch (_: IllegalStateException) {
                    InfoMessage.toast(appContext(), string.notStarted)
                    recStop()
                    return false
                }
            }

            if (!nextRecBuf()) {
                InfoMessage.dialog(
                    this, string.micError, getString(string.prepError, getString(string.no16kHz))
                )
                recStop()
                return false
            }
        }

        super.startRealTime()
        return true
    }

    // Both threads, because usually stops twice, second time due to InterruptedException
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun recStop() {
        record?.run { // null when stopped due to Lifecycle Event
            try {
                stop()
            } catch (e: IllegalStateException) {
            } finally {
                release()
            }
        }
        record = null
        totalBuf = null

        super.stopRealTime()
    }

    @WorkerThread
    override fun run() {
        DebugMode.assertState(
            (record != null) and (byteBuf.isNotEmpty() or shortBuf.isNotEmpty()),
            "Realtime recording should have been cancelled if failed to initialize"
        )
        while ((record != null) and (totalBuf != null)) record?.run {
            if (!nextRecBuf()) recStop() // InterruptedException
            totalBuf = totalBuf?.let { total -> // null when stopping
                (total + (when {
                    shortBuf.isNotEmpty() -> shortBuf.map { it.toFloat() / Short.MAX_VALUE }
                    byteBuf.isNotEmpty() -> byteBuf.map { it.toFloat() / Byte.MAX_VALUE }
                    else -> listOf<Float>().also { DebugMode.assertState(false) }
                })).run { sliceArray(size - TfLiteModel.inNumSamples..lastIndex) }
            }
        }
    }

    private fun nextRecBuf() = record?.run {
        !arrayOf(AudioRecord.ERROR_INVALID_OPERATION, AudioRecord.ERROR_BAD_VALUE).contains(
            when {
                shortBuf.isNotEmpty() -> read(shortBuf, 0, shortBuf.size)
                else -> read(byteBuf, 0, byteBuf.size)
            }
        )
    } ?: false
}