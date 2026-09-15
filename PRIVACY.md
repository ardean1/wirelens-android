# Privacy — WireLens Phone

**Last updated:** 2026-09-15

## Summary

WireLens Phone monitors **per-app network usage** **on your device**. Usage metadata shown in the app stays **on the phone**. The publisher does **not** operate a cloud service that receives your traffic list.

## Data the app processes on-device

- App names / package names (from Android UID mapping)
- Bytes received / transmitted and approximate rates (via `NetworkStatsManager`)
- Heuristic flags derived from those totals (sudden busy, high background use)

## Data we do not collect

- No account required
- No upload of usage logs to Ardean servers
- No sale of personal data
- No decryption of HTTPS / TLS contents
- No destination IP / hostname capture in the default coexistence mode

## Permissions (normal use)

- **Usage Access** (`PACKAGE_USAGE_STATS`) — required to read per-app network stats. You enable this in system Settings.
- **Foreground service / notifications** — so monitoring can continue with a clear ongoing notification.
- **Internet** — declared for completeness; the monitor does not upload usage data.

WireLens does **not** require Android’s **VPN permission** for normal Start Monitor. It deliberately avoids `VpnService` so a commercial VPN can keep the single VPN slot.

## Optional donations

Cash App tips (`$AnthonyDean16`) are optional and handled by Cash App — not through in-app Play Billing in this version.

## Contact

Use the GitHub repository security / issues channels for the published project.
