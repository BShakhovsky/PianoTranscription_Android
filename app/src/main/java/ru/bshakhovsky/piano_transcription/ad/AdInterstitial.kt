package ru.bshakhovsky.piano_transcription.ad

import android.content.Context

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage

class AdInterstitial(lifecycle: Lifecycle, context: Context) :
    AdFailListener(lifecycle, context, "interstitial"), LifecycleObserver {

    private val adInter = InterstitialAd(context).apply {
        adUnitId = "ca-app-pub-3940256099942544/1033173712"
        adListener = this@AdInterstitial
    }

    init {
        lifecycle.addObserver(this)
        load()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun destroyAd() = with(adInter) { adListener = null }

    override fun onAdClosed(): Unit = super.onAdClosed().also { load() }

    fun show(): Unit = with(adInter) {
        when {
            isLoaded -> show()
            DebugMode.debug -> InfoMessage.toast(context.get(), "Ad-interstitial was not loaded")
        }
    }

    private fun load(): Unit = adInter.loadAd(AdRequest.Builder().build())
}