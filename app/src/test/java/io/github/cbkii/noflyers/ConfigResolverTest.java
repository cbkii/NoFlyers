package io.github.cbkii.noflyers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class ConfigResolverTest {
    @Test
    public void overrideWins() {
        assertEquals(HookMode.BLOCK, ConfigResolver.resolve("compat", "block"));
    }

    @Test
    public void defaultMarkerUsesModuleDefault() {
        assertEquals(HookMode.FAIL_APPSET, ConfigResolver.resolve("fail_appset", "default"));
    }

    @Test
    public void missingConfigurationIsSafeCompatibilityMode() {
        assertEquals(HookMode.COMPAT, ConfigResolver.resolve(null, null));
    }

    @Test
    public void persistedDefaultMarkerCannotBecomeRuntimeMode() {
        assertEquals(HookMode.COMPAT, ConfigResolver.resolve("default", null));
    }
}
