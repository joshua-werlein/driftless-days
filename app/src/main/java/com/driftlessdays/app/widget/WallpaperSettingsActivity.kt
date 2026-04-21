package com.driftlessdays.app.widget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.driftlessdays.app.ui.theme.DriftlessDaysTheme
import androidx.core.content.edit

class WallpaperSettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("driftless_prefs", MODE_PRIVATE)

        setContent {
            DriftlessDaysTheme {
                var parallaxEnabled by remember {
                    mutableStateOf(prefs.getBoolean("parallax_enabled", true))
                }
                var selectedCategory by remember {
                    mutableStateOf(prefs.getString("photo_category", "nature") ?: "nature")
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A2E))
                        .padding(24.dp)
                ) {
                    Spacer(modifier = Modifier.height(48.dp))

                    // DD badge + title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(0xFF3C3489), RoundedCornerShape(10.dp))
                        ) {
                            Text(
                                text = "DD",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFAFA9EC)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Driftless Days",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Live wallpaper settings",
                                fontSize = 13.sp,
                                color = Color(0xFF5F5E5A)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    // Parallax toggle
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF16213E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Parallax effect",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Photo shifts as you swipe home screens",
                                    fontSize = 12.sp,
                                    color = Color(0xFF5F5E5A)
                                )
                            }
                            Switch(
                                checked = parallaxEnabled,
                                onCheckedChange = { enabled ->
                                    parallaxEnabled = enabled
                                    prefs.edit {
                                        putBoolean("parallax_enabled", enabled)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF3C3489)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category selection
                    Text(
                        text = "Photo category",
                        fontSize = 13.sp,
                        color = Color(0xFF5F5E5A),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    listOf(
                        "nature" to "Nature",
                        "seasons" to "Seasons",
                        "minimal" to "Minimal"
                    ).forEach { (id, label) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedCategory == id)
                                    Color(0xFF3C3489).copy(alpha = 0.6f)
                                else Color(0xFF16213E)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            onClick = {
                                selectedCategory = id
                                prefs.edit {
                                    putString("photo_category", id)
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 15.sp,
                                    color = Color.White,
                                    fontWeight = if (selectedCategory == id)
                                        FontWeight.Bold else FontWeight.Normal
                                )
                                if (selectedCategory == id) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                Color(0xFFEF9F27),
                                                RoundedCornerShape(4.dp)
                                            )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Calendar widget",
                        fontSize = 13.sp,
                        color = Color(0xFF5F5E5A),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF16213E)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Transparent background",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "See your wallpaper through the widget",
                                    fontSize = 12.sp,
                                    color = Color(0xFF5F5E5A)
                                )
                            }
                            var widgetTransparent by remember {
                                mutableStateOf(prefs.getBoolean("widget_transparent", false))
                            }
                            Switch(
                                checked = widgetTransparent,
                                onCheckedChange = { enabled ->
                                    widgetTransparent = enabled
                                    prefs.edit {
                                        putBoolean("widget_transparent", enabled)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF3C3489)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}