@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL

import android.opengl.GLES31
import java.nio.FloatBuffer
import kotlin.math.pow
import kotlin.math.sqrt

class Primitive(cords : FloatArray, order : IntArray) {

    private val vertices: FloatBuffer; private val normals: FloatBuffer; private val count:Int = order.size

    init { FloatArray(count * 3).also { vertexArray -> order.forEachIndexed { i, j ->
        for (k in 0..2) vertexArray[i * 3 + k] = cords[j * 3 + k] }
        vertices = Utils.allocFloat(vertexArray)

        FloatArray(count * 3).also { normArray -> for (i in 0 until count / 3)
            Triple(    vertexArray[i * 9 + 3] - vertexArray[i * 9],
                       vertexArray[i * 9 + 4] - vertexArray[i * 9 + 1],
                       vertexArray[i * 9 + 5] - vertexArray[i * 9 + 2]).also { (x1, y1, z1) ->
                Triple(vertexArray[i * 9 + 6] - vertexArray[i * 9],
                       vertexArray[i * 9 + 7] - vertexArray[i * 9 + 1],
                       vertexArray[i * 9 + 8] - vertexArray[i * 9 + 2]).also { (x2, y2, z2) ->
                    Triple(y1 * z2 - z1 * y2, z1 * x2 - x1 * z2,
                        x1 * y2 - y1 * x2).also { (crossX, crossY, crossZ) ->
                        sqrt(crossX.pow(2) + crossY.pow(2) + crossZ.pow(2)).also {
                                distance -> for (k in 0..2) {
                            normArray[i * 9 + k * 3]     = crossX / distance
                            normArray[i * 9 + k * 3 + 1] = crossY / distance
                            normArray[i * 9 + k * 3 + 2] = crossZ / distance
                        } }
                    }
                }
            }
            normals = Utils.allocFloat(normArray)
        }
    } }

    fun draw(pos: Int, norm: Int? = null) {
        GLES31.glVertexAttribPointer(pos, 3, GLES31.GL_FLOAT, false, 3 * 4, vertices)
        norm?.let { GLES31.glVertexAttribPointer(norm, 3, GLES31.GL_FLOAT, false, 3 * 4, normals) }
        GLES31.glDrawArrays(GLES31.GL_TRIANGLES, 0, count)
    }
}