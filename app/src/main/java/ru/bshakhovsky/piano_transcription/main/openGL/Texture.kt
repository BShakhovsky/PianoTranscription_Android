package ru.bshakhovsky.piano_transcription.main.openGL

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.opengl.GLUtils

import ru.bshakhovsky.piano_transcription.R.drawable.desk

import ru.bshakhovsky.piano_transcription.main.openGL.shader.Light
import ru.bshakhovsky.piano_transcription.utils.DebugMode

class Texture(resources: Resources, lights: Array<Light>) {

    private val buff = IntArray(4)
    private val texture = IntArray(buff.size + 1)
    private val depthBuff = IntArray(buff.size)

    init {
        GLES.glGenFramebuffers(buff.size, buff, 0)
        GLES.glGenTextures(texture.size, texture, 0)
        GLES.glGenRenderbuffers(depthBuff.size, depthBuff, 0)
        for (i in 0..buff.lastIndex) {
            bindBuff(i)
            size(
                if (i >= lights.size) 0 else lights[i].orthoWidth,
                if (i >= lights.size) 0 else lights[i].orthoHeight, i
            )
            GLES.glFramebufferRenderbuffer(
                GLES.GL_FRAMEBUFFER, GLES.GL_DEPTH_ATTACHMENT, GLES.GL_RENDERBUFFER, depthBuff[i]
            )
            GLES.glFramebufferTexture2D(
                GLES.GL_FRAMEBUFFER, GLES.GL_COLOR_ATTACHMENT0, GLES.GL_TEXTURE_2D, texture[i], 0
            )
            parameteri()
        }
        with(BitmapFactory.decodeResource(resources, desk)) {
            bindTexture(texture.lastIndex)
            parameteri()
            GLUtils.texImage2D(GLES.GL_TEXTURE_2D, 0, this, 0)
            recycle()
        }
        DebugMode.assertState(
            GLES.glCheckFramebufferStatus(GLES.GL_FRAMEBUFFER)
                    == GLES.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT
        )
    }

    fun bindBuff(i: Int): Unit = GLES.glBindFramebuffer(GLES.GL_FRAMEBUFFER, buff[i])

    fun bindTexture(i: Int): Unit = GLES.glBindTexture(GLES.GL_TEXTURE_2D, texture[i])

    fun resizeReflection(width: Int, height: Int): Unit = size(width, height).also {
        DebugMode.assertState(
            GLES.glCheckFramebufferStatus(GLES.GL_FRAMEBUFFER) == GLES.GL_FRAMEBUFFER_COMPLETE
        )
    }

    private fun size(newWidth: Int, newHeight: Int, i: Int = buff.lastIndex) {
        GLES.glBindRenderbuffer(GLES.GL_RENDERBUFFER, depthBuff[i])
        GLES.glRenderbufferStorage(
            GLES.GL_RENDERBUFFER, GLES.GL_DEPTH_COMPONENT24, newWidth, newHeight
        )
        GLES.glBindTexture(GLES.GL_TEXTURE_2D, texture[i])
        GLES.glTexImage2D(
            GLES.GL_TEXTURE_2D, 0, GLES.GL_RGBA, newWidth, newHeight,
            0, GLES.GL_RGBA, GLES.GL_UNSIGNED_BYTE, null
        )
    }

    private fun parameteri() = GLES.glTexParameteri(
        GLES.GL_TEXTURE_2D, GLES.GL_TEXTURE_MIN_FILTER, GLES.GL_LINEAR // or NEAREST
    )/* GLES.GL_TEXTURE_2D, GLES.GL_TEXTURE_MAG_FILTER, GLES.GL_LINEAR // or NEAREST
        GLES.GL_TEXTURE_2D, GLES.GL_TEXTURE_WRAP_S, GLES.GL_CLAMP_TO_BORDER // or EDGE
        GLES.GL_TEXTURE_2D, GLES.GL_TEXTURE_WRAP_T, GLES.GL_CLAMP_TO_BORDER // or EDGE */
}