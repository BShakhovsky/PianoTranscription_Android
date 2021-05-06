package ru.bshakhovsky.piano_transcription.addDialog

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider

import ru.bshakhovsky.piano_transcription.R.string
import ru.bshakhovsky.piano_transcription.databinding.DialogAddBinding

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage
import ru.bshakhovsky.piano_transcription.utils.MicPermission

class AddDialog : DialogFragment() {

    enum class RequestCode(val id: Int) { /*SURF(20)*/OPEN_MEDIA(21), OPEN_MIDI(22), WRITE_3GP(23) }

    private lateinit var binding: DialogAddBinding
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
    ): View = DialogAddBinding.inflate(inflater).apply {
        binding = this
        addModel = model
    }.root

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ): Unit = super.onRequestPermissionsResult(requestCode, permissions, grantResults).also {
        when (requestCode) {
            MicPermission.RecPermission.RECORD.id -> MicPermission.onRequestResult(
                MicPermission.RecPermission.RECORD_SETTINGS.id, grantResults,
                binding.root, activity, this
            ) { model.writeWav() }
            else -> DebugMode.assertArgument(false)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Unit =
        super.onActivityResult(requestCode, resultCode, data).also {
            when (requestCode) {
                MicPermission.RecPermission.RECORD_SETTINGS.id -> MicPermission.onSettingsResult(
                    resultCode, MicPermission.RecPermission.RECORD_SETTINGS.id,
                    binding.root, activity, this
                ) { model.writeWav() }

                RequestCode.WRITE_3GP.id ->
                    if (resultCode != FragmentActivity.RESULT_OK) {
                        InfoMessage.dialog(
                            context, string.warning,
                            getString(string.notSaved, getString(string.record))
                        )
                        DebugMode.assertState(dialog != null)
                        dialog?.dismiss()
                    } else {
                        DebugMode.assertArgument(data != null)
                        model.startRec(data?.data)
                    }

                else -> DebugMode.assertArgument(false)
            }
        }
}