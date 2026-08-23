# Add project-specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# -----------------------------------------------------------------------
# Jetpack Compose
# -----------------------------------------------------------------------
# Compose relies on reflection for the @Composable function transforms.
# The Compose compiler plugin generates stable/immutable annotations that
# R8 needs to see — the kotlin.Metadata annotation must be kept.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# Keep Compose runtime internals referenced by generated code
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# -----------------------------------------------------------------------
# AndroidX / Lifecycle / Navigation
# -----------------------------------------------------------------------
-keep class androidx.lifecycle.** { *; }
-keep class androidx.navigation.** { *; }
-dontwarn androidx.lifecycle.**
-dontwarn androidx.navigation.**

# -----------------------------------------------------------------------
# Kotlin
# -----------------------------------------------------------------------
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Lazy {
    <fields>;
}

# -----------------------------------------------------------------------
# Termux-X11 / Native bridge
# -----------------------------------------------------------------------
# JNI-registered methods must not be renamed.
-keepclasseswithmembernames class * {
    native <methods>;
}

# LoriePreferences and related classes are loaded reflectively by the
# generated Prefs.java stub.
-keep class com.termux.x11.** { *; }
-dontwarn com.termux.x11.**

# -----------------------------------------------------------------------
# AIDL stubs (Binder interfaces)
# -----------------------------------------------------------------------
-keep class ** implements android.os.IInterface { *; }
-keep class ** extends android.os.Binder { *; }

# -----------------------------------------------------------------------
# Serialization / Parcelable
# -----------------------------------------------------------------------
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# -----------------------------------------------------------------------
# Enum classes
# -----------------------------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
