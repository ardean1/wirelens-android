# Android build notes — WireLens Phone

## IDs and SDK

| Field | Value |
|-------|--------|
| applicationId | `com.ardean.wirelens` |
| compileSdk | 36 |
| targetSdk | 36 |
| minSdk | 26 |
| versionCode | 2 |
| versionName | 1.0.1 |

## Sideload (debug)

```bash
./gradlew assembleDebug
mkdir -p dist
cp app/build/outputs/apk/debug/app-debug.apk dist/WireLens-debug.apk
cp dist/WireLens-debug.apk /workspace/WireLens-debug.apk
sha256sum dist/WireLens-debug.apk
```

Phone: Chrome → https://github.com/ardean1/wirelens-android/releases → tap `WireLens-debug.apk` → allow source → Install.

## Play Store later (release AAB)

1. Create a release keystore (keep it private; never commit).
2. Add `key.properties` (gitignored) like Marion Wx Map:
   - `storeFile=...` `storePassword=...` `keyAlias=...` `keyPassword=...`
3. Wire `signingConfigs.release` in `app/build.gradle` (add when ready).
4. `./gradlew bundleRelease` → upload the `.aab` in Play Console.
5. Console assets later: **icon 512**, **feature graphic 1024×500**, screenshots — placeholders noted in `play-store/`.

Debug APK ≠ Play artifact.

## Permissions / coexistence

- Default monitor: **Usage Access** + foreground `dataSync` service — **no VPN slot**.
- Disclose honestly: destination IPs require claiming Android’s VPN slot; this build chooses coexistence with commercial VPNs instead.

## Built debug APK (this environment)

- Paths: `/workspace/wirelens-android/dist/WireLens-debug.apk` and `/workspace/WireLens-debug.apk`
- SHA-256: `229d032fe696c3c9386e583b30bfd706398298dd23d373ec1c8525e737d738a1`
- Size: ~5677 KB
