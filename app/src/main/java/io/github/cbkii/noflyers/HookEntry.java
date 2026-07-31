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

    private static final String[] INTEGRITY_FACTORY_CLASSES = {
        "com.google.android.play.core.integrity.IntegrityManagerFactory",
        "com.google.android.play.core.integrity.StandardIntegrityManagerFactory"
    };

    private static final String[] INTEGRITY_TASK_METHODS = {
        "requestIntegrityToken",
        "prepareIntegrityToken",
        "request"
    };

    // play-services-tasks 18.1.0 dispatches OnSuccessListener callbacks through zzm.
    // Adjacent names are included for nearby releases; suppression remains AppsFlyer-NPE-only.
    private static final String[] TASK_RUNNER_CLASSES = {
        "com.google.android.gms.tasks.zzm",
        "com.google.android.gms.tasks.zzn",
        "com.google.android.gms.tasks.zzl",
        "com.google.android.gms.tasks.zzk"
    };

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

        XposedBridge.log(TAG + " [" + lpparam.packageName + "/" + lpparam.processName
                + "]: active; mode=" + configuration.mode().storedValue());

        ProcessHooks hooks = new ProcessHooks(
                lpparam.packageName,
                lpparam.processName,
                configuration);

        // Fast path: AppsFlyer and Google collectors are often visible now.
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
        private final Set<Class<?>> hookedIntegrityFactories = Collections.newSetFromMap(
                Collections.synchronizedMap(new WeakHashMap<>()));
        private final Set<Class<?>> hookedIntegrityManagers = Collections.newSetFromMap(
                Collections.synchronizedMap(new WeakHashMap<>()));
        private final Set<Class<?>> hookedTaskRunners = Collections.newSetFromMap(
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
                installPlayIntegrityHooks(classLoader);
                installTaskCrashShield(classLoader);
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
                log("AppsFlyer getInstance returned null; no identifier settings applied");
                return;
            }
            callOptional(appsFlyerInstance, "disableAppSetId");
            callOptional(appsFlyerInstance, "setDisableAdvertisingIdentifiers", true);
        }

        private void callOptional(Object receiver, String methodName, Object... args) {
            try {
                XposedHelpers.callMethod(receiver, methodName, args);
            } catch (NoSuchMethodError | XposedHelpers.ClassNotFoundError error) {
                log("AppsFlyer SDK has no " + methodName + " method");
            } catch (Throwable error) {
                logError(methodName + " failed", error);
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
                    failedTaskHook(tasksClass, "App Set ID disabled by NoFlyers"));
            if (hooks.isEmpty()) {
                synchronized (hookedAppSetClients) {
                    hookedAppSetClients.remove(clientClass);
                }
                log("App Set client has no getAppSetIdInfo method: " + clientClass.getName());
            } else {
                log("hooked App Set client: " + clientClass.getName());
            }
        }

        private void installPlayIntegrityHooks(ClassLoader classLoader) {
            Class<?> tasksClass = XposedHelpers.findClassIfExists(TASKS_CLASS, classLoader);
            if (tasksClass == null) {
                return;
            }

            for (String factoryName : INTEGRITY_FACTORY_CLASSES) {
                Class<?> factoryClass = XposedHelpers.findClassIfExists(factoryName, classLoader);
                if (factoryClass == null) {
                    continue;
                }
                synchronized (hookedIntegrityFactories) {
                    if (!hookedIntegrityFactories.add(factoryClass)) {
                        continue;
                    }
                }

                Set<XC_MethodHook.Unhook> hooks = XposedBridge.hookAllMethods(
                        factoryClass,
                        "create",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Object manager = param.getResult();
                                if (manager != null) {
                                    hookIntegrityManager(manager.getClass(), tasksClass);
                                }
                            }
                        });
                if (hooks.isEmpty()) {
                    synchronized (hookedIntegrityFactories) {
                        hookedIntegrityFactories.remove(factoryClass);
                    }
                } else {
                    log("installed Play Integrity factory hook: " + factoryName);
                }
            }
        }

        private void hookIntegrityManager(Class<?> managerClass, Class<?> tasksClass) {
            synchronized (hookedIntegrityManagers) {
                if (!hookedIntegrityManagers.add(managerClass)) {
                    return;
                }
            }

            boolean installed = false;
            for (String methodName : INTEGRITY_TASK_METHODS) {
                Set<XC_MethodHook.Unhook> hooks = XposedBridge.hookAllMethods(
                        managerClass,
                        methodName,
                        failedTaskHook(tasksClass, "Play Integrity disabled by NoFlyers"));
                if (!hooks.isEmpty()) {
                    installed = true;
                    log("hooked Play Integrity method " + managerClass.getName()
                            + "." + methodName);
                }
            }
            if (!installed) {
                synchronized (hookedIntegrityManagers) {
                    hookedIntegrityManagers.remove(managerClass);
                }
            }
        }

        private XC_MethodHook failedTaskHook(Class<?> tasksClass, String reason) {
            return new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    try {
                        Object failedTask = XposedHelpers.callStaticMethod(
                                tasksClass,
                                "forException",
                                new IllegalStateException(reason));
                        param.setResult(failedTask);
                    } catch (Throwable error) {
                        logError("could not create failed Google Task", error);
                    }
                }
            };
        }

        private void installTaskCrashShield(ClassLoader classLoader) {
            for (String runnerName : TASK_RUNNER_CLASSES) {
                Class<?> runnerClass = XposedHelpers.findClassIfExists(runnerName, classLoader);
                if (runnerClass == null) {
                    continue;
                }
                synchronized (hookedTaskRunners) {
                    if (!hookedTaskRunners.add(runnerClass)) {
                        continue;
                    }
                }

                Set<XC_MethodHook.Unhook> hooks = XposedBridge.hookAllMethods(
                        runnerClass,
                        "run",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Throwable throwable = param.getThrowable();
                                if (!AppsFlyerThrowableClassifier
                                        .isSuppressibleTaskCallback(throwable)) {
                                    return;
                                }
                                XposedBridge.log(TAG + " [" + packageName + "/" + processName
                                        + "]: suppressed AppsFlyer Google Task callback NPE: "
                                        + throwable);
                                param.setResult(null);
                            }
                        });
                if (hooks.isEmpty()) {
                    synchronized (hookedTaskRunners) {
                        hookedTaskRunners.remove(runnerClass);
                    }
                } else {
                    log("installed AppsFlyer Task crash shield: " + runnerName);
                }
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
