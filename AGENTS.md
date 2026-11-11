# AGENTS.md

## 交付前注意事项

1. **每次改动都要提交 Git commit**
   每次改动完成后，必须创建一个对应的 Git commit，以便后续追踪和回滚。

2. **每次改动都要有测试，且全部通过后才能交付**
   每次改动后，必须编写或更新相关测试，并在交付给用户前，确保所有测试和验证全部通过。

## 测试与验证方法

### 1. 编译 + 单元测试（每次改动必跑）

```powershell
.\gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain
```

- 改了已有逻辑但测试结果"没变化"时，加 `--rerun-tasks` 强制重跑，避免 Gradle 缓存假绿。
- 单元测试位置：`app/src/test/java/com/dlust/ydhl2026/`。
- ViewModel / 逻辑类测试用 `FakePlaybackEngine` 之类的假实现注入，不依赖真实播放器和设备。
- 只跑单个测试类：

```powershell
.\gradlew :app:testDebugUnitTest --tests "com.dlust.ydhl2026.local.LocalMusicViewModelTest" --console=plain
```

### 2. 安装到模拟器/真机

`adb` 全路径为 `D:\Android\AndroidSDK\platform-tools\adb.exe`（下文简写 `adb`，在 PowerShell 里用全路径）。

不要用 `.\gradlew installDebug`，本项目会报 `INSTALL_FAILED_MEDIA_UNAVAILABLE`。改用 push + `pm install`：

```powershell
adb push app\build\outputs\apk\debug\app-debug.apk /data/local/tmp/echo.apk
adb shell pm install -r -t /data/local/tmp/echo.apk
adb shell am start -n com.dlust.ydhl2026/.MainActivity
```

### 3. UI 改动必须截图自查

涉及界面的改动，不能只看编译通过，必须截图确认实际效果：

```powershell
# 输出目录先建好：app\build\shots
adb shell screencap -p /sdcard/shot.png
adb pull /sdcard/shot.png app\build\shots\shot.png
```

- 截图前先确认当前页面和预期一致（用 `adb shell dumpsys window | Select-String mCurrentFocus` 看前台 Activity）。
- 需要点击/输入时用 `adb shell input tap X Y`、`adb shell input text 关键词`；坐标按截图实际位置推算，点错就重截一张核对。
- 细节（进度线、描边、渐变）要放大看：用 `System.Drawing` 写 PowerShell 脚本裁剪目标区域，避免内联命令的引号转义问题。
- 深色模式和浅色模式都要各看一遍。

### 4. 交付前自查清单

- [ ] `assembleDebug` 通过，无新增警告级错误
- [ ] `testDebugUnitTest` 全部通过（必要时 `--rerun-tasks`）
- [ ] 界面改动已在模拟器上截图核对，深/浅色都看过
- [ ] 已创建对应 Git commit

## 项目结构与架构约定

包名 `com.dlust.ydhl2026`，源码在 `app/src/main/java/com/dlust/ydhl2026/`。

| 目录 | 职责 |
| --- | --- |
| `data/model/` | 纯数据类（Song、Playlist、PlaybackState、LyricLine），不放 Android 依赖 |
| `data/repository/` | 数据来源抽象（`MusicRepository`）与实现（`MockMusicRepository`） |
| `data/playback/` | 播放抽象 `PlaybackEngine` + Media3 实现 `Media3PlaybackEngine` |
| `data/local/` | MediaStore 扫描的 `LocalMusicSource` 与内存缓存 `LocalMusicStore` |
| `ui/<页面>/` | 每个页面一个包：`XxxScreen`（纯 UI）+ `XxxViewModel`（状态与动作） |
| `ui/components/` | 跨页面复用的视觉件（CoverArt、MiniPlayerBar、EmptyState、PressScale、EchoIcons） |
| `ui/theme/` | Color / Type / Theme，视觉常量的唯一来源 |

- **手动 DI**：Repository、播放引擎、扫描器只在 `EchoApp` 里创建，ViewModel 通过构造函数注入，由 `ui/ViewModelFactory.kt` 生产。
  - 禁止在 Composable / ViewModel 内部直接 `new` 依赖。
  - 新增依赖时改三处：`EchoApp` 字段 + `ViewModelFactory` + 对应 ViewModel 构造参数。
- **ViewModel 模式**：`data class XxxUiState` + `sealed interface XxxAction` + 单一 `onAction()` 入口，UI 只读状态、只发动作。
- **播放状态单例**：整个应用共用一个 `PlaybackEngine`，迷你条和全屏播放器操作的是同一份状态，不要各自创建播放器。
- **数据层红线**：UI 层不允许 import `androidx.media3.*`，一切播放操作走 `PlaybackEngine`；换数据源只改 `EchoApp.onCreate()` 里的一行。

## 代码与 UI 约定

- 注释一律用中文，写"为什么"而不是复述代码。
- 颜色只能来自 `ui/theme/Color.kt`，禁止在 Composable 里硬编码 `Color(0xFF...)`（已有常量：`EchoBlue`、`EchoTextPrimaryDark`、`EchoTextSecondary`、`EchoTextTertiary`）。
- 每个界面都要同时支持深/浅色：页面级用 `isDark` 参数（`isSystemInDarkTheme()` 或主题传入）分支取色，不要只按亮色写。
- 通用视觉一律复用 `ui/components` 里的现成件，不要复制一份相似实现：
  - 点击手感 → `Modifier.pressScale(interactionSource)`
  - 空列表/空搜索 → `EmptyState(...)`
  - 歌曲封面 → `CoverArt(seed = song.title, ...)`
  - 图标 → `EchoIcons` 里的 Canvas 图标，保持线宽一致
- 动画时长沿用现有档位，保持全站节奏统一：
  - 120ms 按压反馈 / 180–200ms 颜色与淡入淡出 / 220–260ms 位移与进出场
  - 使用 `tween(...)` 显式指定，并给 `label` 便于调试
- 新页面必须在 `MainActivity` 的导航图里注册，返回行为与现有页面保持一致。

## Git 约定

- 提交信息用中文，一句话说清改了什么、为什么，必要时加正文。
- 一次改动一个 commit，不把无关改动混在一起。
- 只提交源码与配置生效的改动；不要提交 `local.properties`、`build/`、`.gradle/`、`.idea/`。
- `prototype/*.html` 是设计阶段的静态原型，属于产出物，需要入库；原型被放弃时，未入库的直接删除，不在仓库里留废弃文件。
- PowerShell 5 不支持 `$(cat <<'EOF')` 和 `&&`：提交信息先写到临时文件（如 `C:\Users\HK\AppData\Local\Temp\echo_commit_msg.txt`），再 `git commit -F <文件>`。

## 环境说明

- `compileSdk 37` / `targetSdk 37` / `minSdk 24`，Java 11。
- 依赖仓库在 `settings.gradle.kts` 里配置为阿里云镜像，首次构建或清缓存后构建偏慢属正常，不需要改回官方仓库。
- `gradle.properties` 开启了 configuration cache，改动 build 脚本后首次构建会重新配置，属正常现象。
- `local.properties`（内含 SDK 路径）不入库，换机器需自行生成。

## 已知坑速查

| 现象 | 原因 / 处理 |
| --- | --- |
| `INSTALL_FAILED_MEDIA_UNAVAILABLE` | 不要用 `gradlew installDebug`，改 push + `pm install -r -t` |
| 提交时报 heredoc 语法错误 | PowerShell 5 不支持，改用 `git commit -F 文件` |
| 测试结果与改动不符、疑似"假绿" | Gradle 缓存命中，加 `--rerun-tasks` 重跑 |
| `input tap` 点错位置 | 先截图核对实际坐标，别凭布局推算 |
| 裁剪截图时 PowerShell 报引号未闭合 | 内联命令转义不可靠，写成 `.ps1` 脚本再用 `-File` 执行 |
| 本地音乐页列表为空 | 模拟器默认无音频，`adb push` 歌曲到 `/sdcard/Music/` 后触发媒体扫描；媒体扫描广播在部分系统上会被忽略，必要时重启模拟器 |
| 读取不到本地歌曲 | Android 13+ 需 `READ_MEDIA_AUDIO`，以下是 `READ_EXTERNAL_STORAGE`，两者都要在运行时申请 |
| 播放进度条不动 | 进度由 `PlaybackEngine` 每 250ms 回传，UI 侧不要自己起定时器 |
