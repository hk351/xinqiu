package com.dlust.ydhl2026

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dlust.ydhl2026.data.settings.ThemeMode
import com.dlust.ydhl2026.ui.PlayerViewModelFactory
import com.dlust.ydhl2026.ui.home.HomeScreen
import com.dlust.ydhl2026.ui.local.LocalMusicScreen
import com.dlust.ydhl2026.ui.player.NowPlayingScreen
import com.dlust.ydhl2026.ui.player.PlayerViewModel
import com.dlust.ydhl2026.ui.lyricsCache
import com.dlust.ydhl2026.ui.musicRepository
import com.dlust.ydhl2026.ui.onlineLyricsSource
import com.dlust.ydhl2026.ui.playHistoryStore
import com.dlust.ydhl2026.ui.playbackEngine
import com.dlust.ydhl2026.ui.playerBackdropStore
import com.dlust.ydhl2026.ui.playlist.PlaylistScreen
import com.dlust.ydhl2026.ui.screens.LoginScreen
import com.dlust.ydhl2026.ui.screens.SplashScreen
import com.dlust.ydhl2026.ui.theme.EchoBackground
import com.dlust.ydhl2026.ui.theme.EchoBackgroundDark
import com.dlust.ydhl2026.ui.theme.LocalEchoDark
import com.dlust.ydhl2026.ui.theme.Ydhl2026Theme
import com.dlust.ydhl2026.ui.themeModeStore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val themeStore = remember { themeModeStore() }
      val themeMode by themeStore.mode.collectAsStateWithLifecycle()
      val isDark = themeMode.resolveDark(isSystemInDarkTheme())

      // 应用内主题切换后，系统状态栏/导航栏的图标颜色和底色也要同步，
      // 否则 3 键导航栏会一直是系统主题的底色，和页面割裂
      LaunchedEffect(isDark) {
        // Android 15+ 已经强制 edge-to-edge，这行会被系统忽略，页面底色会直接铺到导航栏
        @Suppress("DEPRECATION")
        window.navigationBarColor = (if (isDark) EchoBackgroundDark else EchoBackground).toArgb()
        WindowCompat.getInsetsController(window, window.decorView).apply {
          isAppearanceLightStatusBars = !isDark
          isAppearanceLightNavigationBars = !isDark
        }
      }

      Ydhl2026Theme(darkTheme = isDark) {
        CompositionLocalProvider(LocalEchoDark provides isDark) {
          val navController = rememberNavController()
          // 播放状态要在首页（迷你条）和全屏播放器之间共享，所以提升到 Activity 作用域
          val playerViewModel: PlayerViewModel = viewModel(
            factory = PlayerViewModelFactory(
              engine = playbackEngine(),
              backdropStore = playerBackdropStore(),
              repository = musicRepository(),
              history = playHistoryStore(),
              lyricsSource = onlineLyricsSource(),
              lyricsCache = lyricsCache()
            )
          )
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
                isDark = isDark,
                onToggleDark = {
                  themeStore.setMode(if (isDark) ThemeMode.LIGHT else ThemeMode.DARK)
                },
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
}
