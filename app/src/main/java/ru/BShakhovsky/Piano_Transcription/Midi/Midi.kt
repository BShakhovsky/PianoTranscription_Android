@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.Midi

import android.content.Context
import com.pdrogfer.mididroid.MidiFile
import com.pdrogfer.mididroid.event.*
import com.pdrogfer.mididroid.event.meta.*
import ru.BShakhovsky.Piano_Transcription.Assert
import java.io.InputStream
import kotlin.math.roundToLong

class Midi(inStream: InputStream, untitled: String) {

    companion object { fun minSecStr(context: Context, strId: Int, mSec: Long) =
        context.getString(strId, mSec / 60_000, (mSec / 1_000) % 60) }

    class Chord(var mSec: Long, val notes: MutableMap<Int, Float>)
    class Track(val info: TrackInfo = TrackInfo(), var chords: Array<Chord> = emptyArray())

    val summary = Summary()
    var tracks = emptyArray<Track>()
        private set
    var percuss = emptyArray<TrackInfo>()
        private set
    val badMidi: Boolean
    val dur: Long

    init {
        val midi = MidiFile(inStream)
        Assert.state((midi.resolution and 0x80_00) == 0, "Time-code-based time: ticks per frame * fps SMPTE time = ticks per second")
        badMidi = midi.tracks.isEmpty(); if (!badMidi) {
        val tempos = sortedMapOf<Long, Int>(); val ticks = sortedMapOf<Long, Long>()
        midi.tracks.forEachIndexed { i, track ->
            var curTick = 0L; val curTrack = Track(); var isPercuss = false
            track.events.forEach { event -> with(event) { curTick += delta; ticks += curTick to 0L; when (this) {
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
                is Tempo -> { tempos += curTick to mpqn
                    summary.tempos += Summary.Bpm(curTick, bpm)
                }
                is KeySignature -> {
                    fun majMin(maj: String, minor: String) = if (scale == 0) "$maj-Major" else "$minor-Minor"
                    summary.keys += Summary.Key(curTick, when (key) {
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
                    else -> (if (curTrack.chords.isEmpty()) true
                    else curTrack.chords.last().mSec != curTick).also { newChord -> when (this) {
                        is NoteOn -> { when { newChord -> curTrack.chords += Chord(curTick, mutableMapOf(
                            noteValue - 21 to velocity / 127f))
                            else -> curTrack.chords.last().notes[noteValue - 21] = maxOf(curTrack.chords.last().notes.getOrDefault(
                                noteValue - 21, 0f), velocity / 127f) } }
                        is NoteOff -> { when { newChord -> curTrack.chords += Chord(curTick, mutableMapOf(
                            noteValue - 21 to 0f))
                            else -> curTrack.chords.last().notes.getOrPut(noteValue - 21){ 0f } } }
                        else -> Assert.state((this is NoteAftertouch) or (this is Controller)
                                or (this is ProgramChange) or (this is ChannelAftertouch) or (this is PitchBend))
                    } }
                }
                else -> Assert.state((this is SequencerSpecificEvent) or (this is SequenceNumber) or (this is MidiChannelPrefix)
                        or (this is EndOfTrack) or (this is SmpteOffset) or (this is TimeSignature) or (this is SystemExclusiveEvent)
                        or (this is GenericMetaEvent))
            } } }
                with(curTrack) { with(info) { if (name.isNullOrEmpty()) name = untitled }; when {
                    isPercuss -> percuss += info
                    chords.isEmpty() -> with(info) { if (!(name.isNullOrEmpty() and instrument.isNullOrEmpty()))
                        summary.garbage += "${info.name?:""} ${info.instrument?:""}" }
                    else -> tracks += this
                } }
            }

            if (tempos.isEmpty()) { Assert.state(false); tempos += 0L to 60_000_000 / 120 }
            if (!tempos.containsKey(0)) { Assert.state(false); tempos += 0L to 60_000_000 / 120 }
            var (lastTick, lastMicSec) = 0L to 0L
            ticks.keys.forEach { tick ->
                Assert.state(tempos.containsKey(lastTick))
                ticks[tick] = lastMicSec + (tick - lastTick) * (tempos[lastTick] ?: return@forEach)
                if (tempos.containsKey(tick)) { lastTick = tick; lastMicSec = (ticks[tick] ?: return@forEach) }
            }
            ticks.keys.forEach { ticks[it] = ticks[it]?.div(1_000.0)?.div(midi.resolution)?.roundToLong() }

            summary.tempos.forEach { with(it) { milSec = ticks.getOrDefault(milSec, 0) } }
            summary.keys.forEach { with(it) { milSec = ticks.getOrDefault(milSec, 0) } }
            tracks.forEach { track -> track.chords.forEach { chord -> with(chord) { mSec = ticks.getOrDefault(mSec, 0) } } }
        }
        dur = if (tracks.isEmpty()) 0L else tracks.maxBy { it.chords.last().mSec }!!.chords.last().mSec
    }
}