@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.MainUI

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import ru.BShakhovsky.Piano_Transcription.Utils.DebugMode
import ru.BShakhovsky.Piano_Transcription.MainActivity

import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter

import kotlin.system.exitProcess

class Crash(private val context: Context) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        @Suppress("SpellCheckingInspection")
        // https://medium.com/@ssaurel/how-to-auto-restart-an-android-
        // application-after-a-crash-or-a-force-close-error-1a361677c0ce
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)[
                AlarmManager.RTC, System.currentTimeMillis() + 100] =
            PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_NEW_TASK
                )
                putExtra("Crash", "")
                if (DebugMode.debug) FileOutputStream(
                    File(
                        context.getExternalFilesDir("Errors"), "${Calendar.getInstance().time}.txt"
                    )
                ).use {
                    it.write(StringWriter().let { str ->
                        with(exception) {
                            printStackTrace(PrintWriter(str))
                            putExtra("Crash", localizedMessage)
                        }
                        str.toString().toByteArray()
                    })
                }
            }, PendingIntent.FLAG_ONE_SHOT)
        exitProcess(1)
    }
}