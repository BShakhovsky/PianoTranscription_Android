@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.MainUI

import android.view.MenuItem
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import ru.BShakhovsky.Piano_Transcription.Assert
import ru.BShakhovsky.Piano_Transcription.R

class MenuListener(private val drawer: DrawerLayout) : NavigationView.OnNavigationItemSelectedListener {

    override fun onNavigationItemSelected(item: MenuItem): Boolean { when (item.itemId) {
        R.id.drawerTracks -> tracks()
        R.id.drawerMidi   -> midi()
        R.id.drawerGuide  -> guide()
        else -> Assert.state(false) }
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    fun tracks() {
        Snackbar.make(drawer, "Select tracks", Snackbar.LENGTH_LONG).show()
    }

    fun midi() {
        Snackbar.make(drawer, "Midi info", Snackbar.LENGTH_LONG).show()
    }

    fun guide() {
        Snackbar.make(drawer, "User guide", Snackbar.LENGTH_LONG).show()
    }
}