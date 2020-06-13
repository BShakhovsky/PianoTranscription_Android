package ru.bshakhovsky.piano_transcription.ad

import android.content.Context
import androidx.lifecycle.Lifecycle

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

open class AdFailListener(lifecycle: Lifecycle, c: Context, private val type: String) :
    AdListener() {

    protected val context: WeakPtr<Context> = WeakPtr(lifecycle, c)

    override fun onAdFailedToLoad(error: Int): Unit = super.onAdFailedToLoad(error).also {
        if (DebugMode.debug) when (error) {
            AdRequest.ERROR_CODE_INTERNAL_ERROR -> "Ad-$type server: invalid response"
            AdRequest.ERROR_CODE_INVALID_REQUEST -> "Ad-$type unit ID incorrect"
            AdRequest.ERROR_CODE_NETWORK_ERROR -> "Ad-$type: no internet connection"
            AdRequest.ERROR_CODE_NO_FILL -> "Ad-$type: lack of ad inventory"
            else -> "Ad-$type: unknown error".also { DebugMode.assertArgument(false) }
        }.let { InfoMessage.toast(context.get(), it) }
    }
}