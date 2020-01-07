@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription

import android.opengl.GLES31
import android.opengl.GLException
import android.opengl.Matrix

class Geometry {

    companion object {
        const val whiteLen   = 145f
        const val whiteWid   = 23f
        const val overallLen = whiteWid * 52
    }
    private val whiteGap    = .6f
    private val blackLen    = 85f
    private val blackWid    = 9
    private val blackFillet = 3

    private val whiteLeft  : Primitive
    private val whiteMid   : Primitive
    private val whiteRight : Primitive
    private val black      = Primitive(floatArrayOf(
        whiteWid - blackWid / 2              , whiteWid + blackWid, 0f,
        whiteWid + blackWid / 2              , whiteWid + blackWid, 0f,
        whiteWid - blackWid / 2              , whiteWid + blackWid, blackLen - 3 * blackFillet,
        whiteWid + blackWid / 2              , whiteWid + blackWid, blackLen - 3 * blackFillet,
        whiteWid - blackWid / 2 - blackFillet, whiteWid - blackWid, blackLen,
        whiteWid + blackWid / 2 + blackFillet, whiteWid - blackWid, blackLen,
        whiteWid - blackWid / 2 - blackFillet, whiteWid - blackWid, 0f,
        whiteWid + blackWid / 2 + blackFillet, whiteWid - blackWid, 0f
    ), intArrayOf(0, 2, 1, 1, 2, 3, 2, 4, 3, 3, 4, 5, 0, 6, 2, 2, 6, 4, 1, 3, 7, 7, 3, 5))

    private val pos    : Int
    private val color  : Int
    private val norm   : Int
    private val mv     : Int
    private val mvp    : Int

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
        ).also { whiteMidCords -> intArrayOf(0, 2, 1, 1, 2, 3, 4, 6, 5, 5, 6, 7, 6, 8, 7, 7, 8, 9,
            0, 10, 2, 2, 10, 12, 4, 14, 6, 6, 14, 8,
            1, 3, 11, 11, 3, 13, 5, 7, 15, 15, 7, 9).also { whiteOrder ->

            whiteMid = Primitive(whiteMidCords, whiteOrder)

            whiteMidCords.copyOf().also { whiteLeftCords ->
                for (i in intArrayOf(0, 6, 30, 36)) whiteLeftCords[i] = whiteGap
                whiteLeft = Primitive(whiteLeftCords, whiteOrder)
            }
            for (i in intArrayOf(3, 9, 33, 39)) whiteMidCords[i] = whiteWid - whiteGap
            whiteRight = Primitive(whiteMidCords, whiteOrder)
        } }
        GLES31.glCreateProgram().also { program ->
            fun attachShader(type: Int, shaderCode: String) = GLES31.glAttachShader(program,
                GLES31.glCreateShader(type).also { shader ->
                    GLES31.glShaderSource(shader, shaderCode)
                    GLES31.glCompileShader(shader)
                    if (BuildConfig.DEBUG) GLES31.glGetShaderInfoLog(shader).also { err ->
                        if (err.isNotEmpty()) throw GLException(0, "Shader compile: $err") }
                })
            attachShader(GLES31.GL_VERTEX_SHADER, """
                attribute vec4 pos;
                attribute vec3 norm;
                uniform mat4 mv, mvp;
                
                varying vec3 viewDir, normal;

                void main() {
                    gl_Position = mvp * pos;
                    viewDir = normalize(vec3(mv * pos));
                    normal = norm;
                }""")
            attachShader(GLES31.GL_FRAGMENT_SHADER, """
                precision mediump float;
                uniform vec4 color;

                varying vec3 viewDir, normal;
                
                vec3 Diffuse(const vec3 dir, const vec3 diffColor) {
                    return diffColor * max(-dot(normal, dir), 0.);
                }
                vec3 Specular(const vec3 dir, const vec3 specColor) {
                    return specColor * pow(max(dot(-viewDir, reflect(dir, normal)), 0.), 16.);
                }

                void main() {
                    const vec3      lightDir0 = vec3(-.5265408 , -.5735765 , -.6275069),
                                    lightDir1 = vec3( .7198464 ,  .3420201 ,  .6040227),
                                    lightDir2 = vec3( .4545195 , -.7660444 ,  .4545195);
                    gl_FragColor = color * vec4(vec3( .05333332,  .09882354,  .1819608) // ambient
                          + Diffuse(lightDir0,  vec3(1         ,  .9607844 ,  .8078432))
                          + Diffuse(lightDir1,  vec3( .9647059 ,  .7607844 ,  .4078432))
                          + Diffuse(lightDir2,  vec3( .3231373 ,  .3607844 ,  .3937255))
                         + Specular(lightDir0,  vec3(1         ,  .9607844 ,  .8078432))
                         + Specular(lightDir1,  vec3(0         , 0         , 0        ))
                         + Specular(lightDir2,  vec3( .3231373 ,  .3607844 ,  .3937255)), 1);
                }""")
            GLES31.glLinkProgram(program)
            GLES31.glUseProgram(program)
            GLES31.glEnable(GLES31.GL_CULL_FACE)

            pos = GLES31.glGetAttribLocation(program, "pos")
            norm = GLES31.glGetAttribLocation(program, "norm")
            for (handle in intArrayOf(pos, norm)) GLES31.glEnableVertexAttribArray(handle)
            color = GLES31.glGetUniformLocation(program, "color")
            mv = GLES31.glGetUniformLocation(program, "mv")
            mvp = GLES31.glGetUniformLocation(program, "mvp")
        }
    }

    fun draw(view : FloatArray, viewProjection : FloatArray) { for (note in 0..87) {
        fun drawKey(key : Primitive, offset : Float) {
            GLES31.glVertexAttribPointer(
                pos, 3, GLES31.GL_FLOAT, false, 3 * 4, key.vertices)
            GLES31.glVertexAttribPointer(
                norm, 3, GLES31.GL_FLOAT, false, 3 * 4, key.normals)
            GLES31.glUniform4fv(color, 1,
                if (key == black) floatArrayOf(.15f, .15f, .15f, 1f)
                else floatArrayOf(240 / 255f, 248 / 255f, 255 / 255f, 1f), 0)   // Alice blue

            fun translate(matrix : FloatArray, matHandle : Int) { FloatArray(16).also { matOffset ->
                Matrix.translateM(matOffset, 0, matrix, 0, offset, 0f, 0f)
                GLES31.glUniformMatrix4fv(matHandle, 1, false, matOffset, 0)
            } }
            translate(view, mv)
            translate(viewProjection, mvp)

            GLES31.glDrawArrays(GLES31.GL_TRIANGLES, 0, key.count)
        }
        when (note) {
            0 -> drawKey(whiteLeft, 0f)
            1 -> drawKey(black,     0f)
            2 -> drawKey(whiteRight, whiteWid)
            87 -> (((87 - 3) / 12 * 7 + 2) * whiteWid).also { offset ->
                drawKey(whiteLeft, offset)
                drawKey(whiteRight, offset)
            }
            else -> when ((note - 3) % 12) {
                0  -> drawKey(whiteLeft,  ((note - 3) / 12 * 7 + 2) * whiteWid)
                1  -> drawKey(black,      ((note - 3) / 12 * 7 + 2) * whiteWid)
                2  -> drawKey(whiteMid,   ((note - 3) / 12 * 7 + 3) * whiteWid)
                3  -> drawKey(black,      ((note - 3) / 12 * 7 + 3) * whiteWid)
                4  -> drawKey(whiteRight, ((note - 3) / 12 * 7 + 4) * whiteWid)
                5  -> drawKey(whiteLeft,  ((note - 3) / 12 * 7 + 5) * whiteWid)
                6  -> drawKey(black,      ((note - 3) / 12 * 7 + 5) * whiteWid)
                7  -> drawKey(whiteMid,   ((note - 3) / 12 * 7 + 6) * whiteWid)
                8  -> drawKey(black,      ((note - 3) / 12 * 7 + 6) * whiteWid)

                9  -> drawKey(whiteMid,   ((note - 3) / 12 * 7 + 7) * whiteWid)
                10 -> drawKey(black,      ((note - 3) / 12 * 7 + 7) * whiteWid)
                11 -> drawKey(whiteRight, ((note - 3) / 12 * 7 + 8) * whiteWid)
            }
        } }
    }
}