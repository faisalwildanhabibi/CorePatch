# ProGuard rules for ZygiskCore
-keepattributes *Annotation*
-dontobfuscate

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Keep all ZygiskCore and v7878 runtime classes and methods
-keep class org.lsposed.corepatch.zygisk.** { *; }
-keepclassmembers class org.lsposed.corepatch.zygisk.** { *; }

-keep class com.v7878.** { *; }
-keepclassmembers class com.v7878.** { *; }
