# Contributing to Universal Chat Translator

感谢你帮助改进 Universal Chat Translator。提交 Issue 或 Pull Request 即表示你同意相关贡献按照项目的 [CC BY 4.0](LICENSE) 许可证发布。

## 报告问题

- 先搜索现有 Issue，避免重复。
- 使用 Bug 模板并填写 Minecraft、Fabric Loader、Fabric API、Cloth Config、Mod Menu 和本 Mod 版本。
- 提供可复现步骤、期望结果、实际结果和相关日志。
- 聊天识别问题请提供脱敏后的完整显示格式，不要发布私人聊天、服务器地址、API Key 或配置文件。
- 安全问题不要创建公开 Issue，请遵循 [SECURITY.md](SECURITY.md)。

## 开发流程

1. 使用 Java 21。
2. 从 `main` 创建主题分支。
3. 保持改动聚焦，并同时更新中英文语言文件和文档。
4. 不记录 API Key、Authorization Header、完整请求体或聊天正文。
5. 为协议、消息识别、排序或配置行为添加测试。
6. 提交前运行：

```powershell
.\gradlew test
.\gradlew clean build
```

Linux/macOS 使用 `./gradlew`。

## Pull Request

PR 描述应说明动机、用户可见变化、测试方式和兼容性影响。不要提交 `build/`、`run/`、`.gradle/`、游戏目录、真实配置或编译后的 Mod JAR。

本项目是非官方衍生版本。请保留 `NOTICE.md`、`LICENSE-ORIGINAL-CC0` 及原作者署名。
