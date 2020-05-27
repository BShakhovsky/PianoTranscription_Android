package ru.bshakhovsky.piano_transcription.utils

import android.app.Activity
import android.os.Build
import android.os.StrictMode
import android.view.Gravity
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import java.util.concurrent.Executors

class StrictPolicy(lifecycle: Lifecycle, a: Activity) {

    private val activity = WeakPtr(lifecycle, a)

    init {
        StrictMode.enableDefaults()

        StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                detectContentUriWithoutPermission()//.detectUntaggedSockets()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { //detectNonSdkApiUsage().
                    penaltyListener(
                        Executors.newSingleThreadExecutor(),
                        StrictMode.OnVmViolationListener {
                            with(activity.get()) {
                                runOnUiThread {
                                    Toast.makeText(
                                        applicationContext,
                                        it.localizedMessage ?: "Unknown Vm policy violation",
                                        Toast.LENGTH_LONG
                                    ).apply { setGravity(Gravity.CENTER, 0, 0) }.show()
                                }
                            }
                        })
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                        detectCredentialProtectedWhileLocked().detectImplicitDirectBoot()
                }
            }
        } //.detectAll().detectCleartextNetwork()
            .detectActivityLeaks().detectFileUriExposure().detectLeakedClosableObjects()
            .detectLeakedRegistrationObjects().detectLeakedSqlLiteObjects().penaltyLog().build())

        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                detectUnbufferedIo()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) penaltyListener(
                    Executors.newSingleThreadExecutor(),
                    StrictMode.OnThreadViolationListener { v ->
                        with(activity.get()) {
                            runOnUiThread {
                                Toast.makeText(
                                    applicationContext,
                                    v.localizedMessage ?: "Unknown Thread policy violation",
                                    Toast.LENGTH_LONG
                                ).apply { setGravity(Gravity.CENTER, 0, 0) }.show()
                            }
                        }
                    })
            }
        } //.detectAll().detectCustomSlowCalls().detectDiskReads().detectDiskWrites()
            .detectNetwork().detectResourceMismatches().penaltyDialog().penaltyLog().build())
    }
}