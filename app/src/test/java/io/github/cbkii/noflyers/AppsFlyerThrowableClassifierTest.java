package io.github.cbkii.noflyers;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class AppsFlyerThrowableClassifierTest {
    @Test
    public void acceptsAppsFlyerNullPointer() {
        NullPointerException error = new NullPointerException("must not be null");
        error.setStackTrace(new StackTraceElement[] {
            new StackTraceElement(
                    "com.appsflyer.internal.AFb1cSDK",
                    "AFAdRevenueData",
                    null,
                    18),
            new StackTraceElement(
                    "com.google.android.gms.tasks.zzm",
                    "run",
                    null,
                    26)
        });

        assertTrue(AppsFlyerThrowableClassifier.isSuppressibleTaskCallback(error));
    }

    @Test
    public void acceptsWrappedAppsFlyerNullPointer() {
        NullPointerException cause = new NullPointerException("missing result");
        cause.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("com.appsflyer.internal.i", "onSuccess", null, 5)
        });
        RuntimeException wrapper = new RuntimeException("callback failed", cause);

        assertTrue(AppsFlyerThrowableClassifier.isSuppressibleTaskCallback(wrapper));
    }

    @Test
    public void rejectsNonAppsFlyerNullPointer() {
        NullPointerException error = new NullPointerException("application bug");
        error.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("com.example.app.MainActivity", "onCreate", null, 42)
        });

        assertFalse(AppsFlyerThrowableClassifier.isSuppressibleTaskCallback(error));
    }

    @Test
    public void rejectsAppsFlyerNonNullFailure() {
        IllegalStateException error = new IllegalStateException("network failure");
        error.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("com.appsflyer.internal.SomeCollector", "run", null, 7)
        });

        assertFalse(AppsFlyerThrowableClassifier.isSuppressibleTaskCallback(error));
    }

    @Test
    public void rejectsNull() {
        assertFalse(AppsFlyerThrowableClassifier.isSuppressibleTaskCallback(null));
    }
}
