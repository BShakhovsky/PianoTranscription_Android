package ru.bshakhovsky.piano_transcription.media.graphs

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Looper

import androidx.annotation.MainThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import ru.bshakhovsky.piano_transcription.utils.DebugMode

open class Graphs : ViewModel() {

    protected var scale: Int = 0

    @Suppress("PropertyName")
    protected val _graphBitmap: MutableLiveData<Bitmap> = MutableLiveData()
    val graphBitmap: LiveData<Bitmap>
        get() = _graphBitmap

    val graphDrawable: MutableLiveData<BitmapDrawable> = MutableLiveData()

    lateinit var isVisible: LiveData<Int>
        private set

    @MainThread
    protected fun initialize(s: Int, vis: LiveData<Int>) {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "Graphs should be initialized by MediaActivity UI-thread"
        )
        scale = s
        isVisible = vis
    }

    @MainThread
    override fun onCleared(): Unit = DebugMode.assertState(
        Looper.myLooper() == Looper.getMainLooper(),
        "Graphs should be cleared by MediaActivity UI-thread"
    ).let { graphDrawable.value?.bitmap?.recycle().let { super.onCleared() } }
}