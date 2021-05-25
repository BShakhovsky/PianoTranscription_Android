package ru.bshakhovsky.piano_transcription.main.openGL

import androidx.annotation.CheckResult

import ru.bshakhovsky.piano_transcription.main.openGL.geometry.Geometry
import ru.bshakhovsky.piano_transcription.utils.DebugMode

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

object Utils {

    @CheckResult
    fun allocFloat(cords: FloatArray): FloatBuffer = ByteBuffer.allocateDirect( /* It seems like
    there is no need for all buffers to be the same size, it runs fine on my device and emulators.
    But let's just in case have all the buffers of the same maximum size */
        (Geometry.maxVertices + 1) * 3 * 4 // 3 cords (x/y/z), 4 bytes in float, and +1 just in case
    ).order(ByteOrder.nativeOrder()).asFloatBuffer().put(cords).apply { position(0) }
        .also { DebugMode.assertArgument(cords.size <= Geometry.maxVertices * 3) }
}