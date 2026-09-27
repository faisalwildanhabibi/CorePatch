# ProGuard rules for ZygiskCore
-keepattributes *Annotation*
-dontobfuscate

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Keep all ZygiskCore runtime classes and entry points
-keep class org.lsposed.corepatch.zygisk.** { *; }
-keepclassmembers class org.lsposed.corepatch.zygisk.** { *; }
