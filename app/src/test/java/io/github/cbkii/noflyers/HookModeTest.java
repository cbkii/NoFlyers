package io.github.cbkii.noflyers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class HookModeTest {
    @Test
    public void parsesStoredValueCaseInsensitively() {
        assertEquals(HookMode.FAIL_APPSET, HookMode.fromStored("fail_appset", HookMode.OFF));
        assertEquals(HookMode.BLOCK, HookMode.fromStored("BlOcK", HookMode.OFF));
    }

    @Test
    public void invalidValueUsesFallback() {
        assertEquals(HookMode.COMPAT, HookMode.fromStored("unknown", HookMode.COMPAT));
        assertEquals(HookMode.OFF, HookMode.fromStored(null, HookMode.OFF));
    }
}
