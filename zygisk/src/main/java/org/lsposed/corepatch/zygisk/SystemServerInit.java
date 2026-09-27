package org.lsposed.corepatch.zygisk;

import static android.os.Build.VERSION.SDK_INT;
import static com.v7878.unsafe.Reflection.getHiddenMethod;

import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.unsafe.ClassUtils;
import com.v7878.unsafe.invoke.EmulatedStackFrame;
import com.v7878.unsafe.invoke.Transformers;
import com.v7878.vmtools.Hooks;
import com.v7878.vmtools.Hooks.EntryPointType;

public class SystemServerInit {
    private static final String SYSTEM_SERVER_CLASS = "com.android.server.SystemServer";
    private static final String RUNTIME_INIT_CLASS  = "com.android.internal.os.RuntimeInit";
    private static volatile boolean hooksApplied = false;

    private static void runForSystemServer(EmulatedStackFrame frame) {
        try {
            var accessor = frame.accessor();
            String className  = accessor.getReference(0);
            ClassLoader loader = accessor.getReference(2);
            Logger.i("RuntimeInit hook invoked with className=" + className + " loader=" + loader);

            if (SYSTEM_SERVER_CLASS.equals(className)) {
                if (!hooksApplied) {
                    hooksApplied = true;
                    Logger.i("system_server detected — applying hooks now");
                    EntryPoint.initSystemServer(loader);
                }
            }
        } catch (Throwable th) {
            Logger.e("runForSystemServer error", th);
        }
    }

    @DoNotShrink
    public static void init() {
        try {
            Class<?> initClass = ClassUtils.sysClass(RUNTIME_INIT_CLASS);
            String methodName = (SDK_INT == 26) ? "invokeStaticMain" : "findStaticMain";

            var method = getHiddenMethod(initClass, methodName,
                String.class, String[].class, ClassLoader.class);

            Hooks.hook(method, EntryPointType.CURRENT, (original, frame) -> {
                runForSystemServer(frame);
                Transformers.invokeExact(original, frame);
            }, EntryPointType.DIRECT);

            Logger.i("RuntimeInit." + methodName + " successfully hooked");
        } catch (Throwable th) {
            Logger.e("Failed to hook RuntimeInit", th);
        }
    }
}
