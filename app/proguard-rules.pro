# SmartPay Security-Enhanced ProGuard Rules
# R8 Full Code Obfuscation & Security Configuration

# ===========================================
# SECURITY & OBFUSCATION SETTINGS
# ===========================================

# Enable aggressive obfuscation
-obfuscationdictionary obfuscation-dictionary.txt
-classobfuscationdictionary obfuscation-dictionary.txt
-packageobfuscationdictionary obfuscation-dictionary.txt

# Remove debug information and logging
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Remove stack traces and debug info in release builds
-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
    public java.lang.String getMessage();
    public java.lang.String getLocalizedMessage();
    public java.lang.Throwable getCause();
    public java.lang.StackTraceElement[] getStackTrace();
}

# Remove System.out and System.err calls
-assumenosideeffects class java.lang.System {
    public static void out.println(...);
    public static void err.println(...);
}

# Strip line numbers and source file names for security
-renamesourcefileattribute ""
-keepattributes !SourceFile,!LineNumberTable

# ===========================================
# SMARTPAY APP PROTECTION
# ===========================================

# Protect critical SmartPay classes from being deobfuscated
-keep class com.smartpay.android.security.** { *; }
-keep class com.smartpay.data.network.ApiService { *; }
-keep class com.smartpay.models.** { *; }

# Protect authentication and financial classes
-keep class **.*Activity { *; }
-keepclassmembers class **.*Activity {
    public void onCreate(android.os.Bundle);
    public void onResume();
    public void onPause();
}

# ===========================================
# ANDROID & FRAMEWORK RULES
# ===========================================

# Keep Android components
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# AndroidX Security Crypto
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ===========================================
# RETROFIT & NETWORKING
# ===========================================

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn retrofit2.Platform$Java8

# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ===========================================
# FIREBASE & GOOGLE SERVICES
# ===========================================

-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ===========================================
# CAMERA & QR CODE SCANNING
# ===========================================

# ZXing
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**

# CameraX
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# ===========================================
# COMPOSE & UI
# ===========================================

# Jetpack Compose
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class androidx.compose.** {
    *;
}

# ===========================================
# REFLECTION & SERIALIZATION
# ===========================================

# Keep model classes for JSON serialization
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Prevent stripping of generic signatures
-keepattributes Signature

# ===========================================
# SECURITY: ANTI-REVERSE ENGINEERING
# ===========================================

# Protect against reflection attacks
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Optimize and obfuscate aggressively
-optimizations !code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification