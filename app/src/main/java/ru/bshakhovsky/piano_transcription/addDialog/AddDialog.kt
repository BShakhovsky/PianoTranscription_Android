package ru.bshakhovsky.piano_transcription.addDialog

import android.Manifest
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar

import kotlinx.android.synthetic.main.dialog_add.frameLayout

import ru.bshakhovsky.piano_transcription.R.string
import ru.bshakhovsky.piano_transcription.databinding.DialogAddBinding

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage

class AddDialog : DialogFragment() {

    enum class RequestCode(val id: Int) { SURF(20), OPEN_MEDIA(21), OPEN_MIDI(22), WRITE_3GP(23) }
    enum class Permission(val id: Int) { RECORD(30), RECORD_SETTINGS(31) }

    private lateinit var model: AddModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        DebugMode.assertState(context != null)
        return Dialog((context ?: return super.onCreateDialog(savedInstanceState))).apply {
            model = ViewModelProvider(this@AddDialog).get(AddModel::class.java)
                .apply { initialize(lifecycle, this@AddDialog) }

            DebugMode.assertState(window != null)
            window?.run {
                setGravity(Gravity.BOTTOM or Gravity.END)
                setBackgroundDrawableResource(android.R.color.transparent)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = DialogAddBinding.inflate(inflater).apply { addModel = model }.root

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            Permission.RECORD.id ->
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ) model.writeWav()
                else settings()
            else -> DebugMode.assertArgument(false)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            Permission.RECORD_SETTINGS.id -> {
                DebugMode
                    .assertState((resultCode != FragmentActivity.RESULT_OK) and (activity != null))
                if (activity?.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) model.writeWav()
                else settings()
            }
            RequestCode.WRITE_3GP.id ->
                if (resultCode != FragmentActivity.RESULT_OK) {
                    InfoMessage.dialog(context, string.warning, string.notSaved)
                    DebugMode.assertState(dialog != null)
                    dialog?.dismiss()
                } else {
                    DebugMode.assertArgument(data != null)
                    model.startRec(data?.data)
                }

            else -> DebugMode.assertArgument(false)
        }
    }

    private fun settings() {
        DebugMode.assertState((dialog != null) and (dialog?.frameLayout != null))
        Snackbar.make(dialog?.frameLayout ?: return, string.grantRec, Snackbar.LENGTH_LONG)
            .setAction(string.settings) {
                DebugMode.assertState(context != null)
                context?.run {
                    startActivityForResult(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:$packageName")
                        ), Permission.RECORD_SETTINGS.id
                    )
                    InfoMessage.toast(applicationContext, string.grantRec)
                }
            }.show()
    }
}