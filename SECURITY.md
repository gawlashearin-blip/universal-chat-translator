# Security Policy

## Supported version

Only the latest published release is supported with security fixes.

## Reporting a vulnerability

Please do not disclose vulnerabilities, API keys, private chat content, or server credentials in a public Issue.

Use GitHub's **Private vulnerability reporting** feature on the repository Security page. Include:

- affected version;
- impact and realistic attack scenario;
- reproduction steps or proof of concept;
- any suggested mitigation.

If private vulnerability reporting is unavailable, contact the repository owner through the private contact method shown on their GitHub profile. Do not attach a real `universal-chat-translator.toml`; replace all secrets and private content with placeholders.

## Security boundaries

- API keys are stored as plain text by explicit design and must be protected like any local credential file.
- Remote HTTP does not protect API keys or chat content in transit; use HTTPS for remote providers.
- Chat sent to an online translation engine leaves the Minecraft client and is governed by that provider's policies.
- The mod does not automatically retry a custom-provider failure through Google.
