package io.github.cbkii.noflyers;

import android.app.Application;
import android.content.Context;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** Legacy Xposed entry point used for broad LSPosed/Vector compatibility. */
public final class HookEntry implements IXposedHookLoadPackage {
    private static final String TAG = "NoFlyers";
    private static final String APPS_FLYER_CLASS = "com.appsflyer.AppsFlyerLib";
    private static final String APP_SET_CLASS = "com.google.android.gms.appset.AppSet";
    private static final String TASKS_CLASS = "com.google.android.gms.tasks.Tasks";

    private static final String[] BLOCKED_VOID_METHODS = {
        "start",
        "logEvent",
        "logAdRevenue",
        "sendAdRevenue",
        "performOnDeepLinking",
        "sendPushNotificationData",
        "updateServerUninstallToken"
    };

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (ConfigKeys.MODULE_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        HookConfiguration configuration;
        try {
            configuration = HookConfiguration.load(lpparam.packageName);
        } catch (Throwable error) {
            configuration = new HookConfiguration(ConfigKeys.FALLBACK_MODE, false);
            XposedBridge.log(TAG + ": configuration read failed for "
                    + lpparam.packageName + "; using compatibility mode: " + error);
        }

        if (configuration.mode() == HookMode.OFF) {
            return;
        }

        ProcessHooks hooks = new ProcessHooks(
                lpparam.packageName,
                lpparam.processName,
                configuration);

        // Fast path: AppsFlyer is normally visible from the package class loader now.
        try {
            hooks.install(lpparam.classLoader);
        } catch (Throwable error) {
            XposedBridge.log(TAG + ": initial hook installation failed for "
                    + lpparam.packageName + ": " + error);
        }

        // Retry after Application.attach for split/secondary class-loader arrangements.
        try {
            XposedHelpers.findAndHookMethod(
                    Application.class,
                    "attach",
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            try {
                                Context context = (Context) param.args[0];
                                ClassLoader classLoader = context == null
                                        ? lpparam.classLoader
                                        : context.getClassLoader();
                                hooks.install(classLoader);
                            } catch (Throwable error) {
                                XposedBridge.log(TAG + ": deferred hook installation failed for "
                                        + lpparam.packageName + ": " + error);
                            }
                        }
                    });
        } catch (Throwable error) {
            XposedBridge.log(TAG + ": could not register Application.attach hook for "
                    + lpparam.packageName + ": " + error);
        }
    }

    private static final class ProcessHooks {
        private final String packageName;
        private final String processName;
        private final HookConfiguration configuration;
        private final AtomicBoolean appsFlyerInstalled = new AtomicBoolean(false);
        private final AtomicBoolean appSetEntryInstalled = new AtomicBoolean(false);
        private final Set<Class<?>> hookedAppSetClients = Collections.newSetFromMap(
                Collections.synchronizedMap(new WeakHashMap<>()));

        ProcessHooks(
                String packageName,
                String processName,
                HookConfiguration configuration) {
            this.packageName = packageName;
            this.processName = processName;
            this.configuration = configuration;
        }

        void install(ClassLoader classLoader) {
            if (classLoader == null) {
                return;
            }
            installAppsFlyerHooks(classLoader);
            if (configuration.mode() == HookMode.FAIL_APPSET
                    || configuration.mode() == HookMode.BLOCK) {
                installAppSetHooks(classLoader);
            }
        }

        private void installAppsFlyerHooks(ClassLoader classLoader) {
            if (appsFlyerInstalled.get()) {
                return;
            }

            Class<?> appsFlyerClass = XposedHelpers.findClassIfExists(
                    APPS_FLYER_CLASS,
                    classLoader);
            if (appsFlyerClass == null || !appsFlyerInstalled.compareAndSet(false, true)) {
                return;
            }

            XposedBridge.hookAllMethods(
                    appsFlyerClass,
                    "getInstance",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            disableIdentifiers(param.getResult());
                        }
                    });

            XC_MethodHook beforeInitialisation = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    disableIdentifiers(param.thisObject);
                }
            };
            XposedBridge.hookAllMethods(appsFlyerClass, "init", beforeInitialisation);

            if (configuration.mode() == HookMode.BLOCK) {
                XC_MethodHook block = new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        if ("start".equals(((Method) param.method).getName())) {
                            disableIdentifiers(param.thisObject);
                        }
                        param.setResult(null);
                    }
                };
                for (String methodName : BLOCKED_VOID_METHODS) {
                    XposedBridge.hookAllMethods(appsFlyerClass, methodName, block);
                }
            } else {
                XposedBridge.hookAllMethods(
                        appsFlyerClass,
                        "start",
                        beforeInitialisation);
            }

            log("installed AppsFlyer hooks; mode=" + configuration.mode().storedValue());
        }

        private void disableIdentifiers(Object appsFlyerInstance) {
            if (appsFlyerInstance == null) {
                return;
            }
            try {
                XposedHelpers.callMethod(appsFlyerInstance, "disableAppSetId");
            } catch (NoSuchMethodError | XposedHelpers.ClassNotFoundError error) {
                log("AppsFlyer SDK has no disableAppSetId method");
            } catch (Throwable error) {
                logError("disableAppSetId failed", error);
            }
        }

        private void installAppSetHooks(ClassLoader classLoader) {
            if (appSetEntryInstalled.get()) {
                return;
            }
            Class<?> appSetClass = XposedHelpers.findClassIfExists(APP_SET_CLASS, classLoader);
            Class<?> tasksClass = XposedHelpers.findClassIfExists(TASKS_CLASS, classLoader);
            if (appSetClass == null || tasksClass == null
                    || !appSetEntryInstalled.compareAndSet(false, true)) {
                return;
            }

            XposedBridge.hookAllMethods(
                    appSetClass,
                    "getClient",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object client = param.getResult();
                            if (client != null) {
                                hookAppSetClient(client.getClass(), tasksClass);
                            }
                        }
                    });
            log("installed Google App Set entry hook");
        }

        private void hookAppSetClient(Class<?> clientClass, Class<?> tasksClass) {
            synchronized (hookedAppSetClients) {
                if (!hookedAppSetClients.add(clientClass)) {
                    return;
                }
            }

            Set<XC_MethodHook.Unhook> hooks = XposedBridge.hookAllMethods(
                    clientClass,
                    "getAppSetIdInfo",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object failedTask = XposedHelpers.callStaticMethod(
                                        tasksClass,
                                        "forException",
                                        new IllegalStateException(
                                                "App Set ID disabled by NoFlyers"));
                                param.setResult(failedTask);
                            } catch (Throwable error) {
                                logError("could not create failed App Set Task", error);
                            }
                        }
                    });
            if (hooks.isEmpty()) {
                synchronized (hookedAppSetClients) {
                    hookedAppSetClients.remove(clientClass);
                }
                log("App Set client has no getAppSetIdInfo method: " + clientClass.getName());
            } else {
                log("hooked App Set client: " + clientClass.getName());
            }
        }

        private void log(String message) {
            if (configuration.diagnostics()) {
                XposedBridge.log(TAG + " [" + packageName + "/" + processName + "]: " + message);
            }
        }

        private void logError(String message, Throwable error) {
            XposedBridge.log(TAG + " [" + packageName + "/" + processName + "]: "
                    + message + ": " + error);
        }
    }
}
