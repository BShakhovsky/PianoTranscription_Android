@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL.Shader

import android.content.Context
import android.opengl.GLES32
import ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry.Primitive

class StencilShader(context: Context) : Shader(context, "Stencil") {

    private val mvp = uniform("mvp")

    fun draw(figure: Primitive, viewProjection: FloatArray) {
        GLES32.glEnable(GLES32.GL_STENCIL_TEST)

        GLES32.glStencilFunc(GLES32.GL_ALWAYS, 1, 1)
        GLES32.glStencilOp(GLES32.GL_REPLACE, GLES32.GL_REPLACE, GLES32.GL_REPLACE)
        GLES32.glColorMask(false, false, false, false)

        GLES32.glClear(GLES32.GL_DEPTH_BUFFER_BIT or GLES32.GL_STENCIL_BUFFER_BIT)
        use()
        translate(viewProjection, mvp)
        figure.draw(pos)

        GLES32.glStencilFunc(GLES32.GL_EQUAL, 1, 1)
        GLES32.glStencilOp(GLES32.GL_KEEP, GLES32.GL_KEEP, GLES32.GL_KEEP)
        GLES32.glColorMask(true, true, true, true)
    }
}