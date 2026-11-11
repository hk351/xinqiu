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
