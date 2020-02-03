@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription

import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Process
import java.io.*
import kotlin.system.exitProcess

class Crash(private val context: Context) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exception: Throwable) { with(Intent(context, MainActivity::class.java)) {
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra("Crash", "")
        if (BuildConfig.DEBUG) { StringWriter().also { stack ->
            exception.printStackTrace(PrintWriter(stack))
            putExtra("Crash", stack.toString())
            context.openFileOutput("Log ${Calendar.getInstance().time}.txt",
                Context.MODE_PRIVATE).use { it.write(stack.toString().toByteArray()) }
        } }
        context.startActivity(this) }
        Process.killProcess(Process.myPid())
        exitProcess(0)
    }
}