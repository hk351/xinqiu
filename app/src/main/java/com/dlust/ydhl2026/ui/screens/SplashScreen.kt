package com.dlust.ydhl2026.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val SplashBackground = Color(0xFF1B3FA0)
private val OnSplash = Color.White
private val OnSplashDim = Color.White.copy(alpha = 0.55f)

@Composable
fun SplashScreen(onNavigateToLogin: () -> Unit) {
  LaunchedEffect(Unit) {
    delay(2000)
    onNavigateToLogin()
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(SplashBackground)
  ) {
    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .navigationBarsPadding()
        .padding(bottom = 56.dp)
        .fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = "Echo • 听见每一次循环",
        color = OnSplash,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(Modifier.height(12.dp))
      Box(
        modifier = Modifier
          .width(160.dp)
          .height(1.dp)
          .background(OnSplashDim)
      )
      Spacer(Modifier.height(10.dp))
      Text(
        text = "Echo Music · v1.0",
        color = OnSplashDim,
        fontSize = 11.sp
      )
    }
  }
}
