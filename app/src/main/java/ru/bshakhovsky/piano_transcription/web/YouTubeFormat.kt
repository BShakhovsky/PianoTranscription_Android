package ru.bshakhovsky.piano_transcription.web

@Suppress("SpellCheckingInspection")
// https://github.com/KomeijiKaede/AndroidYoutubeDL
// /blob/experimental/app/src/main/java/net/teamfruit/androidyoutubedl/utils/Extractor.kt

object YouTubeFormat {

    val audios: Map<String, String> = mapOf(
        """audio/mp4; codecs="mp4a.40.2"""" to "m4a",
        """audio/webm; codecs="opus"""" to "aac"
    )

    data class AdaptiveFormat(
        val url: String,
        val mimeType: String,
        @Suppress("SpellCheckingInspection") val itag: Int,

        val averageBitrate: Int,
        val audioSampleRate: String,
        val contentsLength: String
    )

    data class Thumbnails(val thumbnails: List<Thumbnail>)
    data class Thumbnail(
        val url: String,
        val width: String,
        val height: String
    )

    data class StreamingData(val adaptiveFormats: List<AdaptiveFormat>)
    data class PlayabilityStatus(val status: String)
    data class VideoDetails(
        val title: String,
        val author: String,
        val thumbnail: Thumbnails
    )

    data class PlayerResponse(
        val streamingData: StreamingData?,
        val playabilityStatus: PlayabilityStatus,
        val videoDetails: VideoDetails
    )
}