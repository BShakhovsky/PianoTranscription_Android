package ru.bshakhovsky.piano_transcription.main.openGL.shader

import android.content.res.AssetManager
import android.opengl.GLException
import android.opengl.Matrix

import androidx.annotation.CheckResult

import ru.bshakhovsky.piano_transcription.main.openGL.GLES
import ru.bshakhovsky.piano_transcription.utils.DebugMode

import java.io.InputStreamReader

abstract class Shader(assets: AssetManager, name: String) {

    protected val pos: Int
    private val program = GLES.glCreateProgram()

    init {
        fun attachShader(type: Int, glslName: String) = GLES.glAttachShader(program,
            GLES.glCreateShader(type).also { shader ->
                GLES.glShaderSource(
                    shader, InputStreamReader(assets.open("Shader/$glslName.glsl")).readText()
                )
                GLES.glCompileShader(shader)
                if (DebugMode.debug) IntArray(1).let { status ->
                    GLES.glGetShaderiv(shader, GLES.GL_COMPILE_STATUS, status, 0)
                    if (status[0] != GLES.GL_TRUE)
                        throw GLException(0, "Shader compile: ${GLES.glGetShaderInfoLog(shader)}")
                }
            })

        attachShader(GLES.GL_VERTEX_SHADER, "Vertex$name")
        attachShader(GLES.GL_FRAGMENT_SHADER, "Pixel$name")
        GLES.glLinkProgram(program)
        pos = attribute("pos")
        GLES.glEnableVertexAttribArray(pos)
    }

    @CheckResult
    fun uniform(name: String): Int = GLES.glGetUniformLocation(program, name)

    @CheckResult
    protected fun attribute(name: String): Int = GLES.glGetAttribLocation(program, name)

    protected fun use(): Unit = GLES.glUseProgram(program)

    protected fun shiftRotate(
        matrix: FloatArray, matHandle: Int, offset: Float = 0f, angle: Float = 0f
    ): Unit = matrix.copyOf().let { mat ->
        if (angle != 0f) Matrix.rotateM(mat, 0, angle, 1f, 0f, 0f)
        if (offset != 0f) Matrix.translateM(mat, 0, offset, 0f, 0f)
        GLES.glUniformMatrix4fv(matHandle, 1, false, mat, 0)
    }
}