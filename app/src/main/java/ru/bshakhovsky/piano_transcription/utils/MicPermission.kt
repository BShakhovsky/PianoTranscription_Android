package ru.bshakhovsky.piano_transcription.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.View

import androidx.fragment.app.DialogFragment

import com.google.android.material.snackbar.Snackbar

import ru.bshakhovsky.piano_transcription.R.string

object MicPermission {

    enum class RecPermission(val id: Int) {
        RECORD(30), RECORD_SETTINGS(31),
        RECOGNIZE(32), RECOGNIZE_SETTINGS(33)
    }

    fun requestPermission(
        requestCode: Int, activity: Activity?,
        fragment: DialogFragment? = null, action: () -> Unit
    ): Unit? = Manifest.permission.RECORD_AUDIO.let {
        DebugMode.assertState(activity != null)
        activity?.run {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                if (checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED)
                    fragment?.requestPermissions(arrayOf(it), requestCode)
                        ?: requestPermissions(arrayOf(it), requestCode)
                else action()
            else action()
        }
    }

    fun onRequestResult(
        settingsCode: Int, grantResults: IntArray, view: View,
        activity: Activity?, fragment: DialogFragment? = null, action: () -> Unit
    ): Unit =
        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED))
            action() else settings(settingsCode, view, activity, fragment)

    fun onSettingsResult(
        resultCode: Int, settingsCode: Int, view: View,
        activity: Activity?, fragment: DialogFragment? = null, action: () -> Unit
    ) {
        DebugMode.assertState(
            (resultCode != /*Fragment*/Activity.RESULT_OK) and (activity != null)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            if (activity?.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) action() else settings(settingsCode, view, activity, fragment)
        else action()
    }

    private fun settings(
        settingsCode: Int, view: View, activity: Activity?, fragment: DialogFragment?
    ) = Snackbar.make(view, string.grantRec, Snackbar.LENGTH_LONG).setAction(string.settings) {
        DebugMode.assertArgument(
            arrayOf(RecPermission.RECORD_SETTINGS.id, RecPermission.RECOGNIZE_SETTINGS.id)
                .contains(settingsCode)
        )
        DebugMode.assertState(activity != null)
        activity?.run {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                .let { intent ->
                    fragment?.startActivityForResult(intent, settingsCode)
                        ?: startActivityForResult(intent, settingsCode)
                }
            InfoMessage.toast(applicationContext, string.grantRec)
        }
    }.show()
}