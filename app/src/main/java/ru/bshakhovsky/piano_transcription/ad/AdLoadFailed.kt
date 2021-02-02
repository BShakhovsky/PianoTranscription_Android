package ru.bshakhovsky.piano_transcription.ad

import android.content.Context

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage

object AdLoadFailed {

    fun showError(context: Context, type: String, error: LoadAdError?) {
        if (DebugMode.debug) when (error) {
            null -> "Ad-$type: unknown error".also { DebugMode.assertArgument(false) }
            else -> with(error) {
                when (code) {
                    AdRequest.ERROR_CODE_APP_ID_MISSING ->
                        "Ad-$type request not made: missing app ID"
                    AdRequest.ERROR_CODE_INTERNAL_ERROR ->
                        "Ad-$type internal error: invalid response from ad-$type server"
                    AdRequest.ERROR_CODE_INVALID_REQUEST ->
                        "Ad-$type request invalid: incorrect unit ID"
                    AdRequest.ERROR_CODE_MEDIATION_NO_FILL ->
                        "Ad-$type mediation adapter did not fill ad-$type request" +
                                "\nUnderlying cause: ${cause?.message ?: "unknown"}"
                    AdRequest.ERROR_CODE_NETWORK_ERROR ->
                        "Ad-$type request unsuccessful: no internet connection"
                    AdRequest.ERROR_CODE_NO_FILL ->
                        "Ad-$type request successful, but no ad returned: " +
                                "lack of ad-$type inventory"
                    else -> "Ad-$type: unknown error".also { DebugMode.assertArgument(false) }
                } + "\n\n$message"
            }
        }.let { InfoMessage.toast(context, it) }
    }
}