package com.driftlessdays.app.ui.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.driftlessdays.app.data.remote.CalendarEvent
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private val ColorDeepViolet = Color(0xFF3C3489)
private val ColorSoftLavender = Color(0xFFAFA9EC)
private val ColorAmber = Color(0xFFEF9F27)
private val ColorSlate = Color(0xFF5F5E5A)
private val ColorSurface = Color(0xCC16213E)

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel(),
    accountEmail: String? = null,
    targetDate: String? = null,
    onSetWallpaper: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(accountEmail) {
        if (targetDate != null) viewModel.navigateToDate(targetDate)
        if (accountEmail != null) viewModel.loadEvents(accountEmail)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Animated background photo
        val photoUrl = if (uiState.currentView == CalendarView.DAY)
            uiState.dayPhoto?.url ?: uiState.monthPhoto?.url
        else
            uiState.monthPhoto?.url

        AnimatedContent(
            targetState = photoUrl,
            transitionSpec = {
                fadeIn(animationSpec = tween(800)) togetherWith
                        fadeOut(animationSpec = tween(800))
            },
            label = "photo_transition"
        ) { url ->
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1A1A2E)))
            }
        }

        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xCC0D0D1A),
                            Color(0xAA0D0D1A),
                            Color(0xEE0D0D1A)
                        )
                    )
                )
        )

        // Main content with slide transition between month and day
        AnimatedContent(
            targetState = uiState.currentView,
            transitionSpec = {
                if (targetState == CalendarView.DAY) {
                    slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(400)
                    ) togetherWith fadeOut(tween(200))
                } else {
                    fadeIn(tween(300)) togetherWith
                            slideOutVertically(
                                targetOffsetY = { it },
                                animationSpec = tween(400)
                            )
                }
            },
            label = "view_transition"
        ) { view ->
            when (view) {
                CalendarView.MONTH -> MonthView(
                    uiState = uiState,
                    onPrevious = { viewModel.onPreviousMonth() },
                    onNext = { viewModel.onNextMonth() },
                    onDateSelected = { viewModel.onDateSelected(it) },
                    onSettingsClick = { viewModel.togglePhotoSettings() },
                    onSetWallpaper = { onSetWallpaper() },
                    onSignOut = { onSignOut() },
                    getEventsForDate = { viewModel.getEventsForDate(it) }
                )
                CalendarView.DAY -> DayView(
                    uiState = uiState,
                    onBack = { viewModel.onBackToMonth() },
                    onSettingsClick = { viewModel.togglePhotoSettings() },
                    events = viewModel.getEventsForDate(uiState.selectedDate)
                )
            }
        }

        // Photo settings sheet
        if (uiState.showPhotoSettings) {
            PhotoSettingsSheet(
                currentMode = uiState.photoMode,
                onModeSelected = { viewModel.setPhotoMode(it) },
                onDismiss = { viewModel.togglePhotoSettings() }
            )
        }
    }
}

@Composable
private fun MonthView(
    uiState: CalendarUiState,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onSettingsClick: () -> Unit,
    onSetWallpaper: () -> Unit,
    onSignOut: () -> Unit,
    getEventsForDate: (LocalDate) -> List<CalendarEvent>
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffset > 100f) onPrevious()
                        else if (dragOffset < -100f) onNext()
                        dragOffset = 0f
                    },
                    onHorizontalDrag = { _, delta ->
                        dragOffset += delta
                    }
                )
            }
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // DD badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(ColorDeepViolet, RoundedCornerShape(8.dp))
            ) {
                Text(
                    text = "DD",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorSoftLavender
                )
            }

            // Month + year
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = uiState.currentMonth.month.getDisplayName(
                        TextStyle.FULL, Locale.getDefault()
                    ),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = uiState.currentMonth.year.toString(),
                    fontSize = 13.sp,
                    color = ColorSlate
                )
            }

            // Nav + overflow menu
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Text(text = "‹", fontSize = 28.sp, color = ColorSoftLavender)
                }
                IconButton(onClick = onNext) {
                    Text(text = "›", fontSize = 28.sp, color = ColorSoftLavender)
                }
                Box {
                    var menuExpanded by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuExpanded = true }) {
                        Text(text = "⋮", fontSize = 22.sp, color = ColorSoftLavender)
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.background(Color(0xFF16213E))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Photo settings",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onSettingsClick()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Set as wallpaper",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onSetWallpaper()
                            }
                        )
                        HorizontalDivider(color = ColorSlate.copy(alpha = 0.3f))
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Sign out",
                                    color = Color(0xFFFF6B6B),
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onSignOut()
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Day of week header
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = ColorSlate
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Calendar grid — fills remaining space
        ImmersiveCalendarGrid(
            currentMonth = uiState.currentMonth,
            selectedDate = uiState.selectedDate,
            today = uiState.today,
            onDateSelected = onDateSelected,
            getEventsForDate = getEventsForDate,
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ImmersiveCalendarGrid(
    currentMonth: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    getEventsForDate: (LocalDate) -> List<CalendarEvent>,
    modifier: Modifier = Modifier
) {
    val firstDayOfMonth = currentMonth.atDay(1)
    val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7
    val daysInMonth = currentMonth.lengthOfMonth()
    val rows = ((dayOfWeekOffset + daysInMonth) + 6) / 7

    Column(modifier = modifier.fillMaxWidth()) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                for (col in 0..6) {
                    val dayNumber = row * 7 + col - dayOfWeekOffset + 1
                    if (dayNumber !in 1..daysInMonth) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        val date = currentMonth.atDay(dayNumber)
                        val hasEvents = getEventsForDate(date).isNotEmpty()
                        ImmersiveDayCell(
                            day = dayNumber,
                            isSelected = date == selectedDate,
                            isToday = date == today,
                            hasEvents = hasEvents,
                            modifier = Modifier.weight(1f),
                            onClick = { onDateSelected(date) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImmersiveDayCell(
    day: Int,
    isSelected: Boolean,
    isToday: Boolean,
    hasEvents: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor = when {
        isSelected -> ColorDeepViolet.copy(alpha = 0.85f)
        isToday -> Color(0xFF0F3460).copy(alpha = 0.7f)
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> Color.White
        isToday -> ColorAmber
        else -> Color.White.copy(alpha = 0.9f)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .clickable { onClick() }
    ) {
        Text(
            text = day.toString(),
            fontSize = 16.sp,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = textColor,
            textAlign = TextAlign.Center
        )
        // Event dot
        if (hasEvents) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(ColorAmber, CircleShape)
            )
        } else {
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun DayView(
    uiState: CalendarUiState,
    onBack: () -> Unit,
    onSettingsClick: () -> Unit,
    events: List<CalendarEvent>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ColorSoftLavender
                )
            }

            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Photo settings",
                    tint = ColorSoftLavender,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large date display
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = uiState.selectedDate.dayOfMonth.toString(),
                fontSize = 80.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                lineHeight = 80.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = uiState.selectedDate.dayOfWeek.getDisplayName(
                        TextStyle.FULL, Locale.getDefault()
                    ),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorSoftLavender
                )
                Text(
                    text = "${uiState.selectedDate.month.getDisplayName(
                        TextStyle.FULL, Locale.getDefault()
                    )} ${uiState.selectedDate.year}",
                    fontSize = 14.sp,
                    color = ColorSlate
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Events list
        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No events today",
                    fontSize = 16.sp,
                    color = ColorSlate
                )
            }
        } else {
            Text(
                text = "${events.size} event${if (events.size > 1) "s" else ""}",
                fontSize = 13.sp,
                color = ColorAmber,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(events) { event ->
                    EventCard(event = event)
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: CalendarEvent) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ColorSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(ColorAmber, CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = event.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                if (!event.isAllDay && event.startTime != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (event.endTime != null)
                            "${event.startTime} – ${event.endTime}"
                        else event.startTime,
                        fontSize = 12.sp,
                        color = ColorSoftLavender
                    )
                } else {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "All day",
                        fontSize = 12.sp,
                        color = ColorSlate
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoSettingsSheet(
    currentMode: PhotoMode,
    onModeSelected: (PhotoMode) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { },
            colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E)),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Background photo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Choose how photos change when viewing days",
                    fontSize = 13.sp,
                    color = ColorSlate
                )

                Spacer(modifier = Modifier.height(20.dp))

                PhotoModeOption(
                    title = "Same as month",
                    description = "One consistent photo for the whole month",
                    isSelected = currentMode == PhotoMode.SAME_AS_MONTH,
                    onClick = { onModeSelected(PhotoMode.SAME_AS_MONTH) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                PhotoModeOption(
                    title = "Date specific",
                    description = "A different photo for each day",
                    isSelected = currentMode == PhotoMode.DATE_SPECIFIC,
                    onClick = { onModeSelected(PhotoMode.DATE_SPECIFIC) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                PhotoModeOption(
                    title = "Random",
                    description = "Surprise me with a new photo each time",
                    isSelected = currentMode == PhotoMode.RANDOM,
                    onClick = { onModeSelected(PhotoMode.RANDOM) }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun PhotoModeOption(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                ColorDeepViolet.copy(alpha = 0.6f)
            else
                Color(0xFF0F3460).copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(10.dp),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(1.5.dp, ColorSoftLavender)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = ColorSlate
                )
            }
            if (isSelected) {
                Spacer(modifier = Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(ColorAmber, CircleShape)
                )
            }
        }
    }
}