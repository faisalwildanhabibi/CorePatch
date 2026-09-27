package org.lsposed.corepatch.zygisk;

import static com.v7878.zygisk.ZygoteLoader.PACKAGE_SYSTEM_SERVER;

import android.util.Log;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotObfuscateType;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.zygisk.ZygoteLoader;

/**
 * ZygiskCore — CorePatch N features as a Zygisk module.
 *
 * This is the Zygisk module entry point, declared via:
 *   zygisk { entrypoint = "org.lsposed.corepatch.zygisk.Main" }
 *
 * ZygoteLoader calls:
 *   premain() — before process main thread starts (pre-fork, minimal ops only)
 *   main()    — after fork, in the target process
 *
 * Hooks are only applied inside system_server (PACKAGE_SYSTEM_SERVER).
 */
@DoNotShrinkType
@DoNotObfuscateType
public class Main {
    public static final String TAG = "ZygiskCore";

    @SuppressWarnings("unused")
    @DoNotShrink
    @DoNotObfuscate
    public static void premain() {
        // No-op before fork. Heavy operations must happen in main().
    }

    @SuppressWarnings({"unused", "ConfusingMainMethod"})
    @DoNotShrink
    @DoNotObfuscate
    public static void main() {
        String pkg = ZygoteLoader.getPackageName();
        Log.i(TAG, "Injected into: " + pkg);
        try {
            if (BuildConfig.RUN_FOR_SYSTEM_SERVER
                    && PACKAGE_SYSTEM_SERVER.equals(pkg)) {
                // Install hook on RuntimeInit to intercept system_server startup
                SystemServerInit.init();
            }
        } catch (Throwable th) {
            Log.e(TAG, "Initialization error", th);
        }
        Log.i(TAG, "Done");
    }
}
