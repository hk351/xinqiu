# Echo 登录页实施计划

## Context

启动页已完成并经用户确认（纯色深蓝 #1B3FA0、网易云风格、2 秒自动跳转）。当前路由：`splash → home`（MainScreen 占位页）。用户要求新增**强制登录**：启动页之后进入登录页，登录成功才能进主页。项目暂无后端，采用**模拟登录**；登录方式为**密码 / 验证码双 Tab 切换**。

## 方案

### 路由调整（MainActivity.kt）

```
splash --2s--> login --登录成功--> home
```

- `splash` 跳转 `login`：`popUpTo("splash") { inclusive = true }`
- `login` 跳转 `home`：`popUpTo("login") { inclusive = true }`，主页按返回键 = 退出应用，不会回到登录页
- [SplashScreen.kt](file:///D:/Android/流媒体音乐播放器/app/src/main/java/com/dlust/ydhl2026/ui/screens/SplashScreen.kt) 回调参数 `onNavigateToHome` 更名 `onNavigateToLogin`（1 行）

### 登录页 UI（新建 ui/screens/LoginScreen.kt）

白底页面，品牌深蓝 #1B3FA0 作强调色，与启动页呼应。自上而下：

1. 品牌区：`Echo` 深蓝粗体大字 + 副标题「欢迎回来，听见每一次循环」
2. 手机号输入框（校验 `^1\d{10}$`）
3. 登录方式切换：自绘双 Tab「密码登录 / 验证码登录」（Text + 底部蓝色指示条，不用 TabRow 以便控制样式）
4. 密码登录：密码框，右侧文本按钮「显示 / 隐藏」切换明文
5. 验证码登录：验证码框 + 右侧「获取验证码」按钮，点击后 60 秒倒计时（模拟发送，提示测试码）
6. 登录按钮：圆角深蓝填充按钮，输入合法才 enable；点击后 600ms 假 loading（按钮内 CircularProgressIndicator）增强真实感
7. 底部：「其他登录方式」两个圆形占位（"微" / "Q"，点击提示暂未接入）+ 协议文案行（《用户协议》《隐私政策》，本期不强制勾选）

### 模拟登录规则

- 密码登录：手机号合法 + 密码 `123456`
- 验证码登录：输入 `123456`（模拟短信固定码）
- 校验失败：输入框下方显示红色错误文案
- 登录逻辑收敛为单个私有函数，未来接后端只替换该函数；状态用 `remember` 管理，接后端时再抽 ViewModel（避免现在过度设计）

### 不引入新依赖

新版 Compose BOM 不自带 material-icons，全部交互用文本按钮 / Canvas 实现。

## 文件清单

| 文件 | 操作 |
|---|---|
| `app/src/main/java/com/dlust/ydhl2026/ui/screens/LoginScreen.kt` | 新建 |
| `app/src/main/java/com/dlust/ydhl2026/MainActivity.kt` | 加 `login` 路由、改 splash 跳转目标 |
| `app/src/main/java/com/dlust/ydhl2026/ui/screens/SplashScreen.kt` | 回调改名 |

## 验证

1. `gradlew assembleDebug` 编译通过
2. 手动走流程：启动 → 2s → 登录页；错误密码显示错误提示；`123456` 进入主页；主页按返回键退出应用（不回登录页）；验证码 Tab 倒计时 + 固定码走通
3. 测试凭据：任意 11 位合法手机号（如 13800138000），密码/验证码均为 `123456`
