# Kairo

Kairo is a provider-neutral Android AI workspace written in Java. It combines a calm, modern modern assistant-inspired chat surface with live token streaming (caret + speed indicator), an dual-model dual-model comparison, local artifacts, explicit service connectors, and guarded agent tools.

> Kairo is a client: prompts and API traffic go directly from the device to the provider or endpoint selected by the user. It is not an API-key proxy.

## Included

- **Chat workspace** with a modern modern assistant-inspired dark UI, refined message bubbles, model badges, conversation drawer, searchable thread history, automatic titles, rename/share/clear, copy/retry, and starter prompts.
- **Responsive composer + live streaming** with real-time token output, blinking streaming caret, elapsed time + chars/sec indicator, stop-generation, improved markdown (bold/italic/headers/code), bounded attachments, vision hints, voice input, model selection, tool shortcuts, Fast/Balanced/Deep reasoning, Hermes plan/process/review, and a 32k prompt guardrail.
- **Artifacts workspace** for creating private files, generating code through the AI composer, saving generated code blocks, editing in a monospace preview, copying, sharing, exporting, deleting, and bounded safe storage. Language presets cover JavaScript, TypeScript, Kotlin, Java, Linux shell, Python, HTML, CSS, and JSON.
- **Provider adapters** for OpenRouter, fast-provider, Kimi / Moonshot, NVIDIA NIM, Mistral AI, Anthropic Messages, OpenAI, custom OpenAI-compatible endpoints, and Ollama. fast-provider has a one-tap Fast chat route, while Kimi / Moonshot includes candidate deep-reasoning models and uses the same live SSE/stop-generation experience.
- **Web search and dual-model mode** with Brave Search + DuckDuckGo fallback, user-selected source insertion, and polished dual-panel live streaming comparisons (A/B badges, independent stop, copy & save).
- **Connectors workspace** for GitHub, Vercel, n8n, Slack, Notion, Linear, Supabase, and Discord webhooks. Inspect GitHub context, review Vercel projects/deployments, create a confirmed Git-backed deployment, inspect n8n workflows/executions, search Notion or Linear issues, preview Supabase rows, or send reviewed team updates.
- **Device setup and provider login** with a private installation id, local pairing label, device profile, setup checklist, and official browser sign-in links. Kairo never receives provider passwords and stores only encrypted tokens.
- **Secure API key setup** using an AES/GCM key held by Android Keystore. Existing secrets are never displayed back in Settings. Pasting a recognizable provider credential into Chat triggers a local detector, pauses sending, shows only a masked preview, and offers an explicit save-and-remove action; credentials are never saved silently or sent to the model.
- **Memory vault** for user-approved profile, preference, project, instruction, and note context. Memories are encrypted with Android Keystore, bounded before provider requests, editable/deletable from the vault, never inferred into storage without review, and reject recognizable API credentials.
- **Curated model catalog** with free-route / free-tier notes, local Ollama models, and a 50+ entry NVIDIA candidate index. NVIDIA candidates are deliberately labeled as candidates—not free or guaranteed—while the NVIDIA live refresh uses the official `/v1/models` response for the currently saved key, account, region, credits, quotas, and endpoint availability. Live snapshots replace stale IDs.
- **Agent workspace** with Code, Hermes Orchestrator, GitHub, CLI, Safe Phone, Research, Artifact, Browser, Automation, and Arena agents. Hermes makes Plan → Process → Review → Handoff visible and keeps the user in the loop. Skills are configurable in Settings and are injected from a fixed, reviewable catalog; they shape responses but never grant a tool or device permission. An explicit tool registry documents each capability, whether it reads or writes, and whether it uses the network; tools are not silently granted to a model.
- **GitHub tools** for repository summaries, open issues, README pulls, confirmed single-file pushes through the Contents API, confirmed pull-request creation, listing PRs, and inspecting GitHub Actions workflow runs.
- **Advanced tools (v0.2)** including local memory search, artifact unified diff, provider health probes, and redacted conversation export.
- **Sandbox console** with an allow-list (`pwd`, `ls`, `git status`, `git diff --stat`, branch/log commands, `uname -a`, and `id`) and runtime discovery. It is explicitly an Android app-process toolbox rather than a full Linux VM: chaining, redirects, pipes, substitution, root access, mutation, package installs, and arbitrary commands are blocked. Full Ubuntu/Docker is not bundled.
- **Artifact run/check** with a review prompt, private temporary directory, direct runtime invocation when Node, Deno, Python, Java, or Kotlin tooling exists, ten-second timeout, bounded output, and shell syntax checking only. It never downloads runtimes and clearly reports when Android lacks a requested compiler/runtime.
- **Safe phone assistant** with a Panda-like, review-first screen for opening the browser, Android Settings, Wi-Fi settings, camera, or dialer. Actions use visible Android intents and never silently call, message, read private apps, root, or control the device in the background.
- **AI action menu** with one-tap prompts for improving writing, explaining code, reviewing bugs/security, generating tests, converting to TypeScript or Kotlin, extracting JSON, summarizing a thread, and creating complete file artifacts.
- **Professional CI/CD** with active GitHub Actions workflows (`.github/workflows/`): full Android CI (unit tests + debug APK), release pipeline that can publish GitHub Releases, CodeQL security analysis, Dependabot for Actions & Gradle, plus secret-pattern scanning and quality gates. Templates remain available under `docs/github-actions/` for reference.

## Build locally

1. Install Android Studio or the Android SDK, Java 17, and Gradle 8.7.
2. Install Android platform 35 and build tools 35.0.0.
3. From the repository root, run:

   ```bash
   gradle testDebugUnitTest assembleDebug
   ```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

The repository keeps the Gradle distribution out of source control. The included workflow provisions the exact Gradle version, Java runtime, and Android SDK packages on GitHub Actions.

## First run

1. Open **Models** or the model chip in **Chat** and choose a provider/model. Kimi / Moonshot candidate IDs are not guarantees; refresh after adding a Kimi key.
2. Open **Settings → Manage key** for that provider. Kairo encrypts the value locally with Android Keystore.
3. Start a chat. Local Ollama models do not require an API key; for an Android emulator, the default Ollama URL is `http://10.0.2.2:11434`.
4. Use **Web search** from the drawer for live source cards. Add an optional Brave Search key under **Settings → Brave Search** for richer results; otherwise Kairo tries DuckDuckGo Instant Answers.
5. Use **Artifacts** to create/edit private files or tap **Save as file** under a model answer. Use **Model arena** to stream one prompt through two selected models in parallel.
6. Open **Device setup** for the local install checklist and official browser login pages. Return to Kairo and paste only the scoped token needed by each service. You can also paste a recognizable token directly into Chat: Kairo detects it locally, blocks sending, and offers **Save securely** followed by removal from the draft.
7. Open **Memories** from the drawer, Chat menu, or Settings to add and review encrypted durable context. Messages beginning with “remember that”, “I prefer”, “my project is”, and similar phrases produce review-only suggestions; nothing is stored automatically.
8. Open **Skills & language** from the drawer or Settings to select fixed response skills and a JavaScript, TypeScript, Kotlin, Java, or Linux shell artifact preset. Skills are included in the next request's system guidance.
9. Open **Connectors** to configure GitHub, Vercel, n8n, Slack, Notion, Linear, Supabase, or Discord. Vercel can inspect projects/deployments and create a Git-backed deployment; n8n can inspect workflows/executions and run one configured webhook after you review the JSON payload; Linear provides read-only issue search.
10. Use **Safe phone** for visible, confirmation-gated browser/settings/Wi-Fi/camera/dialer intents. Use **Sandbox console** for the bounded local diagnostics allow-list and runtime report; it is not an unrestricted Linux or Ubuntu shell.
11. Use **Settings → Generation controls** to choose Concise/Balanced/Detailed output plus Fast/Balanced/Deep reasoning. Deep mode requests stronger planning and verification without exposing hidden chain-of-thought.
12. For GitHub tools, create a fine-grained token with only the repository permissions you need, save it under **Settings → GitHub Agent**, and review the second confirmation dialog before every write.

Provider free tiers, model IDs, quotas, and availability change. The catalog labels are guidance rather than a promise of unlimited free usage; use **Refresh** when a provider exposes a live model list.

## Project layout

```text
app/src/main/java/com/kairo/app/
├── MainActivity.java              # platform-view workspace UI
├── agent/                         # explicit tools and prompt templates
├── core/                          # Keystore, memory vault, device profile, preferences, history, and artifacts
├── data/                          # model, chat, artifact, agent, and connector catalog types
└── network/                       # provider, search, GitHub, Vercel, n8n, and service REST clients
.github/
├── workflows/
│   ├── android.yml                # CI: unit tests + debug APK + quality gates
│   ├── release.yml                # Release APK + GitHub Release on tags
│   └── codeql.yml                 # CodeQL security analysis
└── dependabot.yml                 # Automated dependency updates
docs/github-actions/               # Reference templates
LICENSE                            # MIT
CONTRIBUTING.md                    # Contribution guide
SECURITY.md                        # Security policy & disclosure
CHANGELOG.md                       # Version history
```

## Security notes

- Do not commit API keys, GitHub tokens, signing files, or `local.properties`.
- Cloud keys and the memory vault are encrypted at rest with Android Keystore and are only read immediately before a request or local review.
- Recognizable credentials pasted into Chat are detected locally, shown only in masked form, blocked from being sent, and saved only after explicit user confirmation. Local chat-history persistence and transcript sharing redact recognized provider credentials as a defense in depth.
- The app allows cleartext traffic only because a user may point Ollama at a local/LAN HTTP endpoint. Cloud provider URLs are HTTPS by default.
- GitHub, Vercel, n8n, Slack, and Discord write operations are separate, visible actions; there is no background push, deployment, workflow activation, message, or webhook behavior. Linear is read-only in Kairo.
- Search results are not silently attached to prompts: the user must select **Use in chat**. Generated artifacts are private app files and are only shared when the user chooses Share. Image attachments are read into a bounded 3 MB inline payload and can be removed from the composer before sending.
- n8n API access uses the configured instance URL and API key. Webhook execution is independently configured and requires a second confirmation showing the JSON payload.
- Supabase access is intentionally read-only and limited to a 20-row table preview. Discord and n8n webhook secrets are stored as encrypted values and are not forwarded to management APIs.
