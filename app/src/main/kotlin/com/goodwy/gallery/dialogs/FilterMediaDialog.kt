package com.goodwy.gallery.dialogs

import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.extensions.getAlertDialogBuilder
import com.goodwy.commons.extensions.setupDialogStuff
import com.goodwy.gallery.R
import com.goodwy.gallery.extensions.config
import com.goodwy.gallery.helpers.*
import kotlinx.android.synthetic.main.dialog_filter_media.view.*

class FilterMediaDialog(val activity: BaseSimpleActivity, val callback: (result: Int) -> Unit) {
    private var view = activity.layoutInflater.inflate(R.layout.dialog_filter_media, null)

    init {
        val filterMedia = activity.config.filterMedia
        view.apply {
            filter_media_images.isChecked = filterMedia and TYPE_IMAGES != 0
            filter_media_videos.isChecked = filterMedia and TYPE_VIDEOS != 0
            filter_media_gifs.isChecked = filterMedia and TYPE_GIFS != 0
            filter_media_raws.isChecked = filterMedia and TYPE_RAWS != 0
            filter_media_svgs.isChecked = filterMedia and TYPE_SVGS != 0
            filter_media_portraits.isChecked = filterMedia and TYPE_PORTRAITS != 0
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, R.string.filter_media)
            }
    }

    private fun dialogConfirmed() {
        var result = 0
        if (view.filter_media_images.isChecked)
            result += TYPE_IMAGES
        if (view.filter_media_videos.isChecked)
            result += TYPE_VIDEOS
        if (view.filter_media_gifs.isChecked)
            result += TYPE_GIFS
        if (view.filter_media_raws.isChecked)
            result += TYPE_RAWS
        if (view.filter_media_svgs.isChecked)
            result += TYPE_SVGS
        if (view.filter_media_portraits.isChecked)
            result += TYPE_PORTRAITS

        if (result == 0) {
            result = getDefaultFileFilter()
        }

        if (activity.config.filterMedia != result) {
            activity.config.filterMedia = result
            callback(result)
        }
    }
}
