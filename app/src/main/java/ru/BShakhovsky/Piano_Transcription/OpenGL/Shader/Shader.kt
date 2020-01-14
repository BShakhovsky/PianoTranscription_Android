@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL.Shader

import android.content.Context
import android.opengl.GLES32
import android.opengl.GLException
import android.opengl.Matrix
import ru.BShakhovsky.Piano_Transcription.BuildConfig
import java.io.InputStreamReader

abstract class Shader(context: Context, name: String) {

    protected val pos: Int
    private val program = GLES32.glCreateProgram()

    init {
        fun attachShader(type: Int, glslName: String) = GLES32.glAttachShader(program,
            GLES32.glCreateShader(type).also { shader ->
                GLES32.glShaderSource(shader, InputStreamReader(
                    context.assets.open("Shader/$glslName.glsl")).readText())
                GLES32.glCompileShader(shader)
                if (BuildConfig.DEBUG) GLES32.glGetShaderInfoLog(shader).also { err ->
                    if (err.isNotEmpty()) throw GLException(0, "Shader compile: $err") }
            })

        attachShader(GLES32.GL_VERTEX_SHADER, "Vertex$name")
        attachShader(GLES32.GL_FRAGMENT_SHADER, "Pixel$name")
        GLES32.glLinkProgram(program)
        pos = attribute("pos")
        GLES32.glEnableVertexAttribArray(pos)
    }

              fun uniform  (name: String) = GLES32.glGetUniformLocation(program, name)
    protected fun attribute(name: String) = GLES32.glGetAttribLocation (program, name)

    protected fun use() { GLES32.glUseProgram(program) }

    protected fun translate(matrix: FloatArray, matHandle: Int, offset: Float = 0f) { FloatArray(16).also { matOffset ->
        Matrix.translateM(matOffset, 0, matrix, 0, offset, 0f, 0f)
        GLES32.glUniformMatrix4fv(matHandle, 1, false, matOffset, 0)
    } }
}