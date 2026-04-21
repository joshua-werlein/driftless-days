package com.driftlessdays.app.widget

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.driftlessdays.app.MainActivity
import java.time.LocalDate
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

class DriftlessPhotoWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val bitmap = WidgetPhotoLoader.loadTodayPhoto()
        if (bitmap != null) WidgetPhotoLoader.saveBitmapToCache(context, bitmap)
        val cachedBitmap = WidgetPhotoLoader.loadBitmapFromCache(context)
        provideContent { PhotoWidgetContent(cachedBitmap) }
    }
}

@Composable
private fun PhotoWidgetContent(photo: Bitmap?) {
    val today = LocalDate.now()
    val dayName = today.dayOfWeek.getDisplayName(JTextStyle.FULL, Locale.getDefault())
    val monthName = today.month.getDisplayName(JTextStyle.FULL, Locale.getDefault())
    val dayNum = today.dayOfMonth.toString()

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.BottomStart
    ) {
        if (photo != null) {
            Image(
                provider = ImageProvider(photo),
                contentDescription = "Today's nature photo",
                contentScale = ContentScale.Crop,
                modifier = GlanceModifier.fillMaxSize()
            )
        } else {
            Box(modifier = GlanceModifier.fillMaxSize().background(Color(0xFF1A1A2E))) {}
        }

        Box(modifier = GlanceModifier.fillMaxSize().background(Color(0xCC000000))) {}

        Column(
            modifier = GlanceModifier.fillMaxSize().padding(14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Box(
                modifier = GlanceModifier
                    .width(32.dp)
                    .height(32.dp)
                    .background(Color(0xFF3C3489))
                    .cornerRadius(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DD",
                    style = TextStyle(
                        color = ColorProvider(day = Color(0xFFAFA9EC), night = Color(0xFFAFA9EC)),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(6.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = dayNum,
                    style = TextStyle(
                        color = ColorProvider(day = Color.White, night = Color.White),
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = GlanceModifier.width(10.dp))
                Column {
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = dayName,
                        style = TextStyle(
                            color = ColorProvider(day = Color(0xFFAFA9EC), night = Color(0xFFAFA9EC)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "$monthName ${today.year}",
                        style = TextStyle(
                            color = ColorProvider(day = Color(0xFFAFA9EC), night = Color(0xFFAFA9EC)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            Text(
                text = "Days worth looking at.",
                style = TextStyle(
                    ColorProvider(day = Color(0x99FFFFFF), night = Color(0x99FFFFFF)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }
    }
}

class DriftlessPhotoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DriftlessPhotoWidget()
}