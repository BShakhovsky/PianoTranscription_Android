@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.Midi

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_midi.*
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
            if (summary.copyright.isEmpty()) removeView(copyrightGroup) else copyright.text = summary.copyright.joinToString("\n")

            if (summary.keys.isEmpty()) removeView(keysGroup)
            else keys.text = summary.keys.joinToString("\n"){ with(it) {
                "${Midi.minSecStr(context, R.string.timeCur, milSec)} $key" } }
            if (summary.tempos.isEmpty()) removeView(temposGroup)
            else tempos.text = summary.tempos.joinToString("\n"){ with(it) {
                "${Midi.minSecStr(context, R.string.timeCur, milSec)} BPM ${bpm.toInt()}" } }

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