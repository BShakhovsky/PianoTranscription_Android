package ru.bshakhovsky.piano_transcription.media.utils

import android.content.Context
import android.os.Looper

import androidx.annotation.CheckResult
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import org.tensorflow.lite.DataType
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.support.model.Model
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

import ru.bshakhovsky.piano_transcription.R.string

import ru.bshakhovsky.piano_transcription.ml.OnsetsFramesWavinput
import ru.bshakhovsky.piano_transcription.utils.DebugMode

import java.io.Closeable

class TfLiteModel : Closeable {

    companion object {
        const val inNumSamples: Int = 17_920
        const val outStepNotes: Int = 32
        const val threshold: Float = 0f
    }

    private lateinit var model: OnsetsFramesWavinput
    private val tensBuff = TensorBuffer.createFixedSize(intArrayOf(inNumSamples), DataType.FLOAT32)

    @WorkerThread
    fun initialize(context: Context) {
        DebugMode.assertState(
            Looper.myLooper() != Looper.getMainLooper(),
            "TfLiteModel should be instantiated in background thread"
        )
        DebugMode.assertState(!::model.isInitialized)
        model = OnsetsFramesWavinput.newInstance(context, Model.Options.Builder().apply {
            @Suppress("SpellCheckingInspection")
            /* https://stackoverflow.com/questions/59968239/
            performance-of-gpu-delegate-nnapi-of-tensorflow-lite-are-almost-the-same-on-and

            NNAPI only works on Android 8.1 or later
            NNAPI may not accelerate on all architectures
            GPU delegate does not support quantized models yet

            java.lang.IllegalArgumentException: Internal error: Failed to apply delegate:
                Following operations are not supported by GPU delegate:
                    GATHER: Operation is not supported.
                    PAD: Invalid paddings tensor shape: expected 4x2 or 3x2, got 2x2
                    SPLIT: Operation is not supported.

                    72 operations will run on the GPU,
                        and the remaining 2658 operations will run on the CPU.

                TfLiteGpuDelegate Init: CONCATENATION:
                    Expected a 4D tensor of shape BxHxWxC but got 1x256
                TfLiteGpuDelegate Prepare: delegate is not initialized

                Node nu
                Node number 2730 (TfLiteGpuDelegateV2) fa

                https://github.com/tensorflow/tensorflow/issues/20187#issuecomment-399642887
                Whether increasing the number of threads can get performance gain
                    is actually system and workload dependent.
                I guess 4 is there just as a convenient hack.
                On most modern cell phones, 4 should work pretty well, except ... */

//            if (CompatibilityList().isDelegateSupportedOnThisDevice)
//                setDevice(Model.Device.GPU) else
            setNumThreads(4)
        }.build())
    }

    @WorkerThread
    suspend fun initialize(context: Context, ffmpegLog: MutableLiveData<String>) {
        initialize(context)
        withContext(Dispatchers.Main) {
            ffmpegLog.value += "\n${
                with(context) {
                    getString(
                        string.gpuCompat,
                        getString(
                            if (CompatibilityList().isDelegateSupportedOnThisDevice)
                                string.compat else string.inCompat
                        )
                    )
                }
            }"
        }
//        withContext(Dispatchers.Main)
//        <string name="numThreads">Processing using %1$d CPU-threads</string>
//        { ffmpegLog.value += "\n${getString(string.numThreads, numThreads)}" }
    }

    @WorkerThread
    override fun close(): Unit = DebugMode.assertState(
        Looper.myLooper() != Looper.getMainLooper(),
        "TfLiteModel should be closed in background thread"
    ).let { model.close() }

    @WorkerThread
    @CheckResult
            /**
             * @return frames, onsets, volumes
             */
    fun process(input: FloatArray): Triple<FloatArray, FloatArray, FloatArray> {
        DebugMode.assertState(
            Looper.myLooper() != Looper.getMainLooper(),
            "TfLiteModel should be processed in background thread"
        )
        tensBuff.loadArray(input)
        with(model.process(tensBuff)) {
            val (frames, onsets, offsets, volumes) = arrayOf(
                outputFeature0AsTensorBuffer, outputFeature1AsTensorBuffer,
                outputFeature2AsTensorBuffer, outputFeature3AsTensorBuffer
            )
            DebugMode.assertArgument(
                frames.shape.contentEquals(intArrayOf(1, outStepNotes, 88))
                        and onsets.shape.contentEquals(intArrayOf(1, outStepNotes, 88))
                        and offsets.shape.contentEquals(intArrayOf(1, outStepNotes, 88))
                        and volumes.shape.contentEquals(intArrayOf(1, outStepNotes, 88))
            )
            return Triple(frames.floatArray.mapIndexed { i, f -> maxOf(onsets.floatArray[i], f) }
                .toFloatArray(), onsets.floatArray, volumes.floatArray)
        }
    }
}