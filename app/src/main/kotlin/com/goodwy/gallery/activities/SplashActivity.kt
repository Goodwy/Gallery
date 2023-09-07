package com.goodwy.gallery.activities

import android.content.Intent
import com.goodwy.commons.activities.BaseSplashActivity
import com.goodwy.commons.helpers.ensureBackgroundThread
import com.goodwy.gallery.extensions.config
import com.goodwy.gallery.extensions.favoritesDB
import com.goodwy.gallery.extensions.getFavoriteFromPath
import com.goodwy.gallery.extensions.mediaDB
import com.goodwy.gallery.models.Favorite

class SplashActivity : BaseSplashActivity() {
    override fun initActivity() {

        // check if previously selected favorite items have been properly migrated into the new Favorites table
        if (config.wereFavoritesMigrated) {
            launchActivity()
        } else {
            if (config.appRunCount == 0) {
                config.wereFavoritesMigrated = true
                launchActivity()
            } else {
                config.wereFavoritesMigrated = true
                ensureBackgroundThread {
                    val favorites = ArrayList<Favorite>()
                    val favoritePaths = mediaDB.getFavorites().map { it.path }.toMutableList() as ArrayList<String>
                    favoritePaths.forEach {
                        favorites.add(getFavoriteFromPath(it))
                    }
                    favoritesDB.insertAll(favorites)

                    runOnUiThread {
                        launchActivity()
                    }
                }
            }
        }
    }

    private fun launchActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
