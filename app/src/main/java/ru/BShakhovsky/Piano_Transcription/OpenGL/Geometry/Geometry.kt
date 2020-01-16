@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.OpenGL.Geometry

class Geometry {

    companion object { const val blackLen = 85f; const val blackWid = 9
        const val whiteLen = 145f; const val whiteWid = 23f; const val overallLen = whiteWid * 52
        const val deskOver = whiteWid * 3; const val deskHeight = whiteWid * 8; const val deskThick = whiteWid * 1.5f
    }
    private val whiteGap = .6f; private val blackFillet = 3

    private val whiteLeft: Primitive; private val whiteMid: Primitive; private val whiteRight: Primitive; private val whiteFull: Primitive
    private val black    = Primitive(floatArrayOf(
            whiteWid - blackWid / 2,                  whiteWid + blackWid,    0f,
            whiteWid + blackWid / 2,                  whiteWid + blackWid,    0f,
            whiteWid - blackWid / 2,                  whiteWid + blackWid,    blackLen - 3 * blackFillet,
            whiteWid + blackWid / 2,                  whiteWid + blackWid,    blackLen - 3 * blackFillet,
            whiteWid - blackWid / 2 - blackFillet,    whiteWid - blackWid,    blackLen,
            whiteWid + blackWid / 2 + blackFillet,    whiteWid - blackWid,    blackLen,
            whiteWid - blackWid / 2 - blackFillet,    whiteWid - blackWid,    0f,
            whiteWid + blackWid / 2 + blackFillet,    whiteWid - blackWid,    0f),
        intArrayOf(0, 2, 1,  1, 2, 3,    2, 4, 3,  3, 4, 5,    0, 6, 2,  2, 6, 4,    1, 3, 7,  7, 3, 5))
    val desk    = Primitive(floatArrayOf(- deskOver,    0f,            0f,
                              overallLen + deskOver,    0f,            0f,
                                         - deskOver,    deskHeight,    0f,
                              overallLen + deskOver,    deskHeight,    0f,

                                         - deskOver,    0f,            -deskThick,
                              overallLen + deskOver,    0f,            -deskThick,
                                         - deskOver,    deskHeight,    -deskThick,
                              overallLen + deskOver,    deskHeight,    -deskThick),
        intArrayOf(0, 1, 2,  2, 1, 3,    2, 3, 6,  6, 3, 7,    0, 2, 6,  6, 4, 0,    1, 5, 3,  3, 5, 7),
        floatArrayOf(1f, 1f, 1f,    1f, 1f, 1f))

    val keys = Array(88){ Key(it) }

    init { floatArrayOf(blackWid / 2f + blackFillet,    whiteWid,    0f,
             whiteWid - blackWid / 2  - blackFillet,    whiteWid,    0f,
                        blackWid / 2f + blackFillet,    whiteWid,    blackLen,
             whiteWid - blackWid / 2  - blackFillet,    whiteWid,    blackLen,
             whiteGap                              ,    whiteWid,    blackLen,
             whiteWid - whiteGap                   ,    whiteWid,    blackLen,

             whiteGap                              ,    whiteWid,    whiteLen,
             whiteWid - whiteGap                   ,    whiteWid,    whiteLen,
             whiteGap                              ,    0f      ,    whiteLen,
             whiteWid - whiteGap                   ,    0f      ,    whiteLen,

                        blackWid / 2f + blackFillet,    0f      ,    0f,
             whiteWid - blackWid / 2  - blackFillet,    0f      ,    0f,
                        blackWid / 2f + blackFillet,    0f      ,    blackLen,
             whiteWid - blackWid / 2  - blackFillet,    0f      ,    blackLen,
             whiteGap                              ,    0f      ,    blackLen,
             whiteWid - whiteGap                   ,    0f      ,    blackLen
    ).also { whiteMidCords -> intArrayOf(0, 2,  1,   1,  2,  3,   4, 6,  5,   5, 6,  7,  6, 8, 7,  7, 8, 9,
                                         0, 10, 2,   2,  10, 12,  4, 14, 6,   6, 14, 8,
                                         1, 3,  11,  11, 3,  13,  5, 7,  15,  15, 7, 9).also { whiteOrder ->

        whiteMid = Primitive(whiteMidCords, whiteOrder)
        whiteMidCords.copyOf().also { whiteLeftCords ->
            for (i in intArrayOf(0, 6, 30, 36)) whiteLeftCords[i] = whiteGap
            whiteLeft = Primitive(whiteLeftCords, whiteOrder)
        }
        for (i in intArrayOf(3, 9, 33, 39)) whiteMidCords[i] = whiteWid - whiteGap
        whiteRight = Primitive(whiteMidCords, whiteOrder)

        for (i in intArrayOf(0, 6, 30, 36)) whiteMidCords[i] = whiteGap
        whiteFull = Primitive(whiteMidCords, whiteOrder)
    } } }

    fun drawKeyboard(drawKey: (key: Primitive, offset: Float, angle: Float, color: FloatArray) -> Unit) {
        keys.forEach { with(it) { drawKey(when (key) { Key.Companion.KeyType.WHITE_LEFT  -> whiteLeft
                                                       Key.Companion.KeyType.WHITE_RIGHT -> whiteRight
                                                       Key.Companion.KeyType.WHITE_MID   -> whiteMid
                                                       Key.Companion.KeyType.WHITE_FULL  -> whiteFull
                                                       Key.Companion.KeyType.BLACK       -> black }, offset, angle, color()) } }
    }
}