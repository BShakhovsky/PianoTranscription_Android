@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.adView
import kotlinx.android.synthetic.main.activity_main.fab
import kotlinx.android.synthetic.main.activity_main.toolbar
import kotlinx.android.synthetic.main.content_main.surfaceView
import ru.BShakhovsky.Piano_Transcription.MainUI.Touch
import ru.BShakhovsky.Piano_Transcription.OpenGL.Render

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        with(surfaceView) {
            setEGLContextClientVersion(3)
            /* https://www.khronos.org/registry/EGL/sdk/docs/man/html/eglChooseConfig.xhtml
                EGL implementors are strongly discouraged, but not proscribed,
                from changing the selection algorithm used by eglChooseConfig.
                Therefore, selections may change from release to release of the client-side library. */
//            setEGLConfigChooser(EGLChooser(depth = 1, stencil = 1))
            Render(context).also { setRenderer(it); setOnTouchListener(Touch(it)) }
        }

        MobileAds.initialize(this)
        with(AdRequest.Builder()){ if (BuildConfig.DEBUG)
            addTestDevice(AdRequest.DEVICE_ID_EMULATOR).addTestDevice("87FD000F52337DF09DBB9E6684B0B878")
            adView.adListener = object : AdListener() { override fun onAdFailedToLoad(error: Int) { Snackbar.make(adView, when (error) {
                AdRequest.ERROR_CODE_INTERNAL_ERROR  -> "Ad-banner server: invalid response"
                AdRequest.ERROR_CODE_INVALID_REQUEST -> "Ad-banner unit ID incorrect"
                AdRequest.ERROR_CODE_NETWORK_ERROR   -> "Ad-banner: network connectivity issue"
                AdRequest.ERROR_CODE_NO_FILL         -> "Ad-banner: lack of ad inventory"
                else                                 -> "Ad-banner error" }, Snackbar.LENGTH_LONG).show() } }
            adView.loadAd(build())
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}