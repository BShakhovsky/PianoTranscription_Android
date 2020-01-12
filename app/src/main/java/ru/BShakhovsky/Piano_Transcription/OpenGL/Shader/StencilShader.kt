@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL.Shader

import android.content.Context
import android.opengl.GLES31
import ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry.Primitive

class StencilShader(context: Context) : Shader(context, "Stencil") {

    private val mvp = uniform("mvp")

    fun draw(figure: Primitive, viewProjection: FloatArray) {
        GLES31.glEnable(GLES31.GL_STENCIL_TEST)

        GLES31.glStencilFunc(GLES31.GL_ALWAYS, 1, 1)
        GLES31.glStencilOp(GLES31.GL_REPLACE, GLES31.GL_REPLACE, GLES31.GL_REPLACE)
        GLES31.glColorMask(false, false, false, false)

        GLES31.glClear(GLES31.GL_DEPTH_BUFFER_BIT or GLES31.GL_STENCIL_BUFFER_BIT)
        use()
        translate(viewProjection, mvp)
        figure.draw(pos)

        GLES31.glStencilFunc(GLES31.GL_EQUAL, 1, 1)
        GLES31.glStencilOp(GLES31.GL_KEEP, GLES31.GL_KEEP, GLES31.GL_KEEP)
        GLES31.glColorMask(true, true, true, true)
    }
}