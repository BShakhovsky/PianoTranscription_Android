@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

import kotlinx.android.synthetic.main.activity_guide.fabGuide
import kotlinx.android.synthetic.main.activity_guide.guideBar

class GuideActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)

        setSupportActionBar(guideBar)
        DebugMode.assertState(supportActionBar != null)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        guideBar.setNavigationOnClickListener(this)

        setFinishOnTouchOutside(true)

        fabGuide.setOnClickListener { Snackbar.make(it, "Share", Snackbar.LENGTH_LONG).show() }
    }

    override fun onClick(view: View?) {
        DebugMode.assertArgument(view != null)
        when (view?.id) {
            -1 -> onBackPressed() // not android.R.id.home
            else -> DebugMode.assertState(false)
        }
    }
}