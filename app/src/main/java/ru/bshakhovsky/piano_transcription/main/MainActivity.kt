package ru.bshakhovsky.piano_transcription.main

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup

import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.CheckResult
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment.InstantiationException
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
import ru.bshakhovsky.piano_transcription.utils.Share
import ru.bshakhovsky.piano_transcription.utils.StrictPolicy

import java.io.FileNotFoundException
import java.util.Locale

class MainActivity : AppCompatActivity() {

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

    @SuppressLint("CheckResult")
    private val getMidi =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            with(result) {
                if (resultCode != RESULT_OK) return@with
                DebugMode.assertState((data != null) and (data?.data != null))
                data?.data?.let { if (!openMidi(it)) showError(string.badFile, string.notMidi) }
            }
        }
    private val getMedia =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            with(result) {
                if (resultCode != RESULT_OK) return@with
                DebugMode.assertState(data != null)
                data?.let { intent ->
                    DebugMode.assertState(intent.data != null)
                    intent.data?.let { openMedia(it, intent.getStringExtra("YouTube Link")) }
                }
            }
        }
    private val getTranscription =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            with(result) {
                if (resultCode != RESULT_OK) return@with
                DebugMode.assertState((data != null) and (data?.data != null))
                data?.data?.let { uri -> openMidi(uri).let { ok -> DebugMode.assertState(ok) } }
            }
        }

    @SuppressLint("MissingSuperCall") // Calling super.onCreate more than once can lead to crashes
    override fun onCreate(savedInstanceState: Bundle?) {
        if (DebugMode.debug) {
            Thread.setDefaultUncaughtExceptionHandler(Crash(getExternalFilesDir("Errors")))
            policy = StrictPolicy(lifecycle, this@MainActivity)
        }
        @Suppress("SpellCheckingInspection") try {
            super.onCreate(savedInstanceState) /* Samsung Galaxy J3 Pro	Android 8.0 (SDK 26)
            androidx.fragment.app.Fragment$InstantiationException -> java.lang.NoSuchMethodException

            https://stackoverflow.com/a/48149345
            https://stackoverflow.com/questions/48057871/
            android-4-2-activity-oncreate-fragment/48149345#48149345

            Looks like system can put incorrect Bundle savedInstanceState on some android devices */
        } catch (_: InstantiationException) {
            super.onCreate(null) /* https://github.com/software-mansion/
            react-native-screens/issues/114#issuecomment-934342627

            Be warned that this can break application functionality.
            This includes the navigation stack which will now be wiped-out.
            In our case, this has an unacceptable side-effect of not being able
            to use the document picker to upload pictures and other media
                    as it would return them to the incorrect state after picking media.

            This fix resolves the crash but as the activity is crashed in background,
            the app looses its state and gets stuck at the first screen.
            Navigation to further screens is blocked with this fix. */
        }
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        with(ViewModelProvider(this)) {
            model = get(MainModel::class.java)
                .apply { initialize(lifecycle, supportFragmentManager, getMidi, getMedia) }
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
                        "87FD000F52337DF09DBB9E6684B0B878", // Samsung Galaxy S9+, Android 10
                        "9AA370206113A6039BC7B5BB02F0F7E8", // Samsung Galaxy A7, Android 6
                        @Suppress("SpellCheckingInspection")
                        "921547C5AAEF85EB72E17DFAE23DD8BA", // Huawei MediaPad X2, Android 5
                        "CA07E6A50A0CCC9296341E801663E998" // HTC Desire 610, Android 4.4
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
            /* TODO: Also pause the loaded MIDI while recording the audio
                Add LiveData<Boolean> isRecording */
        }

        onNewIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        DebugMode.assertArgument(intent != null)
        intent?.run {
            when (action) {
                Intent.ACTION_MAIN -> {
                    if (categories != null)
                        @Suppress("SpellCheckingInspection")
                        /* Samsung Galaxy A11, Tecno CAMON 17, Tecno TECNO SPARK 5 Pro
                        java.lang.NullPointerException */
                        DebugMode.assertState(
                            (categories.size == 1) and hasCategory(Intent.CATEGORY_LAUNCHER)
                        )
                    @Suppress("Reformat") DebugMode.assertState(
                        (type == null) and (data == null) and (dataString == null)
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
                }

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean = super.onCreateOptionsMenu(menu).also {
        menuInflater.inflate(menu_main, menu.also { mainMenu = it })
        mainMenu?.findItem(id.menuGuide)?.isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        super.onOptionsItemSelected(item).also {
            when (item.itemId) {
                id.menuMic -> {
                    if (SystemClock.uptimeMillis() - micClickTime < 1_000) return it
                    micClickTime = SystemClock.uptimeMillis()

                    realTime.toggle() // TODO: Turning the mic on takes a long time
                    if (play.isPlaying.value == true) play.playPause()
                }
                id.menuShare -> Share.share(this)
                else -> DebugMode.assertArgument(false)
            }
        }

    override fun onBackPressed(): Unit = with(binding.drawerLayout) {
        GravityCompat.START.let { if (isDrawerOpen(it)) closeDrawer(it) else super.onBackPressed() }
    }

    fun openMedia(file: Uri, youTubeLink: String? = null) {
        if (!openMidi(file)) getTranscription.launch(Intent(this, MediaActivity::class.java).apply {
            data = file
            youTubeLink?.let { putExtra("YouTube Link", it) }
        })
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
        } catch (e: SecurityException) {
            InfoMessage.dialog(
                this, string.noFile,
                "${getString(string.securityFile)}\n\n${e.localizedMessage ?: e}\n\n$uri"
            )
        }
        return true
    }

    @CheckResult
    private fun showError(@StringRes titleId: Int, @StringRes msgId: Int) =
        InfoMessage.dialog(this, titleId, msgId)
}