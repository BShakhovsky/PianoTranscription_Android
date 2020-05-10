@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.OpenGL

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES32
import android.opengl.GLUtils
import ru.BShakhovsky.Piano_Transcription.Utils.DebugMode
import ru.BShakhovsky.Piano_Transcription.OpenGL.Shader.Light
import ru.BShakhovsky.Piano_Transcription.R

class Texture(context: Context, lights: Array<Light>) {

    private val buff = IntArray(4)
    private val texture = IntArray(buff.size + 1)
    private val depthBuff = IntArray(buff.size)

    init {
        GLES32.glGenFramebuffers(buff.size, buff, 0)
        GLES32.glGenTextures(texture.size, texture, 0)
        GLES32.glGenRenderbuffers(depthBuff.size, depthBuff, 0)
        for (i in 0..buff.lastIndex) {
            bindBuff(i)
            size(
                if (i >= lights.size) 0 else lights[i].orthoWidth,
                if (i >= lights.size) 0 else lights[i].orthoHeight, i
            )
            GLES32.glFramebufferRenderbuffer(
                GLES32.GL_FRAMEBUFFER, GLES32.GL_DEPTH_ATTACHMENT,
                GLES32.GL_RENDERBUFFER, depthBuff[i]
            )
            GLES32.glFramebufferTexture2D(
                GLES32.GL_FRAMEBUFFER, GLES32.GL_COLOR_ATTACHMENT0,
                GLES32.GL_TEXTURE_2D, texture[i], 0
            )
            parameteri()
        }
        with(BitmapFactory.decodeResource(context.resources, R.drawable.desk)) {
            bindTexture(texture.lastIndex)
            parameteri()
            GLUtils.texImage2D(GLES32.GL_TEXTURE_2D, 0, this, 0)
            recycle()
        }
        DebugMode.assertState(
            GLES32.glCheckFramebufferStatus(GLES32.GL_FRAMEBUFFER)
                    == GLES32.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT
        )
    }

    fun bindBuff(i: Int): Unit = GLES32.glBindFramebuffer(GLES32.GL_FRAMEBUFFER, buff[i])

    fun bindTexture(i: Int): Unit = GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[i])

    fun resizeReflection(width: Int, height: Int): Unit = size(width, height).also {
        DebugMode.assertState(
            GLES32.glCheckFramebufferStatus(GLES32.GL_FRAMEBUFFER) == GLES32.GL_FRAMEBUFFER_COMPLETE
        )
    }

    private fun size(newWidth: Int, newHeight: Int, i: Int = buff.lastIndex) {
        GLES32.glBindRenderbuffer(GLES32.GL_RENDERBUFFER, depthBuff[i])
        GLES32.glRenderbufferStorage(
            GLES32.GL_RENDERBUFFER, GLES32.GL_DEPTH_COMPONENT24, newWidth, newHeight
        )
        GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, texture[i])
        GLES32.glTexImage2D(
            GLES32.GL_TEXTURE_2D, 0, GLES32.GL_RGBA, newWidth, newHeight,
            0, GLES32.GL_RGBA, GLES32.GL_UNSIGNED_BYTE, null
        )
    }

    private fun parameteri() = GLES32.glTexParameteri(
        GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER, GLES32.GL_LINEAR // or NEAREST
    )/* GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER, GLES32.GL_LINEAR // or NEAREST
        GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_S, GLES32.GL_CLAMP_TO_BORDER // or EDGE
        GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_WRAP_T, GLES32.GL_CLAMP_TO_BORDER // or EDGE */
}