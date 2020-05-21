package ru.bshakhovsky.piano_transcription.openGL.shader

import android.content.Context
import android.opengl.GLES32
import ru.bshakhovsky.piano_transcription.openGL.Texture
import ru.bshakhovsky.piano_transcription.openGL.geometry.Primitive

class DepthShader(context: Context) : Shader(context, "Shadow") {

    private val depthMVO = uniform("mvo")

    fun prepare(textures: Texture, lights: Array<Light>, lightNo: Int) {
        with(textures) {
            bindBuff(lightNo)
            bindTexture(lightNo)
        }
        with(lights[lightNo]) { GLES32.glViewport(0, 0, orthoWidth, orthoHeight) }
        GLES32.glClearColor(1f, 1f, 1f, 1f)
        GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT or GLES32.GL_DEPTH_BUFFER_BIT)
        use()
    }

    fun draw(key: Primitive, offset: Float, angle: Float, lightOrtho: FloatArray) {
        shiftRotate(lightOrtho, depthMVO, offset, angle)
        key.draw(pos)
    }
}