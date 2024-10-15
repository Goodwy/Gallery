package com.goodwy.gallery.dialogs

import androidx.appcompat.app.AlertDialog
import com.goodwy.commons.extensions.getAlertDialogBuilder
import com.goodwy.commons.extensions.setupDialogStuff
import com.goodwy.gallery.activities.SimpleActivity
import com.goodwy.gallery.databinding.DialogStoragePermissionRequiredBinding

class StoragePermissionRequiredDialog(
    val activity: SimpleActivity,
    val onOkay: () -> Unit,
    val onCancel: () -> Unit,
    val callback: (dialog: AlertDialog) -> Unit
) {

    init {
        val binding = DialogStoragePermissionRequiredBinding.inflate(activity.layoutInflater)
        activity.getAlertDialogBuilder()
            .setPositiveButton(com.goodwy.commons.R.string.go_to_settings) { dialog, _ ->
                dialog.dismiss()
                onOkay()
            }
            .setNegativeButton(com.goodwy.commons.R.string.cancel) { dialog, _ ->
                dialog.dismiss()
                onCancel()
            }
            .apply {
                activity.setupDialogStuff(
                    view = binding.root,
                    dialog = this,
                    cancelOnTouchOutside = false,
                    callback = callback
                )
            }
    }
}
