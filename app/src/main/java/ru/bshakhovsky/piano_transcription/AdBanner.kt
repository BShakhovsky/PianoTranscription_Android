package ru.bshakhovsky.piano_transcription

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

import com.google.android.material.snackbar.Snackbar

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

class AdBanner(lifecycle: Lifecycle, ad: AdView) : AdListener(), LifecycleObserver {

    private val adView = WeakPtr(lifecycle, ad)

    init {
        lifecycle.addObserver(this)
        with(adView.get()) {
            adListener = this@AdBanner
            /* TODO: Load in background thread,
                otherwise SpectrumActivity is leaking after device rotation */
            loadAd(AdRequest.Builder().build())
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun destroyAd() = with(adView.get()) {
        adListener = null
        removeAllViews()
        destroy()
    }

    override fun onAdFailedToLoad(error: Int): Unit = super.onAdFailedToLoad(error).also {
        if (DebugMode.debug) when (error) {
            AdRequest.ERROR_CODE_INTERNAL_ERROR -> "Ad-banner server: invalid response"
            AdRequest.ERROR_CODE_INVALID_REQUEST -> "Ad-banner unit ID incorrect"
            AdRequest.ERROR_CODE_NETWORK_ERROR -> "Ad-banner: no internet connection"
            AdRequest.ERROR_CODE_NO_FILL -> "Ad-banner: lack of ad inventory"
            else -> "Ad-banner error".also { DebugMode.assertArgument(false) }
        }.let { Snackbar.make(adView.get(), it, Snackbar.LENGTH_LONG).show() }
    }
}