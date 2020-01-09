@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL.Shader

import android.content.Context
import android.opengl.GLES31
import ru.BShakhovsky.Piano_Transcription.OpenGL.FrameBuffs
import ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry
import ru.BShakhovsky.Piano_Transcription.OpenGL.Primitive
import ru.BShakhovsky.Piano_Transcription.OpenGL.Shadow

class MainShader(context: Context) : Shader(context, "VertexMain", "PixelMain") {

    var width = 0; var height = 0

    private val mainPos  =  attribute("pos")
    private val norm     =  attribute("norm")
    private val color    =  uniform  ("color")
    private val mv       =  uniform  ("mv")
    private val mvp      =  uniform  ("mvp")
    private val pixel    =  uniform  ("pixel")

    init { for (handle in intArrayOf(mainPos, norm)) GLES31.glEnableVertexAttribArray(handle) }

    fun prepare(shadows: Array<Shadow>, frames: FrameBuffs) {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0)
        GLES31.glViewport(0, 0, width, height)
        GLES31.glClearColor(70 / 255f, 130 / 255f, 180 / 255f, 1f) // Steel blue
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT or GLES31.GL_DEPTH_BUFFER_BIT)
        use()
        shadows.forEachIndexed { lightNo, shadow -> with (shadow) {
            GLES31.glUniform3fv(light, 1, lightDir, 0)
            GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + lightNo)
            frames.bindTexture(lightNo)
            GLES31.glUniform1i(depthBuff, lightNo)
        } }
        GLES31.glUniform1f(pixel, 1 / Geometry.overallLen)
    }

    fun draw(key: Primitive, offset: Float, isBlack: Boolean,
             view: FloatArray, viewProjection: FloatArray, shadows: Array<Shadow>) {
        GLES31.glUniform4fv(color, 1, if (isBlack) floatArrayOf(.15f, .15f, .15f, 1f)
            else floatArrayOf(240 / 255f, 248 / 255f, 255 / 255f, 1f), 0) // Alice blue
        translate(view, mv, offset); translate(viewProjection, mvp, offset)
        shadows.forEach { with (it) { translate(lightOrtho, lightMVO, offset) } }
        key.draw(mainPos, norm)
    }
}