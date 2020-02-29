@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.Midi

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity

import kotlinx.android.synthetic.main.activity_midi.cue
import kotlinx.android.synthetic.main.activity_midi.cueGroup
import kotlinx.android.synthetic.main.activity_midi.copyright
import kotlinx.android.synthetic.main.activity_midi.copyrightGroup
import kotlinx.android.synthetic.main.activity_midi.fabMidi
import kotlinx.android.synthetic.main.activity_midi.garbage
import kotlinx.android.synthetic.main.activity_midi.garbageGroup
import kotlinx.android.synthetic.main.activity_midi.harm
import kotlinx.android.synthetic.main.activity_midi.keys
import kotlinx.android.synthetic.main.activity_midi.keysGroup
import kotlinx.android.synthetic.main.activity_midi.lyrics
import kotlinx.android.synthetic.main.activity_midi.lyricsGroup
import kotlinx.android.synthetic.main.activity_midi.marker
import kotlinx.android.synthetic.main.activity_midi.markerGroup
import kotlinx.android.synthetic.main.activity_midi.midiBar
import kotlinx.android.synthetic.main.activity_midi.midiInfo
import kotlinx.android.synthetic.main.activity_midi.otherText
import kotlinx.android.synthetic.main.activity_midi.otherGroup
import kotlinx.android.synthetic.main.activity_midi.percuss
import kotlinx.android.synthetic.main.activity_midi.tempos
import kotlinx.android.synthetic.main.activity_midi.temposGroup

import ru.BShakhovsky.Piano_Transcription.Assert
import ru.BShakhovsky.Piano_Transcription.R
import kotlin.math.roundToInt

class MidiActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_midi)
        setSupportActionBar(midiBar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setFinishOnTouchOutside(true)
        fabMidi.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).show()
        }
        intent.getParcelableExtra<Summary>("Summary").also { summary ->
            with(midiInfo) {
                Assert.state(summary != null)
                summary?.let { s ->
                    if (s.copyright.isEmpty()) removeView(copyrightGroup)
                    else copyright.text = s.copyright.joinToString("\n")

                    if (s.keys.isEmpty()) removeView(keysGroup)
                    else keys.text = s.keys.joinToString("\n") {
                        with(it) { "${Midi.minSecStr(context, R.string.timeCur, milSec)} $key" }
                    }
                    if (s.tempos.isEmpty()) removeView(temposGroup)
                    else tempos.text = s.tempos.joinToString("\n") {
                        with(it) {
                            "${Midi.minSecStr(
                                context, R.string.timeCur, milSec
                            )} BPM ${bpm.roundToInt()}"
                        }
                    }

                    if (s.lyrics.isEmpty()) removeView(lyricsGroup)
                    else lyrics.text = s.lyrics.joinToString("\n")

                    if (s.marker.isEmpty()) removeView(markerGroup)
                    else marker.text = s.marker.joinToString("\n")

                    if (s.cue.isEmpty()) removeView(cueGroup)
                    else cue.text = s.cue.joinToString("\n")

                    if (s.text.isEmpty()) removeView(otherGroup)
                    else otherText.text = s.text.joinToString("\n")

                    if (s.garbage.isEmpty()) removeView(garbageGroup)
                    else garbage.text = s.garbage.joinToString("\n")
                }
            }
            fun joinText(key: String) =
                with(intent.getParcelableArrayExtra(key)?.joinToString("\n") {
                    with(it as TrackInfo) { "$name ${instrument ?: ""}" }
                }) { if (isNullOrEmpty()) "N/A" else this!! }
            harm.text = joinText("Tracks")
            percuss.text = joinText("Percuss")
        }
    }
}