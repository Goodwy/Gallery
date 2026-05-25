package com.goodwy.gallery.svg

import android.content.Context
import android.graphics.drawable.PictureDrawable

import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.engine.cache.LruResourceCache
import com.bumptech.glide.load.engine.cache.MemorySizeCalculator
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import com.caverock.androidsvg.SVG
import com.bumptech.glide.integration.webp.WebpBitmapFactory
import com.goodwy.gallery.extensions.config

import java.io.InputStream

@GlideModule
class SvgModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.register(SVG::class.java, PictureDrawable::class.java, SvgDrawableTranscoder()).append(InputStream::class.java, SVG::class.java, SvgDecoder())
    }

    override fun applyOptions(context: Context, builder: com.bumptech.glide.GlideBuilder) {
        // CVE-2023-4863: disable vulnerable system WebP decoder once at init
        WebpBitmapFactory.sUseSystemDecoder = false

        // Configure disk cache: size from user config in internal cache directory
        builder.setDiskCache(InternalCacheDiskCacheFactory(context, "image_cache", context.config.diskCacheSizeMB.toLong() * 1024L * 1024L))

        // Configure memory cache: 3 screens worth of memory
        val memorySizeCalculator = MemorySizeCalculator.Builder(context)
            .setMemoryCacheScreens(3f)
            .build()
        builder.setMemoryCache(LruResourceCache(memorySizeCalculator.memoryCacheSize.toLong()))

        // Set default request options
        builder.setDefaultRequestOptions(
            RequestOptions()
                .format(DecodeFormat.PREFER_ARGB_8888)
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
        )
    }

    override fun isManifestParsingEnabled() = false
}
