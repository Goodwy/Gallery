package com.goodwy.gallery.dialogs

import android.graphics.Color
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.goodwy.commons.activities.BaseSimpleActivity
import com.goodwy.commons.dialogs.FilePickerDialog
import com.goodwy.commons.extensions.baseConfig
import com.goodwy.commons.extensions.beGone
import com.goodwy.commons.extensions.beInvisible
import com.goodwy.commons.extensions.beVisibleIf
import com.goodwy.commons.extensions.getAlertDialogBuilder
import com.goodwy.commons.extensions.getDefaultCopyDestinationPath
import com.goodwy.commons.extensions.getProperPrimaryColor
import com.goodwy.commons.extensions.getSurfaceColor
import com.goodwy.commons.extensions.handleHiddenFolderPasswordProtection
import com.goodwy.commons.extensions.handleLockedFolderOpening
import com.goodwy.commons.extensions.hideKeyboard
import com.goodwy.commons.extensions.isBlackTheme
import com.goodwy.commons.extensions.isDynamicTheme
import com.goodwy.commons.extensions.isGone
import com.goodwy.commons.extensions.isInDownloadDir
import com.goodwy.commons.extensions.isRestrictedWithSAFSdk30
import com.goodwy.commons.extensions.setupDialogStuff
import com.goodwy.commons.extensions.toast
import com.goodwy.commons.helpers.VIEW_TYPE_GRID
import com.goodwy.commons.views.MyGridLayoutManager
import com.goodwy.commons.views.MySearchMenuTop
import com.goodwy.gallery.R
import com.goodwy.gallery.adapters.DirectoryAdapter
import com.goodwy.gallery.databinding.DialogDirectoryPickerBinding
import com.goodwy.gallery.extensions.addTempFolderIfNeeded
import com.goodwy.gallery.extensions.config
import com.goodwy.gallery.extensions.getCachedDirectories
import com.goodwy.gallery.extensions.getDirsToShow
import com.goodwy.gallery.extensions.getDistinctPath
import com.goodwy.gallery.extensions.getSortedDirectories
import com.goodwy.gallery.models.Directory

class PickDirectoryDialog(
    val activity: BaseSimpleActivity,
    val sourcePath: String,
    showOtherFolderButton: Boolean,
    val showFavoritesBin: Boolean,
    val isPickingCopyMoveDestination: Boolean,
    val isPickingFolderForWidget: Boolean,
    val callback: (path: String) -> Unit
) {
    private var dialog: AlertDialog? = null
    private var shownDirectories = ArrayList<Directory>()
    private var allDirectories = ArrayList<Directory>()
    private var openedSubfolders = arrayListOf("")
    private var binding = DialogDirectoryPickerBinding.inflate(activity.layoutInflater)
    private var isGridViewType = activity.config.viewTypeFolders == VIEW_TYPE_GRID
    private var showHidden = activity.config.shouldShowHidden
    private var currentPathPrefix = ""
    private val config = activity.config
    private val searchView = binding.folderSearchView
    private val searchEditText = searchView.binding.topToolbarSearch
    private val searchBarContainer = searchView.binding.searchBarContainer

    init {
        (binding.directoriesGrid.layoutManager as MyGridLayoutManager).apply {
            orientation = if (activity.config.scrollHorizontally && isGridViewType) RecyclerView.HORIZONTAL else RecyclerView.VERTICAL
            spanCount = if (isGridViewType) activity.config.dirColumnCnt else 1
        }

        binding.directoriesFastscroller.updateColors(activity.getProperPrimaryColor())

        configureSearchView()

        val builder = activity.getAlertDialogBuilder()
            .setPositiveButton(com.goodwy.commons.R.string.ok, null)
            .setNegativeButton(com.goodwy.commons.R.string.cancel, null)

        if (showOtherFolderButton) {
            builder.setNeutralButton(R.string.other_folder) { dialogInterface, i -> showOtherFolder() }
        }

        builder.apply {
            activity.setupDialogStuff(binding.root, this, com.goodwy.commons.R.string.select_destination) { alertDialog ->
                dialog = alertDialog
                binding.directoriesShowHidden.beVisibleIf(!context.config.shouldShowHidden)
                binding.directoriesShowHidden.setOnClickListener {
                    activity.handleHiddenFolderPasswordProtection {
                        binding.directoriesShowHidden.beGone()
                        showHidden = true
                        fetchDirectories(true)
                    }
                }

                alertDialog.onBackPressedDispatcher.addCallback(alertDialog) {
                    if (searchView.isSearchOpen) {
                        searchView.closeSearch()
                    } else if (activity.config.groupDirectSubfolders) {
                        if (currentPathPrefix.isEmpty()) {
                            isEnabled = false
                            alertDialog.onBackPressedDispatcher.onBackPressed()
                        } else {
                            openedSubfolders.removeAt(openedSubfolders.lastIndex)
                            currentPathPrefix = openedSubfolders.last()
                            gotDirectories(allDirectories)
                        }
                    } else {
                        isEnabled = false
                        alertDialog.onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        }

        fetchDirectories(false)
    }

    private fun configureSearchView() = with(searchView) {
        updateHintText(context.getString(com.goodwy.commons.R.string.search_folders))
        searchEditText.imeOptions = EditorInfo.IME_ACTION_DONE

        toggleHideOnScroll(!config.scrollHorizontally && config.hideTopBarWhenScroll)
        setupMenu()
        setSearchViewListeners()
        updateSearchViewUi()
    }

    private fun MySearchMenuTop.updateSearchViewUi() {
        requireToolbar().beInvisible()

        val backgroundColor = when {
            context.isDynamicTheme() -> resources.getColor(com.goodwy.commons.R.color.you_dialog_background_color, context.theme)
            context.isBlackTheme() -> context.getSurfaceColor()
            else -> context.baseConfig.backgroundColor
        }
        updateColors(background = backgroundColor)
        setBackgroundColor(Color.TRANSPARENT)
//        searchBarContainer.setBackgroundColor(Color.TRANSPARENT)
    }

    private fun MySearchMenuTop.setSearchViewListeners() {
        onSearchOpenListener = {
            updateSearchViewLeftIcon(com.goodwy.commons.R.drawable.ic_chevron_left_vector)
        }

        onSearchClosedListener = {
            searchEditText.clearFocus()
            activity.hideKeyboard(searchEditText)
            updateSearchViewLeftIcon(com.goodwy.commons.R.drawable.ic_search_vector)
        }

        onSearchTextChangedListener = { text ->
            filterFolderListBySearchQuery(text)
            clearSearch()
        }
    }

    private fun updateSearchViewLeftIcon(iconResId: Int) = with(searchView.binding.topToolbarSearchIcon) {
        post {
            setImageResource(iconResId)
        }
    }

    private fun filterFolderListBySearchQuery(query: String) {
        val adapter = binding.directoriesGrid.adapter as? DirectoryAdapter
        var dirsToShow = allDirectories
        if (query.isNotEmpty()) {
            dirsToShow = dirsToShow.filter { it.name.contains(query, true) }.toMutableList() as ArrayList
        }
        dirsToShow = activity.getSortedDirectories(dirsToShow)
        checkPlaceholderVisibility(dirsToShow)

        val filteredFolderListUpdated = adapter?.dirs != dirsToShow
        if (filteredFolderListUpdated) {
            adapter?.updateDirs(dirsToShow)

            binding.directoriesGrid.apply {
                post {
                    scrollToPosition(0)
                }
            }
        }
    }

    private fun checkPlaceholderVisibility(dirs: ArrayList<Directory>) = with(binding) {
        directoriesEmptyPlaceholder.beVisibleIf(dirs.isEmpty())

        if (folderSearchView.isSearchOpen) {
            directoriesEmptyPlaceholder.text = root.context.getString(com.goodwy.commons.R.string.no_items_found)
        }

        directoriesFastscroller.beVisibleIf(directoriesEmptyPlaceholder.isGone())
    }

    private fun fetchDirectories(forceShowHiddenAndExcluded: Boolean) {
        activity.getCachedDirectories(forceShowHidden = forceShowHiddenAndExcluded, forceShowExcluded = forceShowHiddenAndExcluded) {
            if (it.isNotEmpty()) {
                it.forEach {
                    it.subfoldersMediaCount = it.mediaCnt
                }

                activity.runOnUiThread {
                    allDirectories.clear()
                    gotDirectories(activity.addTempFolderIfNeeded(it))
                }
            }
        }
    }

    private fun showOtherFolder() {
        activity.hideKeyboard(searchEditText)
        FilePickerDialog(
            activity = activity,
            currPath = activity.getDefaultCopyDestinationPath(showHidden, sourcePath),
            pickFile = !isPickingCopyMoveDestination && !isPickingFolderForWidget,
            showHidden = showHidden,
            showFAB = true,
            canAddShowHiddenButton = true
        ) {
            config.lastCopyPath = it
            activity.handleLockedFolderOpening(it) { success ->
                if (success) {
                    callback(it)
                }
            }
        }
    }

    private fun gotDirectories(newDirs: ArrayList<Directory>) {
        if (allDirectories.isEmpty()) {
            allDirectories = newDirs.clone() as ArrayList<Directory>
        }

        val distinctDirs = newDirs.filter { showFavoritesBin || (!it.isRecycleBin() && !it.areFavorites()) }.distinctBy { it.path.getDistinctPath() }
            .toMutableList() as ArrayList<Directory>
        val sortedDirs = activity.getSortedDirectories(distinctDirs)
        val dirs = activity.getDirsToShow(sortedDirs, allDirectories, currentPathPrefix).clone() as ArrayList<Directory>
        if (dirs.hashCode() == shownDirectories.hashCode()) {
            return
        }

        shownDirectories = dirs
        val adapter = DirectoryAdapter(activity, dirs.clone() as ArrayList<Directory>, null, binding.directoriesGrid, true) {
            val clickedDir = it as Directory
            val path = clickedDir.path
            if (clickedDir.subfoldersCount == 1 || !activity.config.groupDirectSubfolders) {
                if (isPickingCopyMoveDestination && path.trimEnd('/') == sourcePath) {
                    activity.toast(com.goodwy.commons.R.string.source_and_destination_same)
                    return@DirectoryAdapter
                } else if (isPickingCopyMoveDestination && activity.isRestrictedWithSAFSdk30(path) && !activity.isInDownloadDir(path)) {
                    activity.toast(com.goodwy.commons.R.string.system_folder_copy_restriction, Toast.LENGTH_LONG)
                    return@DirectoryAdapter
                } else {
                    activity.handleLockedFolderOpening(path) { success ->
                        if (success) {
                            callback(path)
                        }
                    }
                    dialog?.dismiss()
                }
            } else {
                currentPathPrefix = path
                openedSubfolders.add(path)
                gotDirectories(allDirectories)
            }
        }

        val scrollHorizontally = activity.config.scrollHorizontally && isGridViewType
        binding.apply {
            directoriesGrid.adapter = adapter
            directoriesFastscroller.setScrollVertically(!scrollHorizontally)
        }
    }
}
