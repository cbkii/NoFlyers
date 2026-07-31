# Architecture

## Scope boundary

Vector/LSPosed decides which processes load NoFlyers. The module never
selects or discovers target apps itself. Its bundled recommended scope contains
only `com.picsart.studio`.

## Configuration

The module app writes ordinary private SharedPreferences. Hooked processes read
those settings through legacy `XSharedPreferences`, as supplied by
LSPosed-compatible frameworks. Settings are loaded once at process start.

Runtime mode resolution is:

1. exact package override, when configured;
2. module-wide default;
3. hard-coded compatibility fallback if settings are absent or unreadable.

## Hook ordering

The module first attempts to install hooks during `handleLoadPackage`. It also
hooks `Application.attach()` and retries with the application class loader to
cover split or secondary class-loader arrangements.

## Compatibility mode

Hooks `AppsFlyerLib.getInstance()`, `init()` and `start()` and invokes the public
`disableAppSetId()` method before AppsFlyer initialisation or startup.

## Failed-App-Set mode

In addition to compatibility mode, the module hooks `AppSet.getClient()`. Once a
concrete client is returned, its `getAppSetIdInfo()` method returns
`Tasks.forException(...)`. It never returns a successful Task containing null.

This changes App Set ID behaviour for all code inside the scoped app, not only
AppsFlyer.

## Full-block mode

The module leaves `getInstance()` and `init()` available so app-side chaining is
less likely to break. It prevents AppsFlyer `start()` and common outbound event
methods from executing.

## Failure policy

- Missing AppsFlyer or App Set classes are normal and do not affect the app.
- Missing `disableAppSetId()` is logged only when diagnostics are enabled.
- Hook setup failures are logged and do not deliberately terminate the target.
- If creation of a failed Google Task fails, the original App Set call proceeds
  rather than returning an invalid object.
