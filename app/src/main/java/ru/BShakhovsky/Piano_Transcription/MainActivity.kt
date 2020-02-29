@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle

import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View

import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import com.google.android.gms.ads.MobileAds
import com.google.android.material.navigation.NavigationView

import kotlinx.android.synthetic.main.activity_main.adMain
import kotlinx.android.synthetic.main.activity_main.control
import kotlinx.android.synthetic.main.activity_main.curTime
import kotlinx.android.synthetic.main.activity_main.drawerLayout
import kotlinx.android.synthetic.main.activity_main.drawerMenu
import kotlinx.android.synthetic.main.activity_main.durTime
import kotlinx.android.synthetic.main.activity_main.mainBar
import kotlinx.android.synthetic.main.activity_main.mainLayout
import kotlinx.android.synthetic.main.activity_main.newMedia
import kotlinx.android.synthetic.main.activity_main.next
import kotlinx.android.synthetic.main.activity_main.playPause
import kotlinx.android.synthetic.main.activity_main.prev
import kotlinx.android.synthetic.main.activity_main.seek
import kotlinx.android.synthetic.main.activity_main.soundBar
import kotlinx.android.synthetic.main.activity_main.soundCount
import kotlinx.android.synthetic.main.activity_main.surfaceView

import ru.BShakhovsky.Piano_Transcription.MainUI.Crash
import ru.BShakhovsky.Piano_Transcription.MainUI.Toggle
import ru.BShakhovsky.Piano_Transcription.MainUI.Touch
import ru.BShakhovsky.Piano_Transcription.Midi.Midi
import ru.BShakhovsky.Piano_Transcription.Midi.MidiActivity
import ru.BShakhovsky.Piano_Transcription.OpenGL.Render

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    CompoundButton.OnCheckedChangeListener, View.OnClickListener, SeekBar.OnSeekBarChangeListener {

    companion object {
        fun msgDialog(
            context: Context, titleId: Int, msgStr: String,
            okId: Int = R.string.ok, okAction: (() -> Unit) = {}
        ): AlertDialog = AlertDialog.Builder(context).setTitle(titleId).setMessage(msgStr)
            .setIcon(R.drawable.info).setPositiveButton(okId) { _, _ -> okAction() }
            .setCancelable(false).show()

        fun msgDialog(
            context: Context, titleId: Int, msgId: Int,
            okId: Int = R.string.ok, okAction: (() -> Unit) = {}
        ): AlertDialog = msgDialog(context, titleId, context.getString(msgId), okId, okAction)
    }

    internal enum class DrawerGroup(val id: Int) { TRACKS(1) }
    internal enum class DrawerItem(val id: Int) { CHECK_ALL(-1) }

    private lateinit var mainMenu: Menu
    private lateinit var render: Render
    private var midi: Midi? = null
    private var play: Play? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        setContentView(R.layout.activity_main)
        setSupportActionBar(mainBar)

        Thread.setDefaultUncaughtExceptionHandler(Crash(this))
        if (intent.hasExtra("Crash")) AlertDialog.Builder(this).setTitle(R.string.error).apply {
            if (BuildConfig.DEBUG) setMessage(intent.getStringExtra("Crash"))
            else setMessage(R.string.crash)
        }.setIcon(R.drawable.info).setPositiveButton("Ok") { _, _ -> }.setCancelable(false).show()

        with(surfaceView) {
            setEGLContextClientVersion(3)
            render = Render(context, playPause, prev, next, soundBar, soundCount)
            setRenderer(render)
            setOnTouchListener(Touch(render))
        }

        drawerMenu.setNavigationItemSelectedListener(this)
        with(drawerMenu.menu) {
            intArrayOf(R.id.drawerMidi, R.id.drawerAll).forEach {
                with(findItem(it).actionView as TextView) {
                    gravity = Gravity.CENTER_VERTICAL
                    setTextColor(getColor(R.color.colorAccent))
                }
            }
            with(findItem(R.id.drawerAll).actionView as Switch) {
                id = DrawerItem.CHECK_ALL.id
                setOnCheckedChangeListener(this@MainActivity)
            }
        }
        midiEnabled(false)
        tracksEnabled(false)
        with(Toggle(this, drawerLayout, mainBar, mainLayout)) {
            drawerLayout.addDrawerListener(this)
            syncState()
        }

        arrayOf<View>(newMedia, playPause, prev, next).forEach { it.setOnClickListener(this) }
        seek.setOnSeekBarChangeListener(this)

        MobileAds.initialize(this)
        AdBanner(adMain)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        mainMenu = menu
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menuTracks -> drawerLayout.openDrawer(GravityCompat.START)
            R.id.menuMidi -> midi()
            R.id.menuGuide -> guide()
            else -> Assert.argument(false)
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.drawerMidi -> midi()
            R.id.drawerGuide -> guide()
            else -> {
                checkGroup(item.groupId, item.itemId)
                (item.actionView as Switch).toggle()
                return false
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onCheckedChanged(button: CompoundButton?, checked: Boolean) {
        when (button?.id) {
            DrawerItem.CHECK_ALL.id -> for (i in 0..(midi ?: return).tracks.lastIndex) {
                with(drawerMenu.menu.findItem(R.id.drawerTracks).subMenu.findItem(i)) {
                    if (checkGroup(groupId, itemId))
                        (actionView as Switch).isChecked = button.isChecked
                }
            }
            else -> {
                Assert.state(
                    (midi != null) and (button?.id in 0..(midi ?: return).tracks.lastIndex)
                )
                play?.let {
                    with(it) {
                        when {
                            (button ?: return).isChecked -> addTrack(button.id)
                            isPlaying and (numSelTracks() == 1) -> {
                                showError(R.string.justTracks, R.string.trackNotSelPlaying)
                                button.performClick()
                                return
                            }
                            else -> removeTrack(button.id)
                        }
                        (drawerMenu.menu.findItem(R.id.drawerAll).actionView as Switch).text =
                            getString(R.string.tracks, numSelTracks(), midi?.tracks?.size)
                    }
                }
            }
        }
    }

    private fun midi() {
        Intent(this, MidiActivity::class.java).also { intent ->
            intent.putExtra("Summary", midi?.summary)
            intent.putExtra("Tracks", midi?.tracks?.map { it.info }?.toTypedArray())
            intent.putExtra("Percuss", midi?.percuss)
            startActivity(intent)
        }
    }

    private fun guide() = startActivity(Intent(this, GuideActivity::class.java))

    private fun checkGroup(groupId: Int, itemId: Int) = when (groupId) {
        0 -> {
            Assert.argument(itemId == R.id.drawerAll)
            false
        }
        DrawerGroup.TRACKS.id -> {
            Assert.argument(itemId in 0..midi!!.tracks.lastIndex)
            true
        }
        else -> {
            Assert.argument(false)
            false
        }
    }

    override fun onBackPressed() {
        with(drawerLayout) {
            GravityCompat.START.also {
                if (isDrawerOpen(it)) closeDrawer(it) else super.onBackPressed()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (!(play ?: return).isPlaying) playPause.performClick()
    }

    override fun onStop() {
        super.onStop()
        if ((play ?: return).isPlaying) playPause.performClick()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode.toShort().toInt()) { // Dialog fragment on super() will receive it
            AddDialog.RequestCode.OPEN_MEDIA.id -> {
                if (resultCode != RESULT_OK) return
                openMidi(data?.data)
            }
            AddDialog.RequestCode.OPEN_MIDI.id -> {
                if (resultCode != RESULT_OK) return
                openMidi(data?.data)
            }
            AddDialog.RequestCode.WRITE_WAV.id, AddDialog.Permission.RECORD_SETTINGS.id ->
                super.onActivityResult(requestCode, resultCode, data)

            else -> Assert.argument(false)
        }
    }

    private fun openMidi(uri: Uri?) {
        uri?.let { u ->
            contentResolver.openInputStream(u)?.use { inStream ->
                with(Midi(inStream, getString(R.string.untitled))) {
                    if (badMidi) {
                        showError(R.string.badFile, R.string.notMidi)
                        return
                    }
                    midi = this
                    midiEnabled(true)
                    tracksEnabled(false) // needs to know old play.isPlaying
                    when {
                        tracks.isNullOrEmpty() -> {
                            showError(R.string.emptyMidi, R.string.noTracks)
                            return
                        }
                        dur == 0L -> {
                            showError(R.string.emptyMidi, R.string.zeroDur)
                            return
                        }
                        else -> {
                            Assert.state(::render.isInitialized)
                            play = Play(render, tracks, playPause, seek)
                            tracksEnabled(true)
                        }
                    }
                }
            }
        }
    }

    private fun midiEnabled(enabled: Boolean) {
        if (::mainMenu.isInitialized) mainMenu.findItem(R.id.menuMidi).isVisible = enabled
        with(drawerMenu.menu.findItem(R.id.drawerMidi)) {
            isEnabled = enabled
            with(actionView as TextView) {
                if (enabled) {
                    midi?.let { midi ->
                        midi.summary.let {
                            text = getString(
                                R.string.keyTemp,
                                if (it.keys.isNullOrEmpty()) "" else it.keys.first().key,
                                if (it.tempos.isNullOrEmpty()) 0 else it.tempos.first().bpm.toInt()
                            )
                        }
                    }
                } else text = getString(R.string.noMidi)
                font(this, enabled)
            }
        }
    }

    private fun tracksEnabled(enabled: Boolean) {
        if (::mainMenu.isInitialized) mainMenu.findItem(R.id.menuTracks).isVisible = enabled
        control.visibility = if (enabled) View.VISIBLE else View.GONE
        if (enabled) {
            durTime.text = Midi.minSecStr(this, R.string.timeOf, (midi ?: return).dur)
            seek.max = (midi ?: return).dur.toInt()
        } else {
            seek.progress = 0
            prev.visibility = View.GONE
            next.visibility = View.VISIBLE
            if ((play ?: return).isPlaying) playPause.performClick()
        }
        with(drawerMenu.menu) {
            with(findItem(R.id.drawerTracks).subMenu) {
                if (enabled) {
                    midi?.tracks?.forEachIndexed { i, track ->
                        with(Switch(this@MainActivity)) {
                            id = i
                            showText = true
                            textOn = "+"
                            textOff = ""
                            setOnCheckedChangeListener(this@MainActivity)
                            add(1, i, Menu.NONE, track.info.name).also {
                                it.icon = getDrawable(R.drawable.queue)
                                it.actionView = this
                            }
                        }
                    }
                } else removeGroup(DrawerGroup.TRACKS.id)
            }
            with(findItem(R.id.drawerAll)) {
                isEnabled = enabled
                with(actionView as Switch) {
                    isEnabled = enabled
                    text = getString(R.string.tracks, 0, if (enabled) midi?.tracks?.size else 0)
                    font(this, enabled)
                    if (enabled) {
                        toggle()
                        if (!isChecked) toggle()
                    }
                }
            }
        }
    }

    private fun font(t: TextView, enabled: Boolean) =
        t.setTypeface(null, if (enabled) Typeface.BOLD_ITALIC else Typeface.ITALIC)

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.newMedia -> AddDialog().show(supportFragmentManager, "Dialog Add")
            else -> play?.let {
                with(it) {
                    seek.progress.also { prevMilSec ->
                        when (view?.id) {
                            R.id.playPause -> {
                                if (!isPlaying and noTracks()) return
                                view.setBackgroundResource(
                                    if (isPlaying) android.R.drawable.ic_media_play
                                    else android.R.drawable.ic_media_pause
                                )
                                toggle()
                                prevNext()
                            }
                            R.id.prev -> if (!isPlaying and !noTracks()) {
                                render.releaseAllKeys()
                                do {
                                    val anyPressed = prevChord()
                                } while (((prevMilSec - seek.progress < 1) or !anyPressed)
                                    and (seek.progress != 0)
                                )
                            }
                            R.id.next -> if (!isPlaying and !noTracks()) {
                                render.releaseAllKeys()
                                do {
                                    val (anyPressed, stop) = nextChord()
                                } while (((seek.progress - prevMilSec < 1) or !anyPressed)
                                    and !stop
                                )
                            }
                            else -> Assert.argument(false)
                        }
                    }
                }
            }
        }
    }

    override fun onProgressChanged(bar: SeekBar?, pos: Int, fromUser: Boolean) {
        curTime.text = Midi.minSecStr(this, R.string.timeCur, pos.toLong())
        prevNext()
        if (fromUser) play?.seek(pos.toLong())
    }

    override fun onStartTrackingTouch(bar: SeekBar?) {}
    override fun onStopTrackingTouch(bar: SeekBar?) {}

    private fun prevNext() {
        with(seek) {
            prev.visibility =
                if (((play ?: return@with).isPlaying) or (progress == 0)) View.INVISIBLE
                else View.VISIBLE
            next.visibility =
                if (((play ?: return@with).isPlaying) or (progress == max)) View.INVISIBLE
                else View.VISIBLE
        }
    }

    private fun noTracks(): Boolean {
        if (play?.numSelTracks() == 0) {
            showError(R.string.justTracks, R.string.trackNotSel)
            drawerLayout.openDrawer(GravityCompat.START)
            return true
        }
        return false
    }

    private fun showError(titleId: Int, msgId: Int) = msgDialog(this, titleId, msgId)
}