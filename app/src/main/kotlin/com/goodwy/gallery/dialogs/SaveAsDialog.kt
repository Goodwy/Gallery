package com.goodwy.gallery.dialogs

import androidx.appcompat.app.AlertDialog
import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.dialogs.FilePickerDialog
import com.goodwy.commons.extensions.getAlertDialogBuilder
import com.goodwy.commons.extensions.getFilenameFromPath
import com.goodwy.commons.extensions.getParentPath
import com.goodwy.commons.extensions.getPicturesDirectoryPath
import com.goodwy.commons.extensions.hideKeyboard
import com.goodwy.commons.extensions.humanizePath
import com.goodwy.commons.extensions.isAValidFilename
import com.goodwy.commons.extensions.isInDownloadDir
import com.goodwy.commons.extensions.isRestrictedWithSAFSdk30
import com.goodwy.commons.extensions.setupDialogStuff
import com.goodwy.commons.extensions.showKeyboard
import com.goodwy.commons.extensions.toast
import com.goodwy.commons.extensions.value
import com.goodwy.commons.helpers.isRPlus
import com.goodwy.gallery.databinding.DialogSaveAsBinding
import com.goodwy.gallery.extensions.ensureWritablePath

class SaveAsDialog(
    val activity: BaseSimpleActivity,
    val path: String,
    val appendFilename: Boolean,
    val cancelCallback: (() -> Unit)? = null,
    val callback: (savePath: String) -> Unit
) {
    private val binding = DialogSaveAsBinding.inflate(activity.layoutInflater)
    private var realPath = path.getParentPath().run {
        if (activity.isRestrictedWithSAFSdk30(this) && !activity.isInDownloadDir(this)) {
            activity.getPicturesDirectoryPath(this)
        } else {
            this
        }
    }

    init {
        binding.apply {
            folderValue.setText("${activity.humanizePath(realPath).trimEnd('/')}/")

            val fullName = path.getFilenameFromPath()
            val dotAt = fullName.lastIndexOf(".")
            var name = fullName

            if (dotAt > 0) {
                name = fullName.substring(0, dotAt)
                val extension = fullName.substring(dotAt + 1)
                extensionValue.setText(extension)
            }

            if (appendFilename) {
                name += "_1"
            }

            filenameValue.setText(name)
            folderValue.setOnClickListener {
                activity.hideKeyboard(folderValue)
                FilePickerDialog(
                    activity = activity,
                    currPath = realPath,
                    pickFile = false,
                    showHidden = false,
                    showFAB = true,
                    canAddShowHiddenButton = true
                ) {
                    folderValue.setText(activity.humanizePath(it))
                    realPath = it
                }
            }
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.goodwy.commons.R.string.ok, null)
            .setNegativeButton(com.goodwy.commons.R.string.cancel) { dialog, which -> cancelCallback?.invoke() }
            .setOnCancelListener { cancelCallback?.invoke() }
            .apply {
                activity.setupDialogStuff(
                    view = binding.root,
                    dialog = this,
                    titleId = com.goodwy.commons.R.string.save_as
                ) { alertDialog ->
                    alertDialog.showKeyboard(binding.filenameValue)
                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        validateAndConfirmPath(alertDialog::dismiss)
                    }
                }
            }
    }

    private fun validateAndConfirmPath(dismiss: () -> Unit) {
        val filename = binding.filenameValue.value
        val extension = binding.extensionValue.value

        if (filename.isEmpty()) {
            activity.toast(com.goodwy.commons.R.string.filename_cannot_be_empty)
            return
        }

        if (extension.isEmpty()) {
            activity.toast(com.goodwy.commons.R.string.extension_cannot_be_empty)
            return
        }

        val newFilename = "$filename.$extension"
        val newPath = "${realPath.trimEnd('/')}/$newFilename"
        if (!newFilename.isAValidFilename()) {
            activity.toast(com.goodwy.commons.R.string.filename_invalid_characters)
            return
        }

        activity.ensureWritablePath(
            targetPath = newPath,
            confirmOverwrite = true,
            onCancel = cancelCallback
        ) {
            callback(newPath)
            dismiss()
        }
    }
}
