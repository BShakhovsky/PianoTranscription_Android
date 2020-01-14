@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL.Shader

import android.content.Context
import android.opengl.GLES32
import ru.BShakhovsky.Piano_Transcription.OpenGL.Texture
import ru.BShakhovsky.Piano_Transcription.OpenGL.Utils

class TextureShader(context: Context) : Shader(context, "2D") {

    companion object { fun sendTexture(textures: Texture, buffNo: Int, frameBuff: Int, texPos: Int, toReflect: Boolean = false) {
        GLES32.glEnableVertexAttribArray(texPos)

        GLES32.glActiveTexture(GLES32.GL_TEXTURE0 + buffNo)
        textures.bindTexture(buffNo)
        GLES32.glUniform1i(frameBuff, buffNo)
        GLES32.glVertexAttribPointer(texPos, 2, GLES32.GL_FLOAT, false, 0,
            Utils.allocFloat(if (toReflect) floatArrayOf(1f, 0f,  0f, 0f,  1f, 1f,    1f, 1f,  0f, 0f,  0f, 1f)
                                       else floatArrayOf(0f, 0f,  1f, 0f,  0f, 1f,    0f, 1f,  1f, 0f,  1f, 1f)))
    } }

    private val texPos    = attribute("texturePos")
    private val frameBuff = uniform  ("frameBuff")

    fun draw(textures: Texture, buffNo: Int, toReflect: Boolean = false) {
        GLES32.glClear(GLES32.GL_DEPTH_BUFFER_BIT)
        use()
        sendTexture(textures, buffNo, frameBuff, texPos, toReflect)
        GLES32.glVertexAttribPointer(pos, 2, GLES32.GL_FLOAT, false, 0,
            Utils.allocFloat(floatArrayOf(-1f, -1f,    1f, -1f,    -1f, 1f,
                                          -1f,  1f,    1f, -1f,     1f, 1f)))
        GLES32.glDrawArrays(GLES32.GL_TRIANGLES, 0, 6)
    }
}