package com.goodwy.gallery

import android.app.Application
import com.anjlab.android.iab.v3.BillingProcessor
import com.anjlab.android.iab.v3.PurchaseInfo
import com.github.ajalt.reprint.core.Reprint
import com.goodwy.commons.extensions.checkUseEnglish
import com.goodwy.commons.extensions.toast
import com.squareup.picasso.Downloader
import com.squareup.picasso.Picasso
import okhttp3.Request
import okhttp3.Response

class App : Application() {

    lateinit var billingProcessor: BillingProcessor

    override fun onCreate() {
        super.onCreate()
        instance = this
        checkUseEnglish()
        Reprint.initialize(this)
        Picasso.setSingletonInstance(Picasso.Builder(this).downloader(object : Downloader {
            override fun load(request: Request) = Response.Builder().build()

            override fun shutdown() {}
        }).build())

        // automatically restores purchases
        billingProcessor = BillingProcessor(
            this, BuildConfig.GOOGLE_PLAY_LICENSING_KEY,
            object : BillingProcessor.IBillingHandler {
                override fun onProductPurchased(productId: String, details: PurchaseInfo?) {}

                override fun onPurchaseHistoryRestored() {
                    toast(R.string.restored_previous_purchase_please_restart)
                }

                override fun onBillingError(errorCode: Int, error: Throwable?) {}

                override fun onBillingInitialized() {}
            })
    }

    override fun onTerminate() {
        super.onTerminate()
        billingProcessor.release()
    }

    companion object {
        private var instance: App? = null

        fun isProVersion(): Boolean {
            return instance!!.billingProcessor.isPurchased(BuildConfig.PRODUCT_ID_X1)
                || instance!!.billingProcessor.isPurchased(BuildConfig.PRODUCT_ID_X2)
                || instance!!.billingProcessor.isPurchased(BuildConfig.PRODUCT_ID_X3)
        }
    }
}
