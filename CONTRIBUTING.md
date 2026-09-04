# Contributing to Kairo

Thank you for your interest in improving Kairo — a provider-neutral Android AI workspace.

## Code of Conduct

Be respectful, constructive, and focused on the technical problem. Harassment or personal attacks will not be tolerated.

## Development setup

1. Clone the repository.
2. Open the project in Android Studio (Hedgehog / Ladybug or newer recommended) or use the command line with JDK 17+.
3. The project uses AGP 8.6.1 and targets SDK 35 / minSdk 26.
4. No third-party runtime libraries are required beyond the Android SDK and JUnit for tests.

```bash
# Run unit tests
gradle testDebugUnitTest

# Assemble debug APK
gradle assembleDebug
```

If a Gradle wrapper is present:

```bash
./gradlew testDebugUnitTest assembleDebug
```

## Pull request process

1. Fork the repository and create a feature branch from `main`.
2. Make focused, well-scoped changes. Prefer small PRs.
3. Add or update unit tests when changing agent policies, catalogs, detectors, or core storage logic.
4. Ensure `testDebugUnitTest` and `assembleDebug` succeed.
5. Update the README or docs if you change user-visible behavior or architecture.
6. Open a pull request with a clear description of *why* the change is needed.

## Design principles (please respect)

- **Explicit over magical** — Tools that perform writes (GitHub push, Vercel deploy, Slack message, webhooks, code execution, phone intents) must require visible user confirmation.
- **No background automation** — The client never performs silent external writes or long-running background jobs on behalf of the user.
- **Local-first security** — API keys and memory vault contents stay encrypted with Android Keystore. Never log or transmit raw secrets.
- **Minimal dependency surface** — Prefer platform APIs. New third-party libraries require strong justification.
- **Auditable agent tools** — New tools belong in `ToolRegistry` with clear descriptions and write/read classification.

## Areas that welcome contributions

- Additional provider adapters or live model catalog refreshers
- New read-only or confirmation-gated tools
- UI polish and accessibility improvements in the platform-view workspace
- Expanded unit tests for policy and catalog classes
- Documentation, translations, and example workflows
- CI / release pipeline improvements

## Reporting security issues

Please do **not** open a public issue for security vulnerabilities. Contact the maintainers privately with details so a coordinated fix can be prepared.

## License

By contributing you agree that your contributions will be licensed under the MIT License that covers this project.
