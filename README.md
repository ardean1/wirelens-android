# WireLens Phone (Android)

**See which apps on this phone are using the network** — per-app bytes in/out, recent rate, and plain-language “why this might matter” hints.

**Free / donation-only.** Optional Cash App tip [$AnthonyDean16](https://cash.app/$AnthonyDean16). Not a paid product. No Play Billing in this build.

> **Not the Windows app.** PC WireLens uses `start.bat` in the [wirelens](https://github.com/ardean1/wirelens) package. This project is the **phone APK** only.

## Project intent (Play + sideload)

- **applicationId (stable):** `com.ardean.wirelens`
- **compileSdk / targetSdk:** **36** (Play requirement)
- **versionCode:** `2` · **versionName:** `1.0.1`
- **Privacy:** all monitoring is **on-device**; usage metadata is **not** uploaded to Ardean or a cloud backend
- **Works beside a real VPN:** default Start Monitor uses **Usage Access** + `NetworkStatsManager` in a foreground service. It does **not** claim Android’s VPN slot (`VpnService` / TUN), so commercial VPNs keep working
- **Honest product:** **not antivirus**, not a malware scanner, not a guarantee — heuristic hints only; HTTPS contents are **never** decrypted
- **Distribution now:** sideload **debug APK** from GitHub Releases (below)
- **Play Store later:** a Play listing **may come later**; Play needs a **release-signed AAB** (not the debug APK). Feature graphic + high-res icon are **later Play Console assets** (see `play-store/`)

## Download the Android app (phone)

1. On your **phone**, open **Chrome** and go to:  
   **https://github.com/ardean1/wirelens-android/releases**

   Direct APK (Chrome on phone):
   **https://github.com/ardean1/wirelens-android/releases/download/android-debug-2026-09-15-vpn-coexist/WireLens-debug.apk**
2. Open the **latest release**.
3. Tap **`WireLens-debug.apk`** (the `.apk` file).
4. Allow install from Chrome / that source if asked → tap **Install**.
5. Brand menus differ — search: `sideload APK` + your phone brand/model.

**Play Store listing may come later.** Until then, use GitHub Releases (or a local build).

Local/debug build path on the build machine: `/workspace/WireLens-debug.apk`.

Optional: scan the APK on VirusTotal yourself for transparency only — **not** a vulnerability-free claim.

## What it shows

- App name / package
- Bytes in / out (recent cumulative window) and approximate rate
- Short **“Why this might matter”** text for heuristic flags (sudden busy, high background use)
- Destination IP/host: **not available in this mode** (honest placeholder). Per-destination visibility would require taking Android’s single VPN slot — we choose coexistence with your real VPN instead

## What “unwanted” means here

Simple **heuristics** so a human can look twice — **not** malware detection.  
**Next step:** note the app → check if you expect it → don’t panic. WireLens does not remove apps.

## How monitoring works

WireLens starts a **foreground service** (`WireLensMonitorService`):

1. Grant **Usage Access** (Settings) when prompted — required for `NetworkStatsManager` per-app totals.
2. Optionally allow notifications (Android 13+) so the ongoing monitor notification can show.
3. The service polls Wi‑Fi / mobile usage by UID, maps UID → app label, and updates the on-screen list with bytes and rates.
4. If a **TRANSPORT_VPN** network is active, status shows that the monitor works **with** your VPN.
5. **Nothing is uploaded.** Stop tears down the foreground service only — it never owns the VPN slot.

### Honest limits

- Not antivirus; not a guarantee
- Usage Access is required (not VPN permission for normal use)
- Destination IPs / hostnames are **not** shown in coexistence mode
- HTTPS / TLS contents are **not** decrypted
- Rates are approximate (poll-interval deltas)
- Debug APK is for sideload/testing — Play upload needs a **release AAB** + signing key you control

## Privacy

See [PRIVACY.md](PRIVACY.md) and [play-store/LISTING.md](play-store/LISTING.md).

## Rebuild (Linux with Android SDK)

```bash
cd /workspace/wirelens-android
# needs ANDROID_HOME or local.properties sdk.dir
./gradlew assembleDebug
mkdir -p dist
cp app/build/outputs/apk/debug/app-debug.apk dist/WireLens-debug.apk
cp dist/WireLens-debug.apk /workspace/WireLens-debug.apk
sha256sum dist/WireLens-debug.apk
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
