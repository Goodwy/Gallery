package com.goodwy.gallery

import android.app.Application
import com.github.ajalt.reprint.core.Reprint
import com.goodwy.commons.extensions.checkUseEnglish
import com.goodwy.commons.helpers.rustore.RuStoreModule
import com.squareup.picasso.Downloader
import com.squareup.picasso.Picasso
import okhttp3.Request
import okhttp3.Response

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        checkUseEnglish()
        RuStoreModule.install(this, "1504831423") //TODO rustore
        Reprint.initialize(this)
        Picasso.setSingletonInstance(Picasso.Builder(this).downloader(object : Downloader {
            override fun load(request: Request) = Response.Builder().build()

            override fun shutdown() {}
        }).build())
    }
}
