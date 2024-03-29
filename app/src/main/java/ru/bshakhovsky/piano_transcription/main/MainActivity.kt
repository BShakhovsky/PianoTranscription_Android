package ru.bshakhovsky.piano_transcription.main

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup

import androidx.annotation.CheckResult
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider

import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

import ru.bshakhovsky.piano_transcription.R.dimen
import ru.bshakhovsky.piano_transcription.R.drawable
import ru.bshakhovsky.piano_transcription.R.id
import ru.bshakhovsky.piano_transcription.R.layout.activity_main
import ru.bshakhovsky.piano_transcription.R.menu.menu_main
import ru.bshakhovsky.piano_transcription.R.string
import ru.bshakhovsky.piano_transcription.databinding.ActivityMainBinding

import ru.bshakhovsky.piano_transcription.ad.AdBanner
import ru.bshakhovsky.piano_transcription.ad.AdInterstitial
import ru.bshakhovsky.piano_transcription.addDialog.AddDialog
import ru.bshakhovsky.piano_transcription.main.mainUI.DrawerMenu
import ru.bshakhovsky.piano_transcription.main.mainUI.PlaySeekBar
import ru.bshakhovsky.piano_transcription.main.openGL.Render
import ru.bshakhovsky.piano_transcription.main.play.Play
import ru.bshakhovsky.piano_transcription.main.play.Sound
import ru.bshakhovsky.piano_transcription.main.play.realtime.TranscribeRT
import ru.bshakhovsky.piano_transcription.media.MediaActivity
import ru.bshakhovsky.piano_transcription.midi.Midi

import ru.bshakhovsky.piano_transcription.utils.Crash
import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage
import ru.bshakhovsky.piano_transcription.utils.MicPermission
import ru.bshakhovsky.piano_transcription.utils.Share
import ru.bshakhovsky.piano_transcription.utils.StrictPolicy

import java.io.FileNotFoundException
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private enum class RequestCode(val id: Int) { SPECTRUM(10) }

    private lateinit var policy: StrictPolicy

    private lateinit var binding: ActivityMainBinding

    private lateinit var model: MainModel
    private lateinit var sound: Sound

    private lateinit var play: Play
    private lateinit var realTime: TranscribeRT
    private lateinit var render: Render

    private var micClickTime = SystemClock.uptimeMillis()
    private var mainMenu: Menu? = null
    private lateinit var drawerListener: DrawerMenu

    private lateinit var interstitial: AdInterstitial

    override fun onCreate(savedInstanceState: Bundle?) {
        if (DebugMode.debug) {
            Thread.setDefaultUncaughtExceptionHandler(Crash(getExternalFilesDir("Errors")))
            policy = StrictPolicy(lifecycle, this@MainActivity)
        }
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        with(ViewModelProvider(this)) {
            model = get(MainModel::class.java)
                .apply { initialize(lifecycle, supportFragmentManager) }
            sound = get(Sound::class.java).apply { load(applicationContext) }
            with(
                DataBindingUtil
                    .setContentView<ActivityMainBinding>(this@MainActivity, activity_main)
            ) {
                binding = this

                mainModel = model
                soundModel = sound

                render =
                    Render(lifecycle, assets, resources, surfaceView, playPause, prev, next, sound)
                play = get(Play::class.java)
                    .apply { initialize(lifecycle, this@MainActivity, drawerLayout, render) }
                playModel = play

                realTime = get(TranscribeRT::class.java)
                    .apply { initialize(application, lifecycle, this@MainActivity, next, render) }

                lifecycleOwner = this@MainActivity
            }
        }

        with(binding) {
            setSupportActionBar(mainBar)
            drawerListener = DrawerMenu(
                lifecycle, mainLayout, this@MainActivity,
                drawerLayout, drawerMenu, mainBar, model, render, play
            ).apply { syncState() }
            drawerMenu.setNavigationItemSelectedListener(drawerListener)
            seek.setOnSeekBarChangeListener(PlaySeekBar(play))

            MobileAds.initialize(applicationContext)
            if (DebugMode.debug) MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder().setTestDeviceIds(
                    listOf(
                        AdRequest.DEVICE_ID_EMULATOR,
                        "87FD000F52337DF09DBB9E6684B0B878",
                        "9AA370206113A6039BC7B5BB02F0F7E8",
                        @Suppress("SpellCheckingInspection")
                        "921547C5AAEF85EB72E17DFAE23DD8BA"
                    )
                ).build()
            )
            AdBanner(lifecycle, applicationContext, adMain, string.bannerMain)
            interstitial = AdInterstitial(lifecycle, this@MainActivity)
        }

        with(realTime) {
            isRecognizing.observe(this@MainActivity) {
                play.isRecognizing.value = it
                mainMenu?.findItem(id.menuMic)?.icon = ContextCompat
                    .getDrawable(applicationContext, if (it) drawable.mic_on else drawable.mic_off)
            }
            play.isPlaying.observe(this@MainActivity)
            { if (it and (isRecognizing.value == true)) toggle() }
        }

        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        DebugMode.assertArgument(intent != null)
        intent?.run {
            when (action) {
                Intent.ACTION_MAIN -> DebugMode.assertState(
                    (categories.size == 1) and hasCategory(Intent.CATEGORY_LAUNCHER)
                            and (type == null) and (data == null) and (dataString == null)
                            // and (extras == null)
                            /* Not null when launched on Emulator 2.7 Q VGA API 24 from main menu:
                            extras != null = dalvik.system.PathClassLoader[DexPathList[
                            [zip file "/data/app/ru.BShakhovsky.Piano_Transcription-2/base.apk"],
                            nativeLibraryDirectories=
                            [/data/app/ru.BShakhovsky.Piano_Transcription-2/lib/x86,
                            /data/app/ru.BShakhovsky.Piano_Transcription-2/base.apk!/lib/x86,
                            /system/lib, /vendor/lib]]] */
                            and (clipData == null)
                )

                Intent.ACTION_VIEW -> DebugMode.assertState(
                    // (categories.size in arrayOf(1, 2)) and (hasCategory(Intent.CATEGORY_DEFAULT)
                    //        or hasCategory(Intent.CATEGORY_BROWSABLE)) and
                    // categories = null when tried to go to website from Google Play
                    (type == null) and (data != null)
                            and with(dataString!!.lowercase(Locale.getDefault())) {
                        startsWith("http://bshakhovsky.github.io") or
                                startsWith("https://bshakhovsky.github.io")
                    } and (clipData == null)
                    // Null when opened from Google Play
                    // Non null when opened from Samsung Internet or Google Chrome
                    // and (extras == null)
                )

                Intent.ACTION_SEND -> {
                    if (categories != null) DebugMode.assertState(
                        (categories.size == 1) and hasCategory(Intent.CATEGORY_DEFAULT)
                    )
                    DebugMode.assertState( // Not null when sent from another App, e.g. content://
                        // com.*.*.rnshare.fileprovider/rnshare_sdcard/data/data/com.*.*/files/*.mid
                        // (data == null) and (dataString == null) and
                        (extras != null) and (clipData != null)
                    )
                    clipData?.run {
                        DebugMode.assertState((itemCount == 1) and (description != null))
                        description?.run {
                            DebugMode.assertState(
                                (mimeTypeCount == 1) and (type != null)
                                        and (type?.substringBefore('/')
                                        in arrayOf("audio", "video"))
                                        and (getMimeType(0).substringBefore('/')
                                        in arrayOf("audio", "video")) and (getItemAt(0) != null)
                            )
                            getItemAt(0)?.run { openMedia(uri) }
                        }
                    }
                }

                // Restarted due to UncaughtExceptionHandler:
                else -> DebugMode.assertState(action == null)
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration): Unit =
        super.onConfigurationChanged(newConfig).also {
            interstitial = AdInterstitial(lifecycle, this)
            with(binding.fabMain.layoutParams as ViewGroup.MarginLayoutParams) {
                with(resources) {
                    when (newConfig.orientation) {
                        // marginEnd does not work on Samsung Galaxy A7, Android 6.0.1 API 23
                        Configuration.ORIENTATION_PORTRAIT, Configuration.ORIENTATION_LANDSCAPE ->
                            rightMargin = getDimension(dimen.fabMargin).toInt()
                        @Suppress("DEPRECATION")
                        Configuration.ORIENTATION_SQUARE, Configuration.ORIENTATION_UNDEFINED ->
                            DebugMode.assertArgument(false)
                        else -> DebugMode.assertArgument(false)
                    }
                }
            }
        }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean = super.onCreateOptionsMenu(menu).also {
        DebugMode.assertArgument(menu != null)
        menuInflater.inflate(menu_main, menu.also { mainMenu = it })
        mainMenu?.findItem(id.menuGuide)?.isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        super.onOptionsItemSelected(item).also {
            when (item.itemId) {
                id.menuMic -> {
                    if (SystemClock.uptimeMillis() - micClickTime < 1_000) return it
                    micClickTime = SystemClock.uptimeMillis()

                    realTime.toggle()
                    if (play.isPlaying.value == true) play.playPause()
                }
                id.menuShare -> Share.share(this)
                else -> DebugMode.assertArgument(false)
            }
        }

    override fun onBackPressed(): Unit = with(binding.drawerLayout) {
        GravityCompat.START.let { if (isDrawerOpen(it)) closeDrawer(it) else super.onBackPressed() }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ): Unit = when (requestCode) {
        MicPermission.RecPermission.RECOGNIZE.id -> MicPermission.onRequestResult(
            MicPermission.RecPermission.RECOGNIZE_SETTINGS.id, grantResults, binding.root, this
        ) { realTime.toggle() }

        // DialogFragment on super() will receive it:
        else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode.toShort().toInt()) { // Dialog fragment on super() will receive it
            RequestCode.SPECTRUM.id -> {
                if (resultCode != RESULT_OK) return
                DebugMode.assertState((data != null) and (data?.data != null))
                data?.data?.let { uri -> openMidi(uri).let { ok -> DebugMode.assertState(ok) } }
            }
            // AddDialog.RequestCode.SURF.id,
            AddDialog.RequestCode.OPEN_MEDIA.id -> {
                if (resultCode != RESULT_OK) return
                DebugMode.assertState(data != null)
                data?.let { intent ->
                    DebugMode.assertState(intent.data != null)
                    intent.data?.let { openMedia(it, intent.getStringExtra("YouTube Link")) }
                }
            }
            AddDialog.RequestCode.OPEN_MIDI.id -> {
                if (resultCode != RESULT_OK) return
                DebugMode.assertState((data != null) and (data?.data != null))
                data?.data?.let { if (!openMidi(it)) showError(string.badFile, string.notMidi) }
            }

            MicPermission.RecPermission.RECOGNIZE_SETTINGS.id -> MicPermission.onSettingsResult(
                resultCode, MicPermission.RecPermission.RECOGNIZE_SETTINGS.id, binding.root, this
            ) { realTime.toggle() }
            AddDialog.RequestCode.WRITE_3GP.id, MicPermission.RecPermission.RECORD_SETTINGS.id ->
                super.onActivityResult(requestCode, resultCode, data)
            else -> DebugMode.assertArgument(false)
        }
    }

    fun openMedia(file: Uri, youTubeLink: String? = null) {
        if (!openMidi(file)) startActivityForResult(
            Intent(this, MediaActivity::class.java).apply {
                data = file
                youTubeLink?.let { putExtra("YouTube Link", it) }
            }, RequestCode.SPECTRUM.id
        )
    }

    @CheckResult
    private fun openMidi(uri: Uri): Boolean {
        try {
            contentResolver.openInputStream(uri).let { inputStream ->
                DebugMode.assertState(inputStream != null)
                inputStream?.use { inStream ->
                    with(Midi(inStream, getString(string.untitled))) {
                        if (badMidi) return false

                        drawerListener.run {
                            midi = this@with
                            midiEnabled(true)
                            tracksEnabled(false) // needs to know old play.isPlaying
                            when {
                                tracks.isNullOrEmpty() ->
                                    showError(string.emptyMidi, string.noTracks)
                                dur == 0L -> showError(string.emptyMidi, string.zeroDur)
                                else -> {
                                    DebugMode.assertState(::render.isInitialized)
                                    play.newMidi(tracks, dur)
                                    tracksEnabled(true)
                                }
                            }
                        }
                    }
                }
                /* Show ad only after inputStream?.use closed.
                Otherwise, if user quickly clicks on ad, then after returning
                java.lang.SecurityException readExceptionWithFileNotFoundExceptionFromParcel */
                interstitial.show()
            }
        } catch (e: FileNotFoundException) {
            InfoMessage.dialog(this, string.noFile, "${e.localizedMessage ?: e}\n\n$uri")
        }
        return true
    }

    @CheckResult
    private fun showError(@StringRes titleId: Int, @StringRes msgId: Int) =
        InfoMessage.dialog(this, titleId, msgId)
}