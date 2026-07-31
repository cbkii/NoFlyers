package io.github.cbkii.noflyers;

import java.util.Locale;

/** Stable persisted modes. Do not rename enum constants without migration. */
public enum HookMode {
    OFF("Off", "Do not change AppsFlyer in this app."),
    COMPAT(
            "Compatibility",
            "Disable AppsFlyer App Set ID collection before SDK initialisation."),
    FAIL_APPSET(
            "Compatibility + fail App Set calls",
            "Also return a completed failed Task from Google App Set ID calls."),
    BLOCK(
            "Full block",
            "Prevent AppsFlyer startup and common outbound event methods."),
    DEFAULT("Use default", "Use the module-wide default mode.");

    private final String label;
    private final String description;

    HookMode(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }

    public String storedValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static HookMode fromStored(String value, HookMode fallback) {
        if (value == null || value.trim().isEmpty()) {
            return fallback;
        }
        try {
            return HookMode.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    public static HookMode[] selectableDefaults() {
        return new HookMode[] {OFF, COMPAT, FAIL_APPSET, BLOCK};
    }

    public static HookMode[] selectableOverrides() {
        return new HookMode[] {DEFAULT, OFF, COMPAT, FAIL_APPSET, BLOCK};
    }
}
