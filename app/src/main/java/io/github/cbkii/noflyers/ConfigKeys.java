package io.github.cbkii.noflyers;

public final class ConfigKeys {
    public static final String MODULE_PACKAGE = "io.github.cbkii.noflyers";
    public static final String PREFS_NAME = "noflyers";
    public static final String DEFAULT_MODE = "default_mode";
    public static final String DIAGNOSTICS = "diagnostics";
    public static final String OVERRIDE_PREFIX = "mode:";
    public static final HookMode FALLBACK_MODE = HookMode.COMPAT;

    private ConfigKeys() {}

    public static String overrideKey(String packageName) {
        return OVERRIDE_PREFIX + packageName;
    }
}
