<div align="center">

<img src="docs/alllm-icon.png" alt="All-LLM logo" width="120"/>

# All-LLM

### The world's first AI software engineering workstation for Android

A platform that does not exist on mobile devices. Run a real Linux development environment, command multi-agent AI engineering teams, automate GitHub workflows, and build Android APKs, all from your phone. No root required.

[![Release v3.5.0](https://img.shields.io/badge/Release-v3.5.0-F28C52?style=flat-square&logo=github&logoColor=white)](https://github.com/NETO-DEV66/All-llm/releases/tag/v3.5.0)
[![CI](https://img.shields.io/github/actions/workflow/status/NETO-DEV66/All-llm/android.yml?branch=main&style=flat-square&label=CI)](https://github.com/NETO-DEV66/All-llm/actions)
[![Android 9+](https://img.shields.io/badge/Android-9%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](#-system-requirements)
[![ARM64](https://img.shields.io/badge/CPU-ARM64-5B8DEF?style=flat-square)](#-system-requirements)
[![License MIT](https://img.shields.io/badge/License-MIT-8B7CF6?style=flat-square)](LICENSE)

<br/>

[**Download APK**](https://github.com/NETO-DEV66/All-llm/releases/download/v3.5.0/alllm-universal-release.apk) &nbsp;·&nbsp;
[**All releases**](https://github.com/NETO-DEV66/All-llm/releases)

</div>

---

## Table of Contents

- [What is All-LLM?](#-what-is-all-llm)
- [Documentation](#-documentation)
- [Design Language](#-design-language)
- [Features](#-features)
  - [Pen Testing](#-pen-testing)
- [System Requirements](#-system-requirements)
- [Installation](#-installation)
- [Building from Source](#-building-from-source)
- [Configuration](#-configuration)
- [Project Structure](#-project-structure)
- [Architecture Overview](#-architecture-overview)
- [Security and Privacy](#-security-and-privacy)
- [Contributing](#-contributing)
- [License](#-license)

---

## 📖 What is All-LLM?

All-LLM is an AI-powered software engineering environment that runs natively on Android. It combines a rootless Linux userspace (Ubuntu 20.04 on ARM64 via PRoot) with a full mobile IDE, multi-agent AI engineering teams, autonomous GitHub workflows, an Android app factory, and hybrid cloud execution.

The app ships as one universal build:

| Edition | Size | Description |
|---|---|---|
| **Universal** | ~150 MB | Downloads Linux runtime stacks on demand from verified releases, SHA-256 checked before use. Best for devices with limited storage. |

Application ID `com.netodev66.alllm`; runtime delivery is the only moving part.

---

## 📚 Documentation

Want the full in-depth guide? Start here:

- [**Architecture Guide**](docs/AGENTIC_MOBILE_IDE_ARCHITECTURE.md) — how the app is put together: agent system, terminal, file manager, marketplace, skills, runtime, and settings.
- [**Play Store Guide**](docs/PLAY_STORE_CHECKLIST.md) — release, review, and content declarations.

---

## 🎨 Design Language

All-LLM uses a liquid-glass design system built on top of Material 3. The look is not
a skin bolted on afterwards: the theme's surface roles themselves are translucent, so
every `Surface`, `Card` and `Scaffold` in the app renders as a pane of glass without
being rewritten one call site at a time.

| Layer | What it does | Where |
|---|---|---|
| `GlassTokens` | Per-theme fills, specular highlights, hairline rims, ambient shadow, scrims | `ui/theme/Glass.kt` |
| `GlassBackdrop` | Animated gradient behind the whole app, single GPU blur pass per frame | `ui/theme/GlassComponents.kt` |
| `GlassSurface`, `GlassPane`, `GlassCard` | The glass containers the screens are built from | `ui/theme/GlassComponents.kt` |
| `Motion` | Shared spring specs and the `bounceClick` press animation | `ui/theme/Motion.kt` |
| `Typography` | Display/headline/body scale tuned for the glass surfaces | `ui/theme/Typography.kt` |

Two deliberate exceptions opt out of translucency. The **code editor** and the
**terminal** use a near-opaque `readingSurface` fill, because 13sp monospace over a
moving gradient is unreadable. Everything else stays glass.

Motion is spring-based rather than tween-based, so presses and screen transitions
settle the way physical objects do instead of easing on a fixed curve.

## ✨ Features

### 📝 Mobile IDE

- **Multi-tab code workspace** with pinned tabs and state restoration across Android backgrounding
- **Built-in file manager**: browse, create, rename, and organize project files without leaving the app
- **Touch-optimized coding toolbar**: auto-pairing brackets, indent controls, comment toggling, quick cursor navigation
- **Universal command palette** for instant navigation between files, chats, terminals, git diffs, and tools
- **Global search and replace** with regex, case sensitivity, and preview-before-replace
- **Crash-safe file operations** using atomic staging transactions

### 💻 Rootless Linux Terminal

- Real Ubuntu 20.04 ARM64 userspace via PRoot - zero root required
- Node.js, Python, Git, Clang/LLVM, Bash/Zsh toolchains
- Persistent terminal sessions, process lifecycle management, and workspace checkpoints/snapshots
- Quick terminal follows the active project's workspace automatically

### 🧠 AI Coding Assistance

- **In-app AI chat**: conversational assistant for coding questions, explanations, and project help, right inside the app
- **Multi-provider gateway**: Anthropic Claude, DeepSeek, OpenRouter, and OpenAI-compatible endpoints with automatic 401/quota failover across accounts
- **Multiple Google accounts** with auto round-robin switching when quota or rate limits are hit
- **Intent classification** (bug fix, feature, refactor, review, explanation, test writing)
- **Context engine** with token-budget-aware relevance ranking over symbols, diffs, and error traces
- **Persistent project memory** retaining architectural decisions and bug history across sessions
- **MCP (Model Context Protocol)** support: connect remote MCP servers, manage server processes, and use a secure credential store
- **Skill packages**: installable expert skill bundles (Compose architect, DevOps packager, and more)

### 🤖 Multi-Agent Engineering Team

- 10 specialized roles: Manager, System Architect, Frontend, Backend, QA, DevOps, Security Auditor, DBA, Docs Writer, UX Designer
- **Real execution**: every role runs genuine agent sessions through the installed runtimes; results are verified, never simulated
- Concurrent DAG task scheduler with per-file locking, budget quotas, and cancellation
- Shared blackboard memory, typed inter-agent messaging, and peer review gates
- Live execution status in the studio header; honest `NOT_CONFIGURED` reporting when a runtime or key is missing

### 🔁 Autonomous GitHub Workflows

- **Issue triage to pull request**: analyzes issues, reproduces bugs, creates branches, edits code, runs real Gradle builds and tests, and opens documented PRs
- **PR automation**: policy-gated auto-merge (CI green, required approvals), reviewer requests, merge-gate status checks
- **Issue management**: rule-based labeling, assignment, and status comments
- **Release automation**: tag creation, generated release notes, asset uploads
- **Workflow dispatch**: trigger and monitor GitHub Actions runs from the app

### 🔍 Semantic Code Intelligence

- AST parsing for Kotlin, Java, TypeScript, JavaScript, Python, Go, Rust, C/C++, and Shell
- Symbol outline, call graphs, and high-speed semantic indexing
- LSP protocol client with real-server detection (honest fallback labeling)

### 🛡️ Safety Engine

- Dangerous command interception (`rm -rf /`, disk formats, fork bombs)
- Sensitive file shielding for `.env`, SSH keys, and keystores
- Granular permission scopes (Once, Session, Project, Always, Deny) with an emergency kill switch and audit logging

### 🎨 Visual Development

- Embedded Chromium preview with live DOM inspector and computed CSS view
- Multi-device viewport presets and localhost port discovery
- Screenshot-based visual dev loop with pixel-diff verification

### 🏭 Android App Factory

- AI app blueprints: screens, navigation, data layer, and state models from a high-level concept
- Project scaffolding for Jetpack Compose, Flutter, React Native, and HTML5 web apps
- On-device Gradle APK builds, APK signing verification, release keystores, and Play Store listing preparation

### 🎯 Pen Testing

A full multi-agent AI penetration testing system running on your phone. Describe a target and a goal in plain language; specialized AI agents plan the work, run professional security tools, and hand you a report.

- One-tap **Setup Pen Testing** from the Studios card: downloads the verified backend package, installs PostgreSQL with pgvector, and boots the backend inside the proot Linux environment. No Docker needed.
- **Open Web UI** launches the backend's web interface at `127.0.0.1:18080` inside the app's own in-app browser.
- Stop, restart, reinstall, and view backend logs from the same screen.
- Requires the core Linux runtime installed first; first setup needs internet once and takes 10-15 minutes.

### ☁️ Cloud Build via GitHub Actions

- Build locally on-device or dispatch to GitHub Actions runners
- GitHub Actions supports any OS: Android, Linux, macOS, Windows, iOS
- Connect via `gh` CLI; dispatch workflows and poll run status from the app
- Cost controls: per-job estimates and rolling 30-day budget caps enforced before dispatch

### 🛒 AI Developer Marketplace

- Curated catalog of agents, MCP servers, skills, and starter templates with one-click install
- Listing validation, TTL-cached discovery, and purchase flow with local entitlement ledger
- Seller center: register, publish, update, unpublish, and view sales reports

### 🔔 Local Notifications

- On-device notification coordinator for runtime and task events
- No cloud messaging service required; notifications stay local to the device

---

## 📋 System Requirements

| Requirement | Minimum | Recommended |
|---|---|---|
| Android version | 9.0 (API 28) | 12+ |
| CPU architecture | ARM64 (aarch64) | ARM64 |
| RAM | 3 GB | 4 GB+ (large builds, multi-agent runs) |
| Free storage | 2 GB | 4 GB (projects, build caches) |
| Root | Not required | - |

The app runs 100% in rootless userspace. No root, no custom ROM, no special permissions beyond normal app sandboxing.

---

## 📲 Installation

1. Go to the [v3.5.0 release page](https://github.com/NETO-DEV66/All-llm/releases/tag/v3.5.0).
2. Download `alllm-universal-release.apk` (signed, installs on any arm64 device).
3. On your phone, allow **Install unknown apps** for your browser or file manager when prompted.
4. Open the APK and tap **Install**.
5. (Online edition) On first launch, the app downloads the verified Linux runtime stacks. Keep the app open and on Wi-Fi during this step.

To update, simply install the newer APK over the existing one. Your projects and settings are preserved (the release APKs are signed with the same permanent release key, so in-place updates always work). The app also checks for new releases on its own and notifies you when an update is available.

---

## 🔨 Building from Source

<details>
<summary><b>Prerequisites</b></summary>

| Tool | Required version | Notes |
|---|---|---|
| JDK | 17 (Temurin recommended) | `java -version` must report 17 |
| Android SDK command-line tools | latest | Set `ANDROID_HOME` / `ANDROID_SDK_ROOT` |
| Android platform | `platforms;android-36` | Compile SDK |
| Android build-tools | `35.0.0` | Used for `apksigner` / `aapt2` in CI |
| Android NDK | `26.1.10909125` (r26b) | Pinned in `app/build.gradle.kts` |
| CMake | `3.22.1` | Only needed when building native code on non-ARM64 hosts |
| Gradle | via wrapper | Do not install separately; use `./gradlew` |
| Git | any recent | With submodule support |
| Disk space | 15 GB free | SDK + NDK + build outputs |

Install the SDK packages (the same command CI uses):

```bash
sdkmanager "platform-tools" "platforms;android-36" "ndk;26.1.10909125" "cmake;3.22.1"
yes | sdkmanager --licenses
```

Key versions used by the project: Android Gradle Plugin `8.13.2`, Kotlin `2.1.0`, Compose BOM `2025.02.00`.

</details>

<details>
<summary><b>Clone</b></summary>

```bash
git clone https://github.com/NETO-DEV66/All-llm.git
cd All-LLM
git checkout v3
git submodule update --init --recursive
```

</details>

<details>
<summary><b>Debug builds</b></summary>

```bash
# Debug build (runtimes downloaded on demand)
./gradlew assembleOnlineDebug
```

APKs land in `app/build/outputs/apk/online/debug/`.

</details>

<details>
<summary><b>Release builds</b></summary>

Release builds require the signing key (see **Release signing** below). With the environment variables set:

```bash
./gradlew assembleOnlineRelease
```

</details>

<details>
<summary><b>Running tests</b></summary>

```bash
# Full unit test suite (1200+ tests)
./gradlew testOnlineDebugUnitTest
```

</details>

### Build variants

| Task | Flavor | Build type | Output |
|---|---|---|---|
| `assembleOnlineDebug` | online | debug | `app/build/outputs/apk/online/debug/` |
| `assembleOnlineRelease` | online | release | `app/build/outputs/apk/online/release/` |
| `testOnlineDebugUnitTest` | online | debug (tests) | test report |

Flavor behavior is controlled by `BuildConfig.OFFLINE_RUNTIME_BUNDLES` and `BuildConfig.APP_VARIANT`. A Play-targeted build is available with `-PplayBuild=true` (targets SDK 36 and runs the Play readiness check via `./gradlew playReadinessCheck -PplayBuild=true`).

### Runtime bundles note

The app ships one universal `online` flavor. It resolves Linux runtime archives (`alllm-*-arm64-*.tar.zst`) at install time from the `NETO-DEV66/All-llm` releases, SHA-256 verified, and publishes them under canonical `alllm-*` names.

---

## ⚙️ Configuration



<details>
<summary><b>API keys and secrets</b></summary>

| Secret | Where it goes | Used for |
|---|---|---|
| `openrouter.apiKey` | `test-secrets.properties` at repo root (gitignored) | Debug-build AI provider testing via `BuildConfig.TEST_OPENROUTER_API_KEY` |
| AI provider keys (Anthropic, OpenAI, etc.) | Entered in-app under provider settings | Stored in the encrypted in-app credential store, never in the repo |
| `MH_RELEASE_*` | Environment variables / CI secrets | Release APK signing (below) |

Create `test-secrets.properties` for local debug builds that exercise AI providers:

```properties
openrouter.apiKey=sk-or-v1-your-key-here
```

The file is optional; builds succeed without it (the key defaults to empty).

</details>

<details>
<summary><b>Release signing</b></summary>

Release APKs are signed with a permanent upload key. Provide it through environment variables (CI reads them from GitHub Actions secrets):

| Environment variable | Purpose |
|---|---|
| `ALLLM_UPLOAD_STORE_FILE` | Path to the `.jks` keystore file |
| `ALLLM_UPLOAD_STORE_PASSWORD` | Keystore password |
| `ALLLM_UPLOAD_KEY_ALIAS` | Key alias |
| `ALLLM_UPLOAD_KEY_PASSWORD` | Key password |

When all four are set, the `upload` signing config is applied to release builds automatically. CI verifies the keystore's SHA-256 certificate fingerprint before building, so a wrong key fails fast instead of shipping a mismatched signature. Never commit keystores or passwords to the repository.

</details>

<details>
<summary><b>Pen Testing</b></summary>

The Pen Testing studio runs a multi-agent AI penetration testing backend on the device.

**What setup does** (one tap on **Setup Pen Testing**):

1. Downloads `pentagi-ondevice-test.tar.gz` (about 57MB) from the `pentagi-ondevice-1` GitHub release and verifies its SHA-256 checksum.
2. Extracts it into the proot Linux environment at `/root/pentagi-setup`.
3. Runs the setup script: installs PostgreSQL with the pgvector extension, initializes the database, and boots the backend with the proot-native executor. No Docker daemon is required.

**Requirements:** the core Linux runtime must be installed first; internet is needed once for the download; first setup takes 10-15 minutes with live progress. Leaving the setup screen cancels the run.

**Using it:** once ready, **Open Web UI** opens the backend interface at `http://127.0.0.1:18080` in the app's in-app browser. **Stop** shuts the backend down; **Start Pen Testing** boots it again; **Reinstall** re-runs setup; **View Logs** tails the backend log at `<rootfs>/root/pentagi-data/backend.log`.

</details>

---

## 🗂️ Project Structure

```
All-LLM/
├── app/
│   ├── build.gradle.kts          # App module: signing, versions
│   └── src/main/
│       ├── java/com/netodev66/alllm/
│       │   ├── agent/            # Multi-agent team runtime, DAG dispatch, roles
│       │   ├── cloud/            # Cloud hybrid execution client and orchestration
│       │   ├── context/          # AI context engine: ranking, budgets, memory
│       │   ├── core/             # OperationResult integrity contract, utilities
│       │   ├── editor/           # Code editor, tabs, search, command palette
│       │   ├── factory/          # Android App Factory: blueprints, scaffolding, builds
│       │   ├── git/              # GitHub automation: workflows, PRs, issues, releases
│       │   ├── lsp/              # Language server protocol client
│       │   ├── marketplace/      # Marketplace: listing, discovery, purchasing
│       │   ├── mcp/              # Model Context Protocol client and servers
│       │   ├── network/          # GitHub REST client, transports
│       │   ├── runtime/          # PRoot Linux runtime installer and sessions
│       │   ├── security/         # Safety gateway, permission engine, auditing
│       │   ├── skills/           # Skill packages
│       │   ├── terminal/         # Terminal emulator and shell sessions
│       │   ├── ui/               # Jetpack Compose screens and ViewModels
│       │   ├── visual/           # Visual dev loop, DOM tools, screenshot diffing
│       │   └── workflow/         # Autonomous agent workflow engine
│       └── res/                  # Android resources
├── .github/workflows/android.yml # CI: build, test, sign, verify, release
├── V3_PROGRESS.md                # Engineering milestone log
├── PRIVACY.md                    # Privacy policy
└── LICENSE                       # MIT
```

---

## 🏗️ Architecture Overview

```
+-------------------------------------------------------------------+
|              All-LLM (v3.5.0)                      |
|                                                                    |
|  +------------------+  +-----------------+  +------------------+   |
|  | Mobile IDE       |  | Visual DevTools |  | Android App      |   |
|  | Editor + Tabs    |  | Chromium + DOM  |  | Factory          |   |
|  | Search + Palette |  | Inspector       |  | Scaffold + Build |   |
|  +------------------+  +-----------------+  +------------------+   |
|                                                                    |
|  +--------------------------------------------------------------+  |
|  |         Multi-Agent Team Orchestration Engine (REAL)         |  |
|  |  Manager > Architect > Specialists > QA > Security Review    |  |
|  |  Real per-role execution - DAG scheduler - Blackboard        |  |
|  |  Honest NOT_CONFIGURED states - no simulated results         |  |
|  +--------------------------------------------------------------+  |
|                                                                    |
|  +---------------+  +----------------+  +----------------------+   |
|  | Cloud Hybrid  |  | AI Context     |  | Marketplace          |   |
|  | Execution     |  | Engine + MCP   |  | Agents, Skills,      |   |
|  | Cost controls |  | Multi-provider |  | Templates            |   |
|  +---------------+  +----------------+  +----------------------+   |
|                                                                    |
|  +--------------------------------------------------------------+  |
|  |              Safety Engine                                   |  |
|  |  Command interception - File shielding - Permissions - Audit |  |
|  +--------------------------------------------------------------+  |
|                                                                    |
|  +--------------------------------------------------------------+  |
|  |  Pen Testing: multi-agent AI penetration testing backend    |  |
|  |  PostgreSQL + pgvector - Web UI at 127.0.0.1:18080          |  |
|  |  One-tap setup - proot-native execution, no Docker needed   |  |
|  +--------------------------------------------------------------+  |
|                                                                    |
|  +--------------------------------------------------------------+  |
|  |        PRoot Linux Userspace (Ubuntu 20.04 ARM64)             |  |
|  |  Node.js - Python - Git - Clang - No root required           |  |
|  +--------------------------------------------------------------+  |
|                                                                    |
|  +--------------------------------------------------------------+  |
|  |  |  Identity - Notifications - Crash Reporting     |  |
|  +--------------------------------------------------------------+  |
+-------------------------------------------------------------------+
```

---

## 🔒 Security and Privacy

1. **Rootless isolation**: all compiler and shell execution runs inside an isolated PRoot Linux userspace. Host Android partitions remain protected.
2. **Dangerous command interception**: real-time inspection blocks destructive commands (`rm -rf /`, disk formats, fork bombs) before execution.
3. **Sensitive file protection**: `.env` files, SSH keys, certificates, and keystores are shielded from agent transcripts and external outputs.
4. **Local-first processing**: code parsing, AST indexing, git operations, and test runs happen on-device.
5. **Honest status reporting**: every subsystem reports verified outcomes only. Unconfigured backends report `NOT_CONFIGURED`; simulations are labeled `SIMULATED` and never count as success.

See [PRIVACY.md](PRIVACY.md) for the full privacy policy.

---

## 🤝 Contributing

1. Fork the repository and create a feature branch (`git checkout -b feature/my-change`).
2. Make your changes with tests. New features must include unit tests; pure-JVM tests are preferred (see existing `*Test.kt` files for the pattern).
3. Run the full verification locally: `./gradlew testOnlineDebugUnitTest assembleOnlineDebug`.
4. Open a pull request against `v3`. CI must be fully green before merge.
5. Follow the `OperationResult` honesty contract: never report success without a verified result, and never commit secrets or keystores.

---

## 📄 License

This project is licensed under the [MIT License](LICENSE).
Copyright (c) 2024-2026 All-LLM Contributors.
