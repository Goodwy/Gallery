package com.goodwy.gallery.dialogs

import android.graphics.Point
import androidx.appcompat.app.AlertDialog
import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.extensions.*
import com.goodwy.commons.helpers.ensureBackgroundThread
import com.goodwy.gallery.R
import com.goodwy.gallery.databinding.DialogResizeMultipleImagesBinding
import com.goodwy.gallery.extensions.ensureWriteAccess
import com.goodwy.gallery.extensions.rescanPathsAndUpdateLastModified
import com.goodwy.gallery.extensions.resizeImage
import java.io.File
import kotlin.math.roundToInt

private const val DEFAULT_RESIZE_FACTOR = "75"

class ResizeMultipleImagesDialog(
    private val activity: BaseSimpleActivity,
    private val imagePaths: List<String>,
    private val imageSizes: List<Point>,
    private val callback: () -> Unit
) {

    private var dialog: AlertDialog? = null
    private val binding = DialogResizeMultipleImagesBinding.inflate(activity.layoutInflater)
    private val progressView = binding.resizeProgress
    private val resizeFactorEditText = binding.resizeFactorEditText

    init {
        resizeFactorEditText.setText(DEFAULT_RESIZE_FACTOR)
        progressView.apply {
            max = imagePaths.size
            setIndicatorColor(activity.getProperPrimaryColor())
        }

        activity.getAlertDialogBuilder()
            .setPositiveButton(com.goodwy.commons.R.string.ok, null)
            .setNegativeButton(com.goodwy.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.resize_multiple_images) { alertDialog ->
                    dialog = alertDialog
                    alertDialog.showKeyboard(resizeFactorEditText)

                    val positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    val negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    positiveButton.setOnClickListener {
                        val resizeFactorText = resizeFactorEditText.text?.toString()
                        if (resizeFactorText.isNullOrEmpty() || resizeFactorText.toInt() !in 10..90) {
                            activity.toast(R.string.resize_factor_error)
                            return@setOnClickListener
                        }

                        val resizeFactor = resizeFactorText.toFloat().div(100)

                        alertDialog.setCanceledOnTouchOutside(false)
                        arrayOf(binding.resizeFactorInputLayout, positiveButton, negativeButton).forEach {
                            it.isEnabled = false
                            it.alpha = 0.6f
                        }
                        resizeImages(resizeFactor)
                    }
                }
            }
    }

    private fun resizeImages(factor: Float) {
        progressView.show()
        with(activity) {
            val newSizes = imageSizes.map {
                val width = (it.x * factor).roundToInt()
                val height = (it.y * factor).roundToInt()
                Point(width, height)
            }

            val parentPath = imagePaths.first().getParentPath()
            val pathsToRescan = arrayListOf<String>()
            val pathLastModifiedMap = mutableMapOf<String, Long>()

            ensureWriteAccess(parentPath) {
                ensureBackgroundThread {
                    for (i in imagePaths.indices) {
                        val path = imagePaths[i]
                        val size = newSizes[i]
                        val lastModified = File(path).lastModified()

                        try {
                            resizeImage(path, path, size) {
                                if (it) {
                                    pathsToRescan.add(path)
                                    pathLastModifiedMap[path] = lastModified
                                    runOnUiThread {
                                        progressView.progress = i + 1
                                    }
                                }
                            }
                        } catch (e: OutOfMemoryError) {
                            toast(com.goodwy.commons.R.string.out_of_memory_error)
                        } catch (e: Exception) {
                            showErrorToast(e)
                        }
                    }

                    val failureCount = imagePaths.size - pathsToRescan.size
                    if (failureCount > 0) {
                        toast(resources.getQuantityString(R.plurals.failed_to_resize_images, failureCount, failureCount))
                    } else {
                        toast(R.string.images_resized_successfully)
                    }

                    rescanPathsAndUpdateLastModified(pathsToRescan, pathLastModifiedMap) {
                        activity.runOnUiThread {
                            dialog?.dismiss()
                            callback.invoke()
                        }
                    }
                }
            }
        }
    }
}
