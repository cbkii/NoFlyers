# Pixel / Vector validation

## Before testing

- Keep the existing HMA-OSS relationship hiding between PicsArt and Google Play
  components unchanged.
- Install NoFlyers.
- Open it once and press **Add PicsArt preset**.
- In Vector/LSPosed, enable NoFlyers and scope only
  `com.picsart.studio` for the first test.
- Enable diagnostic logs in NoFlyers.
- Restart PicsArt. Reboot only if Vector does not inject after a force-stop.

## Capture

Run from a root-capable Termux shell:

```bash
su -c 'logcat -c'
su -c 'am force-stop com.picsart.studio'
su -c 'monkey -p com.picsart.studio -c android.intent.category.LAUNCHER 1' \
    >/dev/null 2>&1
sleep 8
su -c 'logcat -d -v threadtime' \
    | grep -Ei -B20 -A30 \
        'NoFlyers|com\.picsart\.studio|AppsFlyer|AppSet|AndroidRuntime'
```

## Expected result

With the preset mode, the Xposed log should show:

```text
installed AppsFlyer hooks; mode=fail_appset
installed Google App Set entry hook
hooked App Set client
```

PicsArt should remain running and the prior
`AFb1hSDK.getMediationNetwork` null-pointer crash should be absent.

## Fallback

If the same AppsFlyer crash remains:

1. Change the PicsArt override to **Full block**.
2. Force-stop and reopen PicsArt.
3. Repeat the capture once.

Do not change HMA-OSS, Google Play services, Play Integrity modules, or other
variables during this comparison.
