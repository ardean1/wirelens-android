# WireLens Phone (Android)

**See which apps on this phone are making network connections** — destinations (IP/host), counts, and plain-language “why this might matter” hints.

**Free / donation-only.** Optional Cash App tip [$AnthonyDean16](https://cash.app/$AnthonyDean16). Not a paid product. No Play Billing in this build.

> **Not the Windows app.** PC WireLens uses `start.bat` in the [wirelens](https://github.com/ardean1/wirelens) package. This project is the **phone APK** only.

## Project intent (Play + sideload)

- **applicationId (stable):** `com.ardean.wirelens`
- **compileSdk / targetSdk:** **36** (Play requirement)
- **versionCode:** `1` · **versionName:** `1.0.0`
- **Privacy:** all monitoring is **on-device**; traffic metadata is **not** uploaded to Ardean or a cloud backend
- **VPN permission:** Android will ask you to approve a **local VPN-style monitor** so WireLens can see per-app destinations **without root**. This is not a commercial VPN and does not send your traffic to a remote VPN server for “privacy browsing”
- **Honest product:** **not antivirus**, not a malware scanner, not a guarantee — heuristic hints only; HTTPS contents are **never** decrypted
- **Distribution now:** sideload **debug APK** from GitHub Releases (below)
- **Play Store later:** a Play listing **may come later**; Play needs a **release-signed AAB** (not the debug APK). Feature graphic + high-res icon are **later Play Console assets** (see `play-store/`)

## Download the Android app (phone)

1. On your **phone**, open **Chrome** and go to:  
   **https://github.com/ardean1/wirelens-android/releases**

   Direct APK (Chrome on phone):
   **https://github.com/ardean1/wirelens-android/releases/download/android-debug-2026-09-15/WireLens-debug.apk**
2. Open the **latest release**.
3. Tap **`WireLens-debug.apk`** (the `.apk` file).
4. Allow install from Chrome / that source if asked → tap **Install**.
5. Brand menus differ — search: `sideload APK` + your phone brand/model.

**Play Store listing may come later.** Until then, use GitHub Releases (or a local build).

Local/debug build path on the build machine: `/workspace/WireLens-debug.apk`.

Optional: scan the APK on VirusTotal yourself for transparency only — **not** a vulnerability-free claim.

## What it shows

- App name (from UID / package)
- Destination IP (and hostname when DNS was seen)
- Protocol / port, packet count
- Short **“Why this might matter”** text for heuristic flags (unknown host, uncommon port, CGNAT tag)

## What “unwanted” means here

Simple **heuristics** so a human can look twice — **not** malware detection.  
**Next step:** note the app → check if you expect it → don’t panic. WireLens does not remove apps.

## How monitoring works

WireLens starts a **local `VpnService`**:

1. You approve Android’s VPN consent screen (required for per-app destination visibility without root).
2. Packets are observed on-device; destinations are attributed by **UID → app** when the OS provides it.
3. UDP/TCP are **forwarded locally** so the phone keeps working (best-effort userspace relay — not a full commercial VPN stack).
4. **Nothing is uploaded.** Closing/stopping the monitor tears down the local VPN session.

### Honest limits

- Not antivirus; not a guarantee
- VPN permission prompt is required
- HTTPS / TLS contents are **not** decrypted
- UID attribution can be incomplete on some connections/OS versions
- Debug APK is for sideload/testing — Play upload needs a **release AAB** + signing key you control

## Privacy

See [PRIVACY.md](PRIVACY.md) and [play-store/LISTING.md](play-store/LISTING.md).

## Rebuild (Linux with Android SDK)

```bash
cd /workspace/wirelens-android
# needs ANDROID_HOME or local.properties sdk.dir
./gradlew assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk /workspace/WireLens-debug.apk
```

Release / Play later:

```bash
# Create your own keystore; do not commit secrets.
# Put keystore path + passwords in android/key.properties (gitignored) — same pattern as Marion Wx Map.
./gradlew assembleRelease   # or bundleRelease for Play AAB
```

Document signing in [ANDROID.md](ANDROID.md).

## Optional support

| Method | Handle |
|--------|--------|
| Cash App | [$AnthonyDean16](https://cash.app/$AnthonyDean16) |

Tips are optional and unrelated to runtime. No cloud payment phone-home.

## License

[MIT](LICENSE)

## Credits

Built by **Tony Dean** (Ardean). Assisted by Grok Bot.
