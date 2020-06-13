package ru.bshakhovsky.piano_transcription.ad

import android.content.Context

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView

import ru.bshakhovsky.piano_transcription.utils.WeakPtr

class AdBanner(lifecycle: Lifecycle, ad: AdView, context: Context) :
    AdFailListener(lifecycle, context, "banner"), LifecycleObserver {

    private val adView = WeakPtr(lifecycle, ad)

    init {
        lifecycle.addObserver(this)
        with(adView.get()) {
            adListener = this@AdBanner
            loadAd(AdRequest.Builder().build())
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun destroyAd() = with(adView.get()) {
        adListener = null
        removeAllViews()
        destroy()
    }
}