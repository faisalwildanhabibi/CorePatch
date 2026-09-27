package org.lsposed.corepatch.zygisk;

import static android.os.Build.VERSION.SDK_INT;
import static com.v7878.unsafe.Reflection.getHiddenMethod;

import android.util.Log;

import com.v7878.unsafe.ClassUtils;
import com.v7878.unsafe.invoke.EmulatedStackFrame;
import com.v7878.unsafe.invoke.Transformers;
import com.v7878.vmtools.Hooks;
import com.v7878.vmtools.Hooks.EntryPointType;

/**
 * SystemServerInit — Hooks into system_server bootstrap to apply CorePatch N hooks.
 *
 * Strategy (from PMPatch):
 *   Hook RuntimeInit.findStaticMain() — called right before SystemServer.main() runs.
 *   When the class argument == "com.android.server.SystemServer", we know
 *   we're inside system_server's startup sequence.
 *
 *   At that point, the system_server ClassLoader is available and all
 *   PMS/ECM/AppOps classes can be hooked before they are first used.
 */
public class SystemServerInit {
    private static final String SYSTEM_SERVER_CLASS = "com.android.server.SystemServer";
    private static final String RUNTIME_INIT_CLASS  = "com.android.internal.os.RuntimeInit";

    /** Called inside the hook — inspect frame to detect system_server startup. */
    private static void runForSystemServer(EmulatedStackFrame frame) {
        var accessor = frame.accessor();
        // Arg 0 = String className, Arg 2 = ClassLoader
        String className  = accessor.getReference(0);
        ClassLoader loader = accessor.getReference(2);

        if (SYSTEM_SERVER_CLASS.equals(className)) {
            Log.i(Main.TAG, "system_server detected — applying hooks");
            try {
                EntryPoint.initSystemServer(loader);
            } catch (Throwable th) {
                Log.e(Main.TAG, "Hook application failed", th);
            }
        }
    }

    /** Install the RuntimeInit hook. Called from Main.main() in system_server. */
    public static void init() {
        Class<?> initClass = ClassUtils.sysClass(RUNTIME_INIT_CLASS);

        // API 26 (Oreo): invokeStaticMain; API 27+ (Oreo MR1+): findStaticMain
        String methodName = (SDK_INT == 26) ? "invokeStaticMain" : "findStaticMain";

        var method = getHiddenMethod(initClass, methodName,
            String.class, String[].class, ClassLoader.class);

        Hooks.hook(method, EntryPointType.CURRENT, (original, frame) -> {
            try {
                runForSystemServer(frame);
            } catch (Throwable th) {
                Log.e(Main.TAG, "SystemServerInit hook error", th);
            }
            Transformers.invokeExact(original, frame);
        }, EntryPointType.DIRECT);

        Log.i(Main.TAG, "RuntimeInit." + methodName + " hooked");
    }
}
