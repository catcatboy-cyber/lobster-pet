# Bitrise 构建指南

## 📋 准备工作

### 1. 创建 GitHub 仓库

1. 访问 https://github.com/new
2. 仓库名填 `LobsterPet`（或其他名字）
3. 选择 **Private**（私人）或 **Public**（公开）
4. 点击 **Create repository**

### 2. 上传代码到 GitHub

在 `LobsterPet` 文件夹里执行：

```bash
# 初始化 git 仓库
git init

# 添加所有文件
git add .

# 提交
git commit -m "Initial commit: 语音龙虾桌面宠物"

# 添加远程仓库（把 YOUR_USERNAME 换成你的 GitHub 用户名）
git remote add origin https://github.com/YOUR_USERNAME/LobsterPet.git

# 推送代码
git push -u origin main
```

---

## 🚀 在 Bitrise 上配置

### 3. 注册/登录 Bitrise

1. 访问 https://bitrise.io
2. 用 GitHub 账号登录（最方便）

### 4. 添加应用

1. 点击 **Add new app**
2. 选择 **GitHub** 作为代码托管平台
3. 授权 Bitrise 访问你的仓库
4. 找到并选择 `LobsterPet` 仓库
5. 点击 **Next**

### 5. 配置构建

1. **Branch**: 选择 `main`
2. **Project type**: 选择 **Android**
3. **Gradle file**: 选择 `app/build.gradle.kts`
4. **Build variant**: 选择 `release` 或 `debug`

Bitrise 会自动识别 `bitrise.yml` 文件。

### 6. 设置环境变量（可选）

如果你想签名 APK（推荐）：

1. 在 Bitrise 控制台点击 **Workflow** → **Env Vars**
2. 添加以下变量：

```
BITRISEIO_ANDROID_KEYSTORE_URL: https://... (你的密钥库文件链接)
BITRISEIO_ANDROID_KEYSTORE_PASSWORD: your_password
BITRISEIO_ANDROID_KEYSTORE_ALIAS: your_alias
BITRISEIO_ANDROID_KEYSTORE_PRIVATE_KEY_PASSWORD: your_key_password
```

如果不设置签名，Bitrise 会生成未签名的 Debug APK。

---

## 📱 获取 APK

### 方式 1：自动构建

每次你推送代码到 GitHub，Bitrise 会自动构建 APK。

### 方式 2：手动触发

1. 在 Bitrise 控制台点击 **Build**
2. 选择 **build-debug** workflow
3. 点击 **Start Build**

### 下载 APK

构建完成后：
1. 点击构建记录
2. 找到 **APPS & ARTIFACTS** 标签
3. 下载 `app-release.apk` 或 `app-debug.apk`

---

## 🔧 常见问题

### Q: 构建失败，提示 "Gradle wrapper not found"
A: 确保 `gradlew` 文件已提交到 git：
```bash
git add gradlew
git commit -m "Add gradle wrapper"
git push
```

### Q: 如何生成签名密钥？
A: 在本地运行：
```bash
cd LobsterPet
keytool -genkey -v -keystore lobster.keystore -alias lobster -keyalg RSA -keysize 2048 -validity 10000
```
然后把 `lobster.keystore` 上传到 Bitrise 的 **Code Signing** 页面。

### Q: 构建太慢？
A: 在 `bitrise.yml` 里启用了 Gradle 缓存，第二次构建会快很多。

---

## 📝 项目文件说明

```
LobsterPet/
├── app/                    # Android 应用代码
├── bitrise.yml            # Bitrise 配置文件 ✅
├── gradle/                # Gradle wrapper
├── build.gradle.kts       # 项目级构建配置
├── settings.gradle.kts    # 项目设置
└── README.md              # 项目说明
```

---

## ✅ 检查清单

上传代码前确认：
- [ ] 所有代码文件已提交
- [ ] `bitrise.yml` 已添加
- [ ] `gradlew` 文件可执行（`chmod +x gradlew`）
- [ ] 代码已推送到 GitHub

Bitrise 配置：
- [ ] 已添加 GitHub 仓库
- [ ] 已选择 Android 项目类型
- [ ] 已运行至少一次构建测试

---

有问题随时问我！
