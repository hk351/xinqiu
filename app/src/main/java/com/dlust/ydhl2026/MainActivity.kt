package com.dlust.ydhl2026

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dlust.ydhl2026.ui.home.HomeScreen
import com.dlust.ydhl2026.ui.screens.LoginScreen
import com.dlust.ydhl2026.ui.screens.SplashScreen
import com.dlust.ydhl2026.ui.theme.Ydhl2026Theme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      Ydhl2026Theme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "splash") {
          composable(route = "splash") {
            SplashScreen(
              onNavigateToLogin = {
                navController.navigate(route = "login") {
                  popUpTo(route = "splash") { inclusive = true }
                }
              }
            )
          }
          composable(route = "login") {
            LoginScreen(
              onLoginSuccess = {
                navController.navigate(route = "home") {
                  popUpTo(route = "login") { inclusive = true }
                }
              }
            )
          }
          composable(route = "home") {
            val context = LocalContext.current
            HomeScreen(
              onOpenPlayer = {
                Toast
                  .makeText(context, "全屏播放器下一轮实现", Toast.LENGTH_SHORT)
                  .show()
              },
              onPlaylistClick = { playlist ->
                Toast
                  .makeText(context, "${playlist.name} 详情页下一轮实现", Toast.LENGTH_SHORT)
                  .show()
              }
            )
          }
        }
      }
    }
  }
}
