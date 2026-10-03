# 第三方词库与工具说明

项目自己的源码许可证由根目录的 `LICENSE` 确定。以下材料保留各自的原许可证，不能用项目许可证覆盖它们。

## Rime 简体拼音词库

- 文件：`app/src/main/assets/pinyin_simp.dict.yaml`。
- 上游项目：[rime/rime-pinyin-simp](https://github.com/rime/rime-pinyin-simp)。文件头说明它源自 Android Pinyin IME。
- 许可证：Apache License 2.0。
- 随项目保留的原文：`app/src/main/assets/licenses/rime-pinyin-simp-LICENSE.txt` 和 `rime-pinyin-simp-AUTHORS.txt`。
- 当前导入文件保留原文件头。本项目另外维护的 `chat_phrases.tsv` 不应被当成上游原词库。
- 词库文件对应上游提交 `0c6861ef7420ee780270ca6d993d18d4101049d0`，导入时未改写；运行时排除 YAML 元数据和无效条目，并建立本地索引。

## 英文词表

- 文件：`app/src/main/assets/english.tsv`。
- 文件头标明词条来自 [en-wl/wordlist 的 SCOWL / ESDB](https://github.com/en-wl/wordlist)，词表范围为 size ≤ 40；常用词排序与词形由本地处理调整。
- 完整许可与版权说明保留在 `app/src/main/assets/licenses/SCOWL-Copyright.txt`，包括其不同来源的附加说明。
- 导入源为上游 v2 分支提交 `1e5b7d3a72f47a71da5d28686c1dd4b397178485` 的 `scowl-pre.txt`；选取 size ≤ 40、ASCII 小写基础词，带地域标签的词选取美国英语（A）。去重后为 size 35 / 40 分别赋本地相对权重 4000 / 2000，少量自编常用词和词形使用另外的本地排序权重。这些数字不是上游给出的真实语言使用频次。

## Gradle Wrapper

`gradlew`、`gradlew.bat` 和 `gradle/wrapper` 使用 Gradle 9.3.1 的官方生成工具生成；Gradle 项目采用 Apache License 2.0。参见 [Gradle 官方源码与许可证](https://github.com/gradle/gradle)。它是构建工具，不打包进输入法运行时。

HeliBoard 与 FlorisBoard 的链接用于布局设计参考，本项目没有复制它们的代码或资源。
