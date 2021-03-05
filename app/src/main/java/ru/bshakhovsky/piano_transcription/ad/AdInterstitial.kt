package ru.bshakhovsky.piano_transcription.ad

import android.app.Activity

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError

import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

class AdInterstitial(lifecycle: Lifecycle, a: Activity) :
    FullScreenContentCallback(), LifecycleObserver {

    private val activity = WeakPtr(lifecycle, a)
    private var adInter: InterstitialAd? = null

    init {
        lifecycle.addObserver(this)
        load()
    }

    private fun load() = activity.get().applicationContext.let { appContext ->
        InterstitialAd.load( // TODO Interstitial unit ID
            appContext, "ca-app-pub-3940256099942544/1033173712", AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) = super.onAdLoaded(ad)
                    .also { adInter = ad.apply { fullScreenContentCallback = this@AdInterstitial } }

                override fun onAdFailedToLoad(error: LoadAdError) = super.onAdFailedToLoad(error)
                    .also { AdLoadFailed.showError(appContext, "interstitial", error) }
            }
        )
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun destroyAd() = run { adInter = null }

    fun show(): Unit = with(activity.get()) {
        adInter?.show(this) ?: load().also {
            if (DebugMode.debug)
                InfoMessage.toast(applicationContext, "Ad-interstitial was not loaded")
        }
    }

    override fun onAdDismissedFullScreenContent(): Unit =
        super.onAdDismissedFullScreenContent().also { load() }
}