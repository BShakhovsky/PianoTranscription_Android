package ru.bshakhovsky.piano_transcription.media

import android.app.Activity
import android.net.Uri
import android.view.animation.AnimationUtils
import android.widget.FrameLayout

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

import ru.bshakhovsky.piano_transcription.R.anim.anim_graph
import ru.bshakhovsky.piano_transcription.R.string

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

import kotlin.io.path.ExperimentalPathApi

class DecodeThread(
    lifecycle: Lifecycle, a: Activity, w: FrameLayout,// s: FrameLayout,
    private val rawAudio: RawAudio, private val graphs: Graphs, private val uri: Uri?
) : Runnable, LifecycleObserver {

    private val activity = WeakPtr(lifecycle, a)
    private val waveGraph = WeakPtr(lifecycle, w)
//    private val spectrum = WeakPtr(lifecycle, s)

    private val thread = Thread(this).apply { start() }

    init {
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun cancelDecoding() = thread.interrupt()

    @ExperimentalPathApi
    override fun run() {
        /* TODO: Increase delay (dirty hack during opening and decoding file
            to show activity UI (frozen though) instead of black screen) */
        Thread.sleep(500)
        activity.get().runOnUiThread {
            with(rawAudio) {
                if (rawData == null) {
                    DebugMode.assertState(
                        graphs.waveGraph.value == null, "Unnecessary second FFmpeg call"
                    )
                    DebugMode.assertState(uri != null)
                    uri?.let { decode(it) }
                    waveGraph.get()
                        .startAnimation(AnimationUtils.loadAnimation(activity.get(), anim_graph))
                }
                try {
                    rawData?.let { graphs.drawWave(it, activity.get().resources) }
                } catch (e: OutOfMemoryError) {
                    with(activity.get()) {
                        getString(
                            string.memoryRawGraph,
                            e.localizedMessage ?: e
                        ).let { errMsg ->
                            ffmpegLog.value += "\n\n$errMsg"
                            InfoMessage.dialog(this, string.error, errMsg)
                        }
                    }
                }
            }
        }
    }
}