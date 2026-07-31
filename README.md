# NoFlyers

NoFlyers is a per-app Xposed module for LSPosed-compatible frameworks. It
contains AppsFlyer failures when Google Play collectors are unavailable or
intentionally hidden, and can optionally stop AppsFlyer entirely.

NoFlyers does not modify or re-sign target applications. Vector/LSPosed controls
which app processes load its hooks.

## Modes

- **Compatibility** — invokes AppsFlyer’s public `disableAppSetId()` and
  `setDisableAdvertisingIdentifiers(true)` before SDK initialisation and startup.
- **Compatibility + contain Google callbacks** — also forces Google App Set ID
  and Play Integrity entry points onto completed failed Tasks. As a final safety
  boundary, it suppresses only Google Task callback null-pointer failures whose
  cause/stack originates in `com.appsflyer.internal`.
- **Full block** — includes callback containment and prevents AppsFlyer startup
  plus common outbound event methods. Attribution and AppsFlyer deferred deep
  links may stop working.
- **Off** — makes no AppsFlyer changes for that package.

The persisted internal name of the second mode remains `fail_appset` for
configuration compatibility. Compatibility is the module-wide default. The
PicsArt preset uses **Compatibility + contain Google callbacks**.

## Installation

1. Install a qualified NoFlyers APK.
2. Open NoFlyers and configure the default mode or exact-package overrides.
3. Enable NoFlyers in Vector/LSPosed.
4. Scope it only to the applications that need the hook, such as
   `com.picsart.studio` or `com.tuya.smartlife`.
5. Do **not** scope Android, Google Play services or the Play Store.
6. Force-stop and reopen each target application.

A reboot is normally required only when first enabling the module or when the
framework does not inject it after restarting the target application.

## HMA-OSS isolation

Keep the target app ↔ Google Play relationship hiding unchanged while testing.
Use **Compatibility + contain Google callbacks** for apps showing an AppsFlyer
`onSuccess` crash through `com.google.android.gms.tasks`.

NoFlyers does not suppress application exceptions without an AppsFlyer frame.
For example, an obfuscated Koin `getInstance(...) must not be null` failure cannot
be attributed to AppsFlyer from that stack alone and requires APK/log analysis.

## Diagnostics

Every scoped process emits one activation line regardless of the diagnostics
setting:

```text
NoFlyers [package/process]: active; mode=fail_appset
```

Enable **Diagnostic Xposed logs** for individual hook-installation messages.
Hook failures and contained AppsFlyer callback crashes are always logged.

See [`docs/DEVICE_TEST.md`](docs/DEVICE_TEST.md) for the controlled Pixel/Vector
validation procedure.

## Compatibility and boundaries

- Android 9 or later (`minSdk 28`).
- Legacy Xposed API 82 for broad LSPosed/Vector compatibility.
- Hooks stable public class names where available:
  `com.appsflyer.AppsFlyerLib`, Google App Set ID and Google Play Integrity.
- The observed `play-services-tasks@@18.1.0` AppsFlyer callback path is guarded at
  its Task runner, but only for classified AppsFlyer null-pointer failures.
- Does not hook PicsArt’s changing `AF*SDK` class names directly.
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
