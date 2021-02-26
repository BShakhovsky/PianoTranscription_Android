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
import ru.bshakhovsky.piano_transcription.utils.DebugMode

import java.nio.ByteOrder
import java.nio.channels.FileChannel

import kotlin.math.absoluteValue

class WaveGraph : Graphs() {

    init {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "Raw wave graph should be initialized by MediaActivity UI-thread"
        )
        super.initialize(
            10, Transformations.map(graphDrawable)
            { if (it.bitmap == null) View.VISIBLE else View.GONE }
        )
    }

    @WorkerThread
    suspend fun drawWave(rawData: FileChannel) {
        DebugMode.assertState(
            Looper.myLooper() != Looper.getMainLooper(),
            "Raw wave graph should be drawn in background thread"
        )
        if (graphDrawable.value == null) Bitmap.createBitmap(
            DecodeRoutine.sampleRate, scale * 2, Bitmap.Config.RGB_565
        ).let { bitmap ->
            with(rawData) {
                with(FloatArray(minOf(bitmap.width, (withContext(Dispatchers.IO)
                { @Suppress("BlockingMethodInNonBlockingContext") size() } / 4).toInt()))) {
                    withContext(Dispatchers.IO) {
                        @Suppress("BlockingMethodInNonBlockingContext")
                        map(FileChannel.MapMode.READ_ONLY, 0, size * 4L)
                    }.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(this)
                    with(Canvas(bitmap)) {
                        drawColor(Color.WHITE)
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
            }
            DebugMode
                .assertState(graphDrawable.value == null, "Unnecessary second bitmap creation")
            withContext(Dispatchers.Main) { _graphBitmap.value = bitmap }
        }
    }
}