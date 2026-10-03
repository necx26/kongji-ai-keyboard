# 控机 AI 输入法 · Kongji AI Keyboard

**帮你回复的好用助手**

控机 AI 输入法是一款面向 Android 的ai智能输入法
支持 **Android 11 及以上**，无需root

> 项目仍在开发中。当前源码版本为 **v0.3.1**，本机新安装包在 `release/控机AI输入法-v0.3.1.apk`。新版增加中文错序与邻键误触候选，例子与实现见 [中文打字纠错说明](docs/中文打字纠错-v0.3.1.md)。本机 APK 与 GitHub Release 的发布状态分别管理。项目源码许可证尚待确定，详见文末的许可说明。

[安装使用](#安装与启用) · [配置 API](#配置模型接口) · [自行编译](#编译项目) · [参与开发](CONTRIBUTING.md) · [提交与同步](GitHub提交与同步.md)

## 功能

短小而强悍，调用的是私人api，免费开源
输入法也做了中文适配，可以输入中文，有模糊识别，记忆等功能

AI 回复采用以下流程：

```text
点“帮我回答” → 读取当前页面 → 预览所选内容 → 点“生成回复”
→ 选择候选 → 填入当前输入框 → 自行检查并发送
```

读取屏幕和向 API 提交内容是两个独立步骤。AI 候选按钮只填入文字；回车键会遵循所在应用输入框的行为，可能执行换行、搜索或发送。

## 安装与启用

维护者可在本仓库的 GitHub Releases 中提供已验证的 APK。尚未提供下载包时，可按下方的编译方法生成。维护者本机的 `交付` 目录保留旧版安装包与记录，该目录不纳入 Git 源码提交。

在release里面安装

#下载安装源文件
```
git clone https://github.com/necx26/kongji-ai-keyboard.git
```

只使用普通键盘时，无需配置 API 或开启屏幕读取。部分手机对侧载应用限制无障碍权限，可能需要在系统“应用信息”中允许受限制的设置；具体入口由手机系统决定。

## 配置模型接口

在应用设置中填写以下信息：

| 设置项 | 填写方式 |
| --- | --- |
| 接口地址 | 服务商基础地址，例如 `https://api.example.com/v1`，或完整的 Chat Completions 地址对于中国用户来说，一般我推荐阿里云百炼模型，有免费额度领取，对于非中国大陆，推荐openai api，|
| API Key | 自己的密钥；免鉴权的本地服务可留空 |
| 模型名称 | 服务商提供的实际模型 ID |
| 允许局域网 HTTP | 仅在连接本地模型服务时按需开启；默认使用 HTTPS |

程序会在基础地址后补上 `/chat/completions`。例如 `https://api.example.com/v1` 对应 `https://api.example.com/v1/chat/completions`。示例域名是占位地址，需要替换为实际服务地址。

**提交截图时，模型必须支持图片输入** 使用纯文本模型时，在屏幕预览中取消“同时提交截图”；当前页面需要提供可读取的控件文字。

当前实现使用 OpenAI 兼容的 Chat Completions 请求格式。只有 Responses、Claude 原生协议或其他私有接口的服务，需要额外开发适配器，不能只更换 URL。

连接电脑上的本地服务时，手机与电脑需能互相访问，例如使用 `http://192.168.1.10:8000/v1`。HTTP 选项仅允许私有 IPv4 地址及本机回环地址；手机上的 `localhost` 指手机自身。

## 中文、英文与输入记忆

以下是当前 v0.3.0 源码中的输入能力：

- **连续拼音**：`jintianxiawuwomenqubeijing` 可提供“今天下午我们去北京”等组词候选。
- **逐词选择**：选择前面的词后，保留剩余拼音，继续选择后面的词。
- **简拼与隔音符**：例如 `nh` 对应“你好”，`xi'an` 区分“西安”和“先”。
- **模糊拼音**：声母 `zh/z`、`ch/c`、`sh/s` 与韵母 `an/ang`、`en/eng`、`in/ing` 可分别开关。
- **英文候选**：例如 `hel` 提供 `hello`，`teh` 提供 `the`；点击候选进行替换，按空格保留自己输入的原词。
- **本机记忆**：记录选词习惯、用户词语及后续搭配，帮助调整候选排序。

在 **输入与词库设置** 中可调整模糊拼音、英文候选和记忆开关，添加自定义词语，或清空用户词库与学习记录。中文自定义词语需要填写对应音节的拼音，例如“控机大师 / kong ji da shi”。

输入记忆默认开启，记录保存在手机的应用私有数据库中。关闭记忆后，停止学习并停用已保存的用户词，已有记录保留至手动清空。密码、网址、邮箱和标记为不允许个性化学习的输入框不参与记忆。

## 定制键盘外观

打开 **定制键盘外观**，进入外观工作室。调整时可以点按实时预览，点击 **应用到键盘** 保存，或 **应用并试打** 进入练习页。

| 选项 | 可调范围 |
| --- | --- |
| 主题 | 雾紫、冰蓝、奶油、深空 |
| 点缀色 | 六种预设颜色，或六位 HEX 自定义色 |
| 按键透明度 | 40–96% |
| 圆角 / 间距 | 4–22 dp / 2–7 dp |
| 高度 / 字母字号 | 40–58 dp / 16–24 sp |
| 字母布局 | QWERTY、QWERTZ、AZERTY |
| 数字行与触感 | 独立数字行、轻触反馈、按压回弹动画 |

退出未应用的修改不会覆盖已保存外观；“恢复默认外观”先恢复预览，再应用保存。没有独立数字行时，可以长按第一排字母输入对应数字。

玻璃质感使用本地柔光渐变、微颗粒、半透明表面和边缘高光，不采样聊天画面作为键盘背景。轻触振动的实际效果由手机硬件和系统设置决定。

## 数据与隐私

- 普通中英文输入和词库学习在本机处理，不会随每次打字调用模型 API。
- API Key 使用 Android Keystore 加密保存，不写入应用日志；应用关闭云备份和数据迁移。
- 屏幕读取由用户点击触发，截图和读取文字暂存在内存；取消、切换输入目标或隐藏键盘时清理，不保存为文件。
- 点击“生成回复”后，所选文字与截图会提交到用户配置的 API 地址。服务商的数据处理方式取决于其自身规则。
- 密码输入框禁用屏幕读取。受保护页面和厂商系统限制不会被绕过。
- 本机输入记忆可以关闭和清空，不作为普通打字时的 API 请求内容。

截图可能包含头像、姓名、聊天和其他可见信息，提交前请检查预览。取消请求只能终止本地等待，无法从服务商撤回已提交的数据。

公开分发 APK 时不要内置多人共用的服务商密钥。个人使用可填写自己的 Key，共用服务应由维护者的后端保管密钥。

## 编译项目

本机开发环境使用 JDK 21，源码语言级别为 Java 17。准备 Android SDK Platform 36 和 Build Tools 36.0.0，并通过 `ANDROID_HOME` 或本机 `local.properties` 配置 SDK 路径。

| 项目 | 版本 |
| --- | --- |
| 最低 Android 版本 | Android 11 / API 30 |
| compileSdk / targetSdk | 36 / 35 |
| Android Gradle Plugin | 9.0.0 |
| Gradle Wrapper | 9.3.1，包含下载校验值 |

Windows PowerShell：

```powershell
./gradlew.bat :app:assembleDebug :app:lintDebug
```

Linux / macOS：

```bash
chmod +x gradlew
./gradlew :app:assembleDebug :app:lintDebug
```

构建输出：`app/build/outputs/apk/debug/app-debug.apk`。

首次构建需下载 Gradle 与依赖。不要提交自己的 `local.properties`、API Key、签名密钥或应用私有数据。详细开发与验证方法见 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 项目结构

```text
app/                       Android 输入法应用
  src/main/java/           键盘、输入引擎、屏幕读取与模型接口
  src/main/assets/         中英文词库及其许可证
  src/main/res/            图标、主题和服务配置
docs/                      界面截图与验证记录
tests/                     输入引擎检查、模拟器脚本及本地 API 测试服务
ui-probe/                  独立的模拟器窗口检查工具
gradle/wrapper/            固定版本的 Gradle 构建入口
同步GitHub.ps1             手动同步远程修改的辅助脚本
```

## 验证与当前限制

v0.2.0 已完成编译、Android Lint、Android 16 模拟器输入测试、外观设置测试和本地兼容 API 流程测试，记录见 [v0.2.0 验证记录](docs/validation-v0.2.0.md)。当前 v0.3.0 源码正在继续补测输入与词库功能，不将旧版本的测试结论直接用于新版本。

接口测试使用本地固定回复服务，不代表真实模型的理解能力或回复质量。`tests/mock_api.py` 和 `ui-probe` 不包含在交付的输入法 APK 中。

目前尚未完成各手机品牌、微信/QQ、不同 Android 版本、横屏、平板和长期连续使用的全面验证。当前没有滑行输入、语音输入或大模型级句意纠错，也尚未完成应用商店发布审核。

## 参与开发与同步

欢迎通过 Issue 提供复现步骤，通过 Pull Request 提交改进。建议流程为 **Fork → 新建分支 → 修改与验证 → 提交 PR → 维护者审查并合并**。

远程修改合并后，本地可执行：

```powershell
git pull --ff-only origin main
```

GitHub 上的修改不会自动同步到电脑文件夹。一键同步脚本会检查本地状态，有未提交修改或提交分叉时停止；本项目没有设置后台定时同步。

详细步骤见 [GitHub 提交与同步](GitHub提交与同步.md)，贡献说明见 [CONTRIBUTING.md](CONTRIBUTING.md)。提交截图与日志时，请使用模拟内容并移除真实密钥和私人数据。

## 许可与参考

许可嘛...

- 中文词库：[Rime 袖珍简化字拼音](https://github.com/rime/rime-pinyin-simp)，源自 Android Pinyin IME，保留 Apache 2.0 许可证与作者说明。
- 英文词表：[SCOWL / ESDB](https://github.com/en-wl/wordlist)，保留原版权与许可说明。
- 布局参考：[HeliBoard](https://github.com/HeliBorg/HeliBoard/wiki/2.-Layouts) 与 [FlorisBoard](https://github.com/florisboard/florisboard)。键盘界面由本项目实现，没有复制这两个项目的代码或资源。

完整材料说明见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。
