package com.goodwy.gallery.activities

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.goodwy.commons.extensions.*
import com.goodwy.commons.helpers.REQUEST_CODE_SPEECH_INPUT
import com.goodwy.commons.helpers.VIEW_TYPE_GRID
import com.goodwy.commons.helpers.ensureBackgroundThread
import com.goodwy.commons.models.FileDirItem
import com.goodwy.commons.views.MyGridLayoutManager
import com.goodwy.gallery.R
import com.goodwy.gallery.adapters.MediaAdapter
import com.goodwy.gallery.asynctasks.GetMediaAsynctask
import com.goodwy.gallery.databinding.ActivitySearchBinding
import com.goodwy.gallery.extensions.*
import com.goodwy.gallery.helpers.GridSpacingItemDecoration
import com.goodwy.gallery.helpers.MediaFetcher
import com.goodwy.gallery.helpers.PATH
import com.goodwy.gallery.helpers.SHOW_ALL
import com.goodwy.gallery.helpers.VIDEO_PLAYER_APP
import com.goodwy.gallery.helpers.VIDEO_PLAYER_SYSTEM
import com.goodwy.gallery.interfaces.MediaOperationsListener
import com.goodwy.gallery.models.Medium
import com.goodwy.gallery.models.ThumbnailItem
import java.io.File
import java.util.Objects

class SearchActivity : SimpleActivity(), MediaOperationsListener {
    override var isSearchBarEnabled = true

    private var mLastSearchedText = ""

    private var mCurrAsyncTask: GetMediaAsynctask? = null
    private var mAllMedia = ArrayList<ThumbnailItem>()
    private var isSpeechToTextAvailable = false
    private var wasKeyboardVisible = false

    private val binding by viewBinding(ActivitySearchBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setupOptionsMenu()

        val scrollHorizontally = config.scrollHorizontally
        val view = if (scrollHorizontally) binding.searchFastscroller else binding.searchGrid
        setupEdgeToEdge(
            padTopSystem = listOf(binding.searchMenu),
            padBottomImeAndSystem = listOf(view)
        )
        binding.searchEmptyTextPlaceholder.setTextColor(getProperTextColor())
        getAllMedia()
        binding.searchFastscroller.updateColors(getProperPrimaryColor())

        if (scrollHorizontally) setupKeyboardListener()
    }

    override fun onResume() {
        super.onResume()
        updateMenuColors()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCurrAsyncTask?.stopFetching()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == REQUEST_CODE_SPEECH_INPUT && resultCode == RESULT_OK) {
            if (resultData != null) {
                val res: ArrayList<String> =
                    resultData.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<String>

                val speechToText =  Objects.requireNonNull(res)[0]
                if (speechToText.isNotEmpty()) {
                    binding.searchMenu.setText(speechToText)
                }
            }
        }
    }

    private fun setupOptionsMenu() {
        binding.searchMenu.requireToolbar().inflateMenu(R.menu.menu_search)
        binding.searchMenu.toggleHideOnScroll(config.hideTopBarWhenScroll)

        if (baseConfig.useSpeechToText) {
            isSpeechToTextAvailable = isSpeechToTextAvailable()
            binding.searchMenu.showSpeechToText = isSpeechToTextAvailable
        }

        binding.searchMenu.setupMenu()
        binding.searchMenu.toggleForceArrowBackIcon(true)
        binding.searchMenu.focusView()
        binding.searchMenu.updateHintText(getString(com.goodwy.commons.R.string.search_files))

        binding.searchMenu.onSpeechToTextClickListener = {
            speechToText()
        }

        binding.searchMenu.onNavigateBackClickListener = {
            if (binding.searchMenu.getCurrentQuery().isEmpty()) {
                finish()
            } else {
                binding.searchMenu.closeSearch()
            }
        }

        binding.searchMenu.onSearchTextChangedListener = { text ->
            mLastSearchedText = text
            textChanged(text)
            binding.searchMenu.clearSearch()
        }

        binding.searchMenu.requireToolbar().setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.toggle_filename -> toggleFilenameVisibility()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun updateMenuColors() {
        binding.searchMenu.updateColors()
    }

    private fun textChanged(text: String) {
        ensureBackgroundThread {
            try {
                val filtered = mAllMedia.filter { it is Medium && it.name.contains(text, true) } as ArrayList
                filtered.sortBy { it is Medium && !it.name.startsWith(text, true) }
                val grouped = MediaFetcher(applicationContext).groupMedia(filtered as ArrayList<Medium>, "")
                runOnUiThread {
                    if (grouped.isEmpty()) {
                        binding.searchEmptyTextPlaceholder.text = getString(com.goodwy.commons.R.string.no_items_found)
                        binding.searchEmptyTextPlaceholder.beVisible()
                    } else {
                        binding.searchEmptyTextPlaceholder.beGone()
                    }

                    handleGridSpacing(grouped)
                    getMediaAdapter()?.updateMedia(grouped)
                }
            } catch (ignored: Exception) {
            }
        }
    }

    private fun setupAdapter() {
        val currAdapter = binding.searchGrid.adapter
        if (currAdapter == null) {
            MediaAdapter(this, mAllMedia, this, false, false, "", binding.searchGrid) {
                if (it is Medium) {
                    itemClicked(it.path)
                }
            }.apply {
                binding.searchGrid.adapter = this
            }
            setupLayoutManager()
            handleGridSpacing(mAllMedia)
        } else if (mLastSearchedText.isEmpty()) {
            (currAdapter as MediaAdapter).updateMedia(mAllMedia)
            handleGridSpacing(mAllMedia)
        } else {
            textChanged(mLastSearchedText)
        }

        setupScrollDirection()
    }

    private fun handleGridSpacing(media: ArrayList<ThumbnailItem>) {
        val viewType = config.getFolderViewType(SHOW_ALL)
        if (viewType == VIEW_TYPE_GRID) {
            if (binding.searchGrid.itemDecorationCount > 0) {
                binding.searchGrid.removeItemDecorationAt(0)
            }

            val spanCount = config.mediaColumnCnt
            val spacing = config.thumbnailSpacing
            val decoration = GridSpacingItemDecoration(spanCount, spacing, config.scrollHorizontally, config.fileRoundedCorners, media, true)
            binding.searchGrid.addItemDecoration(decoration)
        }
    }

    private fun getMediaAdapter() = binding.searchGrid.adapter as? MediaAdapter

    private fun toggleFilenameVisibility() {
        config.displayFileNames = !config.displayFileNames
        getMediaAdapter()?.updateDisplayFilenames(config.displayFileNames)
    }

    private fun itemClicked(path: String) {
        if (!path.isVideoFast()) {
            openInViewPager(path)
            return
        }

        when (config.videoPlayerType) {
            VIDEO_PLAYER_SYSTEM -> openPath(path = path, forceChooser = false)
            VIDEO_PLAYER_APP -> if (config.gestureVideoPlayer) launchGesturePlayer(path) else openInViewPager(path)
            else -> openInViewPager(path) // unreachable by design
        }
    }

    private fun openInViewPager(path: String) {
        Intent(this, ViewPagerActivity::class.java).apply {
            putExtra(PATH, path)
            putExtra(SHOW_ALL, false)
            startActivity(this)
        }
    }

    private fun setupLayoutManager() {
        val viewType = config.getFolderViewType(SHOW_ALL)
        if (viewType == VIEW_TYPE_GRID) {
            setupGridLayoutManager()
        } else {
            setupListLayoutManager()
        }
    }

    private fun setupGridLayoutManager() {
        val layoutManager = binding.searchGrid.layoutManager as MyGridLayoutManager
        if (config.scrollHorizontally) {
            layoutManager.orientation = RecyclerView.HORIZONTAL
            binding.searchGrid.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
        } else {
            layoutManager.orientation = RecyclerView.VERTICAL
            binding.searchGrid.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        layoutManager.spanCount = config.mediaColumnCnt
        val adapter = getMediaAdapter()
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (adapter?.isASectionTitle(position) == true) {
                    layoutManager.spanCount
                } else {
                    1
                }
            }
        }
    }

    private fun setupListLayoutManager() {
        val layoutManager = binding.searchGrid.layoutManager as MyGridLayoutManager
        layoutManager.spanCount = 1
        layoutManager.orientation = RecyclerView.VERTICAL
    }

    private fun setupScrollDirection() {
        val viewType = config.getFolderViewType(SHOW_ALL)
        val scrollHorizontally = config.scrollHorizontally && viewType == VIEW_TYPE_GRID
        binding.searchFastscroller.setScrollVertically(!scrollHorizontally)
    }

    private fun getAllMedia() {
        getCachedMedia("") {
            if (it.isNotEmpty()) {
                mAllMedia = it.clone() as ArrayList<ThumbnailItem>
            }
            runOnUiThread {
                setupAdapter()
            }
            startAsyncTask(false)
        }
    }

    private fun startAsyncTask(updateItems: Boolean) {
        mCurrAsyncTask?.stopFetching()
        mCurrAsyncTask = GetMediaAsynctask(applicationContext, "", showAll = true) {
            mAllMedia = it.clone() as ArrayList<ThumbnailItem>
            if (updateItems) {
                textChanged(mLastSearchedText)
            }
        }

        mCurrAsyncTask!!.execute()
    }

    override fun refreshItems() {
        startAsyncTask(true)
    }

    override fun tryDeleteFiles(fileDirItems: ArrayList<FileDirItem>, skipRecycleBin: Boolean) {
        val filtered = fileDirItems.filter { File(it.path).isFile && it.path.isMediaFile() } as ArrayList
        if (filtered.isEmpty()) {
            return
        }

        if (config.useRecycleBin && !skipRecycleBin && !filtered.first().path.startsWith(recycleBinPath)) {
            val movingItems = resources.getQuantityString(com.goodwy.commons.R.plurals.moving_items_into_bin, filtered.size, filtered.size)
            toast(movingItems)

            movePathsInRecycleBin(filtered.map { it.path } as ArrayList<String>) {
                if (it) {
                    deleteFilteredFiles(filtered)
                } else {
                    toast(com.goodwy.commons.R.string.unknown_error_occurred)
                }
            }
        } else {
            val deletingItems = resources.getQuantityString(com.goodwy.commons.R.plurals.deleting_items, filtered.size, filtered.size)
            toast(deletingItems)
            deleteFilteredFiles(filtered)
        }
    }

    private fun deleteFilteredFiles(filtered: ArrayList<FileDirItem>) {
        deleteFiles(filtered) {
            if (!it) {
                toast(com.goodwy.commons.R.string.unknown_error_occurred)
                return@deleteFiles
            }

            mAllMedia.removeAll { filtered.map { it.path }.contains((it as? Medium)?.path) }

            ensureBackgroundThread {
                val useRecycleBin = config.useRecycleBin
                filtered.forEach {
                    if (it.path.startsWith(recycleBinPath) || !useRecycleBin) {
                        deleteDBPath(it.path)
                    }
                }
            }
        }
    }

    override fun selectedPaths(paths: ArrayList<String>) {}

    override fun updateMediaGridDecoration(media: ArrayList<ThumbnailItem>) {}

    // Goodwy
    private fun setupKeyboardListener() {
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val rootView = binding.root
            val displayMetrics = resources.displayMetrics
            val screenHeight = displayMetrics.heightPixels

            // We obtain the height of the visible area
            val rect = android.graphics.Rect()
            rootView.getWindowVisibleDisplayFrame(rect)

            // Calculate the height of the invisible area (potentially the keyboard)
            val heightDiff = screenHeight - rect.bottom

            val isKeyboardVisible = heightDiff > 200.dpToPx(this)

            if (wasKeyboardVisible && !isKeyboardVisible) {
                // The keyboard has just disappeared.
                onKeyboardHidden()
            }

            wasKeyboardVisible = isKeyboardVisible
        }
    }

    private fun onKeyboardHidden() {
        if (config.scrollHorizontally) {
            recreateLayoutManager()
        }
    }

    private fun recreateLayoutManager() {
        if (config.scrollHorizontally) {
            val oldAdapter = binding.searchGrid.adapter
            val scrollPosition = (binding.searchGrid.layoutManager as? GridLayoutManager)?.findFirstVisibleItemPosition() ?: 0

            // Save the adapter
            binding.searchGrid.adapter = null

            // Creating a new layoutManager
            val newLayoutManager = MyGridLayoutManager(this, config.mediaColumnCnt).apply {
                orientation = RecyclerView.HORIZONTAL
                spanCount = config.mediaColumnCnt
            }

            binding.searchGrid.layoutManager = newLayoutManager
            binding.searchGrid.adapter = oldAdapter
            binding.searchGrid.scrollToPosition(scrollPosition)
        }
    }
}
