package ru.bshakhovsky.piano_transcription.utils

import androidx.annotation.CheckResult

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

import java.lang.ref.WeakReference

class WeakPtr<T>(lifecycle: Lifecycle, ref: T) : LifecycleObserver {

    private val ptr = WeakReference(ref)

    init {
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun clearRef() {
        DebugMode.assertState(ptr.get() != null)
        ptr.clear()
        DebugMode.assertState(ptr.get() == null)
    }

    @CheckResult
    fun get(): T = DebugMode.assertState(ptr.get() != null).let { ptr.get()!! }
}