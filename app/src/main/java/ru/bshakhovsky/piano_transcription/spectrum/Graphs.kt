package ru.bshakhovsky.piano_transcription.spectrum

import android.content.res.Resources

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable

import android.view.View

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel

import ru.bshakhovsky.piano_transcription.utils.DebugMode

import java.nio.ByteOrder
import java.nio.channels.FileChannel

import kotlin.math.absoluteValue

class Graphs : ViewModel() {

    companion object {
        private const val waveScale = 10
    }

    private val _waveGraph = MutableLiveData<BitmapDrawable>()
    val waveGraph: LiveData<BitmapDrawable>
        get() = _waveGraph

    val waveVis: LiveData<Int> = Transformations.map(waveGraph)
    { if (it.bitmap == null) View.VISIBLE else View.GONE }

    override fun onCleared(): Unit = waveGraph.value?.bitmap?.recycle().let { super.onCleared() }

    fun drawWave(rawData: FileChannel, resources: Resources): Unit = waveGraph.value?.let {}
        ?: Bitmap.createBitmap(
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
                DebugMode.assertState(waveGraph.value == null, "Unnecessary second bitmap creation")
                _waveGraph.value = BitmapDrawable(resources, bitmap)
            }
        }
}