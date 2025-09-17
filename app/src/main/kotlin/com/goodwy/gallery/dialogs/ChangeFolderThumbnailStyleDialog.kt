package com.goodwy.gallery.dialogs

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.util.TypedValue
import android.widget.RelativeLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.dialogs.RadioGroupDialog
import com.goodwy.commons.extensions.*
import com.goodwy.commons.helpers.FONT_SIZE_EXTRA_LARGE
import com.goodwy.commons.helpers.FONT_SIZE_LARGE
import com.goodwy.commons.helpers.FONT_SIZE_MEDIUM
import com.goodwy.commons.helpers.FONT_SIZE_SMALL
import com.goodwy.commons.models.RadioItem
import com.goodwy.gallery.R
import com.goodwy.gallery.adapters.toItemBinding
import com.goodwy.gallery.databinding.DialogChangeFolderThumbnailStyleBinding
import com.goodwy.gallery.databinding.DirectoryItemGridRoundedCornersBinding
import com.goodwy.gallery.databinding.DirectoryItemGridSquareBinding
import com.goodwy.gallery.extensions.config
import com.goodwy.gallery.extensions.getTextSizeDir
import com.goodwy.gallery.helpers.*

class ChangeFolderThumbnailStyleDialog(val activity: BaseSimpleActivity, val callback: () -> Unit) : DialogInterface.OnClickListener {
    private var config = activity.config
    private val binding = DialogChangeFolderThumbnailStyleBinding.inflate(activity.layoutInflater).apply {
        dialogFolderLimitTitle.isChecked = config.limitFolderTitle
    }
    private var fontSizeDir = config.fontSizeDir

    init {
        activity.getAlertDialogBuilder()
            .setPositiveButton(com.goodwy.commons.R.string.ok, this)
            .setNegativeButton(com.goodwy.commons.R.string.cancel, null)
            .apply {
                activity.setupDialogStuff(binding.root, this, R.string.folder_thumbnail_style) {
                    setupStyle()
                    setupFontSize()
                    setupMediaCount()
                    updateSample()
                }
            }
    }

    private fun setupStyle() {
        val styleRadio = binding.dialogRadioFolderStyle
        styleRadio.setOnCheckedChangeListener { group, checkedId ->
            updateSample()
        }

        val styleBtn = when (config.folderStyle) {
            FOLDER_STYLE_SQUARE -> binding.dialogRadioFolderSquare
            else -> binding.dialogRadioFolderRoundedCorners
        }

        styleBtn.isChecked = true
    }

    private fun setupFontSize() = binding.apply {
        dialogRadioFolderFontSize.text = activity.getFontSizeText()
        dialogRadioFolderFontSizeHolder.setOnClickListener {
            val items = arrayListOf(
                RadioItem(FONT_SIZE_SMALL, activity.getString(com.goodwy.commons.R.string.small)),
                RadioItem(FONT_SIZE_MEDIUM, activity.getString(com.goodwy.commons.R.string.medium)),
                RadioItem(FONT_SIZE_LARGE, activity.getString(com.goodwy.commons.R.string.large)),
                RadioItem(FONT_SIZE_EXTRA_LARGE, activity.getString(com.goodwy.commons.R.string.extra_large)))

            RadioGroupDialog(activity, items, fontSizeDir, com.goodwy.commons.R.string.font_size) {
                fontSizeDir = it as Int
                dialogRadioFolderFontSize.text = activity.getFontSizeText()
                updateSample()
            }
        }
    }

    private fun Context.getFontSizeText() = getString(
        when (fontSizeDir) {
            FONT_SIZE_SMALL -> com.goodwy.commons.R.string.small
            FONT_SIZE_MEDIUM -> com.goodwy.commons.R.string.medium
            FONT_SIZE_LARGE -> com.goodwy.commons.R.string.large
            else -> com.goodwy.commons.R.string.extra_large
        }
    )

    private fun setupMediaCount() {
        val countRadio = binding.dialogRadioFolderCountHolder
        countRadio.setOnCheckedChangeListener { group, checkedId ->
            updateSample()
        }

        val countBtn = when (config.showFolderMediaCount) {
            FOLDER_MEDIA_CNT_LINE -> binding.dialogRadioFolderCountLine
            FOLDER_MEDIA_CNT_BRACKETS -> binding.dialogRadioFolderCountBrackets
            else -> binding.dialogRadioFolderCountNone
        }

        countBtn.isChecked = true
    }

    @SuppressLint("SetTextI18n")
    private fun updateSample() {
        val photoCount = 18
        val folderName = "Camera"
        binding.apply {
            val useRoundedCornersLayout = binding.dialogRadioFolderStyle.checkedRadioButtonId == R.id.dialog_radio_folder_rounded_corners
            binding.dialogFolderSampleHolder.removeAllViews()

            val sampleBinding = if (useRoundedCornersLayout) {
                DirectoryItemGridRoundedCornersBinding.inflate(activity.layoutInflater).toItemBinding()
            } else {
                DirectoryItemGridSquareBinding.inflate(activity.layoutInflater).toItemBinding()
            }
            val sampleView = sampleBinding.root
            binding.dialogFolderSampleHolder.addView(sampleView)

            sampleView.layoutParams.width = activity.resources.getDimension(R.dimen.sample_thumbnail_size).toInt()
            (sampleView.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.CENTER_HORIZONTAL)

            when (binding.dialogRadioFolderCountHolder.checkedRadioButtonId) {
                R.id.dialog_radio_folder_count_line -> {
                    sampleBinding.dirName.text = folderName
                    sampleBinding.photoCnt.text = activity.resources.getQuantityString(com.goodwy.commons.R.plurals.items, photoCount, photoCount)//photoCount.toString()
                    sampleBinding.photoCnt.beVisible()
                }

                R.id.dialog_radio_folder_count_brackets -> {
                    sampleBinding.photoCnt.beGone()
                    sampleBinding.dirName.text = "$folderName ($photoCount)"
                }

                else -> {
                    sampleBinding.dirName.text = folderName
                    sampleBinding.photoCnt.beGone()
                }
            }

            val options = RequestOptions().centerCrop()
            var builder = Glide.with(activity)
                .load(R.drawable.sample_logo)
                .apply(options)

            if (useRoundedCornersLayout) {
                val cornerRadius = root.resources.getDimension(com.goodwy.commons.R.dimen.rounded_corner_radius_big).toInt()
                builder = builder.transform(CenterCrop(), RoundedCorners(cornerRadius))
                sampleBinding.dirName.setTextColor(activity.getProperTextColor())
                sampleBinding.photoCnt.setTextColor(activity.getProperTextColor())
            }
            val fontSize: Float = activity.getTextSizeDir(fontSizeDir)
            sampleBinding.dirName.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)
            sampleBinding.photoCnt.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontSize)

            builder.into(sampleBinding.dirThumbnail)
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val style = when (binding.dialogRadioFolderStyle.checkedRadioButtonId) {
            R.id.dialog_radio_folder_square -> FOLDER_STYLE_SQUARE
            else -> FOLDER_STYLE_ROUNDED_CORNERS
        }

        val count = when (binding.dialogRadioFolderCountHolder.checkedRadioButtonId) {
            R.id.dialog_radio_folder_count_line -> FOLDER_MEDIA_CNT_LINE
            R.id.dialog_radio_folder_count_brackets -> FOLDER_MEDIA_CNT_BRACKETS
            else -> FOLDER_MEDIA_CNT_NONE
        }

        config.folderStyle = style
        config.showFolderMediaCount = count
        config.limitFolderTitle = binding.dialogFolderLimitTitle.isChecked
        if (config.fontSizeDir != fontSizeDir) {
            config.tabsChanged = true
            config.fontSizeDir = fontSizeDir
        }
        callback()
    }
}
