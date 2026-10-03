# 参与开发

欢迎修复键盘输入、外观、无障碍适配和模型接口问题。提交改动前先描述当前行为、预期行为、Android 版本和复现步骤；截图请使用模拟对话或遮盖私人内容。

## 提交改动

1. 在 GitHub Fork 本项目，克隆自己的仓库。
2. 创建分支，例如 `git switch -c fix/keyboard-layout`。
3. 修改对应源码，保留第三方词库的来源与许可证。
4. 检查、提交并推送分支，然后向本项目的 `main` 提交 Pull Request。

PR 请说明解决什么问题、修改后的行为、如何验证，以及仍未验证的手机或接口。测试使用固定回复服务时应明确说明。

## 编译

需要 JDK 与 Android SDK。项目源码采用 Java 17，本机开发环境使用 JDK 21；安装 Android SDK Platform 36 和 Build Tools 36.0.0。配置 `ANDROID_HOME`，或在本机的 `local.properties` 中填写 `sdk.dir`，不要提交自己的 SDK 路径。

Windows PowerShell：

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
```

Linux / macOS：

```bash
chmod +x gradlew
./gradlew :app:assembleDebug :app:lintDebug
```

Wrapper 固定 Gradle 9.3.1，首次使用会下载对应版本并校验 SHA-256。安装包输出到 `app/build/outputs/apk/debug/app-debug.apk`。首次构建需能访问 Gradle、Google Maven 和 Maven Central。

`tests/mock_api.py` 仅供本地兼容接口验证，不是模型。`ui-probe` 和模拟器脚本是独立测试工具，不打包到输入法 APK 中；现有 UI 脚本使用维护者测试机器的 adb 路径和模拟器编号，其他环境需要相应调整。

## 数据处理

不要在代码、Issue、PR 或日志中填写真实 API Key、私人聊天、个人学习词库或签名密钥。修改读屏和回复功能时，应保留预览确认、密码框限制、取消请求和输入目标变化检查。修改数据存储时，应提供迁移与清除行为的说明。
