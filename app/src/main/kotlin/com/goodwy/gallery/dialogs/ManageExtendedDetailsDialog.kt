package com.goodwy.gallery.dialogs

import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.extensions.getAlertDialogBuilder
import com.goodwy.commons.extensions.setupDialogStuff
import com.goodwy.gallery.R
import com.goodwy.gallery.databinding.DialogManageExtendedDetailsBinding
import com.goodwy.gallery.extensions.config
import com.goodwy.gallery.helpers.*

class ManageExtendedDetailsDialog(val activity: BaseSimpleActivity, val callback: (result: Int) -> Unit) {
    private val binding = DialogManageExtendedDetailsBinding.inflate(activity.layoutInflater)

    init {
        val details = activity.config.extendedDetails
        binding.apply {
            manageExtendedDetailsName.isChecked = details and EXT_NAME != 0
            manageExtendedDetailsPath.isChecked = details and EXT_PATH != 0
            manageExtendedDetailsSize.isChecked = details and EXT_SIZE != 0
            manageExtendedDetailsResolution.isChecked = details and EXT_RESOLUTION != 0
            manageExtendedDetailsLastModified.isChecked = details and EXT_LAST_MODIFIED != 0
            manageExtendedDetailsDateTaken.isChecked = details and EXT_DATE_TAKEN != 0
            manageExtendedDetailsCamera.isChecked = details and EXT_CAMERA_MODEL != 0
            manageExtendedDetailsExif.isChecked = details and EXT_EXIF_PROPERTIES != 0
            manageExtendedDetailsGpsCoordinates.isChecked = details and EXT_GPS != 0
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.goodwy.commons.R.string.ok) { dialog, which -> dialogConfirmed() }
            .setNegativeButton(com.goodwy.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.manage_extended_details)
            }
    }

    private fun dialogConfirmed() {
        var result = 0
        binding.apply {
            if (manageExtendedDetailsName.isChecked)
                result += EXT_NAME
            if (manageExtendedDetailsPath.isChecked)
                result += EXT_PATH
            if (manageExtendedDetailsSize.isChecked)
                result += EXT_SIZE
            if (manageExtendedDetailsResolution.isChecked)
                result += EXT_RESOLUTION
            if (manageExtendedDetailsLastModified.isChecked)
                result += EXT_LAST_MODIFIED
            if (manageExtendedDetailsDateTaken.isChecked)
                result += EXT_DATE_TAKEN
            if (manageExtendedDetailsCamera.isChecked)
                result += EXT_CAMERA_MODEL
            if (manageExtendedDetailsExif.isChecked)
                result += EXT_EXIF_PROPERTIES
            if (manageExtendedDetailsGpsCoordinates.isChecked)
                result += EXT_GPS
        }

        activity.config.extendedDetails = result
        callback(result)
    }
}
