package com.driftlessdays.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ColorDeepViolet = Color(0xFF3C3489)
private val ColorSoftLavender = Color(0xFFAFA9EC)
//private val ColorAmber = Color(0xFFEF9F27)
private val ColorSlate = Color(0xFF5F5E5A)
private val ColorBackground = Color(0xFF1A1A2E)
//private val ColorSurface = Color(0xFF16213E)

@Composable
fun SignInScreen(
    onSignInClick: () -> Unit,
    onSkipClick: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
        ) {

            // DD monogram
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .background(ColorDeepViolet, RoundedCornerShape(24.dp))
            ) {
                Text(
                    text = "DD",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorSoftLavender
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Driftless Days",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = ColorSoftLavender
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Days worth looking at.",
                fontSize = 16.sp,
                color = ColorSlate
            )

            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Connect your Google Calendar to see your events alongside Wisconsin's most beautiful places.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Google Sign In button
            Button(
                onClick = onSignInClick,
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorDeepViolet,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = ColorSoftLavender,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Sign in with Google",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Skip for now
            TextButton(onClick = onSkipClick) {
                Text(
                    text = "Skip for now",
                    fontSize = 14.sp,
                    color = ColorSlate
                )
            }

            // Error message
            errorMessage?.let {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF3D1515)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = it,
                        color = Color(0xFFFF6B6B),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}