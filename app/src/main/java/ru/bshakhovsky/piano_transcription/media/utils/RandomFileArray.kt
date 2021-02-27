package ru.bshakhovsky.piano_transcription.media.utils

import androidx.annotation.CheckResult

import ru.bshakhovsky.piano_transcription.utils.DebugMode

import java.nio.ByteOrder
import java.nio.channels.FileChannel

class RandomFileArray {

    var file: FileChannel? = null

    @CheckResult
    fun floatLen(): Int = (file!!.size() / 4).toInt()

    @CheckResult
    fun getArray(maxSize: Int = Int.MAX_VALUE): FloatArray =
        FloatArray(minOf(maxSize, floatLen())).apply {
            DebugMode.assertState(file != null)
            (file ?: return@apply).map(FileChannel.MapMode.READ_ONLY, 0, size * 4L)
                .order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(this)
        }

    @CheckResult
    fun getZeroPadded(biggerSize: Int): FloatArray = getArray().copyOf(biggerSize).also { padArr ->
        DebugMode.assertArgument(biggerSize > floatLen())
        DebugMode.assertState(padArr.slice(floatLen()..padArr.lastIndex).all { it == 0f })
    }
}