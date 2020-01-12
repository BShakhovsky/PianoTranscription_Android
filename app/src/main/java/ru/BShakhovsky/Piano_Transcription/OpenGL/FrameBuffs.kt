@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL

import android.opengl.GLES31
import ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry.Geometry

class FrameBuffs {

    private val buff = IntArray(4); private val texture = IntArray(4)
    private val depthBuff = IntArray(4)

    init {
        GLES31.glGenFramebuffers(4, buff, 0); GLES31.glGenTextures(4, texture, 0)
        GLES31.glGenRenderbuffers(4, depthBuff, 0)
        for (i in 0..3) { GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, buff[i])
            size(Geometry.overallLen.toInt(), Geometry.overallLen.toInt(), i)
            GLES31.glFramebufferRenderbuffer(GLES31.GL_FRAMEBUFFER, GLES31.GL_DEPTH_ATTACHMENT, GLES31.GL_RENDERBUFFER, depthBuff[i])
            GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, texture[i], 0)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR) // or NEAREST
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_LINEAR) // or NEAREST
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)
        }
        assert(GLES31.glCheckFramebufferStatus(GLES31.GL_FRAMEBUFFER) == GLES31.GL_FRAMEBUFFER_COMPLETE)
    }

    fun resizeReflection(width: Int, height: Int) { size(width, height); }
    private fun size(newWidth: Int, newHeight: Int, i: Int = buff.lastIndex) {
        GLES31.glBindRenderbuffer(GLES31.GL_RENDERBUFFER, depthBuff[i])
        GLES31.glRenderbufferStorage(GLES31.GL_RENDERBUFFER, GLES31.GL_DEPTH_COMPONENT24, newWidth, newHeight)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture[i])
        GLES31.glTexImage2D(GLES31.GL_TEXTURE_2D, 0, GLES31.GL_RGBA, newWidth, newHeight, 0, GLES31.GL_RGBA, GLES31.GL_UNSIGNED_BYTE, null)
    }

    fun bindBuff(i: Int = buff.lastIndex) { GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, buff[i]) }
    fun bindTexture(i: Int = texture.lastIndex) { GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture[i]) }
}