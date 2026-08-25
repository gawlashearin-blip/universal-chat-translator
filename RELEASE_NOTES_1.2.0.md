# Universal Chat Translator 1.2.0

首个独立 GitHub Release，适用于 Minecraft Java Edition 1.21.11、Java 21 和 Fabric。

## 主要功能

- Google 免费翻译与自定义 OpenAI `chat/completions` 兼容接口。
- 独立的玩家消息、系统消息和中译英开关。
- 玩家名/系统标签、16 种译文颜色和自身消息抑制。
- 收发消息并发翻译并严格按原顺序显示或发送。
- Base URL、Model、可选 API Key、系统提示词、超时和 Temperature。
- 读取未保存表单值的连接测试及脱敏错误反馈。
- 四个默认未绑定的游戏内快捷键。

## 安装

1. 安装 Fabric Loader、Fabric API、Cloth Config 和 Mod Menu 的 Minecraft 1.21.11 版本。
2. 下载并把 `universal-chat-translator-1.2.0.jar` 放入实例的 `mods` 文件夹。
3. 删除所有更早的 Universal Chat Translator JAR，且不要与原版 Mini Chat Translator 同时启用。

## 隐私提醒

在线翻译会把聊天内容发送给选定供应商。自定义 API Key 以明文保存在 TOML 中；远程服务应使用 HTTPS。本 Mod 不会在自定义服务失败后自动把同一消息转发给 Google。

完整用法、兼容接口示例和故障排查请阅读[中文 README](https://github.com/gawlashearin-blip/universal-chat-translator/blob/main/README.md)。
