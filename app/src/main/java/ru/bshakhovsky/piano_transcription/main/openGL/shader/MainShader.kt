package ru.bshakhovsky.piano_transcription.main.openGL.shader

import android.content.res.AssetManager

import ru.bshakhovsky.piano_transcription.main.openGL.GLES
import ru.bshakhovsky.piano_transcription.main.openGL.Texture
import ru.bshakhovsky.piano_transcription.main.openGL.geometry.Primitive

class MainShader(assets: AssetManager) : Shader(assets, "Main") {

    private val norm = attribute("norm")
    private val color = uniform("color")
    private val mv = uniform("mv")
    private val mvp = uniform("mvp")
    private val inTrV = uniform("inTrV")

    private val pixel0 = uniform("pixel0")
    private val pixel1 = uniform("pixel1")
    private val pixel2 = uniform("pixel2")

    private val texPos = attribute("texturePos")
    private val deskTex = uniform("deskTexture")
    private val withTex = attribute("tex")
    private val shadow = uniform("shadow")

    init {
        intArrayOf(norm, texPos, withTex).forEach { GLES.glEnableVertexAttribArray(it) }
    }

    fun initReflectBuff(textures: Texture, lights: Array<Light>) {
        with(textures) {
            lights.size.also { id ->
                bindBuff(id)
                bindTexture(id)
            }
        }
        GLES.glClearColor(0f, 0f, 0f, 1f)
        GLES.glClear(GLES.GL_COLOR_BUFFER_BIT or GLES.GL_DEPTH_BUFFER_BIT)
        prepare(textures, lights)

        @Suppress("SpellCheckingInspection")
        /* This was the reason of native OpenGL crashes on Qualcomm Adreno GPUs
            drawCotton -> glDrawArrays --> libESXGLESv2_adreno.so
            --> EsxVertexArrayObject::UpdateInternalVbos(
                EsxDrawDescriptor const*, unsigned int, EsxAttributeDesc const*)
            --> memcpy
        Even though cotton and keyboard do not use texture, its buffer should be allocated: */
        TextureShader.sendTexture(textures, lights.size + 1, deskTex, texPos)
    }

    fun initMainScreen(textures: Texture, lights: Array<Light>, transparent: Boolean = false) {
        GLES.glBindFramebuffer(GLES.GL_FRAMEBUFFER, 0)
        if (!transparent) {
            GLES.glClearColor(70 / 255f, 130 / 255f, 180 / 255f, 1f) // Steel blue
            GLES.glClear(GLES.GL_COLOR_BUFFER_BIT)
        }
        GLES.glClear(GLES.GL_DEPTH_BUFFER_BIT)
        GLES.glDisable(GLES.GL_STENCIL_TEST)
        prepare(textures, lights)
    }

    private fun prepare(textures: Texture, lights: Array<Light>) {
        use()
        GLES.glUniform1i(shadow, GLES.GL_TRUE)
        intArrayOf(pixel0, pixel1, pixel2).forEachIndexed { i, handle ->
            with(lights[i]) {
                GLES.glUniform2fv(handle, 1, floatArrayOf(1f / orthoWidth, 1f / orthoHeight), 0)
            }
        }
        lights.forEachIndexed { lightNo, shadow ->
            with(shadow) {
                GLES.glUniform3fv(light, 1, lightDir, 0)
                GLES.glActiveTexture(GLES.GL_TEXTURE0 + lightNo)
                textures.bindTexture(lightNo)
                GLES.glUniform1i(depthBuff, lightNo)
            }
        }
    }

    fun drawDesk(
        desk: Primitive, view: FloatArray, viewProjection: FloatArray, invTransView: FloatArray,
        textures: Texture, texInd: Int
    ) {
        GLES.glEnable(GLES.GL_BLEND)
        // Only light#2 produces shadow on desk, and I do not like this shadow
        GLES.glUniform1i(shadow, GLES.GL_FALSE)

        TextureShader.sendTexture(textures, texInd, deskTex, texPos)
        drawCommon(desk, floatArrayOf(.15f, .15f, .15f, .9f), view, viewProjection, invTransView)

        GLES.glDisable(GLES.GL_BLEND)
        GLES.glUniform1i(shadow, GLES.GL_TRUE)
    }

    fun drawCotton(
        cotton: Primitive,
        view: FloatArray, viewProjection: FloatArray, invTransView: FloatArray, lights: Array<Light>
    ): Unit = drawCommon(
        cotton, floatArrayOf(0xD5.toFloat() / 0xFF, 0f, 0f, 1f),
        view, viewProjection, invTransView, lights
    )

    fun drawKey(
        key: Primitive, offset: Float, angle: Float, col: FloatArray,
        view: FloatArray, viewProjection: FloatArray, invTransView: FloatArray, lights: Array<Light>
    ): Unit = drawCommon(key, col, view, viewProjection, invTransView, lights, offset, angle)

    private fun drawCommon(
        shape: Primitive, col: FloatArray,
        view: FloatArray, viewProjection: FloatArray, invTransView: FloatArray,
        lights: Array<Light>? = null, offset: Float = 0f, angle: Float = 0f
    ) {
        GLES.glVertexAttribPointer(withTex, 1, GLES.GL_FLOAT, false, 0, shape.withTex)
        GLES.glUniform4fv(color, 1, col, 0)
        shiftRotate(view, mv, offset, angle)
        shiftRotate(viewProjection, mvp, offset, angle)
        shiftRotate(invTransView, inTrV, offset, angle)
        lights?.forEach { with(it) { shiftRotate(lightOrtho, lightMVO, offset, angle) } }
        shape.draw(pos, norm)
    }
}