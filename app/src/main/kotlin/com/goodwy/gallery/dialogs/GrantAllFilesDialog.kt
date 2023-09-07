package com.goodwy.gallery.dialogs

import android.view.View
import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.extensions.applyColorFilter
import com.goodwy.commons.extensions.getAlertDialogBuilder
import com.goodwy.commons.extensions.getProperTextColor
import com.goodwy.commons.extensions.setupDialogStuff
import com.goodwy.gallery.R
import com.goodwy.gallery.extensions.launchGrantAllFilesIntent
import kotlinx.android.synthetic.main.dialog_grant_all_files.view.*

class GrantAllFilesDialog(val activity: BaseSimpleActivity) {
    init {
        val view: View = activity.layoutInflater.inflate(R.layout.dialog_grant_all_files, null)
        view.grant_all_files_image.applyColorFilter(activity.getProperTextColor())

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> activity.launchGrantAllFilesIntent() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this, cancelOnTouchOutside = false) { alertDialog -> }
            }
    }
}
