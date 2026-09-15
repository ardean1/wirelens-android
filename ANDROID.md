# Android build notes — WireLens Phone

## IDs and SDK

| Field | Value |
|-------|--------|
| applicationId | `com.ardean.wirelens` |
| compileSdk | 36 |
| targetSdk | 36 |
| minSdk | 26 |
| versionCode | 1 |
| versionName | 1.0.0 |

## Sideload (debug)

```bash
./gradlew assembleDebug
cp app/build/outputs/apk/debug/app-debug.apk /workspace/WireLens-debug.apk
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

## VPN permission

Disclose in listing: local VPN used only to observe destinations on-device; not a remote VPN service.

## Built debug APK (this environment)

- Path: `/workspace/WireLens-debug.apk`
- SHA-256: `7eb254e778b75c982445a94817514c1a073d465b8565ee4016994eeb65fd5dd8`
- Size: ~5.6 MB
