@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL.Shader

import android.content.Context
import android.opengl.GLES31
import android.opengl.GLException
import android.opengl.Matrix
import ru.BShakhovsky.Piano_Transcription.BuildConfig
import java.io.InputStreamReader

abstract class Shader(context: Context, name: String) {

    protected val pos: Int
    private val program = GLES31.glCreateProgram()

    init {
        fun attachShader(type: Int, glslName: String) = GLES31.glAttachShader(program,
            GLES31.glCreateShader(type).also { shader ->
                GLES31.glShaderSource(shader, InputStreamReader(
                    context.assets.open("Shader/$glslName.glsl")).readText())
                GLES31.glCompileShader(shader)
                if (BuildConfig.DEBUG) GLES31.glGetShaderInfoLog(shader).also { err ->
                    if (err.isNotEmpty()) throw GLException(0, "Shader compile: $err") }
            })

        attachShader(GLES31.GL_VERTEX_SHADER, "Vertex$name")
        attachShader(GLES31.GL_FRAGMENT_SHADER, "Pixel$name")
        GLES31.glLinkProgram(program)
        pos = attribute("pos")
        GLES31.glEnableVertexAttribArray(pos)
    }

              fun uniform  (name: String) = GLES31.glGetUniformLocation(program, name)
    protected fun attribute(name: String) = GLES31.glGetAttribLocation (program, name)

    protected fun use() { GLES31.glUseProgram(program) }

    protected fun translate(matrix: FloatArray, matHandle: Int, offset: Float = 0f) { FloatArray(16).also { matOffset ->
        Matrix.translateM(matOffset, 0, matrix, 0, offset, 0f, 0f)
        GLES31.glUniformMatrix4fv(matHandle, 1, false, matOffset, 0)
    } }
}