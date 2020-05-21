package ru.bshakhovsky.piano_transcription.openGL.shader

import android.content.Context
import android.opengl.GLES32
import ru.bshakhovsky.piano_transcription.openGL.Texture
import ru.bshakhovsky.piano_transcription.openGL.Utils

class TextureShader(context: Context) : Shader(context, "2D") {

    companion object {
        fun sendTexture(
            textures: Texture, buffNo: Int, frameBuff: Int, texPos: Int, toReflect: Boolean = false
        ) {
            GLES32.glEnableVertexAttribArray(texPos)

            GLES32.glActiveTexture(GLES32.GL_TEXTURE0 + buffNo)
            textures.bindTexture(buffNo)
            GLES32.glUniform1i(frameBuff, buffNo)
            GLES32.glVertexAttribPointer(
                texPos, 2, GLES32.GL_FLOAT, false, 0, Utils.allocFloat(
                    @Suppress("LongLine", "Reformat")
                    if (toReflect)  floatArrayOf(1f, 0f,  0f, 0f,  1f, 1f,    1f, 1f,  0f, 0f,  0f, 1f)
                    else            floatArrayOf(0f, 0f,  1f, 0f,  0f, 1f,    0f, 1f,  1f, 0f,  1f, 1f)
                )
            )
        }
    }

    private val texPos = attribute("texturePos")
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