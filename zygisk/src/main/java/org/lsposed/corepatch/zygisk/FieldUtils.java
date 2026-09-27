package org.lsposed.corepatch.zygisk;

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
            Class<?> clazz = Class.forName(className, false, loader);
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(null, true);
            Logger.i("Set " + className + "." + fieldName + " = true");
        } catch (ClassNotFoundException ex) {
            // Class not present on this ROM — skip silently
            Logger.d("setStaticBoolean: class not found: " + className);
        } catch (NoSuchFieldException ex) {
            // Field renamed in this ROM — skip
            Logger.d("setStaticBoolean: field not found: " + className + "." + fieldName);
        } catch (Throwable ex) {
            Logger.e("setStaticBoolean failed: " + className + "." + fieldName, ex);
        }
    }
}
