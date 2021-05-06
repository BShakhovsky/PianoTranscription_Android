package ru.bshakhovsky.piano_transcription.main.play.realtime

import android.app.Activity
import android.app.Application
import android.os.Looper
import android.widget.ImageButton

import androidx.annotation.MainThread
import androidx.annotation.WorkerThread

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent

import ru.bshakhovsky.piano_transcription.R.string.recognizing

import ru.bshakhovsky.piano_transcription.main.openGL.Render
import ru.bshakhovsky.piano_transcription.media.utils.TfLiteModel

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage
import ru.bshakhovsky.piano_transcription.utils.MicPermission
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

class TranscribeRT(application: Application) : RealTime(application) {

    private lateinit var next: WeakPtr<ImageButton>
    private lateinit var render: Render
    private lateinit var record: RecordRT

    private val _isRecognizing = MutableLiveData(false)
    val isRecognizing: LiveData<Boolean> get() = _isRecognizing

    fun initialize(
        application: Application, lifecycle: Lifecycle, activity: Activity,
        nxt: ImageButton, r: Render
    ) {
        super.initialize(lifecycle, activity)
        next = WeakPtr(lifecycle, nxt)
        render = r
        record = RecordRT(application).apply { initialize(lifecycle, activity) }
    }

    @MainThread
    fun toggle() {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "Realtime recognition should be toggled from MainActivity UI-thread"
        )
        MicPermission.requestPermission(MicPermission.RecPermission.RECOGNIZE.id, activity.get())
        { if (isRecognizing.value == false) startRecognizing() else stopRecognizing() }
    }

    @MainThread
    private fun startRecognizing() {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "Realtime recognition should be started from MainActivity UI-thread"
        )
        if (!record.recStart()) {
            stopRecognizing()
            return
        }
        super.startRealTime()
        _isRecognizing.value = true
        if (render.trueChord.isEmpty()) next.get().performClick()
        InfoMessage.toast(appContext(), recognizing)
    }

    // Both threads, because usually stops twice, second time due to InterruptedException
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    private fun stopRecognizing() {
        record.recStop()
        super.stopRealTime()

        if (isRecognizing.value == true) // Activity can be destroyed by this time
            activity.get().runOnUiThread { _isRecognizing.value = false }
    }

    @WorkerThread
    override fun run(): Unit = TfLiteModel().apply { initialize(appContext()) }.use { model ->
        with(record) {
            while (totalBuf != null) {
                model.process(totalBuf ?: return@use).first.let { frames ->
                    (0..87).filter { note ->
                        frames.filterIndexed { i, _ -> i % 88 == note }.maxOrNull()
                            ?.let { it >= TfLiteModel.threshold } ?: false
                    }.let { userChord ->
                        with(render) {
                            unHighlightAll()
                            with(trueChord) {
                                if (isNotEmpty() and (userChord.containsAll(this))) {
                                    forEach { highlightKey(it, true) }
                                    try {
                                        Thread.sleep(300)
                                    } catch (e: InterruptedException) {
                                        stopRecognizing()
                                    }
                                    activity.get().runOnUiThread { next.get().performClick() }
                                } else userChord.forEach {
                                    when (it) {
                                        in this -> highlightKey(it, true)
                                        !in prevChord -> highlightKey(it, false)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}