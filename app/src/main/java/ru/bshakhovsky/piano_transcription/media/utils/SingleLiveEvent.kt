package ru.bshakhovsky.piano_transcription.media.utils

import android.os.Looper
import androidx.annotation.MainThread

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer

import ru.bshakhovsky.piano_transcription.utils.DebugMode

class SingleLiveEvent : MutableLiveData<Unit?>() {

    /* https://medium.com/androiddevelopers/
    livedata-with-snackbar-navigation-and-other-events-the-singleliveevent-case-ac2622673150

    https://github.com/android/architecture-samples/blob/dev-to
    do-mvvm-live/todoapp/app/src/main/java/com/example/
    android/architecture/blueprints/todoapp/SingleLiveEvent.java

    https://gist.github.com/JoseAlcerreca/5b661f1800e1e654f07cc54fe87441af
    https://gist.github.com/JoseAlcerreca/e0bba240d9b3cffa258777f12e5c0ae9 */

    private var pending = false

    @MainThread
    override fun observe(owner: LifecycleOwner, observer: Observer<in Unit?>) {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "SingleLiveEvent should be observed from MediaActivity UI-thread"
        )
        DebugMode.assertState(
            !hasActiveObservers(),
            "Multiple observers registered but only one will be notified of changes"
        )
        super.observe(owner) {
            if (pending) {
                pending = false
                observer.onChanged(it)
            }
        }
    }

    @MainThread
    override fun setValue(value: Unit?) {
        DebugMode.assertState(
            Looper.myLooper() == Looper.getMainLooper(),
            "SingleLiveEvent value can be reset only from Main-thread"
        )
        pending = true
        super.setValue(value)
    }

    @MainThread
    fun call(): Unit = DebugMode.assertState(
        Looper.myLooper() == Looper.getMainLooper(),
        "SingleLiveEvent value can be reset only from Main-thread"
    ).let { value = null }
}