@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.OpenGL.Shader

import android.opengl.Matrix
import ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry.Geometry

class Light(
    val lightDir: FloatArray, index: Int, mainShader: Shader, shadowReversed: Boolean = false
) {

    val depthBuff: Int = mainShader.uniform("depthBuff$index")
    val light: Int = mainShader.uniform("light$index")
    val lightMVO: Int = mainShader.uniform("lightMVO$index")
    val lightOrtho: FloatArray = FloatArray(16)

    val orthoWidth: Int
    val orthoHeight: Int

    init {
        FloatArray(16).also { view ->
            Matrix.setLookAtM(
                view, 0,
                Geometry.overallLen / 2 + lightDir[0] * if (shadowReversed) 1 else -1,
                -lightDir[1], -lightDir[2], Geometry.overallLen / 2, 0f, 0f, 0f, 1f, 0f
            )

            var (xLeft, yBottom, zMin) = Triple(
                Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY
            )
            var (xRight, yTop, zMax) = Triple(
                Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY
            )

            FloatArray(4).also { viewCords ->
                @Suppress("LongLine", "Reformat")
                for (cords in arrayOf(
                    floatArrayOf(0f,                                            0f,                     Geometry.whiteLen,      1f),
                    floatArrayOf(0f,                                            Geometry.whiteWid,      Geometry.whiteLen,      1f),
                    floatArrayOf(Geometry.overallLen,                           0f,                     Geometry.whiteLen,      1f),
                    floatArrayOf(Geometry.overallLen,                           Geometry.whiteWid,      Geometry.whiteLen,      1f),

                    floatArrayOf(                       - Geometry.deskOver,    0f,                     -Geometry.deskThick,    1f),
                    floatArrayOf(                       - Geometry.deskOver,    Geometry.deskHeight,    -Geometry.deskThick,    1f),
                    floatArrayOf(                       - Geometry.deskOver,    Geometry.deskHeight,    0f,                     1f),

                    floatArrayOf(Geometry.overallLen    + Geometry.deskOver,    0f,                     -Geometry.deskThick,    1f),
                    floatArrayOf(Geometry.overallLen    + Geometry.deskOver,    Geometry.deskHeight,    -Geometry.deskThick,    1f),
                    floatArrayOf(Geometry.overallLen    + Geometry.deskOver,    Geometry.deskHeight,    0f,                     1f)
                )) {
                    Matrix.multiplyMV(viewCords, 0, view, 0, cords, 0)
                    for (i in 0..viewCords.lastIndex) viewCords[i] /= viewCords.last()

                    xLeft = xLeft.coerceAtMost(viewCords[0])
                    xRight = xRight.coerceAtLeast(viewCords[0])

                    yBottom = yBottom.coerceAtMost(viewCords[1])
                    yTop = yTop.coerceAtLeast(viewCords[1])

                    zMin = zMin.coerceAtMost(viewCords[2])
                    zMax = zMax.coerceAtLeast(viewCords[2])
                }
            }
            FloatArray(16).also { ortho ->
                Matrix.orthoM(ortho, 0, xLeft, xRight, yBottom, yTop, -zMax, -zMin)
                Matrix.multiplyMM(lightOrtho, 0, ortho, 0, view, 0)
            }

            orthoWidth = (xRight - xLeft).toInt()
            orthoHeight = (yTop - yBottom).toInt()
        }
    }
}