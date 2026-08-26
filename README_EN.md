<div align="center">
  <img src="src/main/resources/assets/universal-chat-translator/icon.png" width="128" alt="Universal Chat Translator icon">
  <h1>Universal Chat Translator</h1>
  <p>A universal chat translation client mod for Minecraft 1.21.11 Fabric</p>

  English · [简体中文](README.md)

  [![Build](https://github.com/gawlashearin-blip/universal-chat-translator/actions/workflows/build.yml/badge.svg)](https://github.com/gawlashearin-blip/universal-chat-translator/actions/workflows/build.yml)
  [![Release](https://img.shields.io/github/v/release/gawlashearin-blip/universal-chat-translator?display_name=tag)](https://github.com/gawlashearin-blip/universal-chat-translator/releases/latest)
  [![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-62B47A)](https://www.minecraft.net/)
  [![Fabric](https://img.shields.io/badge/Loader-Fabric-DBD0B4)](https://fabricmc.net/)
  [![License](https://img.shields.io/badge/License-CC%20BY%204.0-lightgrey.svg)](LICENSE)
</div>

Universal Chat Translator translates incoming foreign-language player or system messages into Simplified Chinese and can translate outgoing Chinese chat into English before sending. It uses Google's free translation endpoint by default and supports any local or remote service implementing the OpenAI-compatible `POST /chat/completions` protocol.

> This is an unofficial derivative of Real_SShwer's [Mini Chat Translator](https://github.com/ZHENXUWEI/mini-chat-translator-fabric). It is not affiliated with or endorsed by the original author. See [NOTICE.md](NOTICE.md) for full attribution.

## Features

- Independent controls for player and system messages; action-bar messages are always ignored.
- Native player-profile support and recognition of plugin formats containing levels, icons, ranks, and VIP prefixes.
- Player translations use `[player name] translation`; system translations use `[System] translation`.
- Labels are always gold; translated text supports all 16 standard Minecraft colors and defaults to aqua.
- Successful outgoing Chinese chat displays `[your name] Chinese original` locally while the server normally displays the English translation.
- Native UUID echo suppression and a bounded tracker for recent self messages sent through plugin system channels.
- Concurrent translation requests with strictly ordered incoming and outgoing release queues.
- Google free translation and configurable OpenAI-compatible APIs, with no automatic cross-provider fallback.
- Connection testing, prompt placeholders, timeout, and temperature controls.
- Four optional single-key shortcuts, all unbound by default.

### Chat behavior

Incoming message:

```text
1⚒ Steve: where are you
[Steve] 你在哪里
```

Outgoing Chinese:

```text
[YourName] 大家好
<YourName> Hello everyone
```

The first line is local-only. The second is displayed normally by the server.

## Requirements

| Component | Requirement |
| --- | --- |
| Minecraft Java Edition | 1.21.11 |
| Java | 21 or newer |
| Fabric Loader | 0.18.4 or newer |
| Fabric API | Required |
| Cloth Config | 21.11.153 or newer |
| Mod Menu | 17.0.0-beta.2 or newer |

## Download and installation

1. Download `universal-chat-translator-1.2.1.jar` from [GitHub Releases](https://github.com/gawlashearin-blip/universal-chat-translator/releases/latest).
2. Install Fabric Loader, Fabric API, Cloth Config, and Mod Menu for Minecraft 1.21.11.
3. Place the JAR in the instance's `mods` directory.
4. Remove older `universal-chat-translator-*.jar` and `.jar.disabled` files to prevent multiple versions from coexisting.
5. Start Minecraft and open Universal Chat Translator through Mod Menu.

This mod never reads, copies, or deletes Mini Chat Translator's configuration. Do not enable both mods simultaneously because both may intercept chat.

## Configuration

Settings are stored in:

```text
config/universal-chat-translator.toml
```

| Setting | Default | Description |
| --- | --- | --- |
| Enable translation | On | Master switch |
| Translation engine | Google free | Switch to a custom OpenAI-compatible API if needed |
| Chinese to English before sending | On | Intercepts Chinese chat and sends its translation |
| Translate player messages | On | Translates native and recognized plugin player chat |
| Translate system messages | Off | Translates announcements, notices, and other non-player messages |
| Translation color | Aqua | Any of Minecraft's 16 standard colors |

System messages may include coordinates, menus, and rapidly changing notices, so they default to off. Some servers deliver player chat through the system channel; the mod classifies it as player chat only when an online player name appears in the speaker position.

### Shortcuts

Expand **Shortcuts** in the Mod Menu configuration screen. All four entries default to unbound:

- Open settings
- Toggle player-message translation
- Toggle system-message translation
- Toggle Chinese-to-English sending

Shortcuts are active only while no GUI or chat input is open, and holding a key triggers it once. Toggle shortcuts report their new state in the action bar. They are managed only by this mod and do not appear in Minecraft's standard Controls screen. Duplicate bindings cannot be saved.

## Translation engines

### Google free translation

This is the zero-configuration default. Messages are still sent to an external Google translation endpoint. The free endpoint may be rate-limited, unavailable on some networks, or change in the future.

### Custom OpenAI-compatible API

Version 1.x implements only `POST /chat/completions`. Each request contains the current message only, without chat history or streaming.

| Setting | Default | Description |
| --- | --- | --- |
| Base URL | Empty | A base URL or complete `/chat/completions` endpoint |
| Model | Empty | Model identifier expected by the server |
| API Key | Empty | Sends `Authorization: Bearer <key>` when non-empty |
| System prompt | Built-in translation prompt | Supports `{source_language}` and `{target_language}` |
| Timeout | 30 seconds | Allowed range: 1–120 seconds |
| Temperature | 0.0 | Allowed range: 0.0–2.0 |

Endpoint normalization:

```text
https://example.com/v1                  → https://example.com/v1/chat/completions
https://example.com/v1/chat/completions → unchanged
```

When the API key is empty, the Authorization header is omitted. Redirects are never followed, preventing credentials from being carried to an unexpected host.

#### Ollama

Start Ollama and pull a chat-capable model, then configure:

```text
Base URL: http://localhost:11434/v1
Model:    your Ollama model name, for example qwen3:8b
API Key:  empty
```

Reference: [Ollama OpenAI compatibility](https://docs.ollama.com/api/openai-compatibility).

#### LM Studio

Start the local server from LM Studio's Developer page and load a model, then configure:

```text
Base URL: http://localhost:1234/v1
Model:    the model identifier shown by LM Studio
API Key:  empty unless local authentication is enabled
```

Reference: [LM Studio OpenAI Compatibility](https://lmstudio.ai/docs/developer/openai-compat).

#### Other compatible services

```text
Base URL: https://your-provider.example/v1
Model:    the provider's model identifier
API Key:  the provider-issued secret
```

The service must return standard `choices[0].message.content`. Responses API, custom JSON templates, custom headers, and streaming are not supported.

### Connection test

The test button reads the current unsaved form values, sends `Hello, welcome to the server!`, and requests Simplified Chinese. It does not save configuration or switch engines.

- While running, it immediately changes to **Testing…** and is disabled.
- Success displays latency and returned translation, also in chat when the player is in a world.
- Failure displays a redacted HTTP, protocol, timeout, or response-parsing error.

## Ordering and failure behavior

- Incoming and outgoing messages use separate sequence queues.
- A, B, and C may run concurrently; even if they finish B, C, A, they are released A, B, C.
- Failures and timeouts consume their sequence and cannot permanently block later messages.
- Incoming failure displays a local red error and never automatically falls back to Google.
- Outgoing failure displays an error and sends the original Chinese text.
- Disabling player or system translation suppresses matching results that have not yet been displayed.
- Disabling outgoing translation before a pending request finishes sends the Chinese original.

## Privacy and security

- Online translation sends matching chat content to the selected provider.
- API keys are intentionally stored as plain text in the TOML file. Never share that file.
- Remote HTTP exposes API keys and chat in transit. The mod warns but does not block it; use HTTPS remotely.
- The mod does not log API keys, Authorization headers, full request bodies, or complete chat messages.
- Server error content is extracted, redacted, and truncated.
- A failed custom request never falls back to Google, preventing the same chat from being sent to a second provider.

Do not translate passwords, verification codes, private keys, or other secrets.

## Troubleshooting

### Player messages are not translated

- Confirm both the master switch and player-message translation are enabled.
- Chinese content is not translated into Chinese again.
- Servers may use unusual nickname layouts. Open an Issue with a sanitized complete format and your chat-mod list, not private conversation content.

### A message is labeled `[System]`

The server may send player chat through the system channel with a display name that differs from the online profile. Enable system translation or report the sanitized format to improve recognition.

### Messages appear more than once

Keep only one Universal Chat Translator JAR in `mods`, and do not simultaneously enable Mini Chat Translator or another automatic translation mod.

### A custom endpoint test fails

- Base URL should include `/v1` or end in `/chat/completions`.
- Model must exactly match the server's identifier.
- Check that the service is running, firewall/proxy settings, and API-key permissions.
- HTTP 401 normally indicates bad credentials, 429 means rate limiting, and 5xx is a server error.

### A shortcut does not respond

Shortcuts default to unbound and do not trigger while chat, inventory, menus, or another GUI is open. Assign distinct keys under **Shortcuts** in Mod Menu.

## Development and building

```powershell
git clone https://github.com/gawlashearin-blip/universal-chat-translator.git
cd universal-chat-translator
.\gradlew test
.\gradlew clean build
```

Use `./gradlew` on Linux/macOS. Artifacts are written to `build/libs/`. The project uses Java 21, Fabric Loom, Yarn 1.21.11 mappings, JUnit 5, and MockWebServer.

Read [CONTRIBUTING.md](CONTRIBUTING.md) before submitting changes. Report security issues privately as described in [SECURITY.md](SECURITY.md).

## License and attribution

Universal Chat Translator is distributed under [CC BY 4.0](LICENSE).

It is based on Real_SShwer's [Mini Chat Translator](https://github.com/ZHENXUWEI/mini-chat-translator-fabric), starting from commit `d94f7a03eff323301bd16864052312b6fa143c18` on `fabric-1.21.11-cloth`. The original CC0 notice is preserved in [LICENSE-ORIGINAL-CC0](LICENSE-ORIGINAL-CC0), with attribution and modification details in [NOTICE.md](NOTICE.md).

Minecraft, Fabric, Google, OpenAI, Ollama, and LM Studio are trademarks of their respective owners. This project is not affiliated with or endorsed by them.
