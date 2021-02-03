package ru.bshakhovsky.piano_transcription.main.openGL

import androidx.annotation.CheckResult

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

object Utils {

    @CheckResult
    fun allocFloat(cords: FloatArray): FloatBuffer = ByteBuffer.allocateDirect(cords.size * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().put(cords).apply { position(0) }
}