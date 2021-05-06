package ru.bshakhovsky.piano_transcription.main.play.realtime

import android.app.Activity
import android.app.Application
import android.os.Looper
import androidx.annotation.MainThread

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.VmAppContext
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

abstract class RealTime(application: Application) :
    Runnable, VmAppContext(application), LifecycleObserver {

    protected lateinit var activity: WeakPtr<Activity>

    private var thread: Thread? = null

    protected fun initialize(lifecycle: Lifecycle, a: Activity) {
        lifecycle.addObserver(this)
        activity = WeakPtr(lifecycle, a)
    }

    @MainThread
    protected fun startRealTime() {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "Realtime should be started from MainActivity UI-thread"
        )
        DebugMode.assertState(thread == null, "Realtime started twice")
        thread = Thread(this).apply { start() }
    }

    // Both threads, because usually stops twice, second time due to InterruptedException
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    protected fun stopRealTime() {
        thread?.interrupt() // Already null for the second call
        thread = null
    }
}