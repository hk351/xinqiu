package com.dlust.ydhl2026

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.dlust.ydhl2026.ui.home.HomeScreen
import com.dlust.ydhl2026.ui.player.FullPlayerScreen
import com.dlust.ydhl2026.ui.player.PlayerViewModel
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
        // 播放状态要在首页（迷你条）和全屏播放器之间共享，所以提升到 Activity 作用域
        val playerViewModel: PlayerViewModel = viewModel()
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
              playerViewModel = playerViewModel,
              onOpenPlayer = { navController.navigate(route = "player") },
              onPlaylistClick = { playlist ->
                Toast
                  .makeText(context, "${playlist.name} 详情页下一轮实现", Toast.LENGTH_SHORT)
                  .show()
              }
            )
          }
          composable(route = "player") {
            FullPlayerScreen(
              playerViewModel = playerViewModel,
              onCollapse = { navController.popBackStack() }
            )
          }
        }
      }
    }
  }
}
