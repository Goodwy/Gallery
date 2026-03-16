package com.goodwy.gallery

import com.github.ajalt.reprint.core.Reprint
import com.goodwy.commons.RightApp
import com.goodwy.commons.helpers.PurchaseHelper
import com.squareup.picasso.Downloader
import com.squareup.picasso.Picasso
import okhttp3.Request
import okhttp3.Response

class App : RightApp() {

    override val isAppLockFeatureAvailable = true

    override fun onCreate() {
        super.onCreate()
        PurchaseHelper().initPurchaseIfNeed(this, "1504831423")
        Reprint.initialize(this)
        Picasso.setSingletonInstance(Picasso.Builder(this).downloader(object : Downloader {
            override fun load(request: Request) = Response.Builder().build()

            override fun shutdown() {}
        }).build())
    }
}
