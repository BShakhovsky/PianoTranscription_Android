package ru.bshakhovsky.piano_transcription.media.graphs

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

import android.os.Looper
import android.view.View

import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import ru.bshakhovsky.piano_transcription.utils.DebugMode

import kotlin.math.roundToInt

class RollGraph : Graphs() {

    val rollDur: MutableLiveData<String> = MutableLiveData()
    val isTranscribed: MutableLiveData<Boolean> = MutableLiveData(false)

    private var outOfMem = false

    init {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "Piano-roll graph should be initialized by MediaActivity UI-thread"
        )
        super.initialize(
            2, Transformations.map(isTranscribed) { if (it) View.GONE else View.VISIBLE }
        )
    }

    @WorkerThread
    suspend fun drawNextRoll(frames: FloatArray, onsets: FloatArray, threshold: Float) {
        DebugMode.assertState(
            Looper.myLooper() != Looper.getMainLooper(),
            "Piano-roll should be drawn in background thread"
        )
        if (outOfMem) return
        try {
            Bitmap.createBitmap(
                (graphBitmap.value?.width ?: 0) + frames.size / 88, 88 * scale,
                Bitmap.Config.ARGB_8888
            ).let { bitmap ->
                DebugMode.assertArgument(frames.size == onsets.size)
                with(Canvas(bitmap)) {
                    graphBitmap.value
                        ?.let { drawBitmap(it, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG)) }
                    frames.forEachIndexed { index, value ->
                        if (maxOf(value, onsets[index]) > threshold) drawCircle(
                            bitmap.width - frames.size / 88 + (index / 88).toFloat(),
                            (88f - index % 88) * scale, 1f,
                            Paint().apply {
                                color = Color.BLUE
                                alpha = (maxOf(value, onsets[index]) * 0xFF).roundToInt()
                            }
                        )
                    }
                }
                withContext(Dispatchers.Main) { _graphBitmap.value = bitmap }
            }
        } catch (e: OutOfMemoryError) {
            outOfMem = true
            throw e
        }
    }
}