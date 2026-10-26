# Echo 首页（歌单页）实现规划

## Context

项目 ydhl2026（已定名 **Echo**）已完成启动页、登录页，`MainScreen.kt` 目前只是 "Hello World" 占位。依据课程原型文档《19-流媒体音乐播放器-学生版》**第2页 图2「歌单页线框图」** 与第 9-10 页「阶段三：架构与导航」，本轮要把首页做成真实的 MVVM + UDF 结构：

- 顶部 3 胶囊 Tab（🎵歌单 / 🔎搜索 / 💚收藏）
- 「我的歌单」LazyVerticalGrid 2 列网格
- 「最近播放」LazyRow 横滑
- 全局 MiniPlayerBar（`Scaffold.bottomBar` 槽位，切换 Tab 不能丢失 —— PDF 明确标注的核心考核点）
- ViewModel + StateFlow + 单向数据流（UDF）、手动 DI（Application 持 Repository，构造函数注入，**不用 Hilt**）

**本轮定位**：只做"首页 + 迷你播放条"，播放走 Mock（无真实声音）。全屏播放器（图3）、播放队列（图5）、Media3、Room/DataStore 均留给后续阶段，但接口先留好，换实现时只改 Repository/PlayerViewModel。

## 关键技术决策

| 决策点 | 结论 | 理由 |
|---|---|---|
| 首页形态 | **顶部 3 胶囊 Tab + 底部全局 MiniPlayerBar** | 严格贴合图2；MiniBar 放 `Scaffold.bottomBar`，不在页面内部 |
| 播放能力 | 纯 Mock 假播放（图标切换 + 进度**不**自动推进） | 符合 PDF 阶段二/三；真实播放属阶段四 |
| 封面素材 | Compose 纯绘制（由 id 派生稳定配色） | 零图片素材、零网络，与现有启动页/吉祥物的绘制风格一致 |
| 包结构 | 新增 `data/`、`EchoApp`、`ui/home`、`ui/player`；**splash/login 保持 `ui/screens` 不动** | 避免大范围搬家带来的回归风险 |
| 新增依赖 | 仅 `lifecycle-viewmodel-compose` + `lifecycle-runtime-compose` | 不引入 Coil / Room / DataStore / Media3（后续阶段再加） |

## 一、依赖与工程配置

**`gradle/libs.versions.toml`**（在 `[versions]`/`[libraries]` 追加，与现有 `lifecycleRuntimeKtx = "2.11.0"` 同版本）

```toml
lifecycleViewmodelCompose = "2.11.0"
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }
androidx-lifecycle-runtime-compose  = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleViewmodelCompose" }
```

**`app/build.gradle.kts`** dependencies 追加两行（`collectAsStateWithLifecycle()` 需要 runtime-compose）。

**`AndroidManifest.xml`**：`<application>` 增加 `android:name=".EchoApp"`。

## 二、数据层（新增 `data/`）

**`data/model/Song.kt`** —— 按 PDF 第5页字段原样：`id / title / artist / album? / albumArtUrl? / durationMs / source / sourceUrl / lastModifiedAt`。

**`data/model/Playlist.kt`** —— `id: Long / name / coverUrl? / createdAt / lastModifiedAt / songCount`（`songCount` 供卡片显示"12首"）。

**`data/model/PlaybackState.kt`** —— PDF 版持久化模型（`currentSongId? / currentPositionMs / isPlaying / repeatMode`），本轮只定义不落盘，供阶段五 DataStore 使用。

**`data/model/LyricLine.kt`** —— `timeMs / text`（本轮定义，供图3/图5歌词用）。

**`data/repository/MusicRepository.kt`** —— 接口，方法签名如下（后续换真实数据源只替换实现）：

```kotlin
fun playlists(): Flow<List<Playlist>>
fun recentSongs(): Flow<List<Song>>
fun songsOf(playlistId: Long): Flow<List<Song>>
suspend fun search(query: String): List<Song>
```

**`data/repository/MockMusicRepository.kt`** —— 实现上述接口，内置 **4 个歌单**（跑步歌单/深夜电台/运动必备/怀旧金曲）+ **10 首 Mock 歌曲**（PDF 阶段二要求"10 首 Mock"）。全部用 `flowOf(...)` 返回，不引入数据库。

## 三、手动 DI

**`EchoApp.kt`**（`Application` 子类，与 `MainActivity.kt` 同包）：`lateinit var musicRepository: MusicRepository`，`onCreate` 里实例化 `MockMusicRepository()`。

配套一个 `ui/ViewModelFactory.kt`（或直接在 Composable 内 `viewModel { }` 工厂 lambda）解决"带参数的 ViewModel"——PDF 第10页踩坑表明确要求 `viewModel { PlayerViewModel(repo) }` 而非裸 `viewModel()`。

## 四、状态与 ViewModel（UDF）

**`ui/player/PlayerViewModel.kt`**
- 状态 `PlayerUiState(currentSong: Song?, isPlaying: Boolean, repeatMode: Int)`
- 唯一出口 `val uiState: StateFlow<PlayerUiState>`，私有 `MutableStateFlow` + `update{}`
- 事件（Intent）：`onPlaySong(song)` / `onPlayPause()` / `onNext()` / `onPrevious()` / `onRepeatModeChange()`
- 队列：持有当前 Mock 歌单的歌曲列表，`onNext/onPrevious` 在列表内循环

**`ui/home/HomeViewModel.kt`**
- 状态 `HomeUiState(selectedTab: HomeTab, playlists: List<Playlist>, recentSongs: List<Song>, searchQuery: String, searchResults: List<Song>, isLoading: Boolean)`
- 事件 `HomeAction.SelectTab(tab)` / `HomeAction.SearchQueryChange(text)` / `HomeAction.PlaySong(song)`
- `init` 中从 `MusicRepository` 收 `playlists()`、`recentSongs()`（`stateIn`）；搜索在本轮做本地过滤（不做 debounce，那是阶段四）

**注意**：本轮**不**在首页内驱动进度条自动前进（无真实播放器，避免假动效误导），MiniBar 只反映 `isPlaying` 状态与当前歌曲。

## 五、UI 层（新增 `ui/home/`、`ui/components/`）

沿用现有代码风格：**2 空格缩进**、私有 `@Composable` 命名、中文注释（参照 [LoginScreen.kt](file:///D:/Android/流媒体音乐播放器/app/src/main/java/com/dlust/ydhl2026/ui/screens/LoginScreen.kt) 的组件拆分习惯）。

| 文件 | 职责 |
|---|---|
| `ui/home/HomeScreen.kt` | 页面骨架：`Scaffold(bottomBar = { MiniPlayerBar(...) })` + 顶部 3 胶囊 Tab + 按 `selectedTab` 分发内容 |
| `ui/home/HomeTab.kt` | 枚举 `HomeTab(val label: String, val icon: ImageVector)` |
| `ui/home/PlaylistGridSection.kt` | 「我的歌单」标题 + `LazyVerticalGrid(GridCells.Fixed(2), key = { it.id })` + `PlaylistCoverCard`（**必须传 key**，PDF 标注防重组闪烁） |
| `ui/home/RecentPlayedSection.kt` | 「最近播放」标题 + `LazyRow(key = { it.id })` + 圆形小封面卡 |
| `ui/home/SearchTabContent.kt` | 搜索框 + 结果 `LazyColumn`（封面 + 标题 + 时长）；搜索历史 chip 与 debounce 留给后续 |
| `ui/home/FavoritesTabContent.kt` | 空态占位（"还没有收藏的歌单"），收藏功能属阶段五 |
| `ui/components/MiniPlayerBar.kt` | 悬浮条：封面 + 歌名/歌手 + 播放暂停 + 展开箭头，`onClick` 本轮 Toast 提示 |
| `ui/components/CoverArt.kt` | 通用封面绘制组件：由 `id.hashCode()` 从预设柔和色板取色，画圆角渐变块 + 首字/音符字符，供网格卡、最近播放、MiniBar、搜索结果复用 |

**视觉规范（Echo 风格，替代线框图的淡彩占位块）**
- 主色沿用品牌蓝 `#1B3FA0`；新增 `ui/theme/Color.kt` 中的 `EchoBlue` / `EchoSurfaceDark` 等常量（新文件用，**不改动 LoginScreen 里的私有常量**，避免回归）
- 胶囊 Tab：选中 = 品牌蓝实心 + 白字；未选中 = 透明 + 次级灰字；`--radius-full` 胶囊 + 2dp 内边距
- 卡片圆角 12dp、网格间距 12dp、页面水平内边距 16dp
- MiniBar：深色 `#12151A` 圆角 12dp、高 56dp、水平外边距 12dp、底部 8dp；无当前歌曲时**不渲染**

## 六、导航接线（`MainActivity.kt`）

- `home` 路由：`following` 改为 `HomeScreen(onOpenPlayer = { Toast 提示 })`；删除对 `MainScreen` 的 import
- 本轮**不新增** `player` 路由（全屏播放器属下一轮主题）
- `MainScreen.kt` 使命完成，**删除**（Hello World 占位）

## 七、实施顺序（每步保持可编译）

1. 依赖 + `EchoApp` + Manifest + 5 个数据模型
2. `MusicRepository` 接口 + `MockMusicRepository`（4 歌单 / 10 首歌）
3. `CoverArt` + `MiniPlayerBar` 组件
4. `PlayerViewModel` + `HomeViewModel`（StateFlow + UDF）
5. `HomeScreen` 骨架（Scaffold + 3 Tab）+ 歌单网格 + 最近播放
6. 搜索 Tab（本地过滤）+ 收藏 Tab 空态
7. `MainActivity` 接线、删 `MainScreen.kt`
8. 编译 + 自测

## 八、验证方式

1. `.\gradlew assembleDebug --console=plain` 编译通过
2. 装到设备/模拟器走一遍：
   - 启动页 → 登录（任意 11 位手机号 + `123456`）→ 进入首页
   - 首页见「我的歌单」2 列 4 个卡片（跑步歌单 12首等）+「最近播放」横向可滑
   - 点「最近播放」里的歌 → MiniPlayerBar 从底部出现，显示封面/歌名/歌手
   - 点播放暂停 → 图标在 ▶/⏸ 间切换
   - 切到「搜索」Tab → 输入"周杰伦"能过滤出 Mock 结果；切到「收藏」Tab → 显示空态
   - **切 Tab 时 MiniBar 不消失、歌曲不丢**（本轮最关键的验收点）
3. 代码自查：网格 `key` 已传、状态都在 ViewModel（Composable 内只有瞬时输入态）、无 `viewModel()` 裸调用

## 九、明确不在本轮范围（下一轮）

歌单详情/歌曲列表页、全屏播放器（图3）、播放队列 + 拖拽排序（图5）、Media3 真实播放 + 前台服务 + 通知栏、Room 播放历史/收藏、DataStore 续播、搜索历史 chip + `debounce(300ms)`、微交互动画（`animateFloatAsState` 展开 MiniBar）
