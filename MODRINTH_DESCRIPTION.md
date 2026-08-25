# Universal Chat Translator

**简体中文** | [English](#english)

Universal Chat Translator 是一个适用于 Fabric 客户端的聊天翻译 Mod，可翻译玩家消息和系统消息，也可以在发送前将中文翻译成英文。

## 主要功能

- 分别开启或关闭玩家消息与系统消息翻译
- 中文消息发送前翻译为英文，同时在本地保留中文原文
- Google 免费翻译与自定义 OpenAI 兼容接口
- 自定义 Base URL、模型、API Key、系统提示词、超时和 Temperature
- 在配置页直接测试自定义接口连接
- 玩家名称标签与 16 种 Minecraft 标准译文颜色
- 并发请求按消息原始顺序显示或发送，避免快速聊天乱序
- 四个可选单键快捷键，默认均未绑定
- 跳过自己的服务器回显，避免重复翻译

## 环境要求

- Minecraft 1.21.11
- Java 21
- Fabric Loader 0.18.4 或更高版本
- Fabric API
- Cloth Config API
- Mod Menu

本 Mod 仅需安装在客户端。将 JAR 放入实例的 `mods` 文件夹，并确保不要同时保留多个版本或与原版 Mini Chat Translator 同时启用。

## 翻译服务与隐私

默认使用无需配置的 Google 免费翻译。自定义引擎支持 OpenAI `POST /chat/completions` 兼容接口，包括 Ollama、LM Studio 和远程供应商。

- API Key 会以明文保存在客户端 TOML 配置中。
- 使用远程 HTTP 地址时，聊天正文和密钥可能以明文传输；建议使用 HTTPS。
- 翻译失败时不会自动回退到另一家供应商，避免聊天内容被再次转发。
- 每次请求只发送当前消息，不携带历史聊天上下文。

完整配置和故障排查说明请参阅 [GitHub README](https://github.com/gawlashearin-blip/universal-chat-translator/blob/main/README.md)。

## 来源、许可与 AI 披露

这是基于 Real_SShwer 的 [Mini Chat Translator to Chinese](https://modrinth.com/mod/mini-chat-translator-to-chinese) 制作的独立、非官方衍生版本。原项目使用 CC0-1.0；原始声明副本保存在源码仓库的 `LICENSE-ORIGINAL-CC0` 中。本衍生版本以 CC BY 4.0 发布，并在 `NOTICE.md` 中列出来源和主要修改。

本衍生版本的设计讨论、代码实现、测试、文档与发布准备曾大量使用 OpenAI Codex 生成式 AI 辅助，并由项目维护者提出需求、测试和决定发布内容。Mod 的自定义翻译功能本身也可以连接由用户选择的生成式 AI 服务。原项目的 AI 使用情况不由本项目作出声明。

源码、问题反馈及完整许可信息：[GitHub](https://github.com/gawlashearin-blip/universal-chat-translator)

---

## English

Universal Chat Translator is a client-side Fabric mod that translates player and system messages and can translate Chinese text into English before it is sent.

### Highlights

- Independent toggles for player-message and system-message translation
- Chinese-to-English outgoing translation with a local copy of the Chinese source
- Google Free and custom OpenAI-compatible translation engines
- Configurable base URL, model, API key, system prompt, timeout, and temperature
- Connection testing directly from the configuration screen
- Sender labels and all 16 standard Minecraft text colors
- Concurrent requests released in original message order
- Four optional single-key shortcuts, all unbound by default
- Self-echo suppression to prevent duplicate translation

### Requirements

- Minecraft 1.21.11
- Java 21
- Fabric Loader 0.18.4 or newer
- Fabric API
- Cloth Config API
- Mod Menu

This is a client-side mod. Place the JAR in the instance's `mods` folder. Do not keep multiple versions installed, and avoid enabling it together with the original Mini Chat Translator.

### Translation services and privacy

Google Free is the zero-configuration default. The custom engine supports OpenAI-compatible `POST /chat/completions` endpoints, including Ollama, LM Studio, and remote providers.

- API keys are stored as plaintext in the client-side TOML configuration.
- Remote HTTP endpoints can expose chat text and credentials in transit; HTTPS is strongly recommended.
- Failed requests never fall back automatically to another provider.
- Each request contains only the current message and no chat history.

See the [English GitHub README](https://github.com/gawlashearin-blip/universal-chat-translator/blob/main/README_EN.md) for setup examples and troubleshooting.

### Attribution, license, and AI disclosure

This is an independent, unofficial derivative of Real_SShwer's [Mini Chat Translator to Chinese](https://modrinth.com/mod/mini-chat-translator-to-chinese). The original project is CC0-1.0; a copy of its original declaration is retained as `LICENSE-ORIGINAL-CC0`. This derivative is distributed under CC BY 4.0, with origin and major modifications documented in `NOTICE.md`.

OpenAI Codex generative AI substantially assisted the derivative project's design discussions, code implementation, tests, documentation, and release preparation. The maintainer supplied requirements, performed testing, and made the publishing decisions. The mod's custom translation feature can also connect to a generative-AI service selected by the user. This project makes no representation about AI usage in the original project.

Source code, issue tracker, and complete licensing information: [GitHub](https://github.com/gawlashearin-blip/universal-chat-translator)
