package org.lsposed.corepatch.zygisk;

import android.util.Log;

import java.lang.reflect.Field;

/**
 * FieldUtils — Reflection utilities for setting static fields.
 *
 * Used for:
 *   ReconcilePackageUtils.ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS = true
 */
public class FieldUtils {

    /**
     * Set a static boolean field to true via reflection.
     * Silently skips if class or field is not found.
     */
    public static void setStaticBoolean(ClassLoader loader, String className, String fieldName) {
        try {
            Class<?> clazz = Class.forName(className, true, loader);
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, true);
            if (BuildConfig.DEBUG) {
                Log.i(Main.TAG, "Set " + className + "." + fieldName + " = true");
            }
        } catch (ClassNotFoundException ex) {
            // Class not present on this ROM — skip silently
            Log.d(Main.TAG, "setStaticBoolean: class not found: " + className);
        } catch (NoSuchFieldException ex) {
            // Field renamed in this ROM — skip
            Log.d(Main.TAG, "setStaticBoolean: field not found: " + className + "." + fieldName);
        } catch (Throwable ex) {
            Log.e(Main.TAG, "setStaticBoolean failed: " + className + "." + fieldName, ex);
        }
    }
}
