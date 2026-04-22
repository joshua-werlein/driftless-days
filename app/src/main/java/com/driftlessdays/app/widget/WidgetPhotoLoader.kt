package com.driftlessdays.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object WidgetPhotoLoader {

    private val client = OkHttpClient()
    private const val MAX_WIDTH = 800
    private const val MAX_HEIGHT = 450

    private fun getPhotoUrl(date: LocalDate): String {
        val seed = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return "https://driftless-worker.jjwerlein.workers.dev/photo/nature/$seed"
    }

    suspend fun loadTodayPhoto(): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val url = getPhotoUrl(LocalDate.now())
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.byteStream()?.let { BitmapFactory.decodeStream(it) }
                        ?.let { downsample(it) }
                } else {
                    Log.e("WidgetPhotoLoader", "HTTP ${response.code} for $url")
                    null
                }
            } catch (e: Exception) {
                Log.e("WidgetPhotoLoader", "Failed to load photo", e)
                null
            }
        }
    }

    private fun downsample(bitmap: Bitmap): Bitmap {
        val scale = minOf(MAX_WIDTH.toFloat() / bitmap.width,
                          MAX_HEIGHT.toFloat() / bitmap.height, 1f)
        if (scale >= 1f) return bitmap
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true
        )
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