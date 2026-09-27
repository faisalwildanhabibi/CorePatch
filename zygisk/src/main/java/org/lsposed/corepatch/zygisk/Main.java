package org.lsposed.corepatch.zygisk;

import static com.v7878.zygisk.ZygoteLoader.PACKAGE_SYSTEM_SERVER;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotObfuscateType;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.zygisk.ZygoteLoader;

@DoNotShrinkType
@DoNotObfuscateType
public class Main {
    @SuppressWarnings("unused")
    @DoNotShrink
    @DoNotObfuscate
    public static void premain() {
    }

    @SuppressWarnings({"unused", "ConfusingMainMethod"})
    @DoNotShrink
    @DoNotObfuscate
    public static void main() {
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
