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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

import ru.bshakhovsky.piano_transcription.R.string

import ru.bshakhovsky.piano_transcription.utils.DebugMode
import ru.bshakhovsky.piano_transcription.utils.InfoMessage

import java.lang.ref.WeakReference

class DownloadsReceiver(lifecycle: Lifecycle, a: Activity) :
    BroadcastReceiver(), LifecycleObserver {

    /* Activity is not WeakPtr, because we need it onDestroy (unregister),
    and unfortunately, activity is garbage collected first */
    private val activity = WeakReference(a)
    private val downManager = a.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val downloads = mutableSetOf<Long>()

    init {
        lifecycle.addObserver(this)
        a.registerReceiver(this, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
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

    fun addDownload(webUrl: Uri, fileName: String) {
        downloads += downManager.enqueue(
            DownloadManager.Request(webUrl).setTitle(fileName)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setNotificationVisibility(
                    DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                )
        )
        InfoMessage.toast(activity.get(), string.downStart)
    }

    private fun checkFile(id: Long): Unit = with(downManager) {
        query(DownloadManager.Query().setFilterById(id)).use {
            with(it) { if (!moveToFirst() || !checkStatus(this)) return }
        }
        getUriForDownloadedFile(id).let { downFile ->
            DebugMode.assertState(activity.get() != null)
            activity.get()?.run {
                setResult(Activity.RESULT_OK, Intent().apply { data = downFile })
                finish()
            }
        }
    }

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