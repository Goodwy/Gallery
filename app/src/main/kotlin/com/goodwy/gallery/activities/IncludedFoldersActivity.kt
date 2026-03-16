package com.goodwy.gallery.activities

import android.os.Bundle
import com.goodwy.commons.extensions.beVisibleIf
import com.goodwy.commons.extensions.getProperTextColor
import com.goodwy.commons.extensions.viewBinding
import com.goodwy.commons.helpers.NavigationIcon
import com.goodwy.commons.interfaces.RefreshRecyclerViewListener
import com.goodwy.gallery.R
import com.goodwy.gallery.adapters.ManageFoldersAdapter
import com.goodwy.gallery.databinding.ActivityManageFoldersBinding
import com.goodwy.gallery.extensions.config

class IncludedFoldersActivity : SimpleActivity(), RefreshRecyclerViewListener {

    private val binding by viewBinding(ActivityManageFoldersBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        updateFolders()
        setupOptionsMenu()
        binding.manageFoldersToolbar.title = getString(R.string.include_folders)

        setupEdgeToEdge(
            padTopSystem = listOf(binding.manageFoldersAppbar),
            padBottomSystem = listOf(binding.manageFoldersList)
        )
        setupMaterialScrollListener(binding.manageFoldersList, binding.manageFoldersAppbar)
    }

    override fun onResume() {
        super.onResume()
        setupTopAppBar(binding.manageFoldersAppbar, NavigationIcon.Arrow)
    }

    private fun updateFolders() {
        val folders = ArrayList<String>()
        config.includedFolders.mapTo(folders) { it }
        binding.manageFoldersPlaceholder.apply {
            text = getString(R.string.included_activity_placeholder)
            beVisibleIf(folders.isEmpty())
            setTextColor(getProperTextColor())
        }

        val adapter = ManageFoldersAdapter(this, folders, false, this, binding.manageFoldersList) {}
        binding.manageFoldersList.adapter = adapter
    }

    private fun setupOptionsMenu() {
        binding.manageFoldersToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.add_folder -> addFolder()
                else -> return@setOnMenuItemClickListener false
            }
            return@setOnMenuItemClickListener true
        }
    }

    override fun refreshItems() {
        updateFolders()
    }

    private fun addFolder() {
        showAddIncludedFolderDialog {
            updateFolders()
        }
    }
}
