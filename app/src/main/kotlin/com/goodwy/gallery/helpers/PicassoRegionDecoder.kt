package com.goodwy.gallery.helpers

import android.content.Context
import android.graphics.*
import android.net.Uri
import com.davemorrissey.labs.subscaleview.ImageRegionDecoder

class PicassoRegionDecoder(
    val showHighestQuality: Boolean,
    val screenWidth: Int,
    val screenHeight: Int,
    val minTileDpi: Int
) : ImageRegionDecoder {
    private var decoder: BitmapRegionDecoder? = null
    private val decoderLock = Any()

    override fun init(context: Context, uri: Uri): Point {
        val newUri = Uri.parse(uri.toString().replace("%", "%25").replace("#", "%23"))
        context.contentResolver.openInputStream(newUri)?.use { inputStream ->
            decoder = BitmapRegionDecoder.newInstance(inputStream, false)
        }
        val currentDecoder = decoder ?: throw IllegalStateException("Failed to create BitmapRegionDecoder")
        return Point(currentDecoder.width, currentDecoder.height)
    }

    override fun decodeRegion(rect: Rect, sampleSize: Int): Bitmap {
        synchronized(decoderLock) {
            val currentDecoder = decoder ?: throw IllegalStateException("BitmapRegionDecoder is not initialized")
            var newSampleSize = sampleSize
            if (!showHighestQuality && minTileDpi == LOW_TILE_DPI) {
                if ((rect.width() > rect.height() && screenWidth > screenHeight) || (rect.height() > rect.width() && screenHeight > screenWidth)) {
                    if ((rect.width() / sampleSize > screenWidth || rect.height() / sampleSize > screenHeight)) {
                        newSampleSize *= 2
                    }
                }
            }

            val options = BitmapFactory.Options()
            options.inSampleSize = newSampleSize
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            val bitmap = currentDecoder.decodeRegion(rect, options)
            return bitmap ?: throw RuntimeException("Region decoder returned null bitmap - image format may not be supported")
        }
    }

    override fun isReady() = decoder != null && decoder?.isRecycled == false

    override fun recycle() {
        decoder?.recycle()
        decoder = null
    }
}
