package com.goodwy.gallery.asynctasks

import android.content.Context
import android.os.AsyncTask
import android.util.Log
import com.goodwy.commons.helpers.FAVORITES
import com.goodwy.commons.helpers.SORT_BY_DATE_MODIFIED
import com.goodwy.commons.helpers.SORT_BY_DATE_TAKEN
import com.goodwy.commons.helpers.SORT_BY_SIZE
import android.os.Environment
import com.goodwy.commons.helpers.isRPlus
import com.goodwy.gallery.extensions.config
import com.goodwy.gallery.extensions.getFavoritePaths
import com.goodwy.gallery.helpers.*
import com.goodwy.gallery.models.Medium
import com.goodwy.gallery.models.ThumbnailItem
import kotlinx.coroutines.*

class GetMediaAsynctask(
    val context: Context, val mPath: String, val isPickImage: Boolean = false, val isPickVideo: Boolean = false,
    val showAll: Boolean, val callback: (media: ArrayList<ThumbnailItem>) -> Unit
) :
    AsyncTask<Void, Void, ArrayList<ThumbnailItem>>() {
    companion object {
        private const val TAG = "GetMediaAsynctask"
    }

    private val mediaFetcher = MediaFetcher(context)

    override fun doInBackground(vararg params: Void): ArrayList<ThumbnailItem> {
        val pathToUse = if (showAll) SHOW_ALL else mPath
        val folderGrouping = context.config.getFolderGrouping(pathToUse)
        val folderSorting = context.config.getFolderSorting(pathToUse)
        val getProperDateTaken = folderSorting and SORT_BY_DATE_TAKEN != 0 ||
            folderGrouping and GROUP_BY_DATE_TAKEN_DAILY != 0 ||
            folderGrouping and GROUP_BY_DATE_TAKEN_MONTHLY != 0 ||
            folderGrouping and GROUP_BY_DATE_TAKEN_YEARLY != 0

        val getProperLastModified = folderSorting and SORT_BY_DATE_MODIFIED != 0 ||
            folderGrouping and GROUP_BY_LAST_MODIFIED_DAILY != 0 ||
            folderGrouping and GROUP_BY_LAST_MODIFIED_MONTHLY != 0 ||
            folderGrouping and GROUP_BY_LAST_MODIFIED_YEARLY != 0

        val getProperFileSize = folderSorting and SORT_BY_SIZE != 0
        val favoritePaths = context.getFavoritePaths()
        val getVideoDurations = context.config.showThumbnailVideoDuration
        val lastModifieds = if (getProperLastModified) mediaFetcher.getLastModifieds() else HashMap()
        val dateTakens = if (getProperDateTaken) mediaFetcher.getDateTakens() else HashMap()

        val media = if (showAll) {
            val foldersToScan = mediaFetcher.getFoldersToScan().filter { it != RECYCLE_BIN && it != FAVORITES && !context.config.isFolderProtected(it) }

            val shouldPrefetchAndroid11Files = isRPlus() && !Environment.isExternalStorageManager()
            val prefetchedAndroid11Files = if (shouldPrefetchAndroid11Files) {
                val queryStartedAt = System.currentTimeMillis()
                Log.d(TAG, "showAll refresh: getAndroid11FolderMedia started at $queryStartedAt")
                mediaFetcher.getAndroid11FolderMedia(
                    isPickImage,
                    isPickVideo,
                    favoritePaths,
                    false,
                    getProperDateTaken,
                    HashMap(dateTakens)
                ).also {
                    val queryFinishedAt = System.currentTimeMillis()
                    Log.d(TAG, "showAll refresh: getAndroid11FolderMedia done in ${queryFinishedAt - queryStartedAt}ms")
                }
            } else {
                null
            }

            val allMedia = ArrayList<Medium>()
            val lock = Any()
            runBlocking(Dispatchers.IO) {
                foldersToScan.map { folderPath ->
                    async {
                        if (mediaFetcher.shouldStop) return@async
                        val newMedia = mediaFetcher.getFilesFrom(
                            folderPath, isPickImage, isPickVideo, getProperDateTaken, getProperLastModified,
                            getProperFileSize, favoritePaths, getVideoDurations,
                            HashMap(lastModifieds), HashMap(dateTakens), prefetchedAndroid11Files
                        )
                        synchronized(lock) { allMedia.addAll(newMedia) }
                    }
                }.awaitAll()
            }

            mediaFetcher.sortMedia(allMedia, context.config.getFolderSorting(SHOW_ALL))
            allMedia
        } else {
            mediaFetcher.getFilesFrom(
                mPath, isPickImage, isPickVideo, getProperDateTaken, getProperLastModified, getProperFileSize, favoritePaths,
                getVideoDurations, lastModifieds, dateTakens, null
            )
        }

        return mediaFetcher.groupMedia(media, pathToUse)
    }

    override fun onPostExecute(media: ArrayList<ThumbnailItem>) {
        super.onPostExecute(media)
        callback(media)
    }

    fun stopFetching() {
        mediaFetcher.shouldStop = true
        cancel(true)
    }
}
