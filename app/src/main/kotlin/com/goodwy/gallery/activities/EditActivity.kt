package com.goodwy.gallery.activities

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.exifinterface.media.ExifInterface
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.goodwy.commons.dialogs.ColorPickerDialog
import com.goodwy.commons.dialogs.ConfirmationDialog
import com.goodwy.commons.extensions.*
import com.goodwy.commons.helpers.*
import com.goodwy.commons.models.FileDirItem
import com.goodwy.gallery.BuildConfig
import com.goodwy.gallery.R
import com.goodwy.gallery.adapters.FiltersAdapter
import com.goodwy.gallery.dialogs.OtherAspectRatioDialog
import com.goodwy.gallery.dialogs.ResizeDialog
import com.goodwy.gallery.dialogs.SaveAsDialog
import com.goodwy.gallery.extensions.config
import com.goodwy.gallery.extensions.copyNonDimensionAttributesTo
import com.goodwy.gallery.extensions.fixDateTaken
import com.goodwy.gallery.extensions.openEditor
import com.goodwy.gallery.helpers.*
import com.goodwy.gallery.interfaces.CanvasListener
import com.goodwy.gallery.models.FilterItem
import com.theartofdev.edmodo.cropper.CropImageView
import com.zomato.photofilters.FilterPack
import com.zomato.photofilters.imageprocessors.Filter
import kotlinx.android.synthetic.main.activity_edit.*
import kotlinx.android.synthetic.main.bottom_actions_aspect_ratio.*
import kotlinx.android.synthetic.main.bottom_editor_actions_filter.*
import kotlinx.android.synthetic.main.bottom_editor_crop_rotate_actions.*
import kotlinx.android.synthetic.main.bottom_editor_draw_actions.*
import kotlinx.android.synthetic.main.bottom_editor_primary_actions.*
import java.io.*

class EditActivity : SimpleActivity(), CropImageView.OnCropImageCompleteListener, CanvasListener {
    companion object {
        init {
            System.loadLibrary("NativeImageProcessor")
        }
    }

    private val TEMP_FOLDER_NAME = "images"
    private val ASPECT_X = "aspectX"
    private val ASPECT_Y = "aspectY"
    private val CROP = "crop"

    // constants for bottom primary action groups
    private val PRIMARY_ACTION_NONE = 0
    private val PRIMARY_ACTION_FILTER = 1
    private val PRIMARY_ACTION_CROP_ROTATE = 2
    private val PRIMARY_ACTION_DRAW = 3

    private val CROP_ROTATE_NONE = 0
    private val CROP_ROTATE_ASPECT_RATIO = 1

    private lateinit var eyeDropper: EyeDropper
    private lateinit var saveUri: Uri
    private var uri: Uri? = null
    private var resizeWidth = 0
    private var resizeHeight = 0
    private var drawColor = 0
    private var lastOtherAspectRatio: Pair<Float, Float>? = null
    private var currPrimaryAction = PRIMARY_ACTION_NONE
    private var currCropRotateAction = CROP_ROTATE_ASPECT_RATIO
    private var currAspectRatio = ASPECT_RATIO_FREE
    private var isCropIntent = false
    private var isEditingWithThirdParty = false
    private var isSharingBitmap = false
    private var wasDrawCanvasPositioned = false
    private var oldExif: ExifInterface? = null
    private var filterInitialBitmap: Bitmap? = null
    private var originalUri: Uri? = null
    private var isEraserOn = false
    private var isEyeDropperOn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit)

        editor_coordinator.background = ColorDrawable(Color.BLACK) //TODO always black background
        editor_draw_canvas.mListener = this
        //editor_draw_canvas.updateBackgroundColor(resources.getColor(R.color.theme_black_background_color)) //TODO For Eraser

        eyeDropper = EyeDropper(editor_draw_canvas) { selectedColor ->
            setColor(selectedColor)
        }
        if (checkAppSideloading()) {
            return
        }

        setupOptionsMenu()
        refreshMenuItems()
        handlePermission(getPermissionToRequest()) {
            if (!it) {
                toast(R.string.no_storage_permissions)
                finish()
            }
            initEditActivity()
        }
    }

    override fun onResume() {
        super.onResume()
        isEditingWithThirdParty = false
        bottom_draw_width.setColors(getProperTextColor(), getProperPrimaryColor(), resources.getColor(R.color.white)) //bottom_draw_width.setColors(getProperTextColor(), getProperPrimaryColor(), getProperBackgroundColor())
        //bottom_draw_holder.backgroundTintList = getBottomNavigationBackgroundColor().getColorStateList()

        setupToolbar(editor_toolbar, NavigationIcon.Arrow, Color.BLACK)
        updateNavigationBarColor(Color.BLACK)
        updateStatusbarColor(Color.BLACK)
    }

    override fun onStop() {
        super.onStop()
        if (isEditingWithThirdParty) {
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        editor_draw_canvas.mListener = null
    }

    private fun setupOptionsMenu() {
        editor_toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.undo -> editor_draw_canvas.undo()
                R.id.redo -> editor_draw_canvas.redo()
                R.id.save_as -> saveImage()
                R.id.edit -> editWith()
                R.id.share -> shareImage()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun refreshMenuItems() {
        editor_toolbar.menu.apply {
            findItem(R.id.undo).isVisible = editor_draw_canvas.isVisible()
            findItem(R.id.redo).isVisible = editor_draw_canvas.isVisible()
        }
    }

    private fun initEditActivity() {
        if (intent.data == null) {
            toast(R.string.invalid_image_path)
            finish()
            return
        }

        uri = intent.data!!
        originalUri = uri
        if (uri!!.scheme != "file" && uri!!.scheme != "content") {
            toast(R.string.unknown_file_location)
            finish()
            return
        }

        if (intent.extras?.containsKey(REAL_FILE_PATH) == true) {
            val realPath = intent.extras!!.getString(REAL_FILE_PATH)
            uri = when {
                isPathOnOTG(realPath!!) -> uri
                realPath.startsWith("file:/") -> Uri.parse(realPath)
                else -> Uri.fromFile(File(realPath))
            }
        } else {
            (getRealPathFromURI(uri!!))?.apply {
                uri = Uri.fromFile(File(this))
            }
        }

        saveUri = when {
            intent.extras?.containsKey(MediaStore.EXTRA_OUTPUT) == true && intent.extras!!.get(MediaStore.EXTRA_OUTPUT) is Uri -> intent.extras!!.get(MediaStore.EXTRA_OUTPUT) as Uri
            else -> uri!!
        }

        isCropIntent = intent.extras?.get(CROP) == "true"
        if (isCropIntent) {
            bottom_editor_primary_actions.beGone()
            (bottom_editor_crop_rotate_actions.layoutParams as RelativeLayout.LayoutParams).addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1)
        }

        loadDefaultImageView()
        setupBottomActions()

        if (config.lastEditorCropAspectRatio == ASPECT_RATIO_OTHER) {
            if (config.lastEditorCropOtherAspectRatioX == 0f) {
                config.lastEditorCropOtherAspectRatioX = 1f
            }

            if (config.lastEditorCropOtherAspectRatioY == 0f) {
                config.lastEditorCropOtherAspectRatioY = 1f
            }

            lastOtherAspectRatio = Pair(config.lastEditorCropOtherAspectRatioX, config.lastEditorCropOtherAspectRatioY)
        }
        updateAspectRatio(config.lastEditorCropAspectRatio)
        crop_image_view.guidelines = CropImageView.Guidelines.ON
        bottom_aspect_ratios.beVisible()
    }

    private fun loadDefaultImageView() {
        default_image_view.beVisible()
        crop_image_view.beGone()
        editor_draw_canvas.beGone()
        refreshMenuItems()

        val options = RequestOptions()
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)

        Glide.with(this)
            .asBitmap()
            .load(uri)
            .apply(options)
            .listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                    if (uri != originalUri) {
                        uri = originalUri
                        Handler().post {
                            loadDefaultImageView()
                        }
                    }
                    return false
                }

                override fun onResourceReady(
                    bitmap: Bitmap?,
                    model: Any?,
                    target: Target<Bitmap>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    val currentFilter = getFiltersAdapter()?.getCurrentFilter()
                    if (filterInitialBitmap == null) {
                        bottomFilterClicked() // Default open
                        //loadCropImageView()
                        //bottomCropRotateClicked()
                    }

                    if (filterInitialBitmap != null && currentFilter != null && currentFilter.filter.name != getString(R.string.none)) {
                        default_image_view.onGlobalLayout {
                            applyFilter(currentFilter)
                        }
                    } else {
                        filterInitialBitmap = bitmap
                    }

                    if (isCropIntent) {
                        bottom_primary_crop_rotate.beGone()
                        bottom_primary_draw.beGone()
                    }

                    return false
                }
            }).into(default_image_view)
    }

    private fun loadCropImageView() {
        default_image_view.beGone()
        editor_draw_canvas.beGone()
        refreshMenuItems()
        crop_image_view.apply {
            beVisible()
            setOnCropImageCompleteListener(this@EditActivity)
            setImageUriAsync(uri)
            guidelines = CropImageView.Guidelines.ON

            if (isCropIntent && shouldCropSquare()) {
                currAspectRatio = ASPECT_RATIO_ONE_ONE
                setFixedAspectRatio(true)
                bottom_aspect_ratio.beGone()
            }
        }
    }

    private fun loadDrawCanvas() {
        default_image_view.beGone()
        crop_image_view.beGone()
        editor_draw_canvas.beVisible()
        refreshMenuItems()

        if (!wasDrawCanvasPositioned) {
            wasDrawCanvasPositioned = true
            editor_draw_canvas.onGlobalLayout {
                ensureBackgroundThread {
                    fillCanvasBackground()
                }
            }
        }
    }

    private fun fillCanvasBackground() {
        val size = Point()
        windowManager.defaultDisplay.getSize(size)
        val options = RequestOptions()
            .format(DecodeFormat.PREFER_ARGB_8888)
            .skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .fitCenter()

        try {
            val builder = Glide.with(applicationContext)
                .asBitmap()
                .load(uri)
                .apply(options)
                .into(editor_draw_canvas.width, editor_draw_canvas.height)

            val bitmap = builder.get()
            runOnUiThread {
                editor_draw_canvas.apply {
                    updateBackgroundBitmap(bitmap)
                    layoutParams.width = bitmap.width
                    layoutParams.height = bitmap.height
                    y = (height - bitmap.height) / 2f
                    requestLayout()
                }
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun saveImage() {
        setOldExif()

        if (crop_image_view.isVisible()) {
            crop_image_view.getCroppedImageAsync()
        } else if (editor_draw_canvas.isVisible()) {
            val bitmap = editor_draw_canvas.getBitmap()
            if (saveUri.scheme == "file") {
                SaveAsDialog(this, saveUri.path!!, true) {
                    saveBitmapToFile(bitmap, it, true)
                }
            } else if (saveUri.scheme == "content") {
                val filePathGetter = getNewFilePath()
                SaveAsDialog(this, filePathGetter.first, filePathGetter.second) {
                    saveBitmapToFile(bitmap, it, true)
                }
            }
        } else {
            val currentFilter = getFiltersAdapter()?.getCurrentFilter() ?: return
            val filePathGetter = getNewFilePath()
            SaveAsDialog(this, filePathGetter.first, filePathGetter.second) {
                toast(R.string.saving)

                // clean up everything to free as much memory as possible
                default_image_view.setImageResource(0)
                crop_image_view.setImageBitmap(null)
                bottom_actions_filter_list.adapter = null
                bottom_actions_filter_list.beGone()

                ensureBackgroundThread {
                    try {
                        val originalBitmap = Glide.with(applicationContext).asBitmap().load(uri).submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get()
                        currentFilter.filter.processFilter(originalBitmap)
                        saveBitmapToFile(originalBitmap, it, false)
                    } catch (e: OutOfMemoryError) {
                        toast(R.string.out_of_memory_error)
                    }
                }
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun setOldExif() {
        var inputStream: InputStream? = null
        try {
            if (isNougatPlus()) {
                inputStream = contentResolver.openInputStream(uri!!)
                oldExif = ExifInterface(inputStream!!)
            }
        } catch (e: Exception) {
        } finally {
            inputStream?.close()
        }
    }

    private fun shareImage() {
        ensureBackgroundThread {
            when {
                default_image_view.isVisible() -> {
                    val currentFilter = getFiltersAdapter()?.getCurrentFilter()
                    if (currentFilter == null) {
                        toast(R.string.unknown_error_occurred)
                        return@ensureBackgroundThread
                    }

                    val originalBitmap = Glide.with(applicationContext).asBitmap().load(uri).submit(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL).get()
                    currentFilter.filter.processFilter(originalBitmap)
                    shareBitmap(originalBitmap)
                }
                crop_image_view.isVisible() -> {
                    isSharingBitmap = true
                    runOnUiThread {
                        crop_image_view.getCroppedImageAsync()
                    }
                }
                editor_draw_canvas.isVisible() -> shareBitmap(editor_draw_canvas.getBitmap())
            }
        }
    }

    private fun getTempImagePath(bitmap: Bitmap, callback: (path: String?) -> Unit) {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.PNG, 0, bytes)

        val folder = File(cacheDir, TEMP_FOLDER_NAME)
        if (!folder.exists()) {
            if (!folder.mkdir()) {
                callback(null)
                return
            }
        }

        val filename = applicationContext.getFilenameFromContentUri(saveUri) ?: "tmp.jpg"
        val newPath = "$folder/$filename"
        val fileDirItem = FileDirItem(newPath, filename)
        getFileOutputStream(fileDirItem, true) {
            if (it != null) {
                try {
                    it.write(bytes.toByteArray())
                    callback(newPath)
                } catch (e: Exception) {
                } finally {
                    it.close()
                }
            } else {
                callback("")
            }
        }
    }

    private fun shareBitmap(bitmap: Bitmap) {
        getTempImagePath(bitmap) {
            if (it != null) {
                sharePathIntent(it, BuildConfig.APPLICATION_ID)
            } else {
                toast(R.string.unknown_error_occurred)
            }
        }
    }

    private fun getFiltersAdapter() = bottom_actions_filter_list.adapter as? FiltersAdapter

    private fun setupBottomActions() {
        setupPrimaryActionButtons()
        setupCropRotateActionButtons()
        setupAspectRatioButtons()
        setupDrawButtons()
    }

    private fun setupPrimaryActionButtons() {
        bottom_primary_filter.setOnClickListener {
            bottomFilterClicked()
        }

        bottom_primary_crop_rotate.setOnClickListener {
            bottomCropRotateClicked()
        }

        bottom_primary_draw.setOnClickListener {
            bottomDrawClicked()
        }

        bottom_primary_cancel.setOnClickListener {
            finish()
        }

        bottom_primary_save.setOnClickListener {
            saveImage()
        }
    }

    private fun bottomFilterClicked() {
        currPrimaryAction = if (currPrimaryAction == PRIMARY_ACTION_FILTER) {
            PRIMARY_ACTION_NONE
        } else {
            PRIMARY_ACTION_FILTER
        }
        updatePrimaryActionButtons()
    }

    private fun bottomCropRotateClicked() {
        currPrimaryAction = if (currPrimaryAction == PRIMARY_ACTION_CROP_ROTATE) {
            PRIMARY_ACTION_NONE
        } else {
            PRIMARY_ACTION_CROP_ROTATE
        }
        updatePrimaryActionButtons()
    }

    private fun bottomDrawClicked() {
        currPrimaryAction = if (currPrimaryAction == PRIMARY_ACTION_DRAW) {
            PRIMARY_ACTION_NONE
        } else {
            PRIMARY_ACTION_DRAW
        }
        updatePrimaryActionButtons()
    }

    private fun setupCropRotateActionButtons() {
        bottom_rotate.setOnClickListener {
            crop_image_view.rotateImage(90)
        }

        bottom_resize.beGoneIf(isCropIntent)
        bottom_resize.setOnClickListener {
            resizeImage()
        }

        bottom_flip_horizontally.setOnClickListener {
            crop_image_view.flipImageHorizontally()
        }

        bottom_flip_vertically.setOnClickListener {
            crop_image_view.flipImageVertically()
        }

        bottom_aspect_ratio.setOnClickListener {
            currCropRotateAction = if (currCropRotateAction == CROP_ROTATE_ASPECT_RATIO) {
                crop_image_view.guidelines = CropImageView.Guidelines.OFF
                bottom_aspect_ratios.beGone()
                CROP_ROTATE_NONE
            } else {
                crop_image_view.guidelines = CropImageView.Guidelines.ON
                bottom_aspect_ratios.beVisible()
                CROP_ROTATE_ASPECT_RATIO
            }
            updateCropRotateActionButtons()
        }
    }

    private fun setupAspectRatioButtons() {
        bottom_aspect_ratio_free.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_FREE)
        }

        bottom_aspect_ratio_one_one.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_ONE_ONE)
        }

        bottom_aspect_ratio_four_three.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_FOUR_THREE)
        }

        bottom_aspect_ratio_sixteen_nine.setOnClickListener {
            updateAspectRatio(ASPECT_RATIO_SIXTEEN_NINE)
        }

        bottom_aspect_ratio_other.setOnClickListener {
            OtherAspectRatioDialog(this, lastOtherAspectRatio) {
                lastOtherAspectRatio = it
                config.lastEditorCropOtherAspectRatioX = it.first
                config.lastEditorCropOtherAspectRatioY = it.second
                updateAspectRatio(ASPECT_RATIO_OTHER)
            }
        }

        updateAspectRatioButtons()
    }

    private fun setupDrawButtons() {
        updateDrawColor(config.lastEditorDrawColor)
        bottom_draw_width.progress = config.lastEditorBrushSize
        updateBrushSize(config.lastEditorBrushSize)

        bottom_draw_color_clickable.setOnClickListener {
            ColorPickerDialog(this, drawColor) { wasPositivePressed, color ->
                if (wasPositivePressed) {
                    updateDrawColor(color)
                }
            }
        }

        bottom_draw_width.onSeekBarChangeListener {
            config.lastEditorBrushSize = it
            updateBrushSize(it)
        }

        bottom_draw_undo.setOnClickListener {
            editor_draw_canvas.undo()
        }

        bottom_draw_redo.setOnClickListener {
            editor_draw_canvas.redo()
        }

        bottom_draw_eraser.setOnClickListener { eraserClicked() }
        bottom_draw_eraser.setOnLongClickListener {
            toast(R.string.eraser)
            true
        }

        bottom_draw_eye_dropper.setOnClickListener { eyeDropperClicked() }
        bottom_draw_eye_dropper.setOnLongClickListener {
            toast(R.string.eyedropper)
            true
        }
    }

    private fun updateBrushSize(percent: Int) {
        editor_draw_canvas.updateBrushSize(percent)
        val scale = Math.max(0.03f, percent / 100f)
        bottom_draw_color.scaleX = scale
        bottom_draw_color.scaleY = scale
    }

    private fun updatePrimaryActionButtons() {
        if (crop_image_view.isGone() && currPrimaryAction == PRIMARY_ACTION_CROP_ROTATE) {
            loadCropImageView()
        } else if (default_image_view.isGone() && currPrimaryAction == PRIMARY_ACTION_FILTER) {
            loadDefaultImageView()
        } else if (editor_draw_canvas.isGone() && currPrimaryAction == PRIMARY_ACTION_DRAW) {
            loadDrawCanvas()
        }

        arrayOf(bottom_primary_filter, bottom_primary_crop_rotate, bottom_primary_draw).forEach {
            it.applyColorFilter(Color.WHITE)
        }

        val currentPrimaryActionButton = when (currPrimaryAction) {
            PRIMARY_ACTION_FILTER -> bottom_primary_filter
            PRIMARY_ACTION_CROP_ROTATE -> bottom_primary_crop_rotate
            PRIMARY_ACTION_DRAW -> bottom_primary_draw
            else -> null
        }

        currentPrimaryActionButton?.applyColorFilter(getProperPrimaryColor())
        bottom_editor_filter_actions.beVisibleIf(currPrimaryAction == PRIMARY_ACTION_FILTER)
        bottom_editor_crop_rotate_actions.beVisibleIf(currPrimaryAction == PRIMARY_ACTION_CROP_ROTATE)
        bottom_editor_draw_actions.beVisibleIf(currPrimaryAction == PRIMARY_ACTION_DRAW)

        if (currPrimaryAction == PRIMARY_ACTION_FILTER && bottom_actions_filter_list.adapter == null) {
            ensureBackgroundThread {
                val thumbnailSize = resources.getDimension(R.dimen.bottom_filters_thumbnail_size).toInt()

                val bitmap = try {
                    Glide.with(this)
                        .asBitmap()
                        .load(uri).listener(object : RequestListener<Bitmap> {
                            override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, isFirstResource: Boolean): Boolean {
                                showErrorToast(e.toString())
                                return false
                            }

                            override fun onResourceReady(
                                resource: Bitmap?,
                                model: Any?,
                                target: Target<Bitmap>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ) = false
                        })
                        .submit(thumbnailSize, thumbnailSize)
                        .get()
                } catch (e: GlideException) {
                    showErrorToast(e)
                    finish()
                    return@ensureBackgroundThread
                }

                runOnUiThread {
                    val filterThumbnailsManager = FilterThumbnailsManager()
                    filterThumbnailsManager.clearThumbs()

                    val noFilter = Filter(getString(R.string.none))
                    filterThumbnailsManager.addThumb(FilterItem(bitmap, noFilter))

                    FilterPack.getFilterPack(this).forEach {
                        val filterItem = FilterItem(bitmap, it)
                        filterThumbnailsManager.addThumb(filterItem)
                    }

                    val filterItems = filterThumbnailsManager.processThumbs()
                    val adapter = FiltersAdapter(applicationContext, filterItems) {
                        val layoutManager = bottom_actions_filter_list.layoutManager as LinearLayoutManager
                        applyFilter(filterItems[it])

                        if (it == layoutManager.findLastCompletelyVisibleItemPosition() || it == layoutManager.findLastVisibleItemPosition()) {
                            bottom_actions_filter_list.smoothScrollBy(thumbnailSize, 0)
                        } else if (it == layoutManager.findFirstCompletelyVisibleItemPosition() || it == layoutManager.findFirstVisibleItemPosition()) {
                            bottom_actions_filter_list.smoothScrollBy(-thumbnailSize, 0)
                        }
                    }

                    bottom_actions_filter_list.adapter = adapter
                    adapter.notifyDataSetChanged()
                }
            }
        }

        if (currPrimaryAction != PRIMARY_ACTION_CROP_ROTATE) {
            bottom_aspect_ratios.beGone()
            currCropRotateAction = CROP_ROTATE_NONE
        }
        updateCropRotateActionButtons()
    }

    private fun applyFilter(filterItem: FilterItem) {
        val newBitmap = Bitmap.createBitmap(filterInitialBitmap!!)
        default_image_view.setImageBitmap(filterItem.filter.processFilter(newBitmap))
    }

    private fun updateAspectRatio(aspectRatio: Int) {
        currAspectRatio = aspectRatio
        config.lastEditorCropAspectRatio = aspectRatio
        updateAspectRatioButtons()

        crop_image_view.apply {
            if (aspectRatio == ASPECT_RATIO_FREE) {
                setFixedAspectRatio(false)
            } else {
                val newAspectRatio = when (aspectRatio) {
                    ASPECT_RATIO_ONE_ONE -> Pair(1f, 1f)
                    ASPECT_RATIO_FOUR_THREE -> Pair(4f, 3f)
                    ASPECT_RATIO_SIXTEEN_NINE -> Pair(16f, 9f)
                    else -> Pair(lastOtherAspectRatio!!.first, lastOtherAspectRatio!!.second)
                }

                setAspectRatio(newAspectRatio.first.toInt(), newAspectRatio.second.toInt())
            }
        }
    }

    @SuppressLint("UseCompatTextViewDrawableApis")
    private fun updateAspectRatioButtons() {
        arrayOf(
            bottom_aspect_ratio_free,
            bottom_aspect_ratio_one_one,
            bottom_aspect_ratio_four_three,
            bottom_aspect_ratio_sixteen_nine,
            bottom_aspect_ratio_other
        ).forEach {
            it.setTextColor(Color.WHITE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                it.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }

        val currentAspectRatioButton = when (currAspectRatio) {
            ASPECT_RATIO_FREE -> bottom_aspect_ratio_free
            ASPECT_RATIO_ONE_ONE -> bottom_aspect_ratio_one_one
            ASPECT_RATIO_FOUR_THREE -> bottom_aspect_ratio_four_three
            ASPECT_RATIO_SIXTEEN_NINE -> bottom_aspect_ratio_sixteen_nine
            else -> bottom_aspect_ratio_other
        }

        currentAspectRatioButton.setTextColor(getProperPrimaryColor())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            currentAspectRatioButton.compoundDrawableTintList = ColorStateList.valueOf(getProperPrimaryColor())
        }
    }

    private fun updateCropRotateActionButtons() {
        arrayOf(bottom_aspect_ratio).forEach {
            it.applyColorFilter(Color.WHITE)
        }

        val primaryActionView = when (currCropRotateAction) {
            CROP_ROTATE_ASPECT_RATIO -> bottom_aspect_ratio
            else -> null
        }

        primaryActionView?.applyColorFilter(getProperPrimaryColor())
    }

    private fun updateDrawColor(color: Int) {
        drawColor = color
        //bottom_draw_color.applyColorFilter(color)
        //bottom_draw_color_icon.applyColorFilter(color)
        getBrushPreviewView().setColor(drawColor)
        getBrushIconPreviewView().setColor(drawColor)
        config.lastEditorDrawColor = color
        editor_draw_canvas.updateColor(color)
    }

    private fun resizeImage() {
        val point = getAreaSize()
        if (point == null) {
            toast(R.string.unknown_error_occurred)
            return
        }

        ResizeDialog(this, point) {
            resizeWidth = it.x
            resizeHeight = it.y
            crop_image_view.getCroppedImageAsync()
        }
    }

    private fun shouldCropSquare(): Boolean {
        val extras = intent.extras
        return if (extras != null && extras.containsKey(ASPECT_X) && extras.containsKey(ASPECT_Y)) {
            extras.getInt(ASPECT_X) == extras.getInt(ASPECT_Y)
        } else {
            false
        }
    }

    private fun getAreaSize(): Point? {
        val rect = crop_image_view.cropRect ?: return null
        val rotation = crop_image_view.rotatedDegrees
        return if (rotation == 0 || rotation == 180) {
            Point(rect.width(), rect.height())
        } else {
            Point(rect.height(), rect.width())
        }
    }

    override fun onCropImageComplete(view: CropImageView, result: CropImageView.CropResult) {
        if (result.error == null) {
            setOldExif()

            val bitmap = result.bitmap
            if (isSharingBitmap) {
                isSharingBitmap = false
                shareBitmap(bitmap)
                return
            }

            if (isCropIntent) {
                if (saveUri.scheme == "file") {
                    saveBitmapToFile(bitmap, saveUri.path!!, true)
                } else {
                    var inputStream: InputStream? = null
                    var outputStream: OutputStream? = null
                    try {
                        val stream = ByteArrayOutputStream()
                        bitmap.compress(CompressFormat.JPEG, 100, stream)
                        inputStream = ByteArrayInputStream(stream.toByteArray())
                        outputStream = contentResolver.openOutputStream(saveUri)
                        inputStream.copyTo(outputStream!!)
                    } catch (e: Exception) {
                        showErrorToast(e)
                        return
                    } finally {
                        inputStream?.close()
                        outputStream?.close()
                    }

                    Intent().apply {
                        data = saveUri
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        setResult(RESULT_OK, this)
                    }
                    finish()
                }
            } else if (saveUri.scheme == "file") {
                SaveAsDialog(this, saveUri.path!!, true) {
                    saveBitmapToFile(bitmap, it, true)
                }
            } else if (saveUri.scheme == "content") {
                val filePathGetter = getNewFilePath()
                SaveAsDialog(this, filePathGetter.first, filePathGetter.second) {
                    saveBitmapToFile(bitmap, it, true)
                }
            } else {
                toast(R.string.unknown_file_location)
            }
        } else {
            toast("${getString(R.string.image_editing_failed)}: ${result.error.message}")
        }
    }

    private fun getNewFilePath(): Pair<String, Boolean> {
        var newPath = applicationContext.getRealPathFromURI(saveUri) ?: ""
        if (newPath.startsWith("/mnt/")) {
            newPath = ""
        }

        var shouldAppendFilename = true
        if (newPath.isEmpty()) {
            val filename = applicationContext.getFilenameFromContentUri(saveUri) ?: ""
            if (filename.isNotEmpty()) {
                val path =
                    if (intent.extras?.containsKey(REAL_FILE_PATH) == true) intent.getStringExtra(REAL_FILE_PATH)?.getParentPath() else internalStoragePath
                newPath = "$path/$filename"
                shouldAppendFilename = false
            }
        }

        if (newPath.isEmpty()) {
            newPath = "$internalStoragePath/${getCurrentFormattedDateTime()}.${saveUri.toString().getFilenameExtension()}"
            shouldAppendFilename = false
        }

        return Pair(newPath, shouldAppendFilename)
    }

    private fun saveBitmapToFile(bitmap: Bitmap, path: String, showSavingToast: Boolean) {
        if (!packageName.contains("ywdoog".reversed(), true)) {
            if (baseConfig.appRunCount > 100) {
                val label =
                    "sknahT .moc.elgoog.yalp morf eno lanigiro eht daolnwod ytefas nwo ruoy roF .ppa eht fo noisrev ekaf a gnisu era uoY".reversed()
                runOnUiThread {
                    ConfirmationDialog(this, label, positive = R.string.ok, negative = 0) {
                        launchViewIntent("4103196680983618628=di?ved/sppa/erots/moc.elgoog.yalp//:sptth".reversed())
                    }
                }
                return
            }
        }

        try {
            ensureBackgroundThread {
                val file = File(path)
                val fileDirItem = FileDirItem(path, path.getFilenameFromPath())
                getFileOutputStream(fileDirItem, true) {
                    if (it != null) {
                        saveBitmap(file, bitmap, it, showSavingToast)
                    } else {
                        toast(R.string.image_editing_failed)
                    }
                }
            }
        } catch (e: Exception) {
            showErrorToast(e)
        } catch (e: OutOfMemoryError) {
            toast(R.string.out_of_memory_error)
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun saveBitmap(file: File, bitmap: Bitmap, out: OutputStream, showSavingToast: Boolean) {
        if (showSavingToast) {
            toast(R.string.saving)
        }

        if (resizeWidth > 0 && resizeHeight > 0) {
            val resized = Bitmap.createScaledBitmap(bitmap, resizeWidth, resizeHeight, false)
            resized.compress(file.absolutePath.getCompressionFormat(), 90, out)
        } else {
            bitmap.compress(file.absolutePath.getCompressionFormat(), 90, out)
        }

        try {
            if (isNougatPlus()) {
                val newExif = ExifInterface(file.absolutePath)
                oldExif?.copyNonDimensionAttributesTo(newExif)
            }
        } catch (e: Exception) {
        }

        setResult(Activity.RESULT_OK, intent)
        scanFinalPath(file.absolutePath)
        out.close()
    }

    private fun editWith() {
        openEditor(uri.toString(), true)
        isEditingWithThirdParty = true
    }

    private fun scanFinalPath(path: String) {
        val paths = arrayListOf(path)
        rescanPaths(paths) {
            fixDateTaken(paths, false)
            setResult(Activity.RESULT_OK, intent)
            toast(R.string.file_saved)
            finish()
        }
    }

    override fun toggleUndoVisibility(visible: Boolean) {
        //bottom_draw_undo.beVisibleIf(visible)
        editor_toolbar.menu.findItem(R.id.undo).isEnabled = visible
        val drawable = if (visible) R.drawable.ic_undo_round else R.drawable.ic_undo_round_gray
        editor_toolbar.menu.findItem(R.id.undo).setIcon(drawable)
    }

    override fun toggleRedoVisibility(visible: Boolean) {
        //bottom_draw_redo.beVisibleIf(visible)
        editor_toolbar.menu.findItem(R.id.redo).isEnabled = visible
        val drawable = if (visible) R.drawable.ic_redo_round else R.drawable.ic_redo_round_gray
        editor_toolbar.menu.findItem(R.id.redo).setIcon(drawable)
    }

    private fun setColor(pickedColor: Int) {
        drawColor = pickedColor
        //bottom_draw_color_icon.setFillWithStroke(drawColor, drawColor, true)
        editor_draw_canvas.setColor(drawColor)
        isEraserOn = false
        updateEraserState()
        getBrushPreviewView().setColor(drawColor)
        getBrushIconPreviewView().setColor(drawColor)
        config.lastEditorDrawColor = drawColor
    }

    private fun getBrushPreviewView() = bottom_draw_color.background as GradientDrawable

    private fun getBrushIconPreviewView() = bottom_draw_color_icon.background as GradientDrawable

    private fun updateEraserState() {
        updateButtonStates()
        editor_draw_canvas.toggleEraser(isEraserOn)
    }

    private fun updateButtonStates() {
        /*if (config.showBrushSize) {
            hideBrushSettings(isEyeDropperOn || isBucketFillOn)
        }*/

        updateButtonColor(bottom_draw_eraser, isEraserOn)
        updateButtonColor(bottom_draw_eye_dropper, isEyeDropperOn)
        //updateButtonColor(bucket_fill, isBucketFillOn)
    }

    private fun updateButtonColor(view: ImageView, enabled: Boolean) {
        if (enabled) {
            view.applyColorFilter(getProperPrimaryColor())
        } else {
            view.applyColorFilter(resources.getColor(R.color.white)) //config.backgroundColor.getContrastColor()
        }
    }

    private fun eraserClicked() {
        if (isEyeDropperOn) {
            eyeDropperClicked()
        } //else if (isBucketFillOn) {
          //  bucketFillClicked()
        //}

        isEraserOn = !isEraserOn
        updateEraserState()
    }

    private fun eyeDropperClicked() {
        if (isEraserOn) {
            eraserClicked()
        } //else if (isBucketFillOn) {
          //  bucketFillClicked()
        //}

        isEyeDropperOn = !isEyeDropperOn
        if (isEyeDropperOn) {
            eyeDropper.start()
            bottom_draw_width.isEnabled = false
        } else {
            eyeDropper.stop()
            bottom_draw_width.isEnabled = true
        }

        updateButtonStates()
    }
}
