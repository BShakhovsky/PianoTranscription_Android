package ru.bshakhovsky.piano_transcription.main.openGL.shader

import android.content.res.AssetManager
import ru.bshakhovsky.piano_transcription.main.openGL.GLES
import ru.bshakhovsky.piano_transcription.main.openGL.geometry.Primitive

class StencilShader(assets: AssetManager) : Shader(assets, "Stencil") {

    private val mvp = uniform("mvp")

    fun draw(figure: Primitive, viewProjection: FloatArray) {
        GLES.glEnable(GLES.GL_STENCIL_TEST)

        GLES.glStencilFunc(GLES.GL_ALWAYS, 1, 1)
        GLES.glStencilOp(GLES.GL_REPLACE, GLES.GL_REPLACE, GLES.GL_REPLACE)
        GLES.glColorMask(false, false, false, false)

        GLES.glClear(GLES.GL_DEPTH_BUFFER_BIT or GLES.GL_STENCIL_BUFFER_BIT)
        use()
        shiftRotate(viewProjection, mvp)
        figure.draw(pos)

        GLES.glStencilFunc(GLES.GL_EQUAL, 1, 1)
        GLES.glStencilOp(GLES.GL_KEEP, GLES.GL_KEEP, GLES.GL_KEEP)
        GLES.glColorMask(true, true, true, true)
    }
}