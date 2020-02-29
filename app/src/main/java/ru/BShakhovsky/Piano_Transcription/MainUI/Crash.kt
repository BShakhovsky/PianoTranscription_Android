@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.MainUI

import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Process
import ru.BShakhovsky.Piano_Transcription.BuildConfig
import ru.BShakhovsky.Piano_Transcription.MainActivity

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter

import kotlin.system.exitProcess

class Crash(private val context: Context) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        with(Intent(context, MainActivity::class.java)) {
            addFlags(
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK or
                        Intent.FLAG_ACTIVITY_NEW_TASK
            )
            putExtra("Crash", "")
            if (BuildConfig.DEBUG) {
                StringWriter().also { stack ->
                    exception.printStackTrace(PrintWriter(stack))
                    putExtra("Crash", stack.toString())
                    FileOutputStream(
                        File(
                            context.getExternalFilesDir("Errors"),
                            "${Calendar.getInstance().time}.txt"
                        )
                    ).use { it.write(stack.toString().toByteArray()) }
                }
            }
            context.startActivity(this)
        }
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }
}