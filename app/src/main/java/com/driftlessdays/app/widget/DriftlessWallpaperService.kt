package com.driftlessdays.app.widget

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.core.graphics.toColorInt
import androidx.core.content.edit

class DriftlessWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = DriftlessWallpaperEngine()

    inner class DriftlessWallpaperEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        private var photo: Bitmap? = null
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var xOffset = 0f
        private var parallaxEnabled = true
        private val parallaxAmount = 60f
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        private val drawRunnable = Runnable { draw() }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            loadPhoto()
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunnable)
            scope.cancel()
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            surfaceWidth = width
            surfaceHeight = height
            draw()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            handler.removeCallbacks(drawRunnable)
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            if (parallaxEnabled) {
                this.xOffset = xOffset
                draw()
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                draw()
                loadPhoto()
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        private fun loadPhoto() {
            scope.launch(Dispatchers.IO) {
                try {
                    val prefs = getSharedPreferences("driftless_prefs", MODE_PRIVATE)
                    parallaxEnabled = prefs.getBoolean("parallax_enabled", true)
                    val category = prefs.getString("photo_category", "nature") ?: "nature"

                    // Check if cached photo is from today
                    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val cachedDate = prefs.getString("cached_photo_date", "")

                    if (cachedDate == today) {
                        // Use cached bitmap
                        photo = WidgetPhotoLoader.loadBitmapFromCache(this@DriftlessWallpaperService)
                    } else {
                        // Fetch new photo
                        val url = "https://driftless-worker.jjwerlein.workers.dev/photo/$category/$today"
                        val client = OkHttpClient()
                        val request = Request.Builder().url(url).build()
                        val response = client.newCall(request).execute()
                        val bitmap = if (response.isSuccessful) {
                            response.body?.byteStream()?.let { BitmapFactory.decodeStream(it) }
                        } else {
                            Log.e("DriftlessWallpaper", "Photo fetch failed: HTTP ${response.code} for $url")
                            null
                        }
                        if (bitmap != null) {
                            photo = bitmap
                            WidgetPhotoLoader.saveBitmapToCache(
                                this@DriftlessWallpaperService,
                                bitmap
                            )
                            prefs.edit { putString("cached_photo_date", today) }
                        }
                    }

                    launch(Dispatchers.Main) { draw() }

                } catch (e: Exception) {
                    Log.e("DriftlessWallpaper", "Failed to load photo", e)
                    launch(Dispatchers.Main) { draw() }
                }
            }
        }

        private fun draw() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                canvas?.let { drawFrame(it) }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }

        private fun drawFrame(canvas: Canvas) {
            // Fill background
            canvas.drawColor("#1A1A2E".toColorInt())

            val bitmap = photo ?: return

            // Scale bitmap to fill screen height
            val bitmapAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
            val screenAspect = surfaceWidth.toFloat() / surfaceHeight.toFloat()

            var scaledWidth: Float
            var scaledHeight: Float

            if (bitmapAspect > screenAspect) {
                // Bitmap is wider — constrain to parallax width, scale height proportionally
                scaledWidth = surfaceWidth.toFloat() + (parallaxAmount * 2)
                scaledHeight = scaledWidth / bitmapAspect
            } else {
                // Bitmap is taller — fit width, crop top/bottom
                scaledWidth = surfaceWidth.toFloat() + (parallaxAmount * 2)
                scaledHeight = scaledWidth / bitmapAspect
            }

            if (scaledHeight < surfaceHeight) {
                scaledHeight = surfaceHeight.toFloat()
                scaledWidth = scaledHeight * bitmapAspect
            }

            // Parallax offset — shifts photo left/right based on home screen page
            val maxShift = scaledWidth - surfaceWidth
            val parallaxShift = if (parallaxEnabled) {
                -(xOffset * maxShift)
            } else {
                -(maxShift / 2)
            }

            val top = (surfaceHeight - scaledHeight) / 2f

            val matrix = Matrix()
            matrix.setScale(scaledWidth / bitmap.width, scaledHeight / bitmap.height)
            matrix.postTranslate(parallaxShift, top)

            canvas.drawBitmap(bitmap, matrix, paint)

            // Dark overlay for readability
            val overlayPaint = Paint()
            overlayPaint.color = "#88000000".toColorInt()
            canvas.drawRect(
                0f, 0f,
                surfaceWidth.toFloat(),
                surfaceHeight.toFloat(),
                overlayPaint
            )
        }
    }
}