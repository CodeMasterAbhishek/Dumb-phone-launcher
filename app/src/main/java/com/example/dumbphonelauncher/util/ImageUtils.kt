package com.example.dumbphonelauncher.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

object ImageUtils {
    fun drawableToImageBitmap(drawable: Drawable): ImageBitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap.asImageBitmap()
        }
        // Cap dimensions to prevent OOM from abnormally large drawables
        val maxDimension = 256
        val width = (if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1).coerceAtMost(maxDimension)
        val height = (if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1).coerceAtMost(maxDimension)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }
}
