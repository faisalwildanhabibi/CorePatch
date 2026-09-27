package org.lsposed.corepatch.zygisk;

import static com.v7878.zygisk.ZygoteLoader.PACKAGE_SYSTEM_SERVER;

import android.os.Build;
import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotObfuscateType;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.zygisk.ZygoteLoader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@DoNotShrinkType
@DoNotObfuscateType
public class Main {
    public static final String TAG = Logger.TAG;

    private static Object getUnsafe() {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field f = unsafeClass.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return f.get(null);
        } catch (Throwable t1) {
            try {
                Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
                Constructor<?> c = unsafeClass.getDeclaredConstructor();
                c.setAccessible(true);
                return c.newInstance();
            } catch (Throwable t2) {
                return null;
            }
        }
    }

    private static void setStaticIntUnsafe(Object unsafe, Class<?> clazz, String fieldName, int value) {
        if (unsafe == null) return;
        try {
            Class<?> unsafeClass = unsafe.getClass();
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);

            Method staticFieldOffset = unsafeClass.getMethod("staticFieldOffset", Field.class);
            Method staticFieldBase = unsafeClass.getMethod("staticFieldBase", Field.class);
            Method putInt = unsafeClass.getMethod("putInt", Object.class, long.class, int.class);

            long offset = (long) staticFieldOffset.invoke(unsafe, field);
            Object base = staticFieldBase.invoke(unsafe, field);
            putInt.invoke(unsafe, base, offset, value);

            Logger.i("Adapted " + clazz.getSimpleName() + "." + fieldName + " to " + value + " via Unsafe");
        } catch (Throwable th) {
            Logger.w("Failed to adapt " + clazz.getSimpleName() + "." + fieldName + ": " + th.getMessage());
        }
    }

    private static void adaptForAndroid17() {
        Object unsafe = getUnsafe();
        if (unsafe == null) {
            Logger.w("sun.misc.Unsafe not available");
            return;
        }

        // Adapt Build.VERSION.SDK_INT
        try {
            if (Build.VERSION.SDK_INT > 36) {
                setStaticIntUnsafe(unsafe, Build.VERSION.class, "SDK_INT", 36);
            }
        } catch (Throwable ignored) {
        }

        // Adapt Build.VERSION.SDK_INT_FULL
        try {
            setStaticIntUnsafe(unsafe, Build.VERSION.class, "SDK_INT_FULL", 3600000);
        } catch (Throwable ignored) {
        }

        // Adapt com.v7878.misc.Version.CORRECT_SDK_INT
        try {
            Class<?> versionClass = Class.forName("com.v7878.misc.Version");
            setStaticIntUnsafe(unsafe, versionClass, "CORRECT_SDK_INT", 36);
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
