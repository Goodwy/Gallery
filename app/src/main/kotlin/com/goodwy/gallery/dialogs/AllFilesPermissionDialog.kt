package com.goodwy.gallery.dialogs

import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.extensions.getAlertDialogBuilder
import com.goodwy.commons.extensions.setupDialogStuff
import com.goodwy.gallery.R

class AllFilesPermissionDialog(
    val activity: BaseSimpleActivity, message: String = "", val callback: (result: Boolean) -> Unit, val neutralPressed: () -> Unit
) {
    private var dialog: AlertDialog? = null

    init {
        val view = activity.layoutInflater.inflate(com.goodwy.commons.R.layout.dialog_message, null)
        view.findViewById<TextView>(R.id.message).text = message

        activity.getAlertDialogBuilder().setPositiveButton(R.string.all_files) { dialog, which -> positivePressed() }
            .setNeutralButton(R.string.media_only) { dialog, which -> neutralPressed() }
            .apply {
                activity.setupDialogStuff(view, this) { alertDialog ->
                    dialog = alertDialog
                }
            }
    }

    private fun positivePressed() {
        dialog?.dismiss()
        callback(true)
    }
}
