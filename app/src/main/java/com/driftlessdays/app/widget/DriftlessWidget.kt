package com.driftlessdays.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.driftlessdays.app.MainActivity
import java.time.LocalDate
import java.time.format.TextStyle as JTextStyle
import java.util.Locale
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.width
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class DriftlessWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent()
        }
    }
}

@Composable
private fun WidgetContent() {
    val today = LocalDate.now()
    val dayName = today.dayOfWeek.getDisplayName(JTextStyle.FULL, Locale.getDefault())
    val monthName = today.month.getDisplayName(JTextStyle.SHORT, Locale.getDefault())
    val dayNum = today.dayOfMonth.toString()

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E))
            .clickable(actionStartActivity<MainActivity>()),
        contentAlignment = Alignment.BottomStart
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // DD monogram badge
            Box(
                modifier = GlanceModifier
                    .width(36.dp)
                    .height(36.dp)
                    .background(Color(0xFF3C3489))
                    .cornerRadius(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DD",
                    style = TextStyle(
                        color = ColorProvider(day = Color(0xFFAFA9EC), night = Color(0xFFAFA9EC)),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            // Day number
            Text(
                text = dayNum,
                style = TextStyle(
                    color = ColorProvider(day = Color.White, night = Color.White),

                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            // Day name + month
            Text(
                text = "$dayName, $monthName ${today.year}",
                style = TextStyle(
                    color = ColorProvider(day = Color(0xFFAFA9EC), night = Color(0xFFAFA9EC)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            )

            Spacer(modifier = GlanceModifier.height(4.dp))

            // Tagline
            Text(
                text = "Days worth looking at.",
                style = TextStyle(
                    color = ColorProvider(day = Color(0xFF5F5E5A), night = Color(0xFF5F5E5A)),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Normal
                )
            )
        }
    }
}

class DriftlessWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DriftlessWidget()
}