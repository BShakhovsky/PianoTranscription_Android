package ru.bshakhovsky.piano_transcription.main.mainUI

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Build

import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View

import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView

import androidx.annotation.CheckResult
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout

import androidx.lifecycle.Lifecycle

import com.google.android.material.navigation.NavigationView
import com.google.android.play.core.review.ReviewManagerFactory

import ru.bshakhovsky.piano_transcription.R.color.colorAccent
import ru.bshakhovsky.piano_transcription.R.drawable.queue
import ru.bshakhovsky.piano_transcription.R // id
import ru.bshakhovsky.piano_transcription.R.id
import ru.bshakhovsky.piano_transcription.R.string

import ru.bshakhovsky.piano_transcription.GuideActivity
import ru.bshakhovsky.piano_transcription.main.MainModel
import ru.bshakhovsky.piano_transcription.main.openGL.Render
import ru.bshakhovsky.piano_transcription.main.play.Play
import ru.bshakhovsky.piano_transcription.midi.Midi
import ru.bshakhovsky.piano_transcription.midi.MidiActivity
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage
import ru.bshakhovsky.piano_transcription.utils.WeakPtr

class DrawerMenu(
    lifecycle: Lifecycle, view: View, a: Activity,
    dLayout: DrawerLayout, dMenu: NavigationView, toolbar: Toolbar,
    private val model: MainModel, private val render: Render, private val play: Play
) : ActionBarDrawerToggle(a, dLayout, toolbar, string.app_name, string.app_name),
    NavigationView.OnNavigationItemSelectedListener, CompoundButton.OnCheckedChangeListener {

    var midi: Midi? = null

    private val content = WeakPtr(lifecycle, view)

    override fun onDrawerOpened(drawerView: View) {}
    override fun onDrawerClosed(drawerView: View) {}
    override fun onDrawerStateChanged(newState: Int) {}

    override fun onDrawerSlide(drawerView: View, slideOffset: Float): Unit = with(content.get()) {
        translationX = drawerView.width * slideOffset / 2
        /* TODO: Samsung Galaxy A11, Samsung Galaxy A51
            java.lang.IllegalArgumentException at android.view.View.sanitizeFloatPropertyValue */
        scaleX = 1 - slideOffset * drawerView.width / width
        scaleY = 1 - slideOffset * drawerView.width / width
    }


    private enum class DrawerGroup(val id: Int) { TRACKS(1) }
    private enum class DrawerItem(val id: Int) { CHECK_ALL(-1) }

    private val activity = WeakPtr(lifecycle, a)
    private val drawerLayout = WeakPtr(lifecycle, dLayout)
    private val drawerMenu = WeakPtr(lifecycle, dMenu)

    init {
        with(dMenu.menu) {
            intArrayOf(id.drawerMidi, id.drawerAll).forEach {
                with(findItem(it).actionView as TextView) {
                    gravity = Gravity.CENTER_VERTICAL
                    setTextColor(ContextCompat.getColor(context, colorAccent))
                }
            }
            with(findItem(id.drawerAll).actionView as Switch) {
                id = DrawerItem.CHECK_ALL.id
                setOnCheckedChangeListener(this@DrawerMenu)
            }
        }
        dLayout.addDrawerListener(this)

        midiEnabled(false)
        tracksEnabled(false)
    }

    fun midiEnabled(enabled: Boolean): Unit =
        with(drawerMenu.get().menu.findItem(id.drawerMidi)) {
            isEnabled = enabled
            (actionView as TextView).let { t ->
                with(activity.get()) {
                    if (enabled) {
                        DebugMode.assertState((midi != null) and (midi?.summary != null))
                        midi?.summary?.run {
                            t.text = getString(
                                string.keyTemp,
                                if (keys.isNullOrEmpty()) "" else keys.first().key,
                                if (tempos.isNullOrEmpty()) 0 else tempos.first().bpm.toInt()
                            )
                        }
                    } else t.text = getString(string.noMidi)
                    font(t, enabled)
                }
            }
        }

    fun tracksEnabled(enabled: Boolean): Unit = with(drawerMenu.get().menu) {
        model.contVis.value = if (enabled) View.VISIBLE else View.GONE
        with(activity.get()) {
            with(findItem(id.drawerTracks).subMenu) {
                if (enabled) {
                    DebugMode.assertState((midi != null) and (midi?.tracks != null))
                    midi?.tracks?.forEachIndexed { i, track ->
                        with(add(1, i, Menu.NONE, track.info.name)) {
                            icon = ContextCompat.getDrawable(applicationContext, queue)
                            // applicationContext for some reason makes it green, but I like it
                            // activity would keep it pink as the main Switch for all tracks:
                            actionView = Switch(applicationContext).apply {
                                id = i
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                                    showText = true
                                textOn = "+"
                                textOff = ""
                                setOnCheckedChangeListener(this@DrawerMenu)
                            }
                        }
                    }
                } else removeGroup(DrawerGroup.TRACKS.id)
            }
            with(findItem(id.drawerAll)) {
                isEnabled = enabled
                with(actionView as Switch) { // pink, all other Switches for single tracks are green
                    isEnabled = enabled
                    text = getString(string.tracks, 0, if (enabled) midi?.tracks?.size else 0)
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


    override fun onNavigationItemSelected(item: MenuItem): Boolean = true.also {
        activity.get().run {
            when (item.itemId) {
                id.drawerMidi -> startActivity(Intent(this, MidiActivity::class.java).apply {
                    DebugMode.assertState(midi != null)
                    midi?.run {
                        putExtra("Summary", summary)
                        putExtra("Tracks", tracks.map { it.info }.toTypedArray())
                        putExtra("Percuss", percuss)
                    }
                })
                id.drawerGuide -> startActivity(Intent(this, GuideActivity::class.java))
                id.drawerRate -> with(ReviewManagerFactory.create(applicationContext)) {
                    requestReviewFlow().addOnCompleteListener {
                        with(it) {
                            if (isSuccessful) launchReviewFlow(this@run, result)
                                .addOnCompleteListener {
                                    InfoMessage.toast(applicationContext, string.feedBack)
                                } else InfoMessage.toast(
                                applicationContext,
                                exception?.localizedMessage ?: getString(string.unknown)
                            )
                        }
                    }
                }
                else -> {
                    checkGroup(item.groupId, item.itemId)
                    (item.actionView as Switch).toggle()
                    return false
                }
            }
            drawerLayout.get().closeDrawer(GravityCompat.START)
        }
    }

    override fun onCheckedChanged(button: CompoundButton?, checked: Boolean) {
        DebugMode.assertArgument(button != null)
        button?.run {
            DebugMode.assertState(midi != null)
            midi?.tracks?.size?.let { numTracks ->
                drawerMenu.get().menu.run {
                    when (id) {
                        DrawerItem.CHECK_ALL.id -> for (i in 0 until numTracks) {
                            with(findItem(R.id.drawerTracks).subMenu.findItem(i)) {
                                if (checkGroup(groupId, itemId))
                                    (actionView as Switch).isChecked = button.isChecked
                            }
                        }
                        else -> {
                            DebugMode.assertState(id in 0 until numTracks)
                            play.run {
                                DebugMode.assertState(isPlaying.value != null)
                                with(activity.get()) {
                                    when {
                                        isChecked -> addTrack(id)
                                        (isPlaying.value ?: return@with)
                                                and (numSelTracks() == 1) -> {
                                            InfoMessage.dialog(
                                                this, string.justTracks, string.trackNotSelPlaying
                                            )
                                            performClick()
                                            return@with
                                        }
                                        else -> {
                                            removeTrack(id)
                                            render.releaseAllKeys()
                                        }
                                    }
                                    (findItem(R.id.drawerAll).actionView as Switch).text =
                                        getString(string.tracks, numSelTracks(), numTracks)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @CheckResult
    private fun checkGroup(groupId: Int, itemId: Int) = @Suppress("Reformat") when (groupId) {
        0                       -> false.also { DebugMode.assertArgument(itemId == id.drawerAll) }
        DrawerGroup.TRACKS.id   -> true.also {
            DebugMode.assertState(midi != null)
            midi?.run { DebugMode.assertArgument(itemId in 0..tracks.lastIndex) }
        } else                  -> false.also { DebugMode.assertArgument(false) }
    }
}