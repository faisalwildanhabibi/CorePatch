package org.lsposed.corepatch.zygisk;

import static com.v7878.zygisk.ZygoteLoader.PACKAGE_SYSTEM_SERVER;

import android.os.Build;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotObfuscateType;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.zygisk.ZygoteLoader;
import java.lang.reflect.Field;

@DoNotShrinkType
@DoNotObfuscateType
public class Main {
    public static final String TAG = Logger.TAG;

    private static void adaptForAndroid17() {
        try {
            Field sdkInt = Build.VERSION.class.getDeclaredField("SDK_INT");
            sdkInt.setAccessible(true);
            int current = sdkInt.getInt(null);
            if (current > 36) {
                sdkInt.setInt(null, 36);
                Logger.i("Adapted Build.VERSION.SDK_INT from " + current + " to 36 for VM compatibility");
            }
        } catch (Throwable th) {
            Logger.w("Could not adapt SDK_INT: " + th.getMessage());
        }

        try {
            Field sdkIntFull = Build.VERSION.class.getDeclaredField("SDK_INT_FULL");
            sdkIntFull.setAccessible(true);
            int current = sdkIntFull.getInt(null);
            if (current > 3600000) {
                sdkIntFull.setInt(null, 3600000);
            }
        } catch (Throwable ignored) {
        }

        try {
            Class<?> versionClass = Class.forName("com.v7878.misc.Version");
            Field correctSdk = versionClass.getDeclaredField("CORRECT_SDK_INT");
            correctSdk.setAccessible(true);
            int current = correctSdk.getInt(null);
            if (current > 36) {
                correctSdk.setInt(null, 36);
                Logger.i("Adapted Version.CORRECT_SDK_INT to 36");
            }
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("unused")
    @DoNotShrink
    @DoNotObfuscate
    public static void premain() {
        adaptForAndroid17();
    }

    @SuppressWarnings({"unused", "ConfusingMainMethod"})
    @DoNotShrink
    @DoNotObfuscate
    public static void main() {
        adaptForAndroid17();
        String pkg = ZygoteLoader.getPackageName();
        Logger.i("Injected into: " + pkg);
        try {
            // Apply common cryptographic bypass in all processes (apps + system_server)
            EntryPoint.mainCommon();

            if (BuildConfig.RUN_FOR_SYSTEM_SERVER
                    && PACKAGE_SYSTEM_SERVER.equals(pkg)) {
                // Intercept system_server startup
                SystemServerInit.init();
            }
        } catch (Throwable th) {
            Logger.e("Initialization error", th);
        }
        Logger.i("Main initialization completed for: " + pkg);
    }
}
