package ru.bshakhovsky.piano_transcription.main.play.realtime

import android.app.Application

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder

import android.os.Looper
import androidx.annotation.MainThread
import androidx.annotation.WorkerThread

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent

import ru.bshakhovsky.piano_transcription.R.string

import ru.bshakhovsky.piano_transcription.media.background.DecodeRoutine
import ru.bshakhovsky.piano_transcription.media.utils.TfLiteModel
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage

import java.lang.IllegalStateException

class RecordRT(application: Application) : RealTime(application) {

    private var record: AudioRecord? = null

    private var floatBuf: FloatArray? = null
    var totalBuf: FloatArray? = null
        private set

    @MainThread
    fun recStart(): Boolean {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "Realtime recording should be started from MainActivity UI-thread"
        )
        DebugMode.assertState(record == null, "Realtime recording started twice")
        if (record == null) {
            AudioRecord.getMinBufferSize(
                DecodeRoutine.sampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_FLOAT
            ).also {
                if (!arrayOf(AudioRecord.ERROR, AudioRecord.ERROR_BAD_VALUE).contains(it))
                    record = AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION, DecodeRoutine.sampleRate,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_FLOAT, it
                    ).apply { floatBuf = FloatArray(bufferSizeInFrames) }
            }
        }

        if ((record == null) or (record?.state != AudioRecord.STATE_INITIALIZED)
            or (floatBuf == null)
        ) {
            InfoMessage.dialog(activity.get(), string.error, string.prepError)
            recStop()
            return false
        }

        DebugMode.assertState( // TODO: Somehow it fails sometimes
            totalBuf == null, "Previous recording should have been cleared before recording again"
        )
        totalBuf = FloatArray(TfLiteModel.inNumSamples)
        super.startRealTime()
        DebugMode.assertState(record != null, "Should have already returned from this function")
        record?.run { startRecording() }

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
            (record != null) and (floatBuf != null),
            "Realtime recording should have been cancelled if failed to initialize"
        )
        floatBuf?.let { buf ->
            while ((record != null) and (totalBuf != null)) record?.run {
                if (arrayOf(AudioRecord.ERROR_INVALID_OPERATION, AudioRecord.ERROR_BAD_VALUE)
                        .contains(read(buf, 0, buf.size, AudioRecord.READ_BLOCKING))
                ) recStop() // InterruptedException
                totalBuf = totalBuf?.let { // null when stopping
                    (it + buf).run { sliceArray(size - TfLiteModel.inNumSamples..lastIndex) }
                }
            }
        }
    }
}