package ru.bshakhovsky.piano_transcription.main

import android.content.Intent

import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.FragmentManager

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

import ru.bshakhovsky.piano_transcription.addDialog.AddDialog
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

class MainModel : ViewModel() {

    private lateinit var fragment: WeakPtr<FragmentManager>

    private lateinit var getMidi: WeakPtr<ActivityResultLauncher<Intent>>
    private lateinit var getMedia: WeakPtr<ActivityResultLauncher<Intent>>

    val contVis: MutableLiveData<Int> = MutableLiveData()

    fun initialize(
        lifecycle: Lifecycle, f: FragmentManager,
        midi: ActivityResultLauncher<Intent>, media: ActivityResultLauncher<Intent>
    ) {
        fragment = WeakPtr(lifecycle, f)
        getMidi = WeakPtr(lifecycle, midi)
        getMedia = WeakPtr(lifecycle, media)
    }

    /* TODO: Samsung Galaxy Grand Prime Plus, Android 6.0 (SDK 23)
        java.lang.IllegalStateException at androidx.fragment.app.FragmentManager.checkStateLoss
        -
        From https://github.com/Karumi/Dexter/issues/112#issuecomment-277559367
        Apparently a known issue on Marshmallow:
        https://code.google.com/p/android/issues/detail?id=190966
        -
        Possible workarounds:
        https://medium.com/@alvaro.blanco/avoiding-illegalstateexception-for-dialogfragments-6a8f31c4ce73
        https://stackoverflow.com/questions/54083136/dialogfragment-illegalstateexception-when-screen-goes-off */
    @Suppress("LongLine", "SpellCheckingInspection")
    fun dialogAdd(): Unit = AddDialog(getMidi, getMedia).show(fragment.get(), "Dialog Add")
}