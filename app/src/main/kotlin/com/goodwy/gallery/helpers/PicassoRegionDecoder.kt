package com.goodwy.gallery.helpers

import android.content.Context
import android.graphics.*
import android.net.Uri
import com.davemorrissey.labs.subscaleview.ImageRegionDecoder
import java.io.IOException

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
        // openInputStream() and newInstance() are both documented as nullable on failure (e.g.
        // the file was deleted or became unreadable between the gallery listing it and the
        // user zooming into it) - surface that as a clean IOException instead of an NPE.
        val inputStream = context.contentResolver.openInputStream(newUri)
            ?: throw IOException("Unable to open input stream for $newUri")
        val regionDecoder = BitmapRegionDecoder.newInstance(inputStream, false)
            ?: throw IOException("Unable to create a region decoder for $newUri")
        decoder = regionDecoder
        return Point(regionDecoder.width, regionDecoder.height)
    }

    override fun decodeRegion(rect: Rect, sampleSize: Int): Bitmap {
        synchronized(decoderLock) {
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
            val bitmap = decoder!!.decodeRegion(rect, options)
            return bitmap ?: throw RuntimeException("Region decoder returned null bitmap - image format may not be supported")
        }
    }

    override fun isReady() = decoder != null && !decoder!!.isRecycled

    override fun recycle() {
        decoder!!.recycle()
    }
}
