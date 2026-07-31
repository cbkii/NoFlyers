# Pixel / Vector validation

## Before testing

- Keep the existing HMA-OSS relationship hiding between each target app and Google Play components unchanged.
- Install the test NoFlyers build.
- Open NoFlyers once and press **Add PicsArt preset**.
- Add `com.tuya.smartlife` with **Compatibility + contain Google callbacks** when testing Smart Life.
- In Vector/LSPosed, enable NoFlyers and scope only the target apps being tested.
- Do not scope Android, Google Play services or the Play Store.
- Enable diagnostic logs in NoFlyers.
- Force-stop and reopen each target app. Reboot only if Vector does not inject after an app restart.

## Capture

Run from a root-capable Termux shell, replacing the package when testing Smart Life:

```bash
package='com.picsart.studio'

su -c 'logcat -c'
su -c "am force-stop $package"
su -c "monkey -p $package -c android.intent.category.LAUNCHER 1" \
    >/dev/null 2>&1
sleep 8
su -c 'logcat -d -v threadtime' \
    | grep -Ei -B30 -A40 \
        'NoFlyers|AppsFlyer|AppSet|Integrity|AndroidRuntime|com\.picsart\.studio|com\.tuya\.smartlife'
```

## Required injection evidence

A scoped process must emit the unconditional activation line:

```text
NoFlyers [com.example.app/com.example.app]: active; mode=fail_appset
```

With diagnostics enabled, the guarded mode should additionally show the hooks that exist in that app, for example:

```text
installed AppsFlyer hooks; mode=fail_appset
installed Google App Set entry hook
installed Play Integrity factory hook
installed AppsFlyer Task crash shield: com.google.android.gms.tasks.zzm
```

Not every app bundles every collector, so an absent App Set or Integrity line is not itself a failure. The activation line is mandatory.

## Expected result

The target app remains running. The previous AppsFlyer callback crash must be absent:

```text
com.appsflyer.internal.*.onSuccess
com.google.android.gms.tasks.zzm.run
java.lang.NullPointerException: must not be null
```

If AppsFlyer still throws that exact callback failure, NoFlyers should log:

```text
suppressed AppsFlyer Google Task callback NPE
```

The shield deliberately does not suppress application crashes or non-AppsFlyer exceptions.

## PicsArt-specific limitation

The supplied PicsArt 29.2.4 crash currently ends in an obfuscated Koin factory with:

```text
java.lang.NullPointerException: getInstance(...) must not be null
```

That stack does not identify which singleton returned null and contains no AppsFlyer frame. NoFlyers must not blindly suppress it. If it remains after the AppsFlyer callback crash is contained, collect the full main/system/Xposed log and the installed APK/split list for deobfuscation.

## One-variable comparison

Do not change HMA-OSS, Google Play services, Play Integrity modules, NoFlyers mode or any other variable during a single before/after comparison.
