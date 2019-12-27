@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription

import android.opengl.GLSurfaceView
import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        surfaceView.setEGLContextClientVersion(3)
        PianoRenderer().also { render ->
            surfaceView.setRenderer(render)
            surfaceView.setOnTouchListener(PianoTouchListener(render))
        }
        surfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY

        MobileAds.initialize(this)
        with(AdRequest.Builder()){
            if (BuildConfig.DEBUG) addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
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