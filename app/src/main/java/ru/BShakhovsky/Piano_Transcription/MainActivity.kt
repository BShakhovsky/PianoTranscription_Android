@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import com.google.android.gms.ads.MobileAds
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import ru.BShakhovsky.Piano_Transcription.MainUI.Toggle
import ru.BShakhovsky.Piano_Transcription.MainUI.Touch
import ru.BShakhovsky.Piano_Transcription.Midi.Midi
import ru.BShakhovsky.Piano_Transcription.Midi.MidiActivity
import ru.BShakhovsky.Piano_Transcription.OpenGL.Render

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, CompoundButton.OnCheckedChangeListener {

    internal enum class RequestCode { FILE_OPEN }
    internal enum class DrawerGroup(val id: Int) { TRACKS(1) }
    internal enum class DrawerItem(val id: Int) { CHECK_ALL(-1) }

    private lateinit var mainMenu: Menu
    private var midi: Midi? = null
    private var selTracks = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        setContentView(R.layout.activity_main)
        setSupportActionBar(mainBar)

        fileOpen.setOnClickListener { Intent(Intent.ACTION_GET_CONTENT).also { intent -> intent.type = "*/*"
            startActivityForResult(intent, RequestCode.FILE_OPEN.ordinal) } }

        drawerMenu.setNavigationItemSelectedListener(this)
        with(drawerMenu.menu) {
            fun menuView(item: Int) = findItem(item).actionView as TextView
            menuView(R.id.drawerMidi).text = getString(R.string.noMidi)
            with(menuView(R.id.drawerAll)) {
                isEnabled = false; text = getString(R.string.tracks, 0, 0)
                with(this as Switch) { id = DrawerItem.CHECK_ALL.id; setOnCheckedChangeListener(this@MainActivity) }
            }
            intArrayOf(R.id.drawerMidi, R.id.drawerAll).forEach { with(menuView(it)) {
                gravity = Gravity.CENTER_VERTICAL
                setTypeface(null, Typeface.ITALIC)
            } }
        }
        with(Toggle(this, drawerLayout, mainBar, surfaceView)) {
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
        AdBanner(adMain)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        mainMenu = menu
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean { when (item.itemId) {
        R.id.menuTracks -> drawerLayout.openDrawer(GravityCompat.START)
        R.id.menuMidi -> midi(); R.id.menuGuide -> guide() else -> Assert.state(false) }
        return true
    }
    override fun onNavigationItemSelected(item: MenuItem): Boolean { when (item.itemId) {
        R.id.drawerMidi -> midi(); R.id.drawerGuide -> guide()
        else -> {
            checkGroup(item.groupId, item.itemId)
            (item.actionView as Switch).toggle()
            return false
        } }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
    override fun onCheckedChanged(button: CompoundButton?, checked: Boolean) { when (button?.id) {
        DrawerItem.CHECK_ALL.id -> for (i in 0..(midi ?: return).tracks.lastIndex) {
            with(drawerMenu.menu.findItem(R.id.drawerTracks).subMenu.findItem(i)) {
                if (checkGroup(groupId, itemId)) (actionView as Switch).isChecked = button.isChecked } }
        else -> {
            Assert.state((midi != null) and (button?.id in 0..(midi ?: return).tracks.lastIndex))
            if ((button ?: return).isChecked) selTracks.add(button.id)
            else selTracks.remove(button.id)
            (drawerMenu.menu.findItem(R.id.drawerAll).actionView as Switch).text =
                getString(R.string.tracks, selTracks.size, midi?.tracks?.size)
            Snackbar.make(drawerLayout, "${button.id}", Snackbar.LENGTH_LONG).show()
        }
    } }
    private fun midi() { Intent(this, MidiActivity::class.java).also { intent ->
        intent.putExtra("Duration", midi?.dur)
        intent.putExtra("Summary", midi?.summary)
        intent.putExtra("Tracks", midi?.tracks?.map { it.info }?.toTypedArray())
        intent.putExtra("Percuss", midi?.percuss?.map { it.info }?.toTypedArray())
        startActivity(intent)
    } }
    private fun guide() = startActivity(Intent(this, GuideActivity::class.java))
    private fun checkGroup(groupId: Int, itemId: Int) = when (groupId) {
        0                     -> { Assert.state(itemId == R.id.drawerAll);            false }
        DrawerGroup.TRACKS.id -> { Assert.state(itemId in 0..midi!!.tracks.lastIndex); true }
        else                  -> { Assert.state(false);                               false } }

    override fun onBackPressed() { with(drawerLayout) { GravityCompat.START.also {
        if (isDrawerOpen(it)) closeDrawer(it) else super.onBackPressed() } } }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        when (requestCode) {
            RequestCode.FILE_OPEN.ordinal -> { data?.data?.let { uri ->
                contentResolver.openInputStream(uri)?.let { inStream ->
                    midi = Midi(inStream, getString(R.string.untitled))
                    fun showError(msgId: Int, titleId: Int = R.string.emptyMidi) =
                        AlertDialog.Builder(this).setTitle(titleId).setMessage(msgId).setIcon(R.drawable.info)
                            .setPositiveButton("Ok") { _, _ -> }.setCancelable(false).show()
                    when {
                        midi!!.badMidi               -> { showError(R.string.notMidi, R.string.badFile); return }
                        midi?.tracks.isNullOrEmpty() ->   showError(R.string.noTracks)
                        midi?.dur == 0L              ->   showError(R.string.zeroDur)
                    }
                    intArrayOf(R.id.menuMidi, R.id.menuTracks).forEach { mainMenu.findItem(it).isVisible = true }
                    with(drawerMenu.menu) {
                        with(findItem(R.id.drawerMidi)) { isEnabled = true
                            with(actionView as TextView) { midi?.summary?.let {
                                ((if (it.keys.isNullOrEmpty()) "" else it.keys.first().key) to (if (it.tempos.isNullOrEmpty())
                                    0 else it.tempos.first().bpm.toInt())).also { (key, tempo) -> when (midi?.dur) {
                                    in 0L..60L -> text = getString(R.string.secKeyTemp, midi?.dur?.div(1_000), key, tempo)
                                    in 60L..Long.MAX_VALUE -> text = getString(R.string.minKeyTemp,
                                        midi?.dur?.div(1_000)?.div(60f), key, tempo)
                                    else -> Assert.state(false)
                                } } }
                                setTextColor(getColor(R.color.colorAccent))
                                setTypeface(null, Typeface.BOLD_ITALIC)
                            }
                        }
                        with(findItem(R.id.drawerAll)) { isEnabled = true
                            with(actionView as Switch) { isEnabled = true
                                text = getString(R.string.tracks, 0, midi?.tracks?.size) }
                        }
                        with(findItem(R.id.drawerTracks).subMenu) { removeGroup(DrawerGroup.TRACKS.id)
                            midi?.tracks?.forEachIndexed { i, track -> with(Switch(this@MainActivity)) {
                                id = i; showText = true; textOn = "+"; textOff = ""
                                setOnCheckedChangeListener(this@MainActivity)
                                add(1, i, Menu.NONE, track.info.name).also {
                                    it.icon = getDrawable(R.drawable.queue); it.actionView = this }
                            } }
                        }
                    }
                }
            } }
            else -> Assert.argument(false)
        }
    }
}