# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |
| < 0.1   | :x:                |

## Reporting a Vulnerability

If you discover a security vulnerability in Kairo, please report it responsibly.

**Do not** create a public GitHub issue for security problems.

Instead, open a private security advisory on the repository (GitHub Security Advisories) or contact the maintainers directly.

Please include:

- A clear description of the issue
- Steps to reproduce
- Potential impact
- Any suggested mitigation

You can expect an initial response within a few days. We will work with you to understand and address the issue as quickly as possible.

## Security Design Notes

Kairo is designed with the following principles:

- API keys and the local memory vault are encrypted at rest using the Android Keystore.
- Recognized credentials pasted into chat are detected, masked, and never sent to providers without explicit user confirmation.
- Write operations (GitHub, Vercel, Slack, Discord, n8n, code execution, phone intents) require visible confirmation.
- There is no background push, deployment, or webhook behavior.
- Search results and artifacts are only attached or shared when the user explicitly chooses to do so.
- Cleartext HTTP is allowed only to support local Ollama / LAN endpoints; cloud providers default to HTTPS.

Thank you for helping keep Kairo and its users safe.
