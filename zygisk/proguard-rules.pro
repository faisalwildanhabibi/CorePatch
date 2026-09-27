# ProGuard rules for ZygiskCore
# Keep annotations used by ZygoteLoader
-keepattributes *Annotation*

# Keep R8 annotation processor
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-dontobfuscate

# Keep ZygoteLoader entry points
-keep class org.lsposed.corepatch.zygisk.Main {
    public static void premain();
    public static void main();
}
