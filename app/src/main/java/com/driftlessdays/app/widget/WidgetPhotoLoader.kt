package com.driftlessdays.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object WidgetPhotoLoader {

    private fun getPhotoUrl(date: LocalDate): String {
        val seed = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return "https://driftless-worker.jjwerlein.workers.dev/photo/nature/$seed"
    }

    suspend fun loadTodayPhoto(): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val url = getPhotoUrl(LocalDate.now())
                val bitmap = BitmapFactory.decodeStream(URL(url).openStream())
                bitmap
            } catch (e: Exception) {
                Log.e("WidgetPhotoLoader", "Failed to load photo", e)
                null
            }
        }
    }

    fun saveBitmapToCache(context: Context, bitmap: Bitmap): String {
        val file = java.io.File(context.cacheDir, "widget_photo.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
        }
        return file.absolutePath
    }

    fun loadBitmapFromCache(context: Context): Bitmap? {
        val file = java.io.File(context.cacheDir, "widget_photo.png")
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }
}