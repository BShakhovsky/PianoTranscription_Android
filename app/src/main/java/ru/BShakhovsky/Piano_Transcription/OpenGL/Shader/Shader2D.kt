@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL.Shader

import android.content.Context
import android.opengl.GLES31
import ru.BShakhovsky.Piano_Transcription.OpenGL.FrameBuffs
import ru.BShakhovsky.Piano_Transcription.OpenGL.Utils

@Suppress("unused")
class Shader2D(context: Context) : Shader(context, "Vertex2D", "Pixel2D") {

    private val debugPos2D        = attribute("pos")
    private val debugTexturePos2D = attribute("texturePos")
    private val debugFrameBuff2D  = uniform  ("frameBuff")

    fun draw(buffNo: Int, width: Int, height: Int, frames: FrameBuffs) {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0)
        GLES31.glViewport(0, 0, width, height)
        use()
        frames.bindTexture(buffNo)
        GLES31.glUniform1i(debugFrameBuff2D, 0)

        GLES31.glVertexAttribPointer(debugPos2D, 2, GLES31.GL_FLOAT, false, 0,
            Utils.allocFloat(floatArrayOf(-1f, -1f, 1f, 1f, -1f, 1f, -1f, -1f, 1f, -1f, 1f, 1f)))
        GLES31.glEnableVertexAttribArray(debugPos2D)
        GLES31.glVertexAttribPointer(debugTexturePos2D, 2, GLES31.GL_FLOAT, false, 0,
            Utils.allocFloat(floatArrayOf(0f, 0f, 1f, 1f, 0f, 1f, 0f, 0f, 1f, 0f, 1f, 1f)))
        GLES31.glEnableVertexAttribArray(debugTexturePos2D)
        GLES31.glDrawArrays(GLES31.GL_TRIANGLES, 0, 6)
    }
}