@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.OpenGL

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

object Utils {

    fun allocFloat(cords: FloatArray): FloatBuffer = ByteBuffer.allocateDirect(cords.size * 4)
        .order(ByteOrder.nativeOrder()).asFloatBuffer().put(cords).apply { position(0) }
}