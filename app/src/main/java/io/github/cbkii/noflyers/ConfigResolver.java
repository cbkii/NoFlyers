package io.github.cbkii.noflyers;

public final class ConfigResolver {
    private ConfigResolver() {}

    public static HookMode resolve(String defaultValue, String overrideValue) {
        HookMode defaultMode = HookMode.fromStored(defaultValue, ConfigKeys.FALLBACK_MODE);
        if (defaultMode == HookMode.DEFAULT) {
            defaultMode = ConfigKeys.FALLBACK_MODE;
        }

        HookMode overrideMode = HookMode.fromStored(overrideValue, HookMode.DEFAULT);
        return overrideMode == HookMode.DEFAULT ? defaultMode : overrideMode;
    }
}
