@file:Suppress("SpellCheckingInspection")

package ru.bshakhovsky.piano_transcription.main.openGL

import android.opengl.GLES30

typealias GLES = GLES30 /* Consistent with Manifest's uses-feature glEsVersion="0x00030000"
    Old phones with API <= 6.0 Marshmallow (SDK 23) have OpenGL ES 3.0 & 3.1
    GLES31 or GLES32 would cause crash immediately at Render.onSurfaceCreated:

    java.lang.NoClassDefFoundError: Failed resolution of: Landroid/opengl/GLES32;
    Caused by: java.lang.ClassNotFoundException: Didn't find class "android.opengl.GLES32" on path:
        DexPathList[[zip file "/data/app/ru.BShakhovsky.Piano_Transcription-1/base.apk"],
        nativeLibraryDirectories=[/data/app/ru.BShakhovsky.Piano_Transcription-1/lib/arm,
        /data/app/ru.BShakhovsky.Piano_Transcription-1/base.apk!/lib/armeabi-v7a,
        /vendor/lib, /system/lib]]
    Suppressed: java.lang.ClassNotFoundException: android.opengl.GLES32
    Caused by: java.lang.NoClassDefFoundError:
        Class not found using the boot class loader; no stack trace available */