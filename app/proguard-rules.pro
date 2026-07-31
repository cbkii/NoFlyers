# Xposed loads this class by its exact binary name from assets/xposed_init.
-keep class io.github.cbkii.noflyers.HookEntry { *; }

# The manifest instantiates these components by class name.
-keep class io.github.cbkii.noflyers.MainActivity { *; }

# Keep Xposed callback signatures and annotations intact.
-keepattributes *Annotation*,InnerClasses,EnclosingMethod
-dontwarn de.robv.android.xposed.**
