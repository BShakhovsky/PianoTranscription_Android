package ru.bshakhovsky.piano_transcription.utils

import android.app.Activity
import android.os.Build
import android.os.StrictMode
import androidx.lifecycle.Lifecycle
import java.util.concurrent.Executors

class StrictPolicy(lifecycle: Lifecycle, a: Activity) {

    private val activity = WeakPtr(lifecycle, a)

    init {
        if (DebugMode.debug) {
            StrictMode.enableDefaults()

            StrictMode.setVmPolicy(StrictMode.VmPolicy.Builder().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    detectContentUriWithoutPermission()//.detectUntaggedSockets()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { //detectNonSdkApiUsage().
                        penaltyListener(Executors.newSingleThreadExecutor()) {
                            with(activity.get()) {
                                runOnUiThread {
                                    InfoMessage.toast(
                                        applicationContext,
                                        it.localizedMessage ?: "Unknown Vm policy violation"
                                    )
                                }
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                            detectCredentialProtectedWhileLocked().detectImplicitDirectBoot()
                    }
                }
            } //.detectAll().detectCleartextNetwork()
                .detectActivityLeaks().detectFileUriExposure().detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects().detectLeakedSqlLiteObjects()
                .penaltyLog().build())

            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    detectUnbufferedIo()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                        penaltyListener(Executors.newSingleThreadExecutor()) {
                            with(activity.get()) {
                                runOnUiThread {
                                    InfoMessage.toast(
                                        applicationContext,
                                        it.localizedMessage ?: "Unknown Thread policy violation"
                                    )
                                }
                            }
                        }
                }
            } //.detectAll().detectCustomSlowCalls().detectDiskReads().detectDiskWrites()
                .detectNetwork().detectResourceMismatches().penaltyDialog().penaltyLog().build())
        }
    }
}