package ru.bshakhovsky.piano_transcription.main.openGL.shader

import android.content.res.AssetManager
import android.opengl.GLES32

import ru.bshakhovsky.piano_transcription.main.openGL.Texture
import ru.bshakhovsky.piano_transcription.main.openGL.Utils
import ru.bshakhovsky.piano_transcription.main.openGL.geometry.Geometry
import ru.bshakhovsky.piano_transcription.utils.DebugMode

class TextureShader(assets: AssetManager) : Shader(assets, "2D") {

    companion object {
        fun sendTexture(
            textures: Texture, buffNo: Int, frameBuff: Int, texPos: Int, toReflect: Boolean = false
        ) {
            GLES32.glActiveTexture(GLES32.GL_TEXTURE0 + buffNo)
            textures.bindTexture(buffNo)
            GLES32.glUniform1i(frameBuff, buffNo)
            GLES32.glVertexAttribPointer(
                texPos, 2, GLES32.GL_FLOAT, false, 0, Utils.allocFloat(
                    @Suppress("LongLine", "Reformat")
                    (if (toReflect) floatArrayOf(1f, 0f,  0f, 0f,  1f, 1f,    1f, 1f,  0f, 0f,  0f, 1f)
                    else            floatArrayOf(0f, 0f,  1f, 0f,  0f, 1f,    0f, 1f,  1f, 0f,  1f, 1f))
                        /* Texture array must have the same number of elements
                            as the number of vertices in the current primitive,
                            even though we always use just the first 6 of them
                            (2 triangles for 2D-texture).
                        Otherwise, random native OpenGL crash on startup with one of
                            the following errors in libGLES*.so --> libc.so --> __memcpy:
                                Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR)
                                Fatal signal 11 (SIGSEGV), code 2 (SEGV_ACCERR)
                        It is easier to always resize the array
                            to the maximum possible number of vertices (white key),
                            multiplied by 2 cords (x/y or u/v): */
                        .copyOf(Geometry.maxVertices * 2).also { texArray ->
                            with(texArray) {
                                DebugMode.assertArgument(
                                    (size == Geometry.maxVertices * 2) and
                                            sliceArray(12..lastIndex).all { it == 0f }
                                )
                            }
                        }
                )
            )
        }
    }

    private val texPos = attribute("texturePos").also { GLES32.glEnableVertexAttribArray(it) }
    private val frameBuff = uniform("frameBuff")

    fun draw(textures: Texture, buffNo: Int, toReflect: Boolean = false) {
        GLES32.glClear(GLES32.GL_DEPTH_BUFFER_BIT)
        use()
        sendTexture(textures, buffNo, frameBuff, texPos, toReflect)
        GLES32.glVertexAttribPointer(
            pos, 2, GLES32.GL_FLOAT, false, 0, Utils.allocFloat(
                @Suppress("Reformat")
                floatArrayOf(-1f, -1f,   1f, -1f,   -1f, 1f,        -1f, 1f,   1f, -1f,   1f, 1f)
            )
        )
        GLES32.glDrawArrays(GLES32.GL_TRIANGLES, 0, 6)
    }
}