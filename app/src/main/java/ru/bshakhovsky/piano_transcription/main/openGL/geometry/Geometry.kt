package ru.bshakhovsky.piano_transcription.main.openGL.geometry

import ru.bshakhovsky.piano_transcription.utils.DebugMode

class Geometry {

    companion object {
        const val maxVertices: Int = 42

        const val blackLen: Float = 85f
        const val blackWid: Float = 9f

        const val whiteLen: Float = 145f
        const val whiteWid: Float = 23f
        const val overallLen: Float = whiteWid * 52

        const val deskOver: Float = whiteWid * 3
        const val deskHeight: Float = whiteWid * 8
        const val deskThick: Float = whiteWid * 1.5f

        const val cotThick: Float = blackWid / 2
    }

    private val whiteGap = whiteWid / 38
    private val filletFront = blackWid
    private val filletSide = filletFront / 3

    private val whiteLeft: Primitive
    private val whiteMid: Primitive
    private val whiteRight: Primitive
    private val whiteFull: Primitive

    @Suppress("Reformat") private val black = Primitive(
        floatArrayOf(
            whiteWid - blackWid / 2,                whiteWid + blackWid,    cotThick,
            whiteWid + blackWid / 2,                whiteWid + blackWid,    cotThick,
            whiteWid - blackWid / 2,                whiteWid + blackWid,    blackLen,
            whiteWid + blackWid / 2,                whiteWid + blackWid,    blackLen,

            whiteWid - blackWid / 2 - filletSide,   whiteWid - blackWid,    blackLen + filletFront,
            whiteWid + blackWid / 2 + filletSide,   whiteWid - blackWid,    blackLen + filletFront,
            whiteWid - blackWid / 2 - filletSide,   whiteWid - blackWid,    cotThick,
            whiteWid + blackWid / 2 + filletSide,   whiteWid - blackWid,    cotThick
        ),
        intArrayOf(
            0, 2, 1,  1, 2, 3,    2, 4, 3,  3, 4, 5,    0, 6, 2,  2, 6, 4,    1, 3, 7,  7, 3, 5,
            0, 1, 6,  6, 1, 7).also { DebugMode.assertArgument(it.size < maxVertices) }
    )
    @Suppress("Reformat") val desk: Primitive = Primitive(
        floatArrayOf(
                        - deskOver,     0f,             0f,
            overallLen  + deskOver,     0f,             0f,
                        - deskOver,     deskHeight,     0f,
            overallLen  + deskOver,     deskHeight,     0f,

                        - deskOver,     0f,             -deskThick,
            overallLen  + deskOver,     0f,             -deskThick,
                        - deskOver,     deskHeight,     -deskThick,
            overallLen  + deskOver,     deskHeight,     -deskThick
        ),
        intArrayOf(0, 1, 2,  2, 1, 3,    2, 3, 6,  6, 3, 7)
            .also { DebugMode.assertArgument(it.size < maxVertices) },
        floatArrayOf(1f, 1f, 1f,    1f, 1f, 1f)
    )
    @Suppress("Reformat") val cotton: Primitive = Primitive(
        floatArrayOf(
            0f,             whiteWid,               cotThick,
            overallLen,     whiteWid,               cotThick,
            0f,             whiteWid + cotThick,    cotThick,
            overallLen,     whiteWid + cotThick,    cotThick,

            0f,             whiteWid,               0f,
            overallLen,     whiteWid,               0f,
            0f,             whiteWid + cotThick,    0f,
            overallLen,     whiteWid + cotThick,    0f
        ),
        intArrayOf(0, 1, 2,  2, 1, 3,    2, 3, 6,  6, 3, 7)
            .also { DebugMode.assertArgument(it.size < maxVertices) })

    val keys: Array<Key> = Array(88) { Key(it) }

    init {
        @Suppress("Reformat") floatArrayOf(
                        blackWid / 2f   + filletSide,   whiteWid,   0f,
            whiteWid -  blackWid / 2    - filletSide,   whiteWid,   0f,
                        blackWid / 2f   + filletSide,   whiteWid,   blackLen + filletFront,
            whiteWid -  blackWid / 2    - filletSide,   whiteWid,   blackLen + filletFront,
                        whiteGap,                       whiteWid,   blackLen + filletFront,
            whiteWid -  whiteGap,                       whiteWid,   blackLen + filletFront,

                        whiteGap,                       whiteWid,   whiteLen,
            whiteWid -  whiteGap,                       whiteWid,   whiteLen,
                        whiteGap,                       0f,         whiteLen,
            whiteWid -  whiteGap,                       0f,         whiteLen,

                        blackWid / 2f   + filletSide,   0f,         0f,
            whiteWid -  blackWid / 2    - filletSide,   0f,         0f,
                        blackWid / 2f   + filletSide,   0f,         blackLen + filletFront,
            whiteWid -  blackWid / 2    - filletSide,   0f,         blackLen + filletFront,
                        whiteGap,                       0f,         blackLen + filletFront,
            whiteWid -  whiteGap,                       0f,         blackLen + filletFront
        ).also { whiteMidCords ->
            intArrayOf(
                0, 2,  1,   1,  2,  3,   4, 6,  5,   5, 6,  7,  6, 8, 7,  7, 8, 9,
                0, 10, 2,   2,  10, 12,  4, 14, 6,   6, 14, 8,
                1, 3,  11,  11, 3,  13,  5, 7,  15,  15, 7, 9
            ).also { whiteOrder ->
                DebugMode.assertArgument(whiteOrder.size == maxVertices)

                whiteMid = Primitive(whiteMidCords, whiteOrder)
                whiteMidCords.copyOf().also { whiteLeftCords ->
                    for (i in intArrayOf(0, 6, 30, 36)) whiteLeftCords[i] = whiteGap
                    whiteLeft = Primitive(whiteLeftCords, whiteOrder)
                }
                for (i in intArrayOf(3, 9, 33, 39)) whiteMidCords[i] = whiteWid - whiteGap
                whiteRight = Primitive(whiteMidCords, whiteOrder)

                for (i in intArrayOf(0, 6, 30, 36)) whiteMidCords[i] = whiteGap
                whiteFull = Primitive(whiteMidCords, whiteOrder)
            }
        }
    }

    fun drawKeyboard(
        drawKey: (key: Primitive, offset: Float, angle: Float, color: FloatArray) -> Unit
    ): Unit = keys.forEach {
        with(it) {
            drawKey(
                @Suppress("Reformat") when (key) {
                    Key.KeyType.WHITE_LEFT  -> whiteLeft
                    Key.KeyType.WHITE_RIGHT -> whiteRight
                    Key.KeyType.WHITE_MID   -> whiteMid
                    Key.KeyType.WHITE_FULL  -> whiteFull
                    Key.KeyType.BLACK       -> black
                }, offset, angle, color()
            )
        }
    }
}