package io.github.cbkii.noflyers;

import de.robv.android.xposed.XSharedPreferences;

public final class HookConfiguration {
    private final HookMode mode;
    private final boolean diagnostics;

    HookConfiguration(HookMode mode, boolean diagnostics) {
        this.mode = mode;
        this.diagnostics = diagnostics;
    }

    public HookMode mode() {
        return mode;
    }

    public boolean diagnostics() {
        return diagnostics;
    }

    public static HookConfiguration load(String targetPackage) {
        XSharedPreferences preferences =
                new XSharedPreferences(ConfigKeys.MODULE_PACKAGE, ConfigKeys.PREFS_NAME);
        preferences.makeWorldReadable();
        preferences.reload();

        String defaultValue = preferences.getString(ConfigKeys.DEFAULT_MODE, null);
        String overrideValue = preferences.getString(ConfigKeys.overrideKey(targetPackage), null);
        HookMode mode = ConfigResolver.resolve(defaultValue, overrideValue);
        boolean diagnostics = preferences.getBoolean(ConfigKeys.DIAGNOSTICS, false);
        return new HookConfiguration(mode, diagnostics);
    }
}
