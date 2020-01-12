@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL.Shader

import android.content.Context
import android.opengl.GLES31
import ru.BShakhovsky.Piano_Transcription.OpenGL.FrameBuffs
import ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry.Geometry
import ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry.Primitive

class MainShader(context: Context) : Shader(context, "Main") {

    private val norm     = attribute("norm")
    private val color    = uniform("color")
    private val mv       = uniform("mv")
    private val mvp      = uniform("mvp")
    private val inTrV    = uniform("inTrV")
    private val pixel    = uniform("pixel")

    private val shadow   = uniform("shadow")
    private val spec     = uniform("specular")
    private var withSpec = GLES31.GL_TRUE

    init { GLES31.glEnableVertexAttribArray(norm) }

    fun initReflectBuff(frames: FrameBuffs, shadows: Array<Shadow>) {
        frames.bindBuff(); frames.bindTexture()
        GLES31.glClearColor(0f, 0f, 0f, 1f)
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT or GLES31.GL_DEPTH_BUFFER_BIT)
        withSpec = GLES31.GL_FALSE
        prepare(frames, shadows)
    }
    fun initMainScreen(frames: FrameBuffs, shadows: Array<Shadow>, transparent: Boolean = false) {
        GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, 0)
        if (!transparent) {
            GLES31.glClearColor(70 / 255f, 130 / 255f, 180 / 255f, 1f) // Steel blue
            GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT)
        }
        GLES31.glClear(GLES31.GL_DEPTH_BUFFER_BIT)
        GLES31.glDisable(GLES31.GL_STENCIL_TEST)
        withSpec = GLES31.GL_TRUE
        prepare(frames, shadows)
    }

    private fun prepare(frames: FrameBuffs, shadows: Array<Shadow>) {
        use()
        GLES31.glUniform1i(spec, withSpec)
        GLES31.glUniform1i(shadow, GLES31.GL_TRUE)
        GLES31.glUniform1f(pixel, 1 / Geometry.overallLen)
        shadows.forEachIndexed { lightNo, shadow -> with(shadow) {
            GLES31.glUniform3fv(light, 1, lightDir, 0)
            GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + lightNo)
            frames.bindTexture(lightNo)
            GLES31.glUniform1i(depthBuff, lightNo)
        } }
    }

    fun drawDesk(desk: Primitive, view: FloatArray, viewProjection: FloatArray, invTransView: FloatArray) {
        GLES31.glEnable(GLES31.GL_BLEND)
        GLES31.glUniform1i(shadow, GLES31.GL_FALSE)

        GLES31.glUniform4fv(color, 1, floatArrayOf(.15f, .15f, .15f, .9f), 0)
        translate(view, mv); translate(viewProjection, mvp); translate(invTransView, inTrV)
        desk.draw(pos, norm)

        GLES31.glDisable(GLES31.GL_BLEND)
        GLES31.glUniform1i(shadow, GLES31.GL_TRUE)
    }
    fun drawKey(key: Primitive, offset: Float, isBlack: Boolean,
                view: FloatArray, viewProjection: FloatArray, invTransView: FloatArray, shadows: Array<Shadow>) {
        GLES31.glUniform4fv(color, 1, if (isBlack) floatArrayOf(.15f, .15f, .15f, 1f)
            else floatArrayOf(240 / 255f, 248 / 255f, 255 / 255f, 1f), 0) // Alice blue
        translate(view, mv, offset); translate(viewProjection, mvp, offset); translate(invTransView, inTrV, offset)
        shadows.forEach { with (it) { translate(lightOrtho, lightMVO, offset) } }
        key.draw(pos, norm)
    }
}