package ru.bshakhovsky.piano_transcription.utils

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Calendar

import kotlin.system.exitProcess

/* File is not WeakReference, because we need this variable on crash for writing log,
and cannot be sure that the variable's destructor itself
(its lifecycle callback) will not be called first (cleaned by GC) */
class Crash(private val extErrorDir: File?) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        if (DebugMode.debug)
            FileOutputStream(File(extErrorDir, "${Calendar.getInstance().time}.txt")).use {
                it.write(StringWriter().let { str ->
                    exception.printStackTrace(PrintWriter(str))
                    str.toString().toByteArray()
                })
            }
        exitProcess(1)
    }
}