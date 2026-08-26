package com.andrew.hamsterpet

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import java.util.concurrent.ConcurrentHashMap

internal class ConcurrentResourceCache<T : Any> {
    private val values = ConcurrentHashMap<Int, T>()

    fun get(@DrawableRes resourceId: Int, loader: () -> T): T =
        values.computeIfAbsent(resourceId) { loader() }
}

internal object SpriteAtlasCache {
    private val bitmaps = ConcurrentResourceCache<Bitmap>()

    fun get(resources: Resources, @DrawableRes resourceId: Int): Bitmap =
        bitmaps.get(resourceId) {
            BitmapFactory.decodeResource(resources, resourceId)
                ?: error("Unable to decode sprite atlas resource $resourceId")
        }
}
