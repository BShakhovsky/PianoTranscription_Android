@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL.Shader

import android.content.Context
import android.opengl.GLES31
import ru.BShakhovsky.Piano_Transcription.OpenGL.FrameBuffs
import ru.BShakhovsky.Piano_Transcription.OpenGL.Utils

class Shader2D(context: Context) : Shader(context, "2D") {

    private val debugTexturePos2D = attribute("texturePos")
    private val debugFrameBuff2D  = uniform  ("frameBuff")

    fun draw(frames: FrameBuffs, buffNo: Int, toReflect: Boolean = false) {
        GLES31.glClear(GLES31.GL_DEPTH_BUFFER_BIT)
        use()
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + buffNo)
        frames.bindTexture(buffNo)
        GLES31.glUniform1i(debugFrameBuff2D, buffNo)

        GLES31.glVertexAttribPointer(pos, 2, GLES31.GL_FLOAT, false, 0,
            Utils.allocFloat(floatArrayOf(-1f, -1f, 1f, 1f, -1f, 1f,
                                          -1f, -1f, 1f, -1f, 1f, 1f)))
        GLES31.glEnableVertexAttribArray(pos)
        GLES31.glVertexAttribPointer(debugTexturePos2D, 2, GLES31.GL_FLOAT, false, 0,
            Utils.allocFloat(if (toReflect) floatArrayOf(1f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0f, 1f)
                                       else floatArrayOf(0f, 0f, 1f, 1f, 0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f)))
        GLES31.glEnableVertexAttribArray(debugTexturePos2D)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLES, 0, 6)
    }
}