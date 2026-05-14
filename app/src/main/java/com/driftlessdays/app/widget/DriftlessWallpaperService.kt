package com.driftlessdays.app.widget

import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Handler
import android.os.HandlerThread
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.view.animation.DecelerateInterpolator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import android.util.Log
import androidx.core.content.edit
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.core.graphics.toColorInt
import androidx.core.net.toUri

class DriftlessWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = DriftlessWallpaperEngine()

    inner class DriftlessWallpaperEngine : Engine() {

        private val drawThread = HandlerThread("DriftlessDrawThread").also { it.start() }
        private val drawHandler = Handler(drawThread.looper)
        private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        private var photo: Bitmap? = null
        private var surfaceWidth = 0
        private var surfaceHeight = 0
        private var xOffset = 0f
        private var parallaxEnabled = true
        private var offsetAnimator: ValueAnimator? = null
        private val parallaxAmount = 350f
        private var sensorManager: SensorManager? = null
        private var accelerometer: Sensor? = null
        private var filteredAccel = 0f
        private var filteredAccelY = 0f
        private var gravityBaselineY = Float.MAX_VALUE
        private val accelAlpha = 0.85f
        private var lastOffsetX = Float.MAX_VALUE
        private var lastOffsetY = Float.MAX_VALUE
        private val httpClient = OkHttpClient()
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        private val drawRunnable = Runnable { draw() }

        // Runs on drawThread (registered with drawHandler below)
        private val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val magnitude = kotlin.math.sqrt(
                    event.values[0] * event.values[0] +
                            event.values[1] * event.values[1] +
                            event.values[2] * event.values[2]
                )
                if (kotlin.math.abs(magnitude - SensorManager.GRAVITY_EARTH) < 0.8f) return
                if (!parallaxEnabled) return
                if (gravityBaselineY == Float.MAX_VALUE) { gravityBaselineY = event.values[1] }
                filteredAccel = accelAlpha * filteredAccel + (1f - accelAlpha) * event.values[0]
                filteredAccelY = accelAlpha * filteredAccelY + (1f - accelAlpha) * (event.values[1] - gravityBaselineY)
                val newOffsetX = -(filteredAccel / 5f).coerceIn(-1f, 1f) * (parallaxAmount * 0.35f)
                val newOffsetY = -(filteredAccelY / 3f).coerceIn(-1f, 1f) * (parallaxAmount * 0.35f)
                if (kotlin.math.abs(newOffsetX - lastOffsetX) <= 1.5f && kotlin.math.abs(newOffsetY - lastOffsetY) <= 2.5f) return
                lastOffsetX = newOffsetX
                lastOffsetY = newOffsetY
                draw()
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            Log.d("DriftlessParallax", "onCreate isPreview=$isPreview")
            setTouchEventsEnabled(false)
            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
            accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            loadPhoto()
        }

        override fun onDestroy() {
            super.onDestroy()
            sensorManager?.unregisterListener(sensorListener)
            offsetAnimator?.cancel()
            drawHandler.removeCallbacks(drawRunnable)
            drawThread.quit()
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
            drawHandler.post(drawRunnable)
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            drawHandler.removeCallbacks(drawRunnable)
        }

        override fun onOffsetsChanged(
            xOffset: Float,
            yOffset: Float,
            xOffsetStep: Float,
            yOffsetStep: Float,
            xPixelOffset: Int,
            yPixelOffset: Int
        ) {
            Log.d("DriftlessParallax", "xOffset=$xOffset parallaxEnabled=$parallaxEnabled")
            if (parallaxEnabled) {
                // ValueAnimator must run on main thread; it posts draw calls to drawHandler
                offsetAnimator?.cancel()
                offsetAnimator = ValueAnimator.ofFloat(this.xOffset, xOffset).apply {
                    duration = 300
                    interpolator = DecelerateInterpolator()
                    addUpdateListener {
                        this@DriftlessWallpaperEngine.xOffset = it.animatedValue as Float
                        drawHandler.post(drawRunnable)
                    }
                    start()
                }
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            Log.d("DriftlessParallax", "onVisibilityChanged visible=$visible isPreview=$isPreview photo=${photo != null}")
            if (visible) {
                lastOffsetX = Float.MAX_VALUE
                lastOffsetY = Float.MAX_VALUE
                gravityBaselineY = Float.MAX_VALUE
                // Register with drawHandler so sensor callbacks run on the draw thread, not main thread
                sensorManager?.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_GAME, drawHandler)
                drawHandler.post(drawRunnable)
                loadPhoto()
            } else {
                sensorManager?.unregisterListener(sensorListener)
                drawHandler.removeCallbacks(drawRunnable)
            }
        }

        private fun saveBitmapCache(bmp: Bitmap) {
            try {
                val file = java.io.File(cacheDir, "wallpaper_cache.jpg")
                file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
                val prefs = getSharedPreferences("driftless_prefs", MODE_PRIVATE)
                val category = prefs.getString("photo_category", "nature")
                prefs.edit {
                    putString("cache_date", LocalDate.now().toString())
                    putString("cache_category", category)
                }
            } catch (e: Exception) {
                Log.e("DriftlessWallpaper", "Cache save failed", e)
            }
        }

        private fun loadBitmapCache(): Bitmap? {
            return try {
                val prefs = getSharedPreferences("driftless_prefs", MODE_PRIVATE)
                val cacheDate = prefs.getString("cache_date", null)
                val cacheCategory = prefs.getString("cache_category", null)
                val today = LocalDate.now().toString()
                val currentCategory = prefs.getString("photo_category", "nature")
                val file = java.io.File(cacheDir, "wallpaper_cache.jpg")
                if (cacheDate == today && cacheCategory == currentCategory && file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)
                } else null
            } catch (_: Exception) { null }
        }

        private fun loadPhoto() {
            val cached = loadBitmapCache()
            if (cached != null) {
                photo = cached
                drawHandler.post(drawRunnable)
            }

            Log.d("DriftlessParallax", "loadPhoto start isPreview=$isPreview")
            scope.launch(Dispatchers.IO) {
                Log.d("DriftlessParallax", "loadPhoto IO block entered isPreview=$isPreview")
                try {
                    val prefs = getSharedPreferences("driftless_prefs", MODE_PRIVATE)
                    parallaxEnabled = prefs.getBoolean("parallax_enabled", true)
                    val category = prefs.getString("photo_category", "nature") ?: "nature"
                    val effectiveCategory = when (category) {
                        "seasons_calendar" -> "seasons/${getAstronomicalSeason()}"
                        else -> category
                    }
                    // Personal — load from local URI, skip network
                    if (effectiveCategory == "personal") {
                        val uriString = prefs.getString("personal_photo_uri", null)
                        if (uriString != null) {
                            val uri = uriString.toUri()
                            photo = contentResolver.openInputStream(uri)
                                ?.let { BitmapFactory.decodeStream(it) }
                        }
                        drawHandler.post(drawRunnable)
                        return@launch
                    }
                    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val url = "https://driftless-worker.jjwerlein.workers.dev/photo/$effectiveCategory/$today"
                    Log.d("DriftlessParallax", "loadPhoto fetching $url")
                    val request = Request.Builder().url(url).build()
                    val response = httpClient.newCall(request).execute()
                    Log.d("DriftlessParallax", "loadPhoto fetch response code=${response.code}")
                    if (response.isSuccessful) {
                        val bytes = response.body?.bytes()
                        if (bytes != null) {
                            val exif = androidx.exifinterface.media.ExifInterface(bytes.inputStream())
                            val orientation = exif.getAttributeInt(
                                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                            )
                            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            val matrix = Matrix()
                            when (orientation) {
                                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90  -> matrix.postRotate(90f)
                                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                                androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                            }
                            photo = if (!matrix.isIdentity)
                                Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                            else bmp
                            photo?.let { saveBitmapCache(it) }
                        }
                    } else {
                        Log.e("DriftlessWallpaper", "Photo fetch failed: HTTP ${response.code} for $url")
                    }
                    Log.d("DriftlessParallax", "loadPhoto fetch complete photo=${photo != null}")

                    Log.d("DriftlessParallax", "loadPhoto complete isPreview=$isPreview photo=${photo != null}")
                    drawHandler.post(drawRunnable)

                } catch (e: Exception) {
                    Log.e("DriftlessWallpaper", "Failed to load photo", e)
                    drawHandler.post(drawRunnable)
                } catch (t: Throwable) {
                    Log.e("DriftlessWallpaper", "Throwable in loadPhoto", t)
                    drawHandler.post(drawRunnable)
                }
            }
        }

        private fun getAstronomicalSeason(): String {
            val d = LocalDate.now()
            val m = d.monthValue
            val day = d.dayOfMonth
            @Suppress("KotlinConstantConditions")
            return when {
                (m == 3 && day >= 20) || (m == 4) || (m == 5) || (m == 6 && day < 21) -> "spring"
                (m == 6 && day >= 21) || (m == 7) || (m == 8) || (m == 9 && day < 22) -> "summer"
                (m == 9 && day >= 22) || (m == 10) || (m == 11) || (m == 12 && day < 21) -> "fall"
                else -> "winter"
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
            Log.d("DriftlessParallax", "drawFrame photo=${photo != null} surfaceWidth=$surfaceWidth surfaceHeight=$surfaceHeight")
            // Fill background
            canvas.drawColor("#1A1A2E".toColorInt())

            val bitmap = photo ?: return
            Log.d("DriftlessParallax", "drawFrame bitmap=${bitmap.width}x${bitmap.height} surface=${surfaceWidth}x${surfaceHeight}")

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
                if (scaledWidth < surfaceWidth + parallaxAmount * 2) {
                    scaledWidth = surfaceWidth + parallaxAmount * 2
                    scaledHeight = scaledWidth / bitmapAspect
                }
            }

            if (parallaxEnabled) {
                val verticalBudget = parallaxAmount * 0.35f * 2f
                val overflowScale = (scaledHeight + verticalBudget) / scaledHeight
                scaledWidth *= overflowScale
                scaledHeight *= overflowScale
            }

            // Parallax offset — page-based + accelerometer tilt
            val maxShift = scaledWidth - surfaceWidth
            val accelShift = -(filteredAccel / 5f).coerceIn(-1f, 1f) * (parallaxAmount * 0.4f)
            val rawShift = if (parallaxEnabled) {
                -(xOffset * maxShift) + accelShift
            } else {
                -(maxShift / 2)
            }
            val parallaxShift = rawShift.coerceIn(-maxShift, 0f)

            val accelShiftY = -(filteredAccelY / 3f).coerceIn(-1f, 1f) * (parallaxAmount * 0.35f)
            val maxVerticalShift = ((scaledHeight - surfaceHeight) / 2f).coerceAtLeast(0f)
            val verticalShift = if (parallaxEnabled) accelShiftY.coerceIn(-maxVerticalShift, maxVerticalShift) else 0f
            val top = (surfaceHeight - scaledHeight) / 2f + verticalShift

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