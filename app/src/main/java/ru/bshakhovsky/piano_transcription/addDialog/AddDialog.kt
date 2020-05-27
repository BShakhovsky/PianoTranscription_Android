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
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar

import kotlinx.android.synthetic.main.dialog_add.record

import ru.bshakhovsky.piano_transcription.R.string
import ru.bshakhovsky.piano_transcription.databinding.DialogAddBinding

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.MessageDialog

class AddDialog : DialogFragment() {

    enum class RequestCode(val id: Int) { OPEN_MEDIA(10), OPEN_MIDI(11), WRITE_3GP(12) }
    enum class Permission(val id: Int) { RECORD(20), RECORD_SETTINGS(21) }

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
                // if (resultCode != FragmentActivity.RESULT_OK) for some reason it is never equal
                DebugMode.assertState(activity != null)
                if (activity?.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) model.writeWav()
                else settings()
            }
            RequestCode.WRITE_3GP.id ->
                if (resultCode != FragmentActivity.RESULT_OK) {
                    MessageDialog.show(context, string.warning, string.notSaved)
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
        DebugMode.assertState((dialog != null) and (dialog?.record != null))
        dialog?.record?.let {
            Snackbar.make(it, string.grantRec, Snackbar.LENGTH_LONG)
                .setAction(string.settings) {
                    DebugMode.assertState(context != null)
                    context?.run {
                        startActivityForResult(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:$packageName")
                            ), Permission.RECORD_SETTINGS.id
                        )
                        Toast.makeText(applicationContext, string.grantRec, Toast.LENGTH_LONG)
                            .apply { setGravity(Gravity.CENTER, 0, 0) }.show()
                    }
                }.show()
        }
    }
}