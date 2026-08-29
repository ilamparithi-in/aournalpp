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

# -----------------------------------------------------------------------
# Room DB
# -----------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class androidx.room.** { *; }

# -----------------------------------------------------------------------
# SSHJ / Bouncy Castle / SMBJ / OkHttp / Commons-Net / SLF4J
# -----------------------------------------------------------------------
-keep class net.schmizz.sshj.** { *; }
-keep class com.hierynomus.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-keep class org.apache.commons.net.** { *; }
-keep class org.slf4j.** { *; }
-keep class org.bouncycastle.** { *; }
-dontwarn net.schmizz.sshj.**
-dontwarn com.hierynomus.**
-dontwarn org.apache.commons.net.**
-dontwarn org.bouncycastle.**
-dontwarn org.slf4j.**
-dontwarn okhttp3.**
-dontwarn okio.**
