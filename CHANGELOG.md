# Changelog

## 0.1.6 — 2026-08-01

- Disable AppsFlyer advertising-identifier collection before SDK initialisation.
- Force both App Set ID and Play Integrity collectors onto completed failed Tasks in guarded modes.
- Add a narrow Google Task crash shield that suppresses only null-pointer failures originating from `com.appsflyer.internal`.
- Add an unconditional one-line activation and resolved-mode log for scoped target processes.
- Preserve the stored `fail_appset` mode value while broadening it to contain the observed Google callback failure paths.
- Add regression tests based on the Smart Life AppsFlyer callback stack.

## 0.1.5 — 2026-08-01

- First public NoFlyers release.
- Compatibility mode disables AppsFlyer App Set ID collection before SDK start.
- Optional failed-App-Set mode prevents malformed success callbacks.
- Full-block mode stops AppsFlyer startup and common outbound events.
- Exact-package overrides and a one-tap PicsArt preset.
- Recommended Vector/LSPosed scope for `com.picsart.studio`.
- Optional Xposed diagnostics.
