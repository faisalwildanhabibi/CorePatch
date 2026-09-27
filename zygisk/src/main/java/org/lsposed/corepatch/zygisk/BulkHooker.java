package org.lsposed.corepatch.zygisk;

import com.v7878.vmtools.HookTransformer;
import com.v7878.vmtools.Hooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class BulkHooker {
    public record HookElement(HookTransformer impl, String pattern) {
    }

    private final Map<String, List<HookElement>> hooks = new HashMap<>();

    public void addAll(HookTransformer impl, String clazz, String methodName) {
        addPattern(impl, clazz,
            String.format("%s\\(.*\\).*", Pattern.quote(methodName)));
    }

    public void addExact(HookTransformer impl, String clazz, String methodName,
                         String ret, String... args) {
        addPattern(impl, clazz,
            String.format("%s\\(%s\\)%s",
                Pattern.quote(methodName),
                Pattern.quote(String.join(", ", args)),
                Pattern.quote(ret)));
    }

    public void addPattern(HookTransformer impl, String clazz, String pattern) {
        hooks.computeIfAbsent(clazz, k -> new ArrayList<>())
             .add(new HookElement(impl, pattern));
    }

    public void apply(ClassLoader loader) {
        for (Map.Entry<String, List<HookElement>> entry : hooks.entrySet()) {
            Class<?> clazz;
            try {
                // initialize = false ensures static initializers don't run before system is ready
                clazz = Class.forName(entry.getKey(), false, loader);
            } catch (Throwable ex) {
                Logger.d("Class not found or failed to load (skip): " + entry.getKey() + " -> " + ex.getMessage());
                continue;
            }

            var executables = Utils.getAllExecutables(clazz);
            for (HookElement element : entry.getValue()) {
                try {
                    long count = Stream.of(executables)
                        .filter(Utils.filter(element.pattern()))
                        .peek(executable -> {
                            try {
                                Logger.i("Hooking: " + executable);
                                Hooks.hook(executable,
                                    Hooks.EntryPointType.DIRECT,
                                    element.impl(),
                                    Hooks.EntryPointType.DIRECT);
                            } catch (Throwable th) {
                                Logger.e("Failed to hook " + executable, th);
                            }
                        })
                        .count();

                    if (count == 0) {
                        Logger.d("No method matched in " + entry.getKey()
                            + " pattern=" + element.pattern());
                    }
                } catch (Throwable th) {
                    Logger.e("Error inspecting methods of " + entry.getKey(), th);
                }
            }
        }
    }
}
