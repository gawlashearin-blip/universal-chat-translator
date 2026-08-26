# Changelog

## 1.2.1

- Replaced the production OkHttp/Okio networking stack with Java 21's built-in HTTP client.
- Fixed `NoSuchMethodError` crashes caused by older OkHttp classes bundled by Lunar Client and other modified clients.
- Preserved custom endpoint timeouts, redirect blocking, API key handling, protocol validation, and error redaction.
- Reduced the number of third-party libraries embedded in the release JAR.

## 1.2.0

- Added optional single-key shortcuts for player translation, system translation, Chinese-to-English sending, and opening the config screen.
- Added a Mod Menu shortcuts foldout with duplicate-binding validation; all shortcuts default to unbound.
- Added action-bar toggle feedback, immediate config persistence, and defensive priority for manually duplicated TOML bindings.
- Pending incoming translations are suppressed after their category is disabled; pending outgoing translations send the original text when disabled.

## 1.1.3

- Recognizes online player names after server-specific level, icon, rank, and styled-component prefixes.
- Extracts only the text after the player chat separator, preventing ranks and usernames from being translated as system text.

## 1.1.2

- Restored reliable `ALLOW_CHAT`/`ALLOW_GAME` capture for compatibility with chat-mod and server message pipelines.
- Defers captured messages until the end of the client tick, preserving original-message-before-translation display order.

## 1.1.1

- Fixed player chat being ignored when Fabric supplies a nullable `GameProfile` in the `CHAT` event.
- Added fallback speaker recovery for unsigned chat and stronger plugin nickname/full-width separator recognition.

## 1.1.0

- Fixed the custom API connection-test button and moved it to a reliable root-level config entry.
- Added separate player/system message switches, plugin-chat speaker recognition, and action-bar exclusion.
- Added all 16 standard Minecraft translation colors and sender-name/system labels.
- Prevented native and plugin-channel self messages from being translated again.
- Changed outgoing Chinese display to a single local original line followed by the normal server English echo.
- Added independent ordered asynchronous queues for incoming and outgoing messages.

## 1.0.0

- Forked the Minecraft 1.21.11 Fabric implementation into the independent Universal Chat Translator project.
- Added configurable OpenAI-compatible `chat/completions` translation with Base URL, model, optional API key, system prompt, timeout, and temperature.
- Added an in-game connection test with latency, translated output, and redacted errors.
- Retained Google free translation and removed the Python, Baidu, and Google Cloud engines.
- Added explicit translation failure behavior, redirect protection, HTTP transport warnings, localization, attribution, and protocol tests.
