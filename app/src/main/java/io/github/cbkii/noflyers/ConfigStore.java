package io.github.cbkii.noflyers;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Map;
import java.util.TreeMap;

public final class ConfigStore {
    private final SharedPreferences preferences;

    public ConfigStore(Context context) {
        preferences = context.getSharedPreferences(ConfigKeys.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public HookMode getDefaultMode() {
        HookMode mode = HookMode.fromStored(
                preferences.getString(ConfigKeys.DEFAULT_MODE, null),
                ConfigKeys.FALLBACK_MODE);
        return mode == HookMode.DEFAULT ? ConfigKeys.FALLBACK_MODE : mode;
    }

    public void setDefaultMode(HookMode mode) {
        if (mode == null || mode == HookMode.DEFAULT) {
            throw new IllegalArgumentException("Invalid default mode");
        }
        preferences.edit().putString(ConfigKeys.DEFAULT_MODE, mode.storedValue()).apply();
    }

    public boolean diagnosticsEnabled() {
        return preferences.getBoolean(ConfigKeys.DIAGNOSTICS, false);
    }

    public void setDiagnosticsEnabled(boolean enabled) {
        preferences.edit().putBoolean(ConfigKeys.DIAGNOSTICS, enabled).apply();
    }

    public HookMode getOverride(String packageName) {
        return HookMode.fromStored(
                preferences.getString(ConfigKeys.overrideKey(packageName), null),
                HookMode.DEFAULT);
    }

    public void setOverride(String packageName, HookMode mode) {
        String normalised = packageName == null ? "" : packageName.trim();
        if (!PackageNameValidator.isValid(normalised)) {
            throw new IllegalArgumentException("Invalid Android package name");
        }

        SharedPreferences.Editor editor = preferences.edit();
        if (mode == null || mode == HookMode.DEFAULT) {
            editor.remove(ConfigKeys.overrideKey(normalised));
        } else {
            editor.putString(ConfigKeys.overrideKey(normalised), mode.storedValue());
        }
        editor.apply();
    }

    public void removeOverride(String packageName) {
        preferences.edit().remove(ConfigKeys.overrideKey(packageName)).apply();
    }

    public Map<String, HookMode> getOverrides() {
        Map<String, HookMode> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            if (!entry.getKey().startsWith(ConfigKeys.OVERRIDE_PREFIX)
                    || !(entry.getValue() instanceof String)) {
                continue;
            }
            String packageName = entry.getKey().substring(ConfigKeys.OVERRIDE_PREFIX.length());
            HookMode mode = HookMode.fromStored((String) entry.getValue(), HookMode.DEFAULT);
            if (PackageNameValidator.isValid(packageName) && mode != HookMode.DEFAULT) {
                result.put(packageName, mode);
            }
        }
        return result;
    }
}
