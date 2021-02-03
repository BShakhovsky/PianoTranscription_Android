package ru.bshakhovsky.piano_transcription.web

import android.app.Activity
import android.app.DownloadManager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

import android.database.Cursor
import android.net.Uri
import android.os.Environment

import androidx.annotation.CheckResult
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

import ru.bshakhovsky.piano_transcription.R.string

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage

import java.lang.ref.WeakReference

class DownloadsReceiver : BroadcastReceiver(), LifecycleObserver {

    /* Activity is not WeakPtr, but WeakReference, because we need it onDestroy (unregister),
    and unfortunately, activity is garbage collected first */
    // TODO: Eliminate reference to Activity
    private lateinit var activity: WeakReference<Activity>
    private lateinit var downManager: DownloadManager

    // KeyMap of downloads is instantiated here just once to survive device screen rotation
    private val downloads = mutableMapOf<Long, String>()

    fun initialize(lifecycle: Lifecycle, a: Activity) {
        lifecycle.addObserver(this)
        a.registerReceiver(this, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        activity = WeakReference(a)
        downManager = a.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun unregister() {
        DebugMode.assertState(activity.get() != null)
        activity.get()?.run { unregisterReceiver(this@DownloadsReceiver) }
        activity.clear()
        DebugMode.assertState(activity.get() == null)
    }

    override fun onReceive(c: Context?, intent: Intent?) {
        DebugMode.assertArgument((c == activity.get()) and (intent != null))
        intent?.run {
            when (action) {
                DownloadManager.ACTION_DOWNLOAD_COMPLETE ->
                    getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                        .let { if (it in downloads) checkFile(it) }
                else -> DebugMode.assertArgument(false)
            }
        }
    }

    fun addDownload(youTubeUrl: String, downloadUrl: Uri, fileName: String) {
        if (youTubeUrl in downloads.values) {
            // Download button tapped multiple times, already downloading
            InfoMessage.toast(activity.get()?.applicationContext, string.alreadyDown)
            return
        }
        downloads += downManager.enqueue(
            DownloadManager.Request(downloadUrl).setTitle(fileName)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
        ) to youTubeUrl
        InfoMessage.toast(activity.get()?.applicationContext, string.downStart)
    }

    private fun checkFile(id: Long): Unit = with(downManager) {
        query(DownloadManager.Query().setFilterById(id)).use {
            with(it) { if (!moveToFirst() || !checkStatus(this)) return }
        }
        getUriForDownloadedFile(id).let { downFile ->
            DebugMode.assertState(activity.get() != null)
            activity.get()?.run {
                setResult(Activity.RESULT_OK, Intent().apply {
                    data = downFile
                    DebugMode.assertState(downloads[id] != null)
                    putExtra("YouTube Link", downloads[id])
                })
                finish()
            }
        }
    }

    @CheckResult
    private fun checkStatus(cursor: Cursor) = cursor.run {
        getInt(getColumnIndex(DownloadManager.COLUMN_REASON)).let {
            DebugMode.assertState(activity.get() != null)
            var result = false
            activity.get()?.run {
                InfoMessage.toast(applicationContext, @Suppress("LongLine", "Reformat")
                    when (getInt(getColumnIndex(DownloadManager.COLUMN_STATUS))) {

                        DownloadManager.STATUS_FAILED       -> "${getString(string.downFailed)
                        }: ${getString(when(it) {
                            DownloadManager.ERROR_CANNOT_RESUME         -> string.cannotResume
                            DownloadManager.ERROR_DEVICE_NOT_FOUND      -> string.storageMount
                            DownloadManager.ERROR_FILE_ALREADY_EXISTS   -> string.downExists
                            DownloadManager.ERROR_FILE_ERROR            -> string.storageIssue
                            DownloadManager.ERROR_HTTP_DATA_ERROR       -> string.httpError
                            DownloadManager.ERROR_INSUFFICIENT_SPACE    -> string.memoryDownload
                            DownloadManager.ERROR_TOO_MANY_REDIRECTS    -> string.manyRedirects
                            DownloadManager.ERROR_UNHANDLED_HTTP_CODE   -> string.unhandledHttp
                            DownloadManager.ERROR_UNKNOWN               -> string.unknownReason
                            else -> DebugMode.assertState(false).let { string.unknownReason } })}"

                        DownloadManager.STATUS_PAUSED       -> "${getString(string.downPaused)
                        }: ${getString(when(it) {
                            DownloadManager.PAUSED_QUEUED_FOR_WIFI      -> string.queuedWiFi
                            DownloadManager.PAUSED_WAITING_FOR_NETWORK  -> string.waitNet
                            DownloadManager.PAUSED_WAITING_TO_RETRY     -> string.netRetry
                            DownloadManager.PAUSED_UNKNOWN              -> string.unknownReason
                            else -> DebugMode.assertState(false).let { string.unknownReason } })}"

                        DownloadManager.STATUS_PENDING      -> getString(string.downPending)
                        DownloadManager.STATUS_RUNNING      -> getString(string.downRunning)
                        DownloadManager.STATUS_SUCCESSFUL   -> getString(string.downSuccess).also { result = true }

                        else -> @Suppress("ComplexRedundantLet")
                        DebugMode.assertState(false).let { getString(string.unknownStatus) } })
            }
            result
        }
    }
}