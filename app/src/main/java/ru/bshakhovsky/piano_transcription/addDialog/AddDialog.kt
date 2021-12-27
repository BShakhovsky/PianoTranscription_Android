package ru.bshakhovsky.piano_transcription.addDialog

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider

import ru.bshakhovsky.piano_transcription.R.string
import ru.bshakhovsky.piano_transcription.databinding.DialogAddBinding

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage
import ru.bshakhovsky.piano_transcription.utils.MicPermission
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

import java.util.Calendar

class AddDialog(
    private val getMidi: WeakPtr<ActivityResultLauncher<Intent>>,
    private val getMedia: WeakPtr<ActivityResultLauncher<Intent>>
) : AppCompatDialogFragment() {

    private lateinit var binding: DialogAddBinding
    private lateinit var model: AddModel

    private val fileName3GP =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            with(it) {
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
            }
        }
    private lateinit var recPermission: MicPermission

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        DebugMode.assertState(context != null)
        return Dialog((context ?: return super.onCreateDialog(savedInstanceState))).apply {
            model = ViewModelProvider(this@AddDialog)[AddModel::class.java]
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
        recPermission = MicPermission(lifecycle, root, requireActivity(), this@AddDialog) {
            fileName3GP.launch(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = "audio/3gp"
                addCategory(Intent.CATEGORY_OPENABLE) // don't show list of contacts or timezones
                putExtra(
                    Intent.EXTRA_TITLE,
                    "${getString(string.record)} ${Calendar.getInstance().time}.3gp"
                )
                InfoMessage.toast(context, getString(string.save, getString(string.record)))
            })
        }
        addModel = model
            .apply { initialize(lifecycle, this@AddDialog, getMidi, getMedia, recPermission) }
    }.root
}