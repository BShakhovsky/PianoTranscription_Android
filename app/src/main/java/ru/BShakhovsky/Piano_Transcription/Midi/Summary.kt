@file:Suppress("PackageName")
package ru.BShakhovsky.Piano_Transcription.Midi

import android.os.Parcel
import android.os.Parcelable

class Summary(var text: Array<String> = emptyArray(), var copyright: Array<String> = emptyArray(),
              var lyrics: Array<String> = emptyArray(), var marker: Array<String> = emptyArray(), var cue: Array<String> = emptyArray(),
              var tempos: Array<Bpm> = emptyArray(), var keys: Array<Key> = emptyArray(),
              var garbage: Array<String> = emptyArray()) : Parcelable {

    class Key(val milSec: Long, val key: String) : Parcelable {
        constructor(parcel: Parcel) : this(parcel.readLong(), parcel.readString()!!)
        override fun writeToParcel(parcel: Parcel, flags: Int) { parcel.writeLong(milSec); parcel.writeString(key) }
        override fun describeContents() = 0
        companion object CREATOR : Parcelable.Creator<Key> {
            override fun createFromParcel(parcel: Parcel) = Key(parcel)
            override fun newArray(size: Int) = arrayOfNulls<Key>(size) }
    }

    class Bpm(val milSec: Long, val bpm: Float) : Parcelable {
        constructor(parcel: Parcel) : this(parcel.readLong(), parcel.readFloat())
        override fun writeToParcel(parcel: Parcel, flags: Int) { parcel.writeLong(milSec); parcel.writeFloat(bpm) }
        override fun describeContents() = 0
        companion object CREATOR : Parcelable.Creator<Bpm> {
            override fun createFromParcel(parcel: Parcel) = Bpm(parcel)
            override fun newArray(size: Int) = arrayOfNulls<Bpm>(size) }
    }

    constructor(parcel: Parcel) : this(parcel.createStringArray()!!, parcel.createStringArray()!!,
        parcel.createStringArray()!!, parcel.createStringArray()!!, parcel.createStringArray()!!,
        parcel.createTypedArray(Bpm)!!, parcel.createTypedArray(Key)!!, parcel.createStringArray()!!)

    override fun writeToParcel(parcel: Parcel, flags: Int) { parcel.writeStringArray(text); parcel.writeStringArray(copyright)
        parcel.writeStringArray(lyrics); parcel.writeStringArray(marker); parcel.writeStringArray(cue)
        parcel.writeTypedArray(tempos, flags); parcel.writeTypedArray(keys, flags); parcel.writeStringArray(garbage) }

    override fun describeContents() = 0

    companion object CREATOR : Parcelable.Creator<Summary> {
        override fun createFromParcel(parcel: Parcel) = Summary(parcel)
        override fun newArray(size: Int) = arrayOfNulls<Summary>(size) }
}
