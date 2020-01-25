@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.Midi

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_midi.*
import ru.BShakhovsky.Piano_Transcription.Assert
import ru.BShakhovsky.Piano_Transcription.R

class MidiActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_midi)
        setSupportActionBar(midiBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setFinishOnTouchOutside(true)
        fabMidi.setOnClickListener { view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).show() }
        (intent.getParcelableExtra("Summary") as Summary).also { summary -> with(midiInfo) {
            fun minSec(mSec: Long) = mSec / 60_000 to (mSec / 1_000) % 60
            Assert.argument(intent.hasExtra("Duration"))
            intent.getLongExtra("Duration", -1).also { midiDur ->
                Assert.argument(midiDur != -1L)
                with(minSec(midiDur)) { dur.text = getString(R.string.duration, getString(R.string.time, first, second)) }
            }
            if (summary.copyright.isEmpty()) removeView(copyrightGroup) else copyright.text = summary.copyright.joinToString("\n")

            fun timeStr(mSec: Long) = with(minSec(mSec)) { "Time %02d:%02d --> ".format(first, second) }
            if (summary.keys.isEmpty()) removeView(keysGroup)
            else keys.text = summary.keys.joinToString("\n"){ with(it) { "${timeStr(milSec)}$key" } }
            if (summary.tempos.isEmpty()) removeView(temposGroup)
            else tempos.text = summary.tempos.joinToString("\n"){ with(it) { "${timeStr(milSec)}BPM ${bpm.toInt()}" } }

            if (summary.lyrics .isEmpty()) removeView (lyricsGroup) else lyrics   .text = summary.lyrics .joinToString("\n")
            if (summary.marker .isEmpty()) removeView (markerGroup) else marker   .text = summary.marker .joinToString("\n")
            if (summary.cue    .isEmpty()) removeView    (cueGroup) else cue      .text = summary.cue    .joinToString("\n")
            if (summary.text   .isEmpty()) removeView  (otherGroup) else otherText.text = summary.text   .joinToString("\n")
            if (summary.garbage.isEmpty()) removeView(garbageGroup) else garbage  .text = summary.garbage.joinToString("\n")
        }
            fun joinText(key: String) = with(intent.getParcelableArrayExtra(key)?.joinToString("\n"){
                with(it as TrackInfo) { "$name ${instrument?:""}" } }) { if (isNullOrEmpty()) "N/A" else this!! }
            harm.text = joinText("Tracks")
            percuss.text = joinText("Percuss")
        }
    }
}