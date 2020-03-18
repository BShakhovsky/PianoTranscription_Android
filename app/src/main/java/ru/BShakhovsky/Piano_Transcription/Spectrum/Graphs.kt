@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.Spectrum

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

import ru.BShakhovsky.Piano_Transcription.Utils.DebugMode
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.absoluteValue

class Graphs {

    companion object {
        private const val waveScale = 10
    }

    var waveGraph: Bitmap? = null
        private set

    fun drawWave(rawData: FileChannel): Unit = waveGraph?.let {} ?: Bitmap.createBitmap(
        RawAudio.sampleRate, waveScale * 2, Bitmap.Config.RGB_565
    ).let { bitmap ->
        with(rawData) {
            with(FloatArray(minOf(bitmap.width, (size() / 4).toInt()))) {
                map(FileChannel.MapMode.READ_ONLY, 0, size * 4L)
                    .order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(this)
                with(Canvas(bitmap)) {
                    drawColor(Color.WHITE)
                    with(maxBy { it.absoluteValue }) {
                        when (this) {
                            null -> 1f.also { DebugMode.assertState(false) }
                            0f -> 1f
                            else -> absoluteValue.also { DebugMode.assertState(it < 1.44) }
                        }
                    }.also { maxWave ->
                        slice(0 until lastIndex).forEachIndexed { index, value ->
                            drawLine(
                                index.toFloat(), value / maxWave * waveScale + waveScale,
                                index + 1f, get(index + 1) / maxWave * waveScale + waveScale,
                                Paint().apply { color = Color.BLUE }
                            )
                        }
                    }
                }
            }
            DebugMode.assertState(waveGraph == null, "Unnecessary second bitmap creation")
            waveGraph = bitmap
        }
    }
}