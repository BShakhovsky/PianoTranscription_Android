# Description

Android app which may help you to learn the MIDI chords on your real piano, there is the real-time transcription feature.  The app will recognize the notes you play and highlight them.  Ones you correctly play all the notes of the chord simultaneously, it will proceed to the next MIDI chord, and so on.

Not only MIDI-files are supported, but also MP3, MP4, etc.  If you do not have a MIDI, you can open any audio file (the app can also extract audio-stream of some video formats).  Polyphonic piano transcription feature will generate MIDI from audio/video.

No instrument information is extracted, and all transcribed notes get combined into one part.  The accuracy depends on the complexity of the song, and is obviously higher for solo piano pieces.  Currently, accuracy for piano pieces is around 75%.

Want to transcribe some piano piece from YouTube?  You can google for websites/apps that will download video from YouTube.  You then can open downloaded file in my application.

# For Windows 7 or later click on the following screenshot:

[![](https://GitHub.com/BShakhovsky/PianoTranscription3D/raw/master/Keyboard.png 'Windows 7')](https://GitHub.com/BShakhovsky/PianoTranscription3D/blob/master/README.md)

# For Android 8.0 (API level 26) or higher:

[![](https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png 'Get it on Google Play')](https://play.google.com/store/apps/details?id=ru.BShakhovsky.Piano_Transcription)

# About Midi/Karaoke Files

You can find plenty of them over the internet.  Those *.mid or *.kar files usually consist of several tracks, including percussion.  You probably would not want to play percussion-tracks on piano, because their "MIDI-notes" do not overload correctly on piano-notes.  So, in the most cases, you may choose any kind of tracks, but percussions (like "Drums", "Rhythms", "Hit", "Blow", "Strike", "Clash", etc.) will be disabled.

# Privacy Policy

If you want to record audio using my app, you will have to grant permission to use microphone:

* __*Microphone permission (android.permission.RECORD_AUDIO)*__

The app also uses the following third party services that may collect information used to identify you:

* [Google Play Services](https://www.google.com/policies/privacy)
* [Google AdMob](https://support.google.com/admob/answer/6128543)

For complete __*Privacy Policy*__ see [Privacy Policy](https://BShakhovsky.GitHub.io/PrivacyPolicy)

# How to Use

[![](app/src/main/res/drawable-nodpi/user_guide_01.webp 'Main Activity')](https://play.google.com/store/apps/details?id=ru.BShakhovsky.Piano_Transcription)

1. Open any MIDI- or Karaoke-file or any other audio/video-file (such as MP3, MP4, etc.) or record a piano piece with microphone.

[![](app/src/main/res/drawable-nodpi/user_guide_02.webp 'Attach new audio')](https://play.google.com/store/apps/details?id=ru.BShakhovsky.Piano_Transcription)

2. The program will automatically transcribe audio and save as a MIDI-file.

[![](app/src/main/res/drawable-nodpi/user_guide_03.webp 'Generate MIDI from audio/video')](https://play.google.com/store/apps/details?id=ru.BShakhovsky.Piano_Transcription)

However, no instrument information is extracted, and all transcribed notes get combined into one part.  The accuracy depends on the complexity of the song, and is obviously higher for solo piano pieces.  Accuracy for piano pieces is around 75%.

3. If you opened an existing MIDI-file, select tracks.  Percussion-tracks (like “Drums”, “Rhythms”, “Hit”, “Blow”, “Strike”, “Clash”, etc.) will be disabled.

[![](app/src/main/res/drawable-nodpi/user_guide_04.webp 'Select tracks & MIDI-file info')](https://play.google.com/store/apps/details?id=ru.BShakhovsky.Piano_Transcription)

For some MIDI-files downloaded from Internet, there can be dozens of tracks.

[![](app/src/main/res/drawable-nodpi/user_guide_05.webp 'Select tracks & MIDI-file info')](https://play.google.com/store/apps/details?id=ru.BShakhovsky.Piano_Transcription)

4. If you want just to play the song in real time, tap in the upper-middle area of the screen.  Or if you want to go forward or backwards chord-by-chord, you can pause and tap in the upper-left or upper-right areas of the screen.

[![](app/src/main/res/drawable-nodpi/user_guide_06.webp 'Play/pause/seek/prev/next')](https://play.google.com/store/apps/details?id=ru.BShakhovsky.Piano_Transcription)

5. If you want to learn the MIDI chords on your real piano, you can use the real-time transcription feature.

[![](app/src/main/res/drawable-nodpi/user_guide_07.webp 'Realtime Microphone')](https://play.google.com/store/apps/details?id=ru.BShakhovsky.Piano_Transcription)

Correctly played notes will be highlighted with green color, mistakes - with red.

[![](app/src/main/res/drawable-nodpi/user_guide_08.webp 'Realtime Transcription')](https://play.google.com/store/apps/details?id=ru.BShakhovsky.Piano_Transcription)

6. If somebody talks near your phone, or if you sit it in a loud environment, it will incorrectly recognize too many notes which you actually do not play.  Too many keys will be red in this case and it will be annoying.
   So, if you want better recognition accuracy, ideally there should be no other sounds than your piano.
   If you have convenient headphones which you can connect to your electric piano, there is a life-hack - you can “put on headphones on mobile device” like on the following photo:

[![](app/src/main/res/drawable-nodpi/user_guide_09.webp 'Headphones on mobile')](https://play.google.com/store/apps/details?id=ru.BShakhovsky.Piano_Transcription)

   Turn the volume louder.

[![](app/src/main/res/drawable-nodpi/user_guide_10.webp 'Volume louder')](https://play.google.com/store/apps/details?id=ru.BShakhovsky.Piano_Transcription)

7. Ones you correctly play all the notes of the chord simultaneously (all of the pressed keys are green), it will automatically proceed to the next MIDI chord, and so on.

8. Enjoy :smile: