package com.dlust.ydhl2026

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dlust.ydhl2026.ui.PlayerViewModelFactory
import com.dlust.ydhl2026.ui.home.HomeScreen
import com.dlust.ydhl2026.ui.local.LocalMusicScreen
import com.dlust.ydhl2026.ui.player.NowPlayingScreen
import com.dlust.ydhl2026.ui.player.PlayerViewModel
import com.dlust.ydhl2026.ui.playbackEngine
import com.dlust.ydhl2026.ui.playerBackdropStore
import com.dlust.ydhl2026.ui.playlist.PlaylistScreen
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
        val playerViewModel: PlayerViewModel =
          viewModel(factory = PlayerViewModelFactory(playbackEngine(), playerBackdropStore()))
        NavHost(navController = navController, startDestination = "splash") {
          composable(route = "splash") {
            SplashScreen(
              onFinished = {
                // 启动后直接进主界面，登录改为首页头像里的可选操作
                navController.navigate(route = "home") {
                  popUpTo(route = "splash") { inclusive = true }
                }
              }
            )
          }
          composable(route = "login") {
            LoginScreen(
              onLoginSuccess = { navController.popBackStack() },
              onClose = { navController.popBackStack() }
            )
          }
          composable(route = "home") {
            HomeScreen(
              playerViewModel = playerViewModel,
              onPlaylistClick = { playlist ->
                navController.navigate(route = "playlist/${playlist.id}")
              },
              onOpenLocalMusic = { navController.navigate(route = "local") },
              onOpenProfile = { navController.navigate(route = "login") }
            )
          }
          composable(route = "local") {
            LocalMusicScreen(
              playerViewModel = playerViewModel,
              onBack = { navController.popBackStack() },
              onOpenPlayer = { navController.navigate(route = "player") }
            )
          }
          composable(
            route = "playlist/{playlistId}",
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
          ) { entry ->
            val playlistId = entry.arguments?.getLong("playlistId") ?: 0L
            PlaylistScreen(
              playlistId = playlistId,
              playerViewModel = playerViewModel,
              onBack = { navController.popBackStack() },
              onOpenPlayer = { navController.navigate(route = "player") }
            )
          }
          composable(route = "player") {
            NowPlayingScreen(
              playerViewModel = playerViewModel,
              onCollapse = { navController.popBackStack() }
            )
          }
        }
      }
    }
  }
}
