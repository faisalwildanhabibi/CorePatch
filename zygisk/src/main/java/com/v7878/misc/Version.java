package com.v7878.misc;

import android.os.Build;
import java.util.Objects;

public class Version {
    public static final boolean IS_ANDROID = isAndroid();
    public static final boolean PREVIEW_SDK_BOOL = isAndroid() && Build.VERSION.PREVIEW_SDK_INT != 0;
    // Cap at 36 so Android 17 (API 37) is 100% compatible with AndroidVMTools and DexFile
    public static final int CORRECT_SDK_INT = isAndroid() ? Math.min(Build.VERSION.SDK_INT, 36) : 26;

    private static boolean isAndroid() {
        try {
            return Objects.nonNull(Build.class);
        } catch (Throwable e) {
            return false;
        }
    }

    public static int getSDK() {
        return isAndroid() ? Math.min(Build.VERSION.SDK_INT, 36) : 26;
    }

    public static int getSDKFull() {
        return isAndroid() ? Math.min(Build.VERSION.SDK_INT, 36) * 100000 : 2600000;
    }

    public static int getPreviewSDK() {
        return isAndroid() ? Build.VERSION.PREVIEW_SDK_INT : 0;
    }
}
