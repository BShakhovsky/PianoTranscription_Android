package ru.bshakhovsky.piano_transcription.ad

import android.content.Context
import android.widget.FrameLayout

import androidx.annotation.StringRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

import ru.bshakhovsky.piano_transcription.R.string.bannerDebug

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

class AdBanner(
    lifecycle: Lifecycle, c: Context, adContainer: FrameLayout, @StringRes bannerId: Int
) : AdListener(), LifecycleObserver {

    private val context = WeakPtr(lifecycle, c)

    /* Unfortunately, if Banner-AdView is hard-coded in corresponding XML layout file,
    it won't be destroyed and additional memory will be leaked each time orientation changes, see:
    https://groups.google.com/g/google-admob-ads-sdk/c/9IyjqdmeumM

    So, have to programmatically create and destroy Banner-AdView
    https://stackoverflow.com/a/30925591
    https://stackoverflow.com/questions/6148812/android-admob-causes-memory-leak/30925591#30925591

    However, Interstitial-AdView may still leak */
    private val adView = AdView(c).apply {
        adSize = AdSize.BANNER
        adUnitId = context.getString(if (DebugMode.debug) bannerDebug else bannerId)
        adListener = this@AdBanner

        adContainer.addView(this)
        loadAd(AdRequest.Builder().build())

        lifecycle.addObserver(this@AdBanner)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun destroyAd() = with(adView) {
//        adListener = null
        DebugMode.assertState(parent is FrameLayout)
        (parent as FrameLayout).removeAllViews()
        destroy()
    }

    override fun onAdFailedToLoad(error: LoadAdError?) {
        DebugMode.assertArgument(error != null)
        error?.let { super.onAdFailedToLoad(it) }
        AdLoadFailed.showError(context.get(), "banner", error)
    }
}