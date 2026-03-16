package com.goodwy.gallery.helpers

import android.content.Context
import android.content.res.Configuration
import android.os.Environment
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.goodwy.commons.helpers.BaseConfig
import com.goodwy.commons.helpers.FONT_SIZE_MEDIUM
import com.goodwy.commons.helpers.PROTECTION_PATTERN
import com.goodwy.commons.helpers.SORT_BY_DATE_MODIFIED
import com.goodwy.commons.helpers.SORT_DESCENDING
import com.goodwy.commons.helpers.VIEW_TYPE_GRID
import com.goodwy.gallery.R
import com.goodwy.gallery.models.AlbumCover
import java.util.Arrays
import java.util.Locale

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var directorySorting: Int
        get(): Int = prefs.getInt(DIRECTORY_SORT_ORDER, SORT_BY_DATE_MODIFIED or SORT_DESCENDING)
        set(order) = prefs.edit { putInt(DIRECTORY_SORT_ORDER, order) }

    fun saveFolderGrouping(path: String, value: Int) {
        if (path.isEmpty()) {
            groupBy = value
        } else {
            prefs.edit { putInt(GROUP_FOLDER_PREFIX + path.lowercase(Locale.getDefault()), value) }
        }
    }

    fun getFolderGrouping(path: String): Int {
        var groupBy = prefs.getInt(GROUP_FOLDER_PREFIX + path.lowercase(Locale.getDefault()), groupBy)
        if (path != SHOW_ALL && groupBy and GROUP_BY_FOLDER != 0) {
            groupBy -= GROUP_BY_FOLDER + 1
        }
        return groupBy
    }

    fun removeFolderGrouping(path: String) {
        prefs.edit { remove(GROUP_FOLDER_PREFIX + path.lowercase(Locale.getDefault())) }
    }

    fun hasCustomGrouping(path: String) = prefs.contains(GROUP_FOLDER_PREFIX + path.lowercase(Locale.getDefault()))

    fun saveFolderViewType(path: String, value: Int) {
        if (path.isEmpty()) {
            viewTypeFiles = value
        } else {
            prefs.edit { putInt(VIEW_TYPE_PREFIX + path.lowercase(Locale.getDefault()), value) }
        }
    }

    fun getFolderViewType(path: String) = prefs.getInt(VIEW_TYPE_PREFIX + path.lowercase(Locale.getDefault()), viewTypeFiles)

    fun removeFolderViewType(path: String) {
        prefs.edit { remove(VIEW_TYPE_PREFIX + path.lowercase(Locale.getDefault())) }
    }

    fun hasCustomViewType(path: String) = prefs.contains(VIEW_TYPE_PREFIX + path.lowercase(Locale.getDefault()))

    var wasHideFolderTooltipShown: Boolean
        get() = prefs.getBoolean(HIDE_FOLDER_TOOLTIP_SHOWN, false)
        set(wasShown) = prefs.edit { putBoolean(HIDE_FOLDER_TOOLTIP_SHOWN, wasShown) }

    var shouldShowHidden = showHiddenMedia || temporarilyShowHidden

    var showHiddenMedia: Boolean
        get() = prefs.getBoolean(SHOW_HIDDEN_MEDIA, false)
        set(showHiddenFolders) = prefs.edit { putBoolean(SHOW_HIDDEN_MEDIA, showHiddenFolders) }

    var temporarilyShowHidden: Boolean
        get() = prefs.getBoolean(TEMPORARILY_SHOW_HIDDEN, false)
        set(temporarilyShowHidden) = prefs.edit { putBoolean(TEMPORARILY_SHOW_HIDDEN, temporarilyShowHidden) }

    var temporarilyShowExcluded: Boolean
        get() = prefs.getBoolean(TEMPORARILY_SHOW_EXCLUDED, false)
        set(temporarilyShowExcluded) = prefs.edit { putBoolean(TEMPORARILY_SHOW_EXCLUDED, temporarilyShowExcluded) }

    var isThirdPartyIntent: Boolean
        get() = prefs.getBoolean(IS_THIRD_PARTY_INTENT, false)
        set(isThirdPartyIntent) = prefs.edit { putBoolean(IS_THIRD_PARTY_INTENT, isThirdPartyIntent) }

    var pinnedFolders: Set<String>
        get() = prefs.getStringSet(PINNED_FOLDERS, HashSet<String>())!!
        set(pinnedFolders) = prefs.edit { putStringSet(PINNED_FOLDERS, pinnedFolders) }

    var showAll: Boolean
        get() = prefs.getBoolean(SHOW_ALL, false)
        set(showAll) = prefs.edit { putBoolean(SHOW_ALL, showAll) }

    fun addPinnedFolders(paths: Set<String>) {
        val currPinnedFolders = HashSet<String>(pinnedFolders)
        currPinnedFolders.addAll(paths)
        pinnedFolders = currPinnedFolders.filter { it.isNotEmpty() }.toHashSet()
        if (paths.contains(RECYCLE_BIN)) {
            showRecycleBinLast = false
        }
    }

    fun removePinnedFolders(paths: Set<String>) {
        val currPinnedFolders = HashSet<String>(pinnedFolders)
        currPinnedFolders.removeAll(paths)
        pinnedFolders = currPinnedFolders
    }

    fun addExcludedFolder(path: String) {
        addExcludedFolders(HashSet<String>(Arrays.asList(path)))
    }

    fun addExcludedFolders(paths: Set<String>) {
        val currExcludedFolders = HashSet<String>(excludedFolders)
        currExcludedFolders.addAll(paths)
        excludedFolders = currExcludedFolders.filter { it.isNotEmpty() }.toHashSet()
    }

    fun removeExcludedFolder(path: String) {
        val currExcludedFolders = HashSet<String>(excludedFolders)
        currExcludedFolders.remove(path)
        excludedFolders = currExcludedFolders
    }

    var excludedFolders: MutableSet<String>
        get() = prefs.getStringSet(EXCLUDED_FOLDERS, HashSet())!!
        set(excludedFolders) = prefs.edit { remove(EXCLUDED_FOLDERS).putStringSet(EXCLUDED_FOLDERS, excludedFolders) }

    var isExcludedPasswordProtectionOn: Boolean
        get() = prefs.getBoolean(EXCLUDED_PASSWORD_PROTECTION, false)
        set(isExcludedPasswordProtectionOn) = prefs.edit { putBoolean(EXCLUDED_PASSWORD_PROTECTION, isExcludedPasswordProtectionOn) }

    var excludedPasswordHash: String
        get() = prefs.getString(EXCLUDED_PASSWORD_HASH, "")!!
        set(excludedPasswordHash) = prefs.edit { putString(EXCLUDED_PASSWORD_HASH, excludedPasswordHash) }

    var excludedProtectionType: Int
        get() = prefs.getInt(EXCLUDED_PROTECTION_TYPE, PROTECTION_PATTERN)
        set(excludedProtectionType) = prefs.edit { putInt(EXCLUDED_PROTECTION_TYPE, excludedProtectionType) }

    fun addIncludedFolder(path: String) {
        val currIncludedFolders = HashSet<String>(includedFolders)
        currIncludedFolders.add(path)
        includedFolders = currIncludedFolders
    }

    fun addIncludedFolders(paths: Set<String>) {
        val currIncludedFolders = HashSet<String>(includedFolders)
        currIncludedFolders.addAll(paths)
        includedFolders = currIncludedFolders.filter { it.isNotEmpty() }.toHashSet()
    }

    fun removeIncludedFolder(path: String) {
        val currIncludedFolders = HashSet<String>(includedFolders)
        currIncludedFolders.remove(path)
        includedFolders = currIncludedFolders
    }

    var includedFolders: MutableSet<String>
        get() = prefs.getStringSet(INCLUDED_FOLDERS, HashSet<String>())!!
        set(includedFolders) = prefs.edit { remove(INCLUDED_FOLDERS).putStringSet(INCLUDED_FOLDERS, includedFolders) }

    var autoplayVideos: Boolean
        get() = prefs.getBoolean(AUTOPLAY_VIDEOS, false)
        set(autoplayVideos) = prefs.edit { putBoolean(AUTOPLAY_VIDEOS, autoplayVideos) }

    var animateGifs: Boolean
        get() = prefs.getBoolean(ANIMATE_GIFS, false)
        set(animateGifs) = prefs.edit { putBoolean(ANIMATE_GIFS, animateGifs) }

    var maxBrightness: Boolean
        get() = prefs.getBoolean(MAX_BRIGHTNESS, false)
        set(maxBrightness) = prefs.edit { putBoolean(MAX_BRIGHTNESS, maxBrightness) }

    var ultraHdrRendering: Boolean
        get() = prefs.getBoolean(ULTRA_HDR_RENDERING, true)
        set(ultraHdrRendering) = prefs.edit { putBoolean(ULTRA_HDR_RENDERING, ultraHdrRendering) }

    var playbackSpeed: Float
        get() = prefs.getFloat(PLAYBACK_SPEED, 1f)
        set(playbackSpeed) = prefs.edit { putFloat(PLAYBACK_SPEED, playbackSpeed) }

    var playbackSpeedProgress: Int
        get() = prefs.getInt(PLAYBACK_SPEED_PROGRESS, -1)
        set(playbackSpeedProgress) = prefs.edit { putInt(PLAYBACK_SPEED_PROGRESS, playbackSpeedProgress) }

    var cropThumbnails: Boolean
        get() = prefs.getBoolean(CROP_THUMBNAILS, true)
        set(cropThumbnails) = prefs.edit { putBoolean(CROP_THUMBNAILS, cropThumbnails) }

    var showThumbnailVideoDuration: Boolean
        get() = prefs.getBoolean(SHOW_THUMBNAIL_VIDEO_DURATION, true)
        set(showThumbnailVideoDuration) = prefs.edit { putBoolean(SHOW_THUMBNAIL_VIDEO_DURATION, showThumbnailVideoDuration) }

    var showThumbnailFileTypes: Boolean
        get() = prefs.getBoolean(SHOW_THUMBNAIL_FILE_TYPES, true)
        set(showThumbnailFileTypes) = prefs.edit { putBoolean(SHOW_THUMBNAIL_FILE_TYPES, showThumbnailFileTypes) }

    var markFavoriteItems: Boolean
        get() = prefs.getBoolean(MARK_FAVORITE_ITEMS, true)
        set(markFavoriteItems) = prefs.edit { putBoolean(MARK_FAVORITE_ITEMS, markFavoriteItems) }

    var screenRotation: Int
        get() = prefs.getInt(SCREEN_ROTATION, ROTATE_BY_SYSTEM_SETTING)
        set(screenRotation) = prefs.edit { putInt(SCREEN_ROTATION, screenRotation) }

    var fileLoadingPriority: Int
        get() = prefs.getInt(FILE_LOADING_PRIORITY, PRIORITY_SPEED)
        set(fileLoadingPriority) = prefs.edit { putInt(FILE_LOADING_PRIORITY, fileLoadingPriority) }

    var loopVideos: Boolean
        get() = prefs.getBoolean(LOOP_VIDEOS, false)
        set(loop) = prefs.edit { putBoolean(LOOP_VIDEOS, loop) }

    var muteVideos: Boolean
        get() = prefs.getBoolean(MUTE_VIDEOS, false)
        set(muteVideos) = prefs.edit { putBoolean(MUTE_VIDEOS, muteVideos) }

    var gestureVideoPlayer: Boolean
        get() = prefs.getBoolean(GESTURE_VIDEO_PLAYER, false)
        set(gestureVideoPlayer) = prefs.edit {
            putBoolean(GESTURE_VIDEO_PLAYER, gestureVideoPlayer)
        }

    var videoPlayerType: Int
        get() = prefs.getInt(VIDEO_PLAYER_TYPE, if (appRunCount <= 1) VIDEO_PLAYER_APP else VIDEO_PLAYER_SYSTEM)
        set(videoPlayerType) = prefs.edit { putInt(VIDEO_PLAYER_TYPE, videoPlayerType) }

    var displayFileNames: Boolean
        get() = prefs.getBoolean(DISPLAY_FILE_NAMES, false)
        set(display) = prefs.edit { putBoolean(DISPLAY_FILE_NAMES, display) }

    var blackBackground: Boolean
        get() = prefs.getBoolean(BLACK_BACKGROUND, true)
        set(blackBackground) = prefs.edit { putBoolean(BLACK_BACKGROUND, blackBackground) }

    var filterMedia: Int
        get() = prefs.getInt(FILTER_MEDIA, getDefaultFileFilter())
        set(filterMedia) = prefs.edit { putInt(FILTER_MEDIA, filterMedia) }

    var dirColumnCnt: Int
        get() = prefs.getInt(getDirectoryColumnsField(), getDefaultDirectoryColumnCount())
        set(dirColumnCnt) = prefs.edit { putInt(getDirectoryColumnsField(), dirColumnCnt) }

    var defaultFolder: String
        get() = prefs.getString(DEFAULT_FOLDER, "")!!
        set(defaultFolder) = prefs.edit { putString(DEFAULT_FOLDER, defaultFolder) }

    var allowInstantChange: Boolean
        get() = prefs.getBoolean(ALLOW_INSTANT_CHANGE, false)
        set(allowInstantChange) = prefs.edit { putBoolean(ALLOW_INSTANT_CHANGE, allowInstantChange) }

    private fun getDirectoryColumnsField(): String {
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        return if (isPortrait) {
            if (scrollHorizontally) {
                DIR_HORIZONTAL_COLUMN_CNT
            } else {
                DIR_COLUMN_CNT
            }
        } else {
            if (scrollHorizontally) {
                DIR_LANDSCAPE_HORIZONTAL_COLUMN_CNT
            } else {
                DIR_LANDSCAPE_COLUMN_CNT
            }
        }
    }

    private fun getDefaultDirectoryColumnCount() = context.resources.getInteger(
        if (scrollHorizontally) {
            R.integer.directory_columns_horizontal_scroll
        } else {
            R.integer.directory_columns_vertical_scroll
        }
    )

    var mediaColumnCnt: Int
        get() = prefs.getInt(getMediaColumnsField(), getDefaultMediaColumnCount())
        set(mediaColumnCnt) = prefs.edit { putInt(getMediaColumnsField(), mediaColumnCnt) }

    private fun getMediaColumnsField(): String {
        val isPortrait = context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        return if (isPortrait) {
            if (scrollHorizontally) {
                MEDIA_HORIZONTAL_COLUMN_CNT
            } else {
                MEDIA_COLUMN_CNT
            }
        } else {
            if (scrollHorizontally) {
                MEDIA_LANDSCAPE_HORIZONTAL_COLUMN_CNT
            } else {
                MEDIA_LANDSCAPE_COLUMN_CNT
            }
        }
    }

    private fun getDefaultMediaColumnCount() = context.resources.getInteger(
        if (scrollHorizontally) {
            R.integer.media_columns_horizontal_scroll
        } else {
            R.integer.media_columns_vertical_scroll
        }
    )

    var albumCovers: String
        get() = prefs.getString(ALBUM_COVERS, "")!!
        set(albumCovers) = prefs.edit { putString(ALBUM_COVERS, albumCovers) }

    fun parseAlbumCovers(): ArrayList<AlbumCover> {
        val listType = object : TypeToken<List<AlbumCover>>() {}.type
        return Gson().fromJson<ArrayList<AlbumCover>>(albumCovers, listType) ?: ArrayList(1)
    }

    var hideSystemUI: Boolean
        get() = prefs.getBoolean(HIDE_SYSTEM_UI, false)
        set(hideSystemUI) = prefs.edit { putBoolean(HIDE_SYSTEM_UI, hideSystemUI) }

    var deleteEmptyFolders: Boolean
        get() = prefs.getBoolean(DELETE_EMPTY_FOLDERS, false)
        set(deleteEmptyFolders) = prefs.edit { putBoolean(DELETE_EMPTY_FOLDERS, deleteEmptyFolders) }

    var keepScreenOn: Boolean
        get() = prefs.getBoolean(KEEP_SCREEN_ON, true)
        set(keepScreenOn) = prefs.edit { putBoolean(KEEP_SCREEN_ON, keepScreenOn) }

    var allowPhotoGestures: Boolean
        get() = prefs.getBoolean(ALLOW_PHOTO_GESTURES, false)
        set(allowPhotoGestures) = prefs.edit { putBoolean(ALLOW_PHOTO_GESTURES, allowPhotoGestures) }

    var allowVideoGestures: Boolean
        get() = prefs.getBoolean(ALLOW_VIDEO_GESTURES, true)
        set(allowVideoGestures) = prefs.edit { putBoolean(ALLOW_VIDEO_GESTURES, allowVideoGestures) }

    var slideshowInterval: Int
        get() = prefs.getInt(SLIDESHOW_INTERVAL, SLIDESHOW_DEFAULT_INTERVAL)
        set(slideshowInterval) = prefs.edit { putInt(SLIDESHOW_INTERVAL, slideshowInterval) }

    var slideshowIncludeVideos: Boolean
        get() = prefs.getBoolean(SLIDESHOW_INCLUDE_VIDEOS, false)
        set(slideshowIncludeVideos) = prefs.edit { putBoolean(SLIDESHOW_INCLUDE_VIDEOS, slideshowIncludeVideos) }

    var slideshowIncludeGIFs: Boolean
        get() = prefs.getBoolean(SLIDESHOW_INCLUDE_GIFS, false)
        set(slideshowIncludeGIFs) = prefs.edit { putBoolean(SLIDESHOW_INCLUDE_GIFS, slideshowIncludeGIFs) }

    var slideshowRandomOrder: Boolean
        get() = prefs.getBoolean(SLIDESHOW_RANDOM_ORDER, false)
        set(slideshowRandomOrder) = prefs.edit { putBoolean(SLIDESHOW_RANDOM_ORDER, slideshowRandomOrder) }

    var slideshowMoveBackwards: Boolean
        get() = prefs.getBoolean(SLIDESHOW_MOVE_BACKWARDS, false)
        set(slideshowMoveBackwards) = prefs.edit { putBoolean(SLIDESHOW_MOVE_BACKWARDS, slideshowMoveBackwards) }

    var slideshowAnimation: Int
        get() = prefs.getInt(SLIDESHOW_ANIMATION, SLIDESHOW_ANIMATION_SLIDE)
        set(slideshowAnimation) = prefs.edit { putInt(SLIDESHOW_ANIMATION, slideshowAnimation) }

    var loopSlideshow: Boolean
        get() = prefs.getBoolean(SLIDESHOW_LOOP, false)
        set(loopSlideshow) = prefs.edit { putBoolean(SLIDESHOW_LOOP, loopSlideshow) }

    var tempFolderPath: String
        get() = prefs.getString(TEMP_FOLDER_PATH, "")!!
        set(tempFolderPath) = prefs.edit { putString(TEMP_FOLDER_PATH, tempFolderPath) }

    var viewTypeFolders: Int
        get() = prefs.getInt(VIEW_TYPE_FOLDERS, VIEW_TYPE_GRID)
        set(viewTypeFolders) = prefs.edit { putInt(VIEW_TYPE_FOLDERS, viewTypeFolders) }

    var viewTypeFiles: Int
        get() = prefs.getInt(VIEW_TYPE_FILES, VIEW_TYPE_GRID)
        set(viewTypeFiles) = prefs.edit { putInt(VIEW_TYPE_FILES, viewTypeFiles) }

    var showExtendedDetails: Boolean
        get() = prefs.getBoolean(SHOW_EXTENDED_DETAILS, false)
        set(showExtendedDetails) = prefs.edit { putBoolean(SHOW_EXTENDED_DETAILS, showExtendedDetails) }

    var hideExtendedDetails: Boolean
        get() = prefs.getBoolean(HIDE_EXTENDED_DETAILS, true)
        set(hideExtendedDetails) = prefs.edit { putBoolean(HIDE_EXTENDED_DETAILS, hideExtendedDetails) }

    var extendedDetails: Int
        get() = prefs.getInt(EXTENDED_DETAILS, EXT_RESOLUTION or EXT_LAST_MODIFIED or EXT_EXIF_PROPERTIES)
        set(extendedDetails) = prefs.edit { putInt(EXTENDED_DETAILS, extendedDetails) }

    var wasNewAppShown: Boolean
        get() = prefs.getBoolean(WAS_NEW_APP_SHOWN, false)
        set(wasNewAppShown) = prefs.edit { putBoolean(WAS_NEW_APP_SHOWN, wasNewAppShown) }

    var lastFilepickerPath: String
        get() = prefs.getString(LAST_FILEPICKER_PATH, "")!!
        set(lastFilepickerPath) = prefs.edit { putString(LAST_FILEPICKER_PATH, lastFilepickerPath) }

    var tempSkipDeleteConfirmation: Boolean
        get() = prefs.getBoolean(TEMP_SKIP_DELETE_CONFIRMATION, false)
        set(tempSkipDeleteConfirmation) = prefs.edit { putBoolean(TEMP_SKIP_DELETE_CONFIRMATION, tempSkipDeleteConfirmation) }

    var tempSkipRecycleBin: Boolean
        get() = prefs.getBoolean(TEMP_SKIP_RECYCLE_BIN, false)
        set(tempSkipRecycleBin) = prefs.edit { putBoolean(TEMP_SKIP_RECYCLE_BIN, tempSkipRecycleBin) }

    var wereFavoritesPinned: Boolean
        get() = prefs.getBoolean(WERE_FAVORITES_PINNED, false)
        set(wereFavoritesPinned) = prefs.edit { putBoolean(WERE_FAVORITES_PINNED, wereFavoritesPinned) }

    var wasRecycleBinPinned: Boolean
        get() = prefs.getBoolean(WAS_RECYCLE_BIN_PINNED, false)
        set(wasRecycleBinPinned) = prefs.edit { putBoolean(WAS_RECYCLE_BIN_PINNED, wasRecycleBinPinned) }

    var wasSVGShowingHandled: Boolean
        get() = prefs.getBoolean(WAS_SVG_SHOWING_HANDLED, false)
        set(wasSVGShowingHandled) = prefs.edit { putBoolean(WAS_SVG_SHOWING_HANDLED, wasSVGShowingHandled) }

    var groupBy: Int
        get() = prefs.getInt(GROUP_BY, GROUP_BY_LAST_MODIFIED_DAILY)
        set(groupBy) = prefs.edit { putInt(GROUP_BY, groupBy) }

    var useRecycleBin: Boolean
        get() = prefs.getBoolean(USE_RECYCLE_BIN, true)
        set(useRecycleBin) = prefs.edit { putBoolean(USE_RECYCLE_BIN, useRecycleBin) }

    var bottomActions: Boolean
        get() = prefs.getBoolean(BOTTOM_ACTIONS, true)
        set(bottomActions) = prefs.edit { putBoolean(BOTTOM_ACTIONS, bottomActions) }

    fun removeLastVideoPosition(path: String) {
        prefs.edit { remove("$LAST_VIDEO_POSITION_PREFIX${path.lowercase(Locale.getDefault())}") }
    }

    fun saveLastVideoPosition(path: String, value: Int) {
        if (path.isNotEmpty()) {
            prefs.edit { putInt("$LAST_VIDEO_POSITION_PREFIX${path.lowercase(Locale.getDefault())}", value) }
        }
    }

    fun getLastVideoPosition(path: String) = prefs.getInt("$LAST_VIDEO_POSITION_PREFIX${path.lowercase(Locale.getDefault())}", 0)

    fun getAllLastVideoPositions() = prefs.all.filterKeys {
        it.startsWith(LAST_VIDEO_POSITION_PREFIX)
    }

    var rememberLastVideoPosition: Boolean
        get() = prefs.getBoolean(REMEMBER_LAST_VIDEO_POSITION, false)
        set(rememberLastVideoPosition) {
            if (!rememberLastVideoPosition) {
                getAllLastVideoPositions().forEach {
                    prefs.edit { remove(it.key) }
                }
            }
            prefs.edit { putBoolean(REMEMBER_LAST_VIDEO_POSITION, rememberLastVideoPosition) }
        }

    var visibleBottomActions: Int
        get() = prefs.getInt(VISIBLE_BOTTOM_ACTIONS, DEFAULT_BOTTOM_ACTIONS)
        set(visibleBottomActions) = prefs.edit { putInt(VISIBLE_BOTTOM_ACTIONS, visibleBottomActions) }

    // if a user hides a folder, then enables temporary hidden folder displaying, make sure we show it properly
    var everShownFolders: Set<String>
        get() = prefs.getStringSet(EVER_SHOWN_FOLDERS, getEverShownFolders())!!
        set(everShownFolders) = prefs.edit { putStringSet(EVER_SHOWN_FOLDERS, everShownFolders) }

    private fun getEverShownFolders() = hashSetOf(
        internalStoragePath,
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath,
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath,
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath,
        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).absolutePath}/Screenshots",
        "$internalStoragePath/WhatsApp/Media/WhatsApp Images",
        "$internalStoragePath/WhatsApp/Media/WhatsApp Images/Sent",
        "$internalStoragePath/WhatsApp/Media/WhatsApp Video",
        "$internalStoragePath/WhatsApp/Media/WhatsApp Video/Sent",
        "$internalStoragePath/WhatsApp/Media/.Statuses",
        "$internalStoragePath/Android/media/com.whatsapp/WhatsApp/Media",
        "$internalStoragePath/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images",
        "$internalStoragePath/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video"
    )

    var showRecycleBinAtFolders: Boolean
        get() = prefs.getBoolean(SHOW_RECYCLE_BIN_AT_FOLDERS, true)
        set(showRecycleBinAtFolders) = prefs.edit { putBoolean(SHOW_RECYCLE_BIN_AT_FOLDERS, showRecycleBinAtFolders) }

    var allowZoomingImages: Boolean
        get() = prefs.getBoolean(ALLOW_ZOOMING_IMAGES, true)
        set(allowZoomingImages) = prefs.edit { putBoolean(ALLOW_ZOOMING_IMAGES, allowZoomingImages) }

    var lastBinCheck: Long
        get() = prefs.getLong(LAST_BIN_CHECK, 0L)
        set(lastBinCheck) = prefs.edit { putLong(LAST_BIN_CHECK, lastBinCheck) }

    var lastMediaDbMaintenance: Long
        get() = prefs.getLong(LAST_MEDIA_DB_MAINTENANCE, 0L)
        set(lastMaintenance) = prefs.edit().putLong(LAST_MEDIA_DB_MAINTENANCE, lastMaintenance).apply()

    var showHighestQuality: Boolean
        get() = prefs.getBoolean(SHOW_HIGHEST_QUALITY, false)
        set(showHighestQuality) = prefs.edit { putBoolean(SHOW_HIGHEST_QUALITY, showHighestQuality) }

    var showRecycleBinLast: Boolean
        get() = prefs.getBoolean(SHOW_RECYCLE_BIN_LAST, false)
        set(showRecycleBinLast) = prefs.edit { putBoolean(SHOW_RECYCLE_BIN_LAST, showRecycleBinLast) }

    var allowDownGesture: Boolean
        get() = prefs.getBoolean(ALLOW_DOWN_GESTURE, true)
        set(allowDownGesture) = prefs.edit { putBoolean(ALLOW_DOWN_GESTURE, allowDownGesture) }

    var lastEditorCropAspectRatio: Int
        get() = prefs.getInt(LAST_EDITOR_CROP_ASPECT_RATIO, ASPECT_RATIO_FREE)
        set(lastEditorCropAspectRatio) = prefs.edit { putInt(LAST_EDITOR_CROP_ASPECT_RATIO, lastEditorCropAspectRatio) }

    var lastEditorCropOtherAspectRatioX: Float
        get() = prefs.getFloat(LAST_EDITOR_CROP_OTHER_ASPECT_RATIO_X, 2f)
        set(lastEditorCropOtherAspectRatioX) = prefs.edit { putFloat(LAST_EDITOR_CROP_OTHER_ASPECT_RATIO_X, lastEditorCropOtherAspectRatioX) }

    var lastEditorCropOtherAspectRatioY: Float
        get() = prefs.getFloat(LAST_EDITOR_CROP_OTHER_ASPECT_RATIO_Y, 1f)
        set(lastEditorCropOtherAspectRatioY) = prefs.edit { putFloat(LAST_EDITOR_CROP_OTHER_ASPECT_RATIO_Y, lastEditorCropOtherAspectRatioY) }

    var groupDirectSubfolders: Boolean
        get() = prefs.getBoolean(GROUP_DIRECT_SUBFOLDERS, false)
        set(groupDirectSubfolders) = prefs.edit { putBoolean(GROUP_DIRECT_SUBFOLDERS, groupDirectSubfolders) }

    var showWidgetFolderName: Boolean
        get() = prefs.getBoolean(SHOW_WIDGET_FOLDER_NAME, true)
        set(showWidgetFolderName) = prefs.edit { putBoolean(SHOW_WIDGET_FOLDER_NAME, showWidgetFolderName) }

    var allowOneToOneZoom: Boolean
        get() = prefs.getBoolean(ALLOW_ONE_TO_ONE_ZOOM, true)
        set(allowOneToOneZoom) = prefs.edit { putBoolean(ALLOW_ONE_TO_ONE_ZOOM, allowOneToOneZoom) }

    var allowRotatingWithGestures: Boolean
        get() = prefs.getBoolean(ALLOW_ROTATING_WITH_GESTURES, true)
        set(allowRotatingWithGestures) = prefs.edit { putBoolean(ALLOW_ROTATING_WITH_GESTURES, allowRotatingWithGestures) }

    var lastEditorDrawColor: Int
        get() = prefs.getInt(LAST_EDITOR_DRAW_COLOR, primaryColor)
        set(lastEditorDrawColor) = prefs.edit { putInt(LAST_EDITOR_DRAW_COLOR, lastEditorDrawColor) }

    var lastEditorBrushSize: Int
        get() = prefs.getInt(LAST_EDITOR_BRUSH_SIZE, 50)
        set(lastEditorBrushSize) = prefs.edit { putInt(LAST_EDITOR_BRUSH_SIZE, lastEditorBrushSize) }

    var showNotch: Boolean
        get() = prefs.getBoolean(SHOW_NOTCH, true)
        set(showNotch) = prefs.edit { putBoolean(SHOW_NOTCH, showNotch) }

    val showNotchFlow = ::showNotch.asFlowNonNull(true)

    var spamFoldersChecked: Boolean
        get() = prefs.getBoolean(SPAM_FOLDERS_CHECKED, false)
        set(spamFoldersChecked) = prefs.edit { putBoolean(SPAM_FOLDERS_CHECKED, spamFoldersChecked) }

    var editorBrushColor: Int
        get() = prefs.getInt(EDITOR_BRUSH_COLOR, -1)
        set(editorBrushColor) = prefs.edit { putInt(EDITOR_BRUSH_COLOR, editorBrushColor) }

    var editorBrushHardness: Float
        get() = prefs.getFloat(EDITOR_BRUSH_HARDNESS, 0.5f)
        set(editorBrushHardness) = prefs.edit { putFloat(EDITOR_BRUSH_HARDNESS, editorBrushHardness) }

    var editorBrushSize: Float
        get() = prefs.getFloat(EDITOR_BRUSH_SIZE, 0.05f)
        set(editorBrushSize) = prefs.edit { putFloat(EDITOR_BRUSH_SIZE, editorBrushSize) }

    var wereFavoritesMigrated: Boolean
        get() = prefs.getBoolean(WERE_FAVORITES_MIGRATED, false)
        set(wereFavoritesMigrated) = prefs.edit { putBoolean(WERE_FAVORITES_MIGRATED, wereFavoritesMigrated) }

    var showFolderMediaCount: Int
        get() = prefs.getInt(FOLDER_MEDIA_COUNT, FOLDER_MEDIA_CNT_LINE)
        set(showFolderMediaCount) = prefs.edit { putInt(FOLDER_MEDIA_COUNT, showFolderMediaCount) }

    var folderStyle: Int
        get() = prefs.getInt(FOLDER_THUMBNAIL_STYLE, FOLDER_STYLE_ROUNDED_CORNERS)
        set(folderStyle) = prefs.edit { putInt(FOLDER_THUMBNAIL_STYLE, folderStyle) }

    var limitFolderTitle: Boolean
        get() = prefs.getBoolean(LIMIT_FOLDER_TITLE, false)
        set(limitFolderTitle) = prefs.edit { putBoolean(LIMIT_FOLDER_TITLE, limitFolderTitle) }

    var thumbnailSpacing: Int
        get() = prefs.getInt(THUMBNAIL_SPACING, 6)
        set(thumbnailSpacing) = prefs.edit { putInt(THUMBNAIL_SPACING, thumbnailSpacing) }

    var fileRoundedCorners: Boolean
        get() = prefs.getBoolean(FILE_ROUNDED_CORNERS, false)
        set(fileRoundedCorners) = prefs.edit { putBoolean(FILE_ROUNDED_CORNERS, fileRoundedCorners) }

    var customFoldersOrder: String
        get() = prefs.getString(CUSTOM_FOLDERS_ORDER, "")!!
        set(customFoldersOrder) = prefs.edit { putString(CUSTOM_FOLDERS_ORDER, customFoldersOrder) }

    var avoidShowingAllFilesPrompt: Boolean
        get() = prefs.getBoolean(AVOID_SHOWING_ALL_FILES_PROMPT, false)
        set(avoidShowingAllFilesPrompt) = prefs.edit { putBoolean(AVOID_SHOWING_ALL_FILES_PROMPT, avoidShowingAllFilesPrompt) }

    var searchAllFilesByDefault: Boolean
        get() = prefs.getBoolean(SEARCH_ALL_FILES_BY_DEFAULT, false)
        set(searchAllFilesByDefault) = prefs.edit { putBoolean(SEARCH_ALL_FILES_BY_DEFAULT, searchAllFilesByDefault) }

    var lastExportedFavoritesFolder: String
        get() = prefs.getString(LAST_EXPORTED_FAVORITES_FOLDER, "")!!
        set(lastExportedFavoritesFolder) = prefs.edit { putString(LAST_EXPORTED_FAVORITES_FOLDER, lastExportedFavoritesFolder) }

    var showPermissionRationale: Boolean
        get() = prefs.getBoolean(SHOW_PERMISSION_RATIONALE, false)
        set(showPermissionRationale) = prefs.edit { putBoolean(SHOW_PERMISSION_RATIONALE, showPermissionRationale) }

    //Goodwy
    var showWarning: Boolean
        get() = prefs.getBoolean(SHOW_WARNING, true)
        set(showWarning) = prefs.edit { putBoolean(SHOW_WARNING, showWarning) }

    var hideGroupingBar: Boolean
        get() = prefs.getBoolean(HIDE_GROUPING_BAR, false)
        set(hideGroupingBar) = prefs.edit { putBoolean(HIDE_GROUPING_BAR, hideGroupingBar) }

    var hideGroupingButton: Boolean
        get() = prefs.getBoolean(HIDE_GROUPING_BUTTON, false)
        set(hideGroupingButton) = prefs.edit { putBoolean(HIDE_GROUPING_BUTTON, hideGroupingButton) }

    var hideGroupingBarWhenScroll: Boolean
        get() = prefs.getBoolean(HIDE_GROUPING_BAR_WHEN_SCROLLING, false)
        set(hideGroupingBarWhenScroll) = prefs.edit { putBoolean(HIDE_GROUPING_BAR_WHEN_SCROLLING, hideGroupingBarWhenScroll) }

    var fontSizeDir: Int
        get() = prefs.getInt(FONT_SIZE_DIR, FONT_SIZE_MEDIUM)
        set(size) = prefs.edit { putInt(FONT_SIZE_DIR, size) }

    var diskCacheSizeMB: Int
        get() = prefs.getInt(DISK_CACHE_SIZE_MB, DEFAULT_DISK_CACHE_SIZE_MB).coerceIn(MIN_DISK_CACHE_SIZE_MB, MAX_DISK_CACHE_SIZE_MB)
        set(value) = prefs.edit().putInt(DISK_CACHE_SIZE_MB, value.coerceIn(MIN_DISK_CACHE_SIZE_MB, MAX_DISK_CACHE_SIZE_MB)).apply()
}
