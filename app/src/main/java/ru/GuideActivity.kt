package ru.bshakhovsky.piano_transcription

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

import kotlinx.android.synthetic.main.activity_guide.fabGuide
import kotlinx.android.synthetic.main.activity_guide.guideBar

import ru.bshakhovsky.piano_transcription.utils.DebugMode

class GuideActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)

        setSupportActionBar(guideBar)
        DebugMode.assertState(supportActionBar != null)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        guideBar.setNavigationOnClickListener(this)

        setFinishOnTouchOutside(true)

        fabGuide.setOnClickListener {
            // TODO: User Guide --> "Share" button
            Snackbar.make(it, "Replace with your own action", Snackbar.LENGTH_LONG).show()
        }
    }

    override fun onClick(view: View?) {
        DebugMode.assertArgument(view != null)
        when (view?.id) {
            -1 -> onBackPressed() // not android.R.id.home
            else -> DebugMode.assertState(false)
        }
    }
}