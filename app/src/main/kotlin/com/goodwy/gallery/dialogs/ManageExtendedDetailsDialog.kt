package com.goodwy.gallery.dialogs

import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.extensions.getAlertDialogBuilder
import com.goodwy.commons.extensions.setupDialogStuff
import com.goodwy.gallery.R
import com.goodwy.gallery.extensions.config
import com.goodwy.gallery.helpers.*
import kotlinx.android.synthetic.main.dialog_manage_extended_details.view.*

class ManageExtendedDetailsDialog(val activity: BaseSimpleActivity, val callback: (result: Int) -> Unit) {
    private var view = activity.layoutInflater.inflate(R.layout.dialog_manage_extended_details, null)

    init {
        val details = activity.config.extendedDetails
        view.apply {
            manage_extended_details_name.isChecked = details and EXT_NAME != 0
            manage_extended_details_path.isChecked = details and EXT_PATH != 0
            manage_extended_details_size.isChecked = details and EXT_SIZE != 0
            manage_extended_details_resolution.isChecked = details and EXT_RESOLUTION != 0
            manage_extended_details_last_modified.isChecked = details and EXT_LAST_MODIFIED != 0
            manage_extended_details_date_taken.isChecked = details and EXT_DATE_TAKEN != 0
            manage_extended_details_camera.isChecked = details and EXT_CAMERA_MODEL != 0
            manage_extended_details_exif.isChecked = details and EXT_EXIF_PROPERTIES != 0
            manage_extended_details_gps_coordinates.isChecked = details and EXT_GPS != 0
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(view, this)
            }
    }

    private fun dialogConfirmed() {
        var result = 0
        view.apply {
            if (manage_extended_details_name.isChecked)
                result += EXT_NAME
            if (manage_extended_details_path.isChecked)
                result += EXT_PATH
            if (manage_extended_details_size.isChecked)
                result += EXT_SIZE
            if (manage_extended_details_resolution.isChecked)
                result += EXT_RESOLUTION
            if (manage_extended_details_last_modified.isChecked)
                result += EXT_LAST_MODIFIED
            if (manage_extended_details_date_taken.isChecked)
                result += EXT_DATE_TAKEN
            if (manage_extended_details_camera.isChecked)
                result += EXT_CAMERA_MODEL
            if (manage_extended_details_exif.isChecked)
                result += EXT_EXIF_PROPERTIES
            if (manage_extended_details_gps_coordinates.isChecked)
                result += EXT_GPS
        }

        activity.config.extendedDetails = result
        callback(result)
    }
}
