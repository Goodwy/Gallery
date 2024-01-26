package com.goodwy.gallery.dialogs

import androidx.appcompat.app.AlertDialog
import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.dialogs.ConfirmationDialog
import com.goodwy.commons.dialogs.FilePickerDialog
import com.goodwy.commons.extensions.*
import com.goodwy.gallery.R
import com.goodwy.gallery.databinding.DialogExportFavoritesBinding
import com.goodwy.gallery.extensions.config

class ExportFavoritesDialog(
    val activity: BaseSimpleActivity, val defaultFilename: String, val hidePath: Boolean,
    callback: (path: String, filename: String) -> Unit
) {
    init {
        val lastUsedFolder = activity.config.lastExportedFavoritesFolder
        var folder = if (lastUsedFolder.isNotEmpty() && activity.getDoesFilePathExist(lastUsedFolder)) {
            lastUsedFolder
        } else {
            activity.internalStoragePath
        }

        val binding = DialogExportFavoritesBinding.inflate(activity.layoutInflater).apply {
            exportFavoritesFilename.setText(defaultFilename.removeSuffix(".txt"))

            if (hidePath) {
                exportFavoritesPathLabel.beGone()
                exportFavoritesPath.beGone()
            } else {
                exportFavoritesPath.text = activity.humanizePath(folder)
                exportFavoritesPath.setOnClickListener {
                    FilePickerDialog(activity, folder, false, showFAB = true) {
                        exportFavoritesPath.text = activity.humanizePath(it)
                        folder = it
                    }
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.goodwy.commons.R.string.ok, null)
            .setNegativeButton(com.goodwy.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.export_favorite_paths) { alertDialog ->
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        var filename = binding.exportFavoritesFilename.value
                        if (filename.isEmpty()) {
                            activity.toast(com.goodwy.commons.R.string.filename_cannot_be_empty)
                            return@setOnClickListener
                        }

                        filename += ".txt"
                        val newPath = "${folder.trimEnd('/')}/$filename"
                        if (!newPath.getFilenameFromPath().isAValidFilename()) {
                            activity.toast(com.goodwy.commons.R.string.filename_invalid_characters)
                            return@setOnClickListener
                        }

                        activity.config.lastExportedFavoritesFolder = folder
                        if (!hidePath && activity.getDoesFilePathExist(newPath)) {
                            val title = String.format(
                                activity.getString(com.goodwy.commons.R.string.file_already_exists_overwrite),
                                newPath.getFilenameFromPath()
                            )
                            ConfirmationDialog(activity, title) {
                                callback(newPath, filename)
                                alertDialog.dismiss()
                            }
                        } else {
                            callback(newPath, filename)
                            alertDialog.dismiss()
                        }
                    }
                }
            }
    }
}
