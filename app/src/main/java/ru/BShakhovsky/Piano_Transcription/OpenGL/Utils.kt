@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

object Utils {

    fun allocFloat(cords : FloatArray) : FloatBuffer =
        ByteBuffer.allocateDirect(cords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply { put(cords); position(0) }
        }
}