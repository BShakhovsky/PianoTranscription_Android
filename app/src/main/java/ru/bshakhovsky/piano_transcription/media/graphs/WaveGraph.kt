package ru.bshakhovsky.piano_transcription.media.graphs

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

import android.os.Looper
import android.view.View

import androidx.annotation.WorkerThread
import androidx.lifecycle.Transformations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import ru.bshakhovsky.piano_transcription.media.background.DecodeRoutine
import ru.bshakhovsky.piano_transcription.media.utils.RandomFileArray
import ru.bshakhovsky.piano_transcription.utils.DebugMode

import kotlin.math.absoluteValue

class WaveGraph : Graphs() {

    init {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "Raw wave graph should be initialized by MediaActivity UI-thread"
        )
        super.initialize(
            10, Transformations.map(graphBitmap) { if (it == null) View.VISIBLE else View.GONE }
        )
    }

    @WorkerThread
    suspend fun drawWave(rawWave: RandomFileArray) {
        DebugMode.assertState(
            Looper.myLooper() != Looper.getMainLooper(),
            "Raw wave graph should be drawn in background thread"
        )
        if (graphDrawable.value == null) Bitmap.createBitmap(
            DecodeRoutine.sampleRate, scale * 2, Bitmap.Config.ARGB_8888
        ).let { bitmap ->
            with(rawWave.getArray(bitmap.width)) {
                with(Canvas(bitmap)) {
                    with(maxByOrNull { it.absoluteValue }) {
                        when (this) {
                            null -> 1f.also { DebugMode.assertState(false) }
                            0f -> 1f
                            else -> absoluteValue.also { DebugMode.assertState(it < 1.44) }
                        }
                    }.also { maxWave ->
                        slice(0 until lastIndex).forEachIndexed { index, value ->
                            drawLine(
                                index.toFloat(), value / maxWave * scale + scale,
                                index + 1f, get(index + 1) / maxWave * scale + scale,
                                Paint().apply { color = Color.BLUE }
                            )
                        }
                    }
                }
            }
            DebugMode
                .assertState(graphDrawable.value == null, "Unnecessary second bitmap creation")
            withContext(Dispatchers.Main) { _graphBitmap.value = bitmap }
        }
    }
}