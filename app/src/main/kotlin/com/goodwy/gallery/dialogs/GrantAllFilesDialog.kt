package com.goodwy.gallery.dialogs

import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.extensions.applyColorFilter
import com.goodwy.commons.extensions.getAlertDialogBuilder
import com.goodwy.commons.extensions.getProperTextColor
import com.goodwy.commons.extensions.setupDialogStuff
import com.goodwy.gallery.databinding.DialogGrantAllFilesBinding
import com.goodwy.gallery.extensions.launchGrantAllFilesIntent

class GrantAllFilesDialog(val activity: BaseSimpleActivity) {
    init {
        val binding = DialogGrantAllFilesBinding.inflate(activity.layoutInflater)
        binding.grantAllFilesImage.applyColorFilter(activity.getProperTextColor())

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.goodwy.commons.R.string.ok) { dialog, which -> activity.launchGrantAllFilesIntent() }
            .setNegativeButton(com.goodwy.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this) { alertDialog -> }
            }
    }
}
