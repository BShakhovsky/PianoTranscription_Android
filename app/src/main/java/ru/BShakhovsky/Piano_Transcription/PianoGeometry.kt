@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription

import android.opengl.GLES31
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PianoGeometry {

    private val program = GLES31.glCreateProgram()

    val whiteLen = 145f
    val whiteWid = 23f
    private val whiteGap = .2f
    private val blackLen = 85f
    private val blackWid = 9
    private val blackFillet = 3.5f

    private fun allocCords(cords : FloatArray) = ByteBuffer.allocateDirect(
        cords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(cords)
                position(0)
            }
        }
    private fun allocOrder(orderBuff : ShortArray) = ByteBuffer.allocateDirect(
        orderBuff.size * 2).run {
        order(ByteOrder.nativeOrder())
        asShortBuffer().apply {
            put(orderBuff)
            position(0)
        }
    }

    private val whiteLeft  : FloatBuffer
    private val whiteMid   : FloatBuffer
    private val whiteRight : FloatBuffer
    private val blackCords = allocCords(floatArrayOf(
        whiteWid - blackWid / 2              , whiteWid + blackWid / 2, 0f,
        whiteWid + blackWid / 2              , whiteWid + blackWid / 2, 0f,
        whiteWid - blackWid / 2              , whiteWid + blackWid / 2, blackLen - 2 * blackFillet,
        whiteWid + blackWid / 2              , whiteWid + blackWid / 2, blackLen - 2 * blackFillet,
        whiteWid - blackWid / 2 - blackFillet, whiteWid - blackWid / 2, blackLen,
        whiteWid + blackWid / 2 + blackFillet, whiteWid - blackWid / 2, blackLen,
        whiteWid - blackWid / 2 - blackFillet, whiteWid - blackWid / 2, 0f,
        whiteWid + blackWid / 2 + blackFillet, whiteWid - blackWid / 2, 0f
    ))
    private val whiteOrder = allocOrder(shortArrayOf(
        0, 2, 1, 1, 2, 3, 4, 6, 5, 5, 6, 7, 6, 8, 7, 7, 8, 9,
        0, 10, 2, 2, 10, 12, 4, 14, 6, 6, 14, 8, 1, 3, 11, 11, 3, 13, 5, 7, 15, 15, 7, 9))
    private val blackOrder = allocOrder(shortArrayOf(
        0, 2, 1, 1, 2, 3, 2, 4, 3, 3, 4, 5, 0, 6, 2, 2, 6, 4, 1, 3, 7, 7, 3, 5))

    private val pos : Int
    private val color : Int

    init {
        floatArrayOf(  blackWid / 2f + blackFillet, whiteWid, 0f,
            whiteWid - blackWid / 2  - blackFillet, whiteWid, 0f,
                       blackWid / 2f + blackFillet, whiteWid, blackLen,
            whiteWid - blackWid / 2  - blackFillet, whiteWid, blackLen,
            whiteGap                              , whiteWid, blackLen,
            whiteWid - whiteGap                   , whiteWid, blackLen,

            whiteGap                              , whiteWid, whiteLen,
            whiteWid - whiteGap                   , whiteWid, whiteLen,
            whiteGap                              , 0f      , whiteLen,
            whiteWid - whiteGap                   , 0f      , whiteLen,

                       blackWid / 2f + blackFillet, 0f      , 0f,
            whiteWid - blackWid / 2  - blackFillet, 0f      , 0f,
                       blackWid / 2f + blackFillet, 0f      , blackLen,
            whiteWid - blackWid / 2  - blackFillet, 0f      , blackLen,
            whiteGap                              , 0f      , blackLen,
            whiteWid - whiteGap                   , 0f      , blackLen
        ).also { whiteMidCords ->
            whiteMid = allocCords(whiteMidCords)
            whiteMidCords.copyOf().also { whiteLeftCords ->
                whiteLeftCords[0] = whiteGap
                whiteLeftCords[6] = whiteGap
                whiteLeftCords[30] = whiteGap
                whiteLeftCords[36] = whiteGap
                whiteLeft = allocCords(whiteLeftCords)
            }
            whiteMidCords.also { whiteRightCords ->
                whiteRightCords[3] = whiteWid - whiteGap
                whiteRightCords[9] = whiteWid - whiteGap
                whiteRightCords[33] = whiteWid - whiteGap
                whiteRightCords[39] = whiteWid - whiteGap
                whiteRight = allocCords(whiteRightCords)
            }
        }

        fun attachShader(type: Int, shaderCode: String) = GLES31.glAttachShader(program,
            GLES31.glCreateShader(type).also { shader ->
                GLES31.glShaderSource(shader, shaderCode)
                GLES31.glCompileShader(shader)
            })
        attachShader(GLES31.GL_VERTEX_SHADER, """
            attribute vec4 pos;
            uniform mat4 mvp;
            void main() {
                gl_Position = mvp * pos;
            }""")
        attachShader(GLES31.GL_FRAGMENT_SHADER, """
            precision mediump float;
            uniform vec4 color;
            void main() {
                gl_FragColor = color;
            }""")
        GLES31.glLinkProgram(program)
        GLES31.glUseProgram(program)
        GLES31.glEnable(GLES31.GL_CULL_FACE)

        pos = GLES31.glGetAttribLocation(program, "pos")
        color = GLES31.glGetUniformLocation(program, "color")
        GLES31.glEnableVertexAttribArray(pos)
    }

    fun draw(mvp : FloatArray) {
        GLES31.glGetUniformLocation(program, "mvp").also { mvpHandle -> for (note in 0..87) {
            fun drawKey(key: FloatBuffer, offset: Float) {
                GLES31.glVertexAttribPointer(
                    pos, 3, GLES31.GL_FLOAT, false, 3 * 4, key
                )
                GLES31.glUniform4fv(color, 1,
                    if (key == blackCords) floatArrayOf(.15f, .15f, .15f, 1f)
                    else floatArrayOf(240 / 255f, 248 / 255f, 255 / 255f, 1f), 0)   // Alice blue
                FloatArray(16).also {mvpOffset ->
                    Matrix.translateM(mvpOffset, 0, mvp, 0, offset, 0f, 0f)
                    GLES31.glUniformMatrix4fv(mvpHandle, 1, false, mvpOffset, 0)
                }
                GLES31.glDrawElements(
                    GLES31.GL_TRIANGLES, if (key == blackCords) 24 else 42,
                    GLES31.GL_UNSIGNED_SHORT, if (key == blackCords) blackOrder else whiteOrder
                )
            }
            when (note) {
                0 -> drawKey(whiteLeft, 0f)
                1 -> drawKey(blackCords, 0f)
                2 -> drawKey(whiteRight, whiteWid)
                87 -> (((87 - 3) / 12 * 7 + 2) * whiteWid).also { offset ->
                    drawKey(whiteLeft, offset)
                    drawKey(whiteRight, offset)
                }
                else -> when ((note - 3) % 12) {
                    0  -> drawKey(whiteLeft,  ((note - 3) / 12 * 7 + 2) * whiteWid)
                    1  -> drawKey(blackCords, ((note - 3) / 12 * 7 + 2) * whiteWid)
                    2  -> drawKey(whiteMid,   ((note - 3) / 12 * 7 + 3) * whiteWid)
                    3  -> drawKey(blackCords, ((note - 3) / 12 * 7 + 3) * whiteWid)
                    4  -> drawKey(whiteRight, ((note - 3) / 12 * 7 + 4) * whiteWid)
                    5  -> drawKey(whiteLeft,  ((note - 3) / 12 * 7 + 5) * whiteWid)
                    6  -> drawKey(blackCords, ((note - 3) / 12 * 7 + 5) * whiteWid)
                    7  -> drawKey(whiteMid,   ((note - 3) / 12 * 7 + 6) * whiteWid)
                    8  -> drawKey(blackCords, ((note - 3) / 12 * 7 + 6) * whiteWid)

                    9  -> drawKey(whiteMid,   ((note - 3) / 12 * 7 + 7) * whiteWid)
                    10 -> drawKey(blackCords, ((note - 3) / 12 * 7 + 7) * whiteWid)
                    11 -> drawKey(whiteRight, ((note - 3) / 12 * 7 + 8) * whiteWid)
                }
            }
        } }
    }
}