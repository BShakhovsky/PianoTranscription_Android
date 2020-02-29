@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.material.snackbar.Snackbar

class AdBanner(private val adView: AdView) : AdListener() {

    init {
        with(AdRequest.Builder()) {
            if (BuildConfig.DEBUG) addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("87FD000F52337DF09DBB9E6684B0B878")
            with(adView) {
                adListener = this@AdBanner
                loadAd(build())
            }
        }
    }

    override fun onAdFailedToLoad(error: Int) {
        super.onAdFailedToLoad(error)
        if (BuildConfig.DEBUG) Snackbar.make(
            adView,
            @Suppress("Reformat")
            when (error) {
                AdRequest.ERROR_CODE_INTERNAL_ERROR     -> "Ad-banner server: invalid response"
                AdRequest.ERROR_CODE_INVALID_REQUEST    -> "Ad-banner unit ID incorrect"
                AdRequest.ERROR_CODE_NETWORK_ERROR      -> "Ad-banner: no internet connection"
                AdRequest.ERROR_CODE_NO_FILL            -> "Ad-banner: lack of ad inventory"
                else -> { Assert.argument(false); "Ad-banner error" }
            }, Snackbar.LENGTH_LONG
        ).show()
    }
}