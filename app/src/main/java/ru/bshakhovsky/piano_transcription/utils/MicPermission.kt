package ru.bshakhovsky.piano_transcription.utils

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle

import com.google.android.material.snackbar.Snackbar

import ru.bshakhovsky.piano_transcription.R.string

class MicPermission(
    lifecycle: Lifecycle, v: View, activity: ComponentActivity,
    fragment: Fragment? = null, private val action: () -> Unit
) {

    private val activity = WeakPtr(lifecycle, activity)
    private val view = WeakPtr(lifecycle, v)

    private val recPermission = (fragment ?: activity)
        .registerForActivityResult(ActivityResultContracts.RequestPermission())
        { if (it) action() else settings() }
    private val recSettings = (fragment ?: activity)
        .registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            DebugMode.assertState(it.resultCode != /*Fragment*/ComponentActivity.RESULT_OK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) action() else settings()
            else action()
        }

    // TODO: Turning the mic on takes a long time
    fun requestPermission(): Unit = Manifest.permission.RECORD_AUDIO.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            if (activity.get().checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED)
                action() else recPermission.launch(it)
        else action()
    }

    private fun settings(): Unit = Snackbar.make(view.get(), string.grantRec, Snackbar.LENGTH_LONG)
        .setAction(string.settings) {
            activity.get().run {
                recSettings.launch(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:$packageName")
                    )
                )
                InfoMessage.toast(applicationContext, string.grantRec)
            }
        }.show()
}