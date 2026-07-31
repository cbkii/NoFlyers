package io.github.cbkii.noflyers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PackageNameValidatorTest {
    @Test
    public void acceptsNormalAndroidPackageNames() {
        assertTrue(PackageNameValidator.isValid("com.picsart.studio"));
        assertTrue(PackageNameValidator.isValid("io.github.example_app.module2"));
    }

    @Test
    public void rejectsUnsafeOrAmbiguousValues() {
        assertFalse(PackageNameValidator.isValid(null));
        assertFalse(PackageNameValidator.isValid(""));
        assertFalse(PackageNameValidator.isValid("picsart"));
        assertFalse(PackageNameValidator.isValid("com..picsart"));
        assertFalse(PackageNameValidator.isValid("com.picsart.*"));
        assertFalse(PackageNameValidator.isValid("1com.picsart"));
    }
}
