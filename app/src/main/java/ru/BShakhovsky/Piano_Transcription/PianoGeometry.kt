@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription

import android.opengl.GLES31
import java.nio.ByteBuffer
import java.nio.ByteOrder

class PianoGeometry {

    private val program = GLES31.glCreateProgram()

    private val squareCoords = floatArrayOf(
        -.5f,  .5f, 0f,     -.5f, -.5f, 0f,     .5f, -.5f, 0f,      .5f,  .5f, 0f)
    private val drawOrder = shortArrayOf(0, 1, 2, 0, 2, 3)
    private val vertexBuffer = ByteBuffer.allocateDirect(squareCoords.size * 4).run {
        order(ByteOrder.nativeOrder())
        asFloatBuffer().apply {
            put(squareCoords)
            position(0)
        }
    }
    private val drawListBuffer = ByteBuffer.allocateDirect(drawOrder.size * 2).run {
        order(ByteOrder.nativeOrder())
        asShortBuffer().apply {
            put(drawOrder)
            position(0)
        }
    }

    private val pos : Int

    init {
        fun attachShader(type: Int, shaderCode: String) = GLES31.glAttachShader(program,
            GLES31.glCreateShader(type).also { shader ->
                GLES31.glShaderSource(shader, shaderCode)
                GLES31.glCompileShader(shader)
            })
        attachShader(GLES31.GL_VERTEX_SHADER, """
            attribute vec4 pos;
            uniform mat4 mvp;
            void main() {
                gl_Position = mvp * pos;
            }""")
        attachShader(GLES31.GL_FRAGMENT_SHADER, """
            precision mediump float;
            uniform vec4 color;
            void main() {
                gl_FragColor = color;
            }""")
        GLES31.glLinkProgram(program)
        GLES31.glUseProgram(program)
        GLES31.glEnable(GLES31.GL_CULL_FACE)

        pos = GLES31.glGetAttribLocation(program, "pos")
        GLES31.glVertexAttribPointer(
            pos, 3, GLES31.GL_FLOAT, false, 3 * 4, vertexBuffer)
        GLES31.glEnableVertexAttribArray(pos)

        GLES31.glGetUniformLocation(program, "color").also { color ->
            GLES31.glUniform4fv(color, 1, floatArrayOf(240 / 255f, 248 / 255f, 255 / 255f, 1f), 0)
        }
    }

    fun draw(mvp : FloatArray) {
        GLES31.glGetUniformLocation(program, "mvp").also { mvpHandle ->
            GLES31.glUniformMatrix4fv(mvpHandle, 1, false, mvp, 0) }

        GLES31.glDrawElements(GLES31.GL_TRIANGLES, 6, GLES31.GL_UNSIGNED_SHORT, drawListBuffer)
//        GLES31.glDisableVertexAttribArray(pos)
    }
}