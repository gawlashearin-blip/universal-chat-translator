# Universal Chat Translator 1.2.1

## 兼容性修复

- 将正式版本的 OkHttp/Okio 网络实现替换为 Java 21 内置 HTTP 客户端。
- 修复 Lunar Client 或其他修改版客户端优先加载不兼容 `okhttp3.RequestBody` 时产生的 `NoSuchMethodError`。
- 保留 OpenAI 兼容请求格式、自定义超时、禁止重定向、可选 Bearer 验证、响应校验与错误脱敏。
- Google 免费翻译继续可用，不引入替代网络依赖。
- 正式 JAR 从约 1.5 MB 缩小至约 0.4 MB。

## 安装要求

- Minecraft 1.21.11
- Java 21
- Fabric Loader 0.18.4 或更高版本
- Fabric API
- Cloth Config API
- Mod Menu

本版本面向 PCL、HMCL 等标准 Fabric 启动环境，并移除了 Lunar Client Fabric Profile 中已确认的网络库冲突。Badlion Client 不支持任意第三方 Fabric Mod。

升级时请删除旧的 `universal-chat-translator-*.jar` 和 `.jar.disabled`，确保 `mods` 文件夹中只保留 1.2.1。

---

## Compatibility fix

- Replaced production OkHttp and Okio usage with Java 21's built-in HTTP client.
- Fixed the `NoSuchMethodError` caused when Lunar Client or another modified client loads an incompatible `okhttp3.RequestBody` class first.
- Preserved OpenAI-compatible request formatting, configurable timeouts, redirect blocking, optional bearer authentication, response validation, and error redaction.
- Preserved the Google Free translation engine without adding a replacement network dependency.
- Reduced the release JAR from approximately 1.5 MB to approximately 0.4 MB.

## Requirements

- Minecraft 1.21.11
- Java 21
- Fabric Loader 0.18.4 or newer
- Fabric API
- Cloth Config API
- Mod Menu

This release targets standard Fabric launchers such as PCL and HMCL and removes the known networking-library conflict observed on Lunar Client Fabric profiles. Badlion Client does not support arbitrary third-party Fabric mods.

When upgrading, remove older `universal-chat-translator-*.jar` and `.jar.disabled` files so that only 1.2.1 remains in the `mods` directory.
