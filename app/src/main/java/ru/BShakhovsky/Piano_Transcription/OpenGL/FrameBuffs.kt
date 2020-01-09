@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL

import android.opengl.GLES31

class FrameBuffs {

    private val buff = IntArray(3); private val texture = IntArray(3)

    init {
        GLES31.glGenFramebuffers(3, buff, 0); GLES31.glGenTextures(3, texture, 0)
        IntArray(3).also { depthBuff ->
            GLES31.glGenRenderbuffers(3, depthBuff, 0)
            for (i in 0..2) {
                GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, buff[i])

                GLES31.glBindRenderbuffer(GLES31.GL_RENDERBUFFER, depthBuff[i])
                GLES31.glFramebufferRenderbuffer(GLES31.GL_FRAMEBUFFER, GLES31.GL_DEPTH_ATTACHMENT, GLES31.GL_RENDERBUFFER, depthBuff[i])
                GLES31.glRenderbufferStorage(GLES31.GL_RENDERBUFFER, GLES31.GL_DEPTH_COMPONENT24,
                    Geometry.overallLen.toInt(), Geometry.overallLen.toInt())

                GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture[i])
                GLES31.glFramebufferTexture2D(GLES31.GL_FRAMEBUFFER, GLES31.GL_COLOR_ATTACHMENT0, GLES31.GL_TEXTURE_2D, texture[i], 0)
                GLES31.glTexImage2D(GLES31.GL_TEXTURE_2D, 0, GLES31.GL_RGBA, Geometry.overallLen.toInt(),
                    Geometry.overallLen.toInt(), 0, GLES31.GL_RGBA, GLES31.GL_UNSIGNED_BYTE, null)

                GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MIN_FILTER, GLES31.GL_LINEAR)
/*
                GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_MAG_FILTER, GLES31.GL_NEAREST)
                GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_S, GLES31.GL_CLAMP_TO_EDGE)
                GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GLES31.GL_TEXTURE_WRAP_T, GLES31.GL_CLAMP_TO_EDGE)
*/
            }
        }
        assert(GLES31.glCheckFramebufferStatus(GLES31.GL_FRAMEBUFFER) == GLES31.GL_FRAMEBUFFER_COMPLETE)
    }

    fun bindBuff(i: Int) { GLES31.glBindFramebuffer(GLES31.GL_FRAMEBUFFER, buff[i]) }
    fun bindTexture(i: Int) { GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, texture[i]) }
}