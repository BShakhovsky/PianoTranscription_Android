@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.Midi

import android.content.Context
import com.pdrogfer.mididroid.MidiFile
import com.pdrogfer.mididroid.event.*
import com.pdrogfer.mididroid.event.meta.*
import com.pdrogfer.mididroid.util.MidiUtil
import ru.BShakhovsky.Piano_Transcription.Assert
import java.io.InputStream

class Midi(inStream: InputStream, untitled: String) {

    companion object { fun minSecStr(context: Context, strId: Int, mSec: Long) =
        context.getString(strId, mSec / 60_000, (mSec / 1_000) % 60) }

    class Note(val note: Int, val vel: Float, val isOn: Boolean)
    class Chord(var mSec: Long, var notes: Array<Note>)
    class Track(val info: TrackInfo = TrackInfo(), var chords: Array<Chord> = emptyArray())

    val summary = Summary()
    var tracks = emptyArray<Track>()
        private set
    var percuss = emptyArray<Track>()
        private set
    val badMidi: Boolean
    val dur: Long

    init {
        val midi = MidiFile(inStream)
        Assert.state((midi.resolution and 0x80_00) == 0, "Time-code-based time: ticks per frame * fps SMPTE time = ticks per second")
        badMidi = midi.tracks.isEmpty()

        data class TempoSet(val tick: Long, val quartNote: Int)
        var tempos = emptyArray<TempoSet>()
        midi.tracks.forEachIndexed { i, track ->
            var curTick = 0L; var curMilSec = 0L; val curTrack = Track(); var isPercuss = false
            track.events.forEach { event -> with(event) { curTick += delta
                if (curTick > 0) { if (tempos.isEmpty()) { Assert.state(false)
                    tempos += TempoSet(curTick, 60_000_000 / 120) }
                else tempos.findLast { (tick, _) -> tick <= curTick }.also { Assert.state(it != null)
                    curMilSec += MidiUtil.ticksToMs(delta, (it ?: return@also).quartNote, midi.resolution) } }
                when (this) {
                    is TrackName -> curTrack.info.name = "$i $trackName"
                    is TextualMetaEvent -> when (this) {
                        is Text            -> summary      .text      += text
                        is CopyrightNotice -> summary      .copyright += notice
                        is InstrumentName  -> curTrack.info.instrument = name
                        is Lyrics          -> summary      .lyrics    += lyric
                        is Marker          -> summary      .marker    += markerName
                        is CuePoint        -> summary      .cue       += cue
                        else -> Assert.state(false, "Wrong text meta-event")
                    }
                    is Tempo -> { tempos += TempoSet(curTick, mpqn)
                        summary.tempos += Summary.Bpm(curMilSec, bpm)
                    }
                    is KeySignature -> {
                        fun majMin(maj: String, minor: String) = if (scale == 0) "$maj-Major" else "$minor-Minor"
                        summary.keys += Summary.Key(curMilSec, when (key) {
                            -7 -> majMin("Cb", "Ab")
                            -6 -> majMin("Gb", "Eb")
                            -5 -> majMin("Db", "Bb")
                            -4 -> majMin("Ab", "F")
                            -3 -> majMin("Eb", "C")
                            -2 -> majMin("Bb", "G")
                            -1 -> majMin("F", "D")

                            0 -> "Natural ${majMin("C", "A")}"

                            1 -> majMin("G", "E")
                            2 -> majMin("D", "B")
                            3 -> majMin("A", "F#")
                            4 -> majMin("E", "C#")
                            5 -> majMin("B", "G#")
                            6 -> majMin("F#", "D#")
                            7 -> majMin("C#", "A#")

                            else -> { Assert.state(false, "Wrong key signature"); "unknown" } })
                    }
                    is ChannelEvent -> when (channel) {
                        // (!) Should be 10, but there seems to be bug in library
                        // https://github.com/LeffelMania/android-midi-lib/issues/17
                        9 -> isPercuss = true
                        else -> when (this) {
                            is NoteOn -> { when {(delta == 0L) and curTrack.chords.isNotEmpty() ->
                                curTrack.chords.last().notes +=
                                    Note(noteValue - 21, velocity / 127f, velocity != 0)
                                else -> curTrack.chords += Chord(curMilSec, Array(1) {
                                    Note(noteValue - 21, velocity / 127f, velocity != 0) }) } }
                            is NoteOff -> { when {(delta == 0L) and curTrack.chords.isNotEmpty() ->
                                curTrack.chords.last().notes +=
                                    Note(noteValue - 21, velocity / 127f, false)
                                else -> curTrack.chords += Chord(curMilSec, Array(1) {
                                    Note(noteValue - 21, velocity / 127f, false) }) } }
                            else -> Assert.state((this is NoteAftertouch) or (this is Controller)
                                    or (this is ProgramChange) or (this is ChannelAftertouch) or (this is PitchBend)) }
                    }
                    else -> Assert.state((this is SequencerSpecificEvent) or (this is SequenceNumber) or (this is MidiChannelPrefix)
                            or (this is EndOfTrack) or (this is SmpteOffset) or (this is TimeSignature) or (this is SystemExclusiveEvent)
                            or (this is GenericMetaEvent))
                }
            } }
            with(curTrack) { with(info) { if (name.isNullOrEmpty()) name = untitled }
                when {
                    isPercuss -> percuss += this
                    chords.isEmpty() -> with(info) { if (!(name.isNullOrEmpty() and instrument.isNullOrEmpty()))
                        summary.garbage += "${info.name?:""} ${info.instrument?:""}" }
                    else -> tracks += this
                }
            }
        }
        dur = if (tracks.isEmpty()) 0L else tracks.maxBy { it.chords.last().mSec }!!.chords.last().mSec
    }
}