@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_guide.*

class GuideActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)
        setSupportActionBar(guideBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setFinishOnTouchOutside(true)
        fabGuide.setOnClickListener { Snackbar.make(it, "Share", Snackbar.LENGTH_LONG).show() }
    }
}