@file:Suppress("PackageName")

package ru.BShakhovsky.Piano_Transcription.Midi

import android.os.Parcel
import android.os.Parcelable

class Summary(
    var text: Array<String> = emptyArray(), var copyright: Array<String> = emptyArray(),
    var lyrics: Array<String> = emptyArray(), var marker: Array<String> = emptyArray(),
    var cue: Array<String> = emptyArray(), var tempos: Array<Bpm> = emptyArray(),
    var keys: Array<Key> = emptyArray(), var garbage: Array<String> = emptyArray()
) : Parcelable {

    class Key(var milSec: Long, val key: String) : Parcelable {
        constructor(parcel: Parcel) : this(parcel.readLong(), parcel.readString()!!)

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeLong(milSec)
            parcel.writeString(key)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<Key> {
            override fun createFromParcel(parcel: Parcel): Key = Key(parcel)
            override fun newArray(size: Int): Array<Key?> = arrayOfNulls(size)
        }
    }

    class Bpm(var milSec: Long, val bpm: Float) : Parcelable {
        constructor(parcel: Parcel) : this(parcel.readLong(), parcel.readFloat())

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeLong(milSec)
            parcel.writeFloat(bpm)
        }

        override fun describeContents(): Int = 0

        companion object CREATOR : Parcelable.Creator<Bpm> {
            override fun createFromParcel(parcel: Parcel): Bpm = Bpm(parcel)
            override fun newArray(size: Int): Array<Bpm?> = arrayOfNulls(size)
        }
    }

    constructor(parcel: Parcel) : this(
        parcel.createStringArray()!!, parcel.createStringArray()!!,
        parcel.createStringArray()!!, parcel.createStringArray()!!, parcel.createStringArray()!!,
        parcel.createTypedArray(Bpm)!!, parcel.createTypedArray(Key)!!, parcel.createStringArray()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int): Unit = with(parcel) {
        writeStringArray(text)
        writeStringArray(copyright)
        writeStringArray(lyrics)
        writeStringArray(marker)
        writeStringArray(cue)
        writeTypedArray(tempos, flags)
        writeTypedArray(keys, flags)
        writeStringArray(garbage)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Summary> {
        override fun createFromParcel(parcel: Parcel): Summary = Summary(parcel)
        override fun newArray(size: Int): Array<Summary?> = arrayOfNulls(size)
    }
}
