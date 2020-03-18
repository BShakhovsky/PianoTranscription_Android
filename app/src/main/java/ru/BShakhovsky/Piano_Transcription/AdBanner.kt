@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.snackbar.Snackbar
import ru.BShakhovsky.Piano_Transcription.Utils.DebugMode

class AdBanner(private val adView: AdView) : AdListener() {

    init {
        with(adView) {
            adListener = this@AdBanner
            loadAd(AdRequest.Builder().build())
        }
    }

    override fun onAdFailedToLoad(error: Int) {
        super.onAdFailedToLoad(error)
        if (DebugMode.debug) Snackbar.make(
            adView, when (error) {
                AdRequest.ERROR_CODE_INTERNAL_ERROR -> "Ad-banner server: invalid response"
                AdRequest.ERROR_CODE_INVALID_REQUEST -> "Ad-banner unit ID incorrect"
                AdRequest.ERROR_CODE_NETWORK_ERROR -> "Ad-banner: no internet connection"
                AdRequest.ERROR_CODE_NO_FILL -> "Ad-banner: lack of ad inventory"
                else -> "Ad-banner error".also { DebugMode.assertArgument(false) }
            }, Snackbar.LENGTH_LONG
        ).show()
    }
}