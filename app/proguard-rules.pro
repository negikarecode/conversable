# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserving line number info for debug can be disabled for production release
-keepattributes !SourceFile,!LineNumberTable

# Repackage all internal packages into the root to maximize obfuscation
-repackageclasses ''
-allowaccessmodification
-overloadaggressively

# Keep standard Android entry points
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.content.BroadcastReceiver

# Keep models used by database or serialization libraries from being completely stripped
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
    @androidx.room.PrimaryKey *;
    @androidx.room.ColumnInfo *;
}

# Keep encryption helper class for security
-keep class com.example.security.CryptoHelper { *; }

# Remove logging in production
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

