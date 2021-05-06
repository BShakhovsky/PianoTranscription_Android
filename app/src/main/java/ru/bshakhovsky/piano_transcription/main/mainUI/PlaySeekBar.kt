package ru.bshakhovsky.piano_transcription.main.mainUI

import android.widget.SeekBar

import ru.bshakhovsky.piano_transcription.main.play.Play
import ru.bshakhovsky.piano_transcription.utils.DebugMode

class PlaySeekBar(private val play: Play) : SeekBar.OnSeekBarChangeListener {

    override fun onProgressChanged(bar: SeekBar?, pos: Int, fromUser: Boolean) {
        DebugMode.assertArgument(bar != null)
        with(play) {
            prevNext()
            if (fromUser) seek(pos)
        }
    }

    override fun onStartTrackingTouch(bar: SeekBar?): Unit = DebugMode.assertArgument(bar != null)

    override fun onStopTrackingTouch(bar: SeekBar?) {
        DebugMode.assertArgument(bar != null)
        play.next()
    }
}