@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL.Shader

import android.content.Context
import android.opengl.GLES32
import ru.BShakhovsky.Piano_Transcription.OpenGL.Texture
import ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry.Primitive

class MainShader(context: Context) : Shader(context, "Main") {

    private val norm     = attribute("norm")
    private val color    = uniform("color")
    private val mv       = uniform("mv")
    private val mvp      = uniform("mvp")
    private val inTrV    = uniform("inTrV")

    private val pixel0   = uniform("pixel0")
    private val pixel1   = uniform("pixel1")
    private val pixel2   = uniform("pixel2")

    private val texPos  = attribute("texturePos")
    private val deskTex = uniform  ("deskTexture")
    private val withTex = attribute("tex")
    private val shadow  = uniform  ("shadow")
    private val spec    = uniform  ("specular")
//    private var withSpec = GLES31.GL_TRUE

    init { intArrayOf(norm, texPos, withTex).forEach { GLES32.glEnableVertexAttribArray(it) } }

    fun initReflectBuff(textures: Texture, lights: Array<Light>) {
        textures.bindBuff(lights.size); textures.bindTexture(lights.size)
        GLES32.glClearColor(0f, 0f, 0f, 1f)
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT or GLES32.GL_DEPTH_BUFFER_BIT)
//        withSpec = GLES31.GL_FALSE
        prepare(textures, lights)
    }
    fun initMainScreen(textures: Texture, lights: Array<Light>, transparent: Boolean = false) {
        GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, 0)
        if (!transparent) {
            GLES32.glClearColor(70 / 255f, 130 / 255f, 180 / 255f, 1f) // Steel blue
            GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT)
        }
        GLES32.glClear(GLES32.GL_DEPTH_BUFFER_BIT)
        GLES32.glDisable(GLES32.GL_STENCIL_TEST)
//        withSpec = GLES31.GL_TRUE
        prepare(textures, lights)
    }

    private fun prepare(textures: Texture, lights: Array<Light>) {
        use()
//        GLES31.glUniform1i(spec, withSpec)
        GLES32.glUniform1i(spec, GLES32.GL_TRUE)
        GLES32.glUniform1i(shadow, GLES32.GL_TRUE)
        intArrayOf(pixel0, pixel1, pixel2).forEachIndexed { i, handle ->
            GLES32.glUniform2fv(handle, 1, floatArrayOf(1f / lights[i].orthoWidth, 1f / lights[i].orthoHeight), 0)
        }
        lights.forEachIndexed { lightNo, shadow -> with(shadow) {
            GLES32.glUniform3fv(light, 1, lightDir, 0)
            GLES32.glActiveTexture(GLES32.GL_TEXTURE0 + lightNo)
            textures.bindTexture(lightNo)
            GLES32.glUniform1i(depthBuff, lightNo)
        } }
    }

    fun drawDesk(desk: Primitive, view: FloatArray, viewProjection: FloatArray, invTransView: FloatArray,
                 textures: Texture, texInd: Int) {
        GLES32.glEnable(GLES32.GL_BLEND)
        // Only light#2 gives produces shadow on desk, and I do not like this shadow
        GLES32.glUniform1i(shadow, GLES32.GL_FALSE)

        TextureShader.sendTexture(textures, texInd, deskTex, texPos)
        GLES32.glVertexAttribPointer(withTex, 1, GLES32.GL_FLOAT, false, 0, desk.withTex)

        GLES32.glUniform4fv(color, 1, floatArrayOf(.15f, .15f, .15f, .95f), 0)
        translate(view, mv); translate(viewProjection, mvp); translate(invTransView, inTrV)
        desk.draw(pos, norm)

        GLES32.glDisable(GLES32.GL_BLEND)
        GLES32.glUniform1i(shadow, GLES32.GL_TRUE)
    }
    fun drawKey(key: Primitive, offset: Float, isBlack: Boolean,
                view: FloatArray, viewProjection: FloatArray, invTransView: FloatArray, lights: Array<Light>) {
        GLES32.glVertexAttribPointer(withTex, 1, GLES32.GL_FLOAT, false, 0, key.withTex)
        GLES32.glUniform4fv(color, 1, if (isBlack) floatArrayOf(.15f, .15f, .15f, 1f)
            else floatArrayOf(240 / 255f, 248 / 255f, 255 / 255f, 1f), 0) // Alice blue
        translate(view, mv, offset); translate(viewProjection, mvp, offset); translate(invTransView, inTrV, offset)
        lights.forEach { with(it) { translate(lightOrtho, lightMVO, offset) } }
        key.draw(pos, norm)
    }
}