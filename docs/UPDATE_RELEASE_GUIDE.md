# All-LLM: In-App Updates & Private Release Architecture

This guide explains how **All-LLM** delivers in-app updates for its single universal edition, how to publish future builds from a **private repository**, and how to deploy a zero-token Cloudflare Worker release proxy.

---

## 1. Update Architecture

All-LLM ships one universal `online` build. Linux runtime components are fetched on demand from verified mirrors rather than bundled into the APK:

| Edition | Artifact Name | Approximate Size | Characteristics |
| :--- | :--- | :--- | :--- |
| **Universal** | `alllm-universal-release.apk` | ~85 MB | Lightweight base app. Downloads runtime components dynamically from verified mirrors. |

### How the Release Manifest Works:
1. The APK compiles with `BuildConfig.APP_VARIANT` (`"online"`).
2. The release manifest `alllm-update.json` indexes the build under `artifacts`:
   ```json
   {
     "versionCode": 28,
     "versionName": "2.2.0",
     "notes": "Feature notes...",
     "artifacts": {
       "online": {
         "url": "https://.../alllm-universal-release.apk",
         "sha256": "4b68019e1fa3b07ae7e189873d6ebefae3e0ec05f013d2f928e469e38d77bfbb",
         "sizeBytes": 87421923
       }
     }
   }
   ```
3. `AppUpdater.check()` inspects `BuildConfig.APP_VARIANT` and serves the matching `artifacts` entry.

---

## 2. Managing Updates on a Private GitHub Repository

When your source repository (`NETO-DEV66/All-llm`) is **Private**, GitHub returns **HTTP 404** to unauthenticated download requests. Three production-grade strategies are supported:

### Strategy A: Direct GitHub Releases API with Personal Access Token (Internal Team / Testers)
- In the app, navigate to **Settings → App updates & release channel**.
- Enter:
  - **Manifest / API URL**: `https://api.github.com/repos/NETO-DEV66/All-llm/releases/latest`
  - **Private Repository Token**: A fine-grained GitHub Personal Access Token (PAT) with `Contents: Read-only` permission on `All-LLM`.
- `AppUpdater` connects via `Authorization: Bearer <token>`, fetches release assets, follows the AWS S3 redirect safely without leaking credentials, and downloads the APK.

---

### Strategy B: Free Cloudflare Worker Proxy (Zero-Token in App, Recommended for End-Users)
To distribute updates to users without requiring them to enter a GitHub token or exposing your private source code, use a 100% free Cloudflare Worker as a secure gateway.

#### 1. Deploy the Cloudflare Worker Script:
```javascript
// Cloudflare Worker: updates.yourdomain.workers.dev
const GITHUB_REPO = "NETO-DEV66/All-llm";
// Set GITHUB_TOKEN in Cloudflare Worker Environment Variables (Secrets)
// Token needs: Contents: Read-only on NETO-DEV66/All-llm

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    // 1. Manifest Endpoint (/check or /alllm-update.json)
    if (url.pathname === "/check" || url.pathname.endsWith("alllm-update.json")) {
      const releaseRes = await fetch(`https://api.github.com/repos/${GITHUB_REPO}/releases/latest`, {
        headers: {
          "Authorization": `Bearer ${env.GITHUB_TOKEN}`,
          "User-Agent": "All-LLM-Updater",
          "Accept": "application/vnd.github+json"
        }
      });
      if (!releaseRes.ok) return new Response("Release not found", { status: 404 });
      const release = await releaseRes.json();
      const manifestAsset = release.assets.find(a => a.name === "alllm-update.json");
      if (!manifestAsset) return new Response("Manifest asset missing", { status: 404 });

      // Fetch manifest binary stream
      const manifestRes = await fetch(manifestAsset.url, {
        headers: {
          "Authorization": `Bearer ${env.GITHUB_TOKEN}`,
          "User-Agent": "All-LLM-Updater",
          "Accept": "application/octet-stream"
        }
      });
      return new Response(manifestRes.body, {
        headers: { "Content-Type": "application/json", "Cache-Control": "public, max-age=300" }
      });
    }

    // 2. Direct APK Download Endpoint (/download/online)
    const match = url.pathname.match(/^\/download\/online$/);
    if (match) {
      const targetName = "alllm-universal-release.apk";
      const releaseRes = await fetch(`https://api.github.com/repos/${GITHUB_REPO}/releases/latest`, {
        headers: {
          "Authorization": `Bearer ${env.GITHUB_TOKEN}`,
          "User-Agent": "All-LLM-Updater",
          "Accept": "application/vnd.github+json"
        }
      });
      const release = await releaseRes.json();
      const apkAsset = release.assets.find(a => a.name === targetName);
      if (!apkAsset) return new Response(`Asset ${targetName} not found`, { status: 404 });

      // Fetch asset redirect with stream
      const downloadRes = await fetch(apkAsset.url, {
        headers: {
          "Authorization": `Bearer ${env.GITHUB_TOKEN}`,
          "User-Agent": "All-LLM-Updater",
          "Accept": "application/octet-stream"
        },
        redirect: "follow"
      });
      return new Response(downloadRes.body, {
        headers: {
          "Content-Type": "application/vnd.android.package-archive",
          "Content-Disposition": `attachment; filename="${targetName}"`
        }
      });
    }

    return new Response("All-LLM Update Gateway Active", { status: 200 });
  }
};
```

#### 2. Set the Worker URL in the App:
- Set `appUpdateManifestUrl` in `app/build.gradle.kts` to `https://updates.yourdomain.workers.dev/check`.
- Users download updates instantly without any tokens required in their app!

---

### Strategy C: Dual-Repository Mirror (Private Source + Public Release Binaries)
1. Keep the source code repository (`NETO-DEV66/All-llm`) strictly private.
2. Create an empty public repository (e.g. `NETO-DEV66/All-llm`).
3. In `.github/workflows/android.yml`, add a release step that uploads the compiled APKs and `alllm-update.json` to the public releases repository using `gh release upload --repo NETO-DEV66/All-llm`.

---

## 3. Cryptographic Verification & Tamper Protection

Every downloaded APK must pass three mandatory security checks before `AndroidAppInstaller` is invoked:
1. **Package ID Integrity**: The APK's package name in `AndroidManifest.xml` must match `com.netodev66.alllm`.
2. **SHA-256 Checksum Match**: The file hash must exactly match the checksum specified in `alllm-update.json`.
3. **Permanent Release Certificate Verification**:
   - `AppUpdater.verifyApk()` extracts the APK's signing certificate and compares its SHA-256 digest with the currently installed app's signing key:
   - Expected Release Key SHA-256:
     ```
     86:D4:21:7F:86:E4:0A:B2:58:46:04:C5:6D:C3:6A:FB:54:DF:83:5D:38:2A:A4:FC:65:07:40:C6:00:1D:E0:F5
     ```
   - If an APK was signed with an untrusted debug key or wrong key, installation is aborted immediately with an explicit error to protect users.

---

## 4. How to Cut a New Release in GitHub Actions

To release a new update:
1. Update `versionCode` and `versionName` in `app/build.gradle.kts`:
   ```kotlin
   versionCode = 28
   versionName = "2.2.0"
   ```
2. Commit and tag the commit:
   ```bash
   git add app/build.gradle.kts
   git commit -m "chore(release): v2.2.0"
   git tag v2.2.0
   git push origin main --tags
   ```
3. GitHub Actions (`.github/workflows/android.yml`) will:
   - Build `assembleOnlineRelease`.
   - Sign the APK with the permanent NETO-DEV66 release key.
   - Compute the SHA-256 checksum and file size.
   - Generate `alllm-update.json` with the `online` artifact.
   - Publish the GitHub Release with the tag `v3.5.0`.
4. Users opening All-LLM will automatically see the in-app update banner or dialog and download the new build!
