@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.Spectrum

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

import ru.BShakhovsky.Piano_Transcription.Utils.DebugMode
import kotlin.math.absoluteValue

class Graphs {

    companion object {
        private const val waveScale = 10
    }

    var waveGraph: Bitmap? = null
        private set

    fun drawWave(rawData: FloatArray): Unit = waveGraph?.let {} ?: Bitmap.createBitmap(
        RawAudio.sampleRate, waveScale * 2, Bitmap.Config.RGB_565
    ).let { bitmap ->
        with(rawData) { slice(0 until minOf(bitmap.width, size) - 1) }.also { sliced ->
            with(Canvas(bitmap)) {
                drawColor(Color.WHITE)
                sliced.maxBy { it.absoluteValue }.let {
                    when {
                        it == null -> {
                            DebugMode.assertState(false)
                            1f
                        }
                        it == 0f -> 1f
                        it < 2f -> it
                        else -> {
                            DebugMode.assertState(false)
                            it
                        }
                    }
                }.also { maxWave ->
                    sliced.forEachIndexed { index, value ->
                        drawLine(
                            index.toFloat(), value / maxWave * waveScale + waveScale,
                            index + 1f, rawData[index + 1] / maxWave * waveScale + waveScale,
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