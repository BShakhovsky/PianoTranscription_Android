@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import ru.BShakhovsky.Piano_Transcription.MainUI.MenuListener
import ru.BShakhovsky.Piano_Transcription.MainUI.Toggle
import ru.BShakhovsky.Piano_Transcription.MainUI.Touch
import ru.BShakhovsky.Piano_Transcription.OpenGL.Render

class MainActivity : AppCompatActivity() {

    private lateinit var menuListener: MenuListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        menuListener = MenuListener(drawerLayout)
        drawerMenu.setNavigationItemSelectedListener(menuListener)
        with(drawerMenu.menu) {
            fun menuView(item: Int) = findItem(item).actionView as TextView
            menuView(R.id.drawerTracks).text = getString(R.string.tracks, 0, 0)
            menuView(R.id.drawerMidi).text = getString(R.string.noMidi)
            intArrayOf(R.id.drawerTracks, R.id.drawerMidi).forEach { with(menuView(it)) {
                gravity = Gravity.CENTER_VERTICAL
                setTypeface(null, Typeface.ITALIC)
            } }
        }
        with(Toggle(this, drawerLayout, toolbar, surfaceView)) {
            drawerLayout.addDrawerListener(this); syncState() }

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
        with(AdRequest.Builder()) {
            if (BuildConfig.DEBUG) addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("87FD000F52337DF09DBB9E6684B0B878")
            with(adView) { adListener = AdBanner(this); loadAd(build()) }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu); return true }

    override fun onOptionsItemSelected(item: MenuItem): Boolean { when (item.itemId) {
        R.id.menuTracks -> menuListener.tracks()
        R.id.menuMidi   -> menuListener.midi()
        R.id.menuGuide  -> menuListener.guide()
        else -> Assert.state(false) }
        return true
    }

    override fun onBackPressed() { with(drawerLayout) { GravityCompat.START.also {
        if (isDrawerOpen(it)) closeDrawer(it) else super.onBackPressed() } } }
}