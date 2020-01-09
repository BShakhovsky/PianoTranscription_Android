@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL.Shader

import android.content.Context
import android.opengl.GLES31
import ru.BShakhovsky.Piano_Transcription.OpenGL.FrameBuffs
import ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry
import ru.BShakhovsky.Piano_Transcription.OpenGL.Primitive

class DepthShader(context: Context) : Shader(context, "VertexShadow", "PixelShadow") {

    private val depthPos = attribute("pos")
    private val depthMVO = uniform  ("mvo")

    init { GLES31.glEnableVertexAttribArray(depthPos) }

    fun prepare(frames: FrameBuffs, lightNo: Int) {
        frames.bindBuff(lightNo); frames.bindTexture(lightNo)
        GLES31.glViewport(0, 0, Geometry.overallLen.toInt(), Geometry.overallLen.toInt())
        GLES31.glClearColor(1f, 1f, 1f, 1f)
        GLES31.glClear(GLES31.GL_COLOR_BUFFER_BIT or GLES31.GL_DEPTH_BUFFER_BIT)
        use()
    }

    fun draw(key: Primitive, offset: Float, lightOrtho: FloatArray) {
        translate(lightOrtho, depthMVO, offset)
        key.draw(depthPos)
    }
}