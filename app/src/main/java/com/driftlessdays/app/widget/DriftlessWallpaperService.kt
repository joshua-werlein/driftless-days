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
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import android.view.animation.DecelerateInterpolator
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

        private val sensorListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (!parallaxEnabled) return
                if (gravityBaselineY == Float.MAX_VALUE) { gravityBaselineY = event.values[1] }
                filteredAccel = accelAlpha * filteredAccel + (1f - accelAlpha) * event.values[0]
                filteredAccelY = accelAlpha * filteredAccelY + (1f - accelAlpha) * (event.values[1] - gravityBaselineY)
                val newOffsetX = -(filteredAccel / 5f).coerceIn(-1f, 1f) * (parallaxAmount * 0.35f)
                val newOffsetY = -(filteredAccelY / 3f).coerceIn(-1f, 1f) * (parallaxAmount * 0.35f)
                if (kotlin.math.abs(newOffsetX - lastOffsetX) <= 1.5f && kotlin.math.abs(newOffsetY - lastOffsetY) <= 0.8f) return
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
            Log.d("DriftlessParallax", "xOffset=$xOffset parallaxEnabled=$parallaxEnabled")
            if (parallaxEnabled) {
                offsetAnimator?.cancel()
                offsetAnimator = ValueAnimator.ofFloat(this.xOffset, xOffset).apply {
                    duration = 300
                    interpolator = DecelerateInterpolator()
                    addUpdateListener {
                        this@DriftlessWallpaperEngine.xOffset = it.animatedValue as Float
                        draw()
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
                sensorManager?.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
                draw()
                loadPhoto()
            } else {
                sensorManager?.unregisterListener(sensorListener)
                handler.removeCallbacks(drawRunnable)
            }
        }

        private fun loadPhoto() {
            Log.d("DriftlessParallax", "loadPhoto start isPreview=$isPreview")
            scope.launch(Dispatchers.IO) {
                Log.d("DriftlessParallax", "loadPhoto IO block entered isPreview=$isPreview")
                try {
                    val prefs = getSharedPreferences("driftless_prefs", MODE_PRIVATE)
                    parallaxEnabled = prefs.getBoolean("parallax_enabled", true)
                    val category = prefs.getString("photo_category", "nature") ?: "nature"

                    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val url = "https://driftless-worker.jjwerlein.workers.dev/photo/$category/$today"
                    Log.d("DriftlessParallax", "loadPhoto fetching $url")
                    val request = Request.Builder().url(url).build()
                    val response = httpClient.newCall(request).execute()
                    Log.d("DriftlessParallax", "loadPhoto fetch response code=${response.code}")
                    if (response.isSuccessful) {
                        photo = response.body?.byteStream()?.let { BitmapFactory.decodeStream(it) }
                    } else {
                        Log.e("DriftlessWallpaper", "Photo fetch failed: HTTP ${response.code} for $url")
                    }
                    Log.d("DriftlessParallax", "loadPhoto fetch complete photo=${photo != null}")

                    Log.d("DriftlessParallax", "loadPhoto complete isPreview=$isPreview photo=${photo != null}")
                    launch(Dispatchers.Main) { draw() }

                } catch (e: Exception) {
                    Log.e("DriftlessWallpaper", "Failed to load photo", e)
                    launch(Dispatchers.Main) { draw() }
                } catch (t: Throwable) {
                    Log.e("DriftlessWallpaper", "Throwable in loadPhoto", t)
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