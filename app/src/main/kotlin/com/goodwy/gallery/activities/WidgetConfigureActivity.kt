package com.goodwy.gallery.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.RemoteViews
import com.bumptech.glide.signature.ObjectKey
import com.goodwy.commons.dialogs.ColorPickerDialog
import com.goodwy.commons.extensions.*
import com.goodwy.commons.helpers.ensureBackgroundThread
import com.goodwy.gallery.R
import com.goodwy.gallery.dialogs.PickDirectoryDialog
import com.goodwy.gallery.extensions.*
import com.goodwy.gallery.helpers.MyWidgetProvider
import com.goodwy.gallery.helpers.ROUNDED_CORNERS_NONE
import com.goodwy.gallery.models.Directory
import com.goodwy.gallery.models.Widget
import kotlinx.android.synthetic.main.activity_widget_config.*

class WidgetConfigureActivity : SimpleActivity() {
    private var mBgAlpha = 0f
    private var mWidgetId = 0
    private var mBgColor = 0
    private var mBgColorWithoutTransparency = 0
    private var mTextColor = 0
    private var mFolderPath = ""
    private var mDirectories = ArrayList<Directory>()

    public override fun onCreate(savedInstanceState: Bundle?) {
        useDynamicTheme = false
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_widget_config)
        initVariables()

        mWidgetId = intent.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (mWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
        }

        config_save.setOnClickListener { saveConfig() }
        config_bg_color.setOnClickListener { pickBackgroundColor() }
        config_text_color.setOnClickListener { pickTextColor() }
        folder_picker_value.setOnClickListener { changeSelectedFolder() }
        //config_image_holder.setOnClickListener { changeSelectedFolder() }
        config_image.setOnClickListener { changeSelectedFolder() }
        config_folder_name.setOnClickListener { pickTextColor() }

        updateTextColors(folder_picker_holder)
        val primaryColor = getProperPrimaryColor()
        config_bg_seekbar.setColors(mTextColor, primaryColor, primaryColor)
        //folder_picker_holder.background = ColorDrawable(getProperBackgroundColor())
        folder_picker_holder.background.applyColorFilter(getProperBackgroundColor())

        folder_picker_show_folder_name.isChecked = config.showWidgetFolderName
        handleFolderNameDisplay()
        folder_picker_show_folder_name_holder.setOnClickListener {
            folder_picker_show_folder_name.toggle()
            handleFolderNameDisplay()
        }

        getCachedDirectories(false, false) {
            mDirectories = it
            val path = it.firstOrNull()?.path
            if (path != null) {
                updateFolderImage(path)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        setupToolbar(config_toolbar)
    }

    private fun initVariables() {
        mBgColor = config.widgetBgColor
        mBgAlpha = Color.alpha(mBgColor) / 255f

        mBgColorWithoutTransparency = Color.rgb(Color.red(mBgColor), Color.green(mBgColor), Color.blue(mBgColor))
        config_bg_seekbar.apply {
            progress = (mBgAlpha * 100).toInt()

            onSeekBarChangeListener {
                mBgAlpha = it / 100f
                updateBackgroundColor()
            }
        }
        updateBackgroundColor()

        mTextColor = config.widgetTextColor
        updateTextColor()
    }

    private fun saveConfig() {
        val views = RemoteViews(packageName, R.layout.widget)
        views.setBackgroundColor(R.id.widget_holder, mBgColor)
        AppWidgetManager.getInstance(this)?.updateAppWidget(mWidgetId, views) ?: return
        config.showWidgetFolderName = folder_picker_show_folder_name.isChecked
        val widget = Widget(null, mWidgetId, mFolderPath)
        ensureBackgroundThread {
            widgetsDB.insertOrUpdate(widget)
        }

        storeWidgetColors()
        requestWidgetUpdate()

        Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mWidgetId)
            setResult(Activity.RESULT_OK, this)
        }
        finish()
    }

    private fun storeWidgetColors() {
        config.apply {
            widgetBgColor = mBgColor
            widgetTextColor = mTextColor
        }
    }

    private fun requestWidgetUpdate() {
        Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE, null, this, MyWidgetProvider::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(mWidgetId))
            sendBroadcast(this)
        }
    }

    private fun updateBackgroundColor() {
        mBgColor = mBgColorWithoutTransparency.adjustAlpha(mBgAlpha)
        //config_image_holder.background.applyColorFilter(mBgColor)
        config_bg_color.setFillWithStroke(mBgColor, mBgColor)
        //config_save.backgroundTintList = ColorStateList.valueOf(getProperPrimaryColor())
    }

    private fun updateTextColor() {
        config_folder_name.setTextColor(mTextColor)
        config_text_color.setFillWithStroke(mTextColor, mTextColor)
        config_save.setTextColor(getProperPrimaryColor()) //getProperPrimaryColor().getContrastColor()
    }

    private fun pickBackgroundColor() {
        ColorPickerDialog(this, mBgColorWithoutTransparency) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                mBgColorWithoutTransparency = color
                updateBackgroundColor()
            }
        }
    }

    private fun pickTextColor() {
        ColorPickerDialog(this, mTextColor) { wasPositivePressed, color ->
            if (wasPositivePressed) {
                mTextColor = color
                updateTextColor()
            }
        }
    }

    private fun changeSelectedFolder() {
        PickDirectoryDialog(this, "", false, true, false, true) {
            updateFolderImage(it)
        }
    }

    private fun updateFolderImage(folderPath: String) {
        mFolderPath = folderPath
        runOnUiThread {
            folder_picker_value.text = getFolderNameFromPath(folderPath)
            config_folder_name.text = getFolderNameFromPath(folderPath)
        }

        ensureBackgroundThread {
            val path = directoryDB.getDirectoryThumbnail(folderPath)
            if (path != null) {
                runOnUiThread {
                    val signature = ObjectKey(System.currentTimeMillis().toString())
                    //loadJpg(path, config_image, config.cropThumbnails, ROUNDED_CORNERS_NONE, signature)
                    val radius = resources.getDimensionPixelSize(R.dimen.dialog_corner_radius)
                    loadJpg(path, config_image, config.cropThumbnails, radius, signature)
                }
            }
        }
    }

    private fun handleFolderNameDisplay() {
        val showFolderName = folder_picker_show_folder_name.isChecked
        config_folder_name.beVisibleIf(showFolderName)
    }
}
