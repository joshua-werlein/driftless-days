package com.driftlessdays.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.driftlessdays.app.MainActivity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

class DriftlessCalendarWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("driftless_prefs", Context.MODE_PRIVATE)
        val transparent = prefs.getBoolean("widget_transparent", false)
        provideContent { CalendarWidgetContent(transparent = transparent) }
    }
}

@Composable
private fun CalendarWidgetContent(transparent: Boolean) {
    val today = LocalDate.now()
    val currentMonth = YearMonth.now()
    val context = LocalContext.current
    val bgColor = if (transparent) Color(0x33000000) else Color(0xEE1A1A2E)

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .cornerRadius(16.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(10.dp)
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = GlanceModifier
                        .size(28.dp)
                        .background(Color(0xFF3C3489))
                        .cornerRadius(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DD",
                        style = TextStyle(
                            color = ColorProvider(day = Color(0xFFAFA9EC), night = Color(0xFFAFA9EC)),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    text = currentMonth.month.getDisplayName(
                        JTextStyle.FULL, Locale.getDefault()
                    ) + " " + currentMonth.year,
                    style = TextStyle(
                        color = ColorProvider(day = Color.White, night = Color.White),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = GlanceModifier.height(8.dp))

            Row(modifier = GlanceModifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Box(
                        modifier = GlanceModifier.defaultWeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            style = TextStyle(
                                color = ColorProvider(day = Color(0xFF5F5E5A), night = Color(0xFF5F5E5A)),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }

            Spacer(modifier = GlanceModifier.height(4.dp))

            val firstDayOfMonth = currentMonth.atDay(1)
            val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7
            val daysInMonth = currentMonth.lengthOfMonth()
            val rows = ((dayOfWeekOffset + daysInMonth) + 6) / 7

            for (row in 0 until rows) {
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                ) {
                    for (col in 0..6) {
                        val dayNum = row * 7 + col - dayOfWeekOffset + 1
                        Box(
                            modifier = GlanceModifier.defaultWeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (dayNum in 1..daysInMonth) {
                                val isToday = dayNum == today.dayOfMonth &&
                                        currentMonth.month == today.month &&
                                        currentMonth.year == today.year

                                val cellBg = if (isToday) Color(0xFF3C3489)
                                else Color.Transparent

                                val dateString = currentMonth.atDay(dayNum)
                                    .format(DateTimeFormatter.ISO_LOCAL_DATE)

                                val launchIntent = Intent(
                                    context, MainActivity::class.java
                                ).apply {
                                    putExtra("target_date", dateString)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                                }

                                Box(
                                    modifier = GlanceModifier
                                        .size(28.dp)
                                        .background(cellBg)
                                        .cornerRadius(14.dp)
                                        .clickable(actionStartActivity(launchIntent)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayNum.toString(),
                                        style = TextStyle(
                                            color = if (isToday)
                                                ColorProvider(day = Color(0xFFEF9F27), night = Color(0xFFEF9F27))
                                            else
                                                ColorProvider(day = Color.White, night = Color.White)
                                            ,
                                            fontSize = 11.sp,
                                            fontWeight = if (isToday)
                                                FontWeight.Bold else FontWeight.Normal,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

class DriftlessCalendarWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DriftlessCalendarWidget()
}