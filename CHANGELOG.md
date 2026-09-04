# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.11.0] - 2026-09-04

### High value
- Dev Loop session state + progress bar (persist phase, advance/reset)
- Sandbox file browser (view/edit with diff, share path, delete)
- GitHub commit wizard (repo, branch, path, message, content → diff → push)
- Diff review before sandbox overwrite and GitHub push
- First-run onboarding checklist

### Quality / pro
- Pin / unpin conversations + search sessions
- Approximate token estimate after replies
- Prompt templates library (on-device, editable)
- Metadata backup export (no raw keys)
- Larger text mode + content descriptions

### Connectors
- Generic HTTPS webhook tester with request log
- Read-only GitLab project list + Bitbucket repo list

## [0.9.0] - 2026-09-04

### Added
- **Image studio** — text-to-image via OpenAI-compatible `/images/generations`
- Model + size controls, on-device preview, **save to private sandbox**, share
- `ImageGenerationClient` (b64_json + URL fallback)
- Agent tool `image_generate`
- Drawer + command palette entry

### Notes
- Requires an images-capable API key (e.g. OpenAI). Not all chat providers support image generation.
- Images stay on-device until you share or save them.

## [0.8.0] - 2026-09-04

### Added
- **Dev Loop Agent** — Plan → Code → Test → Review → Edit → Debug → Loop decision
- Dedicated **Dev Loop** workspace with phase cards and one-tap start
- Sandbox **folders**: `src/`, `tests/`, `out/`, `notes/` under app-private phone storage
- Clear **storage location** path shown in UI (app-only internal storage)
- Honest messaging: full Ubuntu VM is not bundled; private sandbox is the on-device alternative

### Why this design
- Closed engineering loops are excellent for quality
- Full Ubuntu inside a normal Android app is unsafe/impractical; private app storage is the right phone model

## [0.7.0] - 2026-09-04

### Added
- **Private file sandbox** with create/list/zip (bounded app directory, not a full Linux VM)
- **Sandbox tools** in the agent registry (list, write, read, zip)
- Expanded **safe CLI allow-list** (more read-only git diagnostics)
- **GitHub token auto-detect** expanded: `ghp_`, `github_pat_`, `gho_`, `ghu_`, `ghs_`, `ghr_`
- **Hermes upgrades**: handoff pack button, structured starters (PR, debug, release, sandbox→zip), stronger Plan/Process/Review/Handoff prompt
- **Git tools** in registry: status, log, commit-file-via-API
- **More connectors**: GitLab (PAT), Bitbucket (token), generic webhook
- Hermes timeline + checkpoint/handoff tool specs

### Security
- Sandbox writes require explicit confirmation
- Zip stays inside private sandbox
- Shell still blocks pipes, redirects, chaining, substitution

## [0.6.0] - 2026-09-04

### Added
- **Dual-column Arena** on landscape / wide screens
- **Home-screen App Widget** (open workspace)
- **App lock unlock gate** (explicit unlock; dependency-free)
- **PDF conversation export** (on-device, redacted)
- **Hermes timeline cards** after Hermes-agent answers
- **GitHub PR comment tool** (confirmation-gated)
- **Email draft connector** (system composer only)
- **Calendar connector** (open system calendar, read-only)
- **Telegram connector card**
- Expanded command palette entries for PDF, lock, email, calendar

### Security / professional
- Manifest declares biometric permission for future stronger unlock paths
- Widget opens the app only; no background agent work
- All new write paths remain review-first

## [0.5.0] - 2026-09-04

### Added
- **Command palette** (toolbar + Ctrl/Cmd+K) for quick navigation
- **Project / system instructions** editor (pinned, prepended to prompts)
- **Local artifact search** (keyword RAG-style injection into composer)
- **Export conversation** as redacted Markdown
- **Arena speed badges** (elapsed time, chars, chars/sec per model)
- **Retry as Fast / Balanced / Deep** on assistant messages
- **Smart memory suggestions** after substantial answers
- **Expanded safe phone control** (Wi-Fi, Bluetooth, Location, Battery, Display, Sound, Apps, Camera, Dialer)
- **Keyboard shortcuts**: Ctrl+K palette, Ctrl+N new chat, Ctrl+E export, Ctrl+, settings, Esc stop
- **More agent tools**: PR review, artifact search, system instructions, memory suggest, Slack/n8n draft queues, Telegram webhook, voice session, arena share
- **Security preferences**: app-lock flag + stronger export redaction
- Continuous voice mode toggle

### Professional / security
- Review-first phone surface documented and expanded
- Export strips common API key / token patterns
- Write tools remain confirmation-gated

## [0.4.0] - 2026-09-04

### Added
- **Suggested follow-up chips** after every assistant answer (assistant-style)
- **Prominent Fast / Balanced / Deep reasoning pills** directly above the composer
- **Light / Dark theme toggle** (one-tap, persists)
- Expanded **AI actions menu** (modern assistant style): improve writing, make concise, expand, explain code, review bugs, generate tests, security review, convert to TS/Kotlin, create file, extract JSON, summarize, compare approaches, brainstorm alternatives
- Arena closer to **dual-model** (live side-by-side branding + clearer A/B)

### Improved
- Full theme-aware color system (proper light mode palette)
- Reasoning mode is now one-tap and always visible
- Follow-up suggestions adapt to code / errors / long answers

## [0.3.0] - 2026-09-04

### Added / Improved
- **Modern modern assistant inspired UI polish**
  - Refined deep dark color palette with softer surfaces and clearer hierarchy
  - Cleaner message bubbles (user bubbles with refined purple, assistant identity + model badge)
  - Live streaming caret (▍) with subtle blink for a true “typing” feel
  - Live processing label now shows elapsed time + characters + approximate chars/sec
- **Better Markdown rendering**
  - Support for italic, improved headers (size + color), cleaner fenced code blocks and inline code
- **dual-model-style comparison upgrades**
  - Clearer A / B badges, better model labels, improved empty & streaming states
  - Streaming caret in both Arena panels
  - Polished intro copy and action buttons
- Version bumped to **0.3.0**

### Notes
The original calm dark aesthetic is preserved and elevated — still private-first, still explicit confirmation for writes.

## [0.2.0] - 2026-09-04

### Added
- Full GitHub Actions workflows under `.github/workflows/` (CI, release, CodeQL)
- Dependabot configuration for GitHub Actions and Gradle
- MIT License
- CONTRIBUTING.md and SECURITY.md
- Expanded tool registry with advanced tools:
  - `github_list_prs` – list pull requests
  - `github_workflow_runs` – inspect GitHub Actions runs
  - `memory_search` – semantic search over local encrypted memory
  - `artifact_diff` – unified diff between artifacts
  - `provider_health` – lightweight provider endpoint health checks
  - `export_conversation` – redacted conversation export (Markdown / JSON)
- Professional project scaffolding (issue templates ready, improved CI quality gates)

### Changed
- Version bumped to 0.2.0
- CI now includes secret scanning and large-file checks
- Release workflow can create GitHub Releases with the APK artifact

### Security
- Added explicit secret-pattern scanning in CI
- Documented responsible disclosure process

## [0.1.0] - Initial public snapshot

- Provider-neutral Android AI workspace
- Chat, artifacts, connectors (GitHub, Vercel, n8n, Slack, Notion, Linear, Supabase, Discord)
- Guarded agent tools and Hermes-style workflow
- Local encrypted memory vault and API key store
- Web search + model arena
- Unit tests for catalogs, policies, and detectors
