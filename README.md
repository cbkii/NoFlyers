# NoFlyers

NoFlyers is a per-app Xposed module for LSPosed-compatible frameworks. It prevents
AppsFlyer from crashing when Google App Set ID services are unavailable or
intentionally hidden, and can optionally stop AppsFlyer entirely.

NoFlyers does not modify or re-sign target applications. Vector/LSPosed controls
which app processes load its hooks.

## Modes

- **Compatibility** — invokes AppsFlyer’s public `disableAppSetId()` before SDK
  initialisation and startup.
- **Compatibility + fail App Set calls** — also makes Google App Set ID requests
  return a completed failed task, preventing malformed success callbacks. This
  affects all App Set ID use in the scoped application.
- **Full block** — prevents AppsFlyer startup and common outbound event methods.
  Attribution and AppsFlyer deferred deep links may stop working.
- **Off** — makes no AppsFlyer changes for that package.

Compatibility is the module-wide default. The PicsArt preset uses
**Compatibility + fail App Set calls**.

## Installation

1. Download and install the `NoFlyers-v0.1.5.apk` release asset.
2. Open NoFlyers and configure the default mode or exact-package overrides.
3. Enable NoFlyers in Vector/LSPosed.
4. Scope it only to the applications that need the hook. Start with
   `com.picsart.studio` for PicsArt.
5. Do **not** scope Android, Google Play services or the Play Store.
6. Force-stop and reopen each target application.

A reboot is normally required only when first enabling the module or when the
framework does not inject it after restarting the target application.

## PicsArt with HMA-OSS

Keep the PicsArt ↔ Google Play relationship hiding unchanged. Add the PicsArt
preset in NoFlyers, then scope NoFlyers only to `com.picsart.studio`. If PicsArt
still crashes, change its override to **Full block** and restart PicsArt.

## Diagnostics

Enable **Diagnostic Xposed logs**, restart the target application, and inspect
the Vector/LSPosed log for entries beginning with:

```text
NoFlyers [package/process]
```

Normal operation is quiet when diagnostics are disabled. Hook failures are
always logged.

## Compatibility and boundaries

- Android 9 or later (`minSdk 28`).
- Legacy Xposed API 82 for broad LSPosed/Vector compatibility.
- Hooks stable public class names where available:
  `com.appsflyer.AppsFlyerLib`, `com.google.android.gms.appset.AppSet`, and
  `com.google.android.gms.tasks.Tasks`.
- Does not hook PicsArt’s obfuscated AppsFlyer internals.
- Reads settings through legacy `XSharedPreferences`, supported by current
  LSPosed-compatible frameworks.
- Loads configuration when the target process starts; restart the app after a
  settings or scope change.

## Build

Requirements:

- JDK 17
- Gradle 8.13
- Android SDK API 36

```bash
gradle --no-daemon testDebugUnitTest lintDebug assembleDebug assembleRelease
```

GitHub Actions performs the same qualification. Stable releases publish the
exact prebuilt and separately signed APK; the release workflow verifies rather
than rebuilds it.

## Licence

MIT
