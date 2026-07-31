package io.github.cbkii.noflyers;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** Identifies only the AppsFlyer null failures that may be contained at a Task boundary. */
final class AppsFlyerThrowableClassifier {
    private static final String APPS_FLYER_PREFIX = "com.appsflyer.internal.";
    private static final int MAX_CHAIN_DEPTH = 24;

    private AppsFlyerThrowableClassifier() {}

    static boolean isSuppressibleTaskCallback(Throwable throwable) {
        if (throwable == null) {
            return false;
        }

        boolean hasNullPointer = false;
        boolean hasAppsFlyerFrame = false;
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Throwable current = throwable;
        int depth = 0;

        while (current != null && depth < MAX_CHAIN_DEPTH && visited.add(current)) {
            if (current instanceof NullPointerException) {
                hasNullPointer = true;
            }
            for (StackTraceElement frame : current.getStackTrace()) {
                if (frame.getClassName().startsWith(APPS_FLYER_PREFIX)) {
                    hasAppsFlyerFrame = true;
                    break;
                }
            }
            if (hasNullPointer && hasAppsFlyerFrame) {
                return true;
            }
            current = current.getCause();
            depth++;
        }
        return false;
    }
}
