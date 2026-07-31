package io.github.cbkii.noflyers;

import java.util.regex.Pattern;

public final class PackageNameValidator {
    private static final Pattern PACKAGE_NAME = Pattern.compile(
            "^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+$");

    private PackageNameValidator() {}

    public static boolean isValid(String packageName) {
        return packageName != null && PACKAGE_NAME.matcher(packageName.trim()).matches();
    }
}
