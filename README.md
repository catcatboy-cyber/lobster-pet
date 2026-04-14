# 语音龙虾桌面宠物

一个能在手机桌面跑来跑去、听语音指令帮你操作社交APP的桌面宠物。

## 功能

- 🦞 桌面悬浮龙虾宠物，会随机游动
- 🎤 语音唤醒和指令识别
- 📱 自动打开社交APP（微信、QQ等）
- ⌨️ 语音转文字自动输入聊天

## 技术栈

- Android Kotlin
- 悬浮窗 (WindowManager)
- SpeechRecognizer (Google/系统语音识别)
- AccessibilityService (辅助功能)
- Lottie/帧动画

## 快速开始

### 方式一：本地构建

```bash
cd LobsterPet
cp local.properties.example local.properties
# 编辑 local.properties 添加你的 Android SDK 路径
./gradlew assembleDebug
```

### 方式二：GitHub Actions 自动构建（推荐）

项目已配置 GitHub Actions，每次推送代码会自动构建 APK。

**使用方法：**
1. 将代码推送到 GitHub 仓库
2. 进入仓库页面 → Actions → Build Android APK
3. 等待构建完成（约 3-5 分钟）
4. 点击最新运行记录，在 Artifacts 中下载 APK

**手动触发构建：**
- 进入 Actions → Build Android APK → Run workflow

构建完成后可下载：
- `lobster-pet-debug-apk` - 调试版本
- `lobster-pet-release-apk` - 发布版本（未签名）

## 权限说明

需要以下权限：
- `SYSTEM_ALERT_WINDOW` - 悬浮窗
- `BIND_ACCESSIBILITY_SERVICE` - 辅助功能（用于自动输入）
- `RECORD_AUDIO` - 语音识别

## 使用方法

1. 安装后授予悬浮窗权限
2. 开启辅助功能权限（设置 > 辅助功能 > 语音龙虾）
3. 说 "小龙虾" 唤醒
4. 说 "打开微信"、"发送你好" 等指令

## 指令列表

| 指令 | 动作 |
|------|------|
| "打开微信/QQ/微博" | 启动对应APP |
| "发送 [文字]" | 在当前聊天窗口输入文字 |
| "退下" | 龙虾隐藏 |
| "过来" | 龙虾游到屏幕中央 |

## 项目结构

```
app/src/main/java/com/lobster/pet/
├── service/
│   ├── FloatingLobsterService.kt    # 悬浮窗服务
│   └── LobsterAccessibilityService.kt # 辅助功能服务
├── view/
│   └── LobsterView.kt               # 龙虾动画View
├── voice/
│   ├── VoiceRecognizer.kt           # 语音识别
│   └── CommandProcessor.kt          # 指令解析
└── MainActivity.kt
```

## 注意事项

- Android 6.0+ 需要动态申请权限
- 部分国产ROM需要手动开启后台运行权限
- 辅助功能可能被系统判定为敏感权限，需要用户手动开启
