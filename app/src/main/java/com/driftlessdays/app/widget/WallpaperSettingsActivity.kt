package com.driftlessdays.app.widget

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.activity.result.PickVisualMediaRequest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent

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

                // Photo picker — no permissions needed (Android Photo Picker API)
                val photoPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri: Uri? ->
                    if (uri != null) {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        prefs.edit { putString("personal_photo_uri", uri.toString()) }
                        selectedCategory = "personal"
                        prefs.edit { putString("photo_category", "personal") }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1A1A2E))
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(modifier = Modifier.height(48.dp))

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

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E)),
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
                                    prefs.edit { putBoolean("parallax_enabled", enabled) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF3C3489)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Photo category",
                        fontSize = 13.sp,
                        color = Color(0xFF5F5E5A),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    listOf(
                        "nature"   to "Nature",
                        "panorama" to "Panorama",
                        "minimal"  to "Minimal"
                    ).forEach { (id, label) ->
                        CategoryCard(
                            label = label,
                            subtitle = null,
                            selected = selectedCategory == id,
                            onClick = {
                                selectedCategory = id
                                prefs.edit { putString("photo_category", id) }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    CategoryCard(
                        label = "Personal",
                        subtitle = if (selectedCategory == "personal") "Tap to change photo"
                        else "Choose a photo from your gallery",
                        selected = selectedCategory == "personal",
                        onClick = {
                            photoPicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Seasons",
                        fontSize = 13.sp,
                        color = Color(0xFF5F5E5A),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    listOf(
                        "seasons"          to Pair("Auto", "Switches by current month"),
                        "seasons_calendar" to Pair("Follow Calendar", "Switches on astronomical start dates"),
                        "seasons/spring"   to Pair("Spring", "Always show spring photos"),
                        "seasons/summer"   to Pair("Summer", "Always show summer photos"),
                        "seasons/fall"     to Pair("Fall", "Always show fall photos"),
                        "seasons/winter"   to Pair("Winter", "Always show winter photos")
                    ).forEach { (id, pair) ->
                        val (label, subtitle) = pair
                        CategoryCard(
                            label = label,
                            subtitle = subtitle,
                            selected = selectedCategory == id,
                            onClick = {
                                selectedCategory = id
                                prefs.edit { putString("photo_category", id) }
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Calendar widget",
                        fontSize = 13.sp,
                        color = Color(0xFF5F5E5A),
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF16213E)),
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
                                    prefs.edit { putBoolean("widget_transparent", enabled) }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF3C3489)
                                )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                                putExtra(
                                    WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                                    ComponentName(
                                        "com.driftlessdays.app",
                                        "com.driftlessdays.app.widget.DriftlessWallpaperService"
                                    )
                                )
                            }
                            startActivity(intent)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3C3489)
                        )
                    ) {
                        Text(
                            text = "Set as Wallpaper",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(
    label: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected)
                Color(0xFF3C3489).copy(alpha = 0.6f)
            else Color(0xFF16213E)
        ),
        shape = RoundedCornerShape(10.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color(0xFF5F5E5A)
                    )
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFEF9F27), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}
