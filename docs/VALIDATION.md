# Validation status

## Completed in the delivery environment

- Pure Java tests for mode parsing, override precedence, safe defaults and
  Android package-name validation.
- All Java sources compiled against local Android and Xposed API signature
  stubs to catch Java syntax, type and hook-signature errors.
- Hook source independently recompiled after the final safety changes.
- Android manifest and resource XML parsed successfully.
- Xposed entry point, metadata and PicsArt recommended-scope packaging checked.
- GitHub Actions workflow parsed as YAML.
- Static checks confirmed that the module does not reference PicsArt's
  obfuscated AppsFlyer internals and requests no Android permissions.

## Not completed in the delivery environment

An actual APK was not assembled here because this runtime has Java but no
Android SDK, Gradle installation, `aapt2`, D8 or APK signer, and its external
binary-download route is unavailable. No APK is claimed or included.

The included workflow installs JDK 17 and Gradle 8.13, then runs:

```text
testDebugUnitTest lintDebug assembleDebug assembleRelease
```

The first device validation remains required because the exact AppsFlyer and
Google Play libraries inside the installed PicsArt APK are runtime inputs.
