package org.lsposed.corepatch.zygisk;

import android.util.Log;

import com.v7878.vmtools.HookTransformer;
import com.v7878.vmtools.Hooks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * BulkHooker — Pattern-based bulk method hooker using AndroidVMTools.
 *
 * Adapted from PMPatch by vova7878.
 *
 * Key change: class-not-found errors are downgraded to DEBUG level
 * (not ERROR), since many classes are optional across Android versions.
 */
public class BulkHooker {
    private static final String TAG = Main.TAG;

    public record HookElement(HookTransformer impl, String pattern) {
    }

    private final Map<String, List<HookElement>> hooks = new HashMap<>();

    /** Add a hook matching any overload of the given method name. */
    public void addAll(HookTransformer impl, String clazz, String methodName) {
        addPattern(impl, clazz,
            String.format("%s\\(.*\\).*", Pattern.quote(methodName)));
    }

    /** Add a hook matching an exact signature. */
    public void addExact(HookTransformer impl, String clazz, String methodName,
                         String ret, String... args) {
        addPattern(impl, clazz,
            String.format("%s\\(%s\\)%s",
                Pattern.quote(methodName),
                Pattern.quote(String.join(", ", args)),
                Pattern.quote(ret)));
    }

    /** Add a hook with a raw regex pattern. */
    public void addPattern(HookTransformer impl, String clazz, String pattern) {
        hooks.computeIfAbsent(clazz, k -> new ArrayList<>())
             .add(new HookElement(impl, pattern));
    }

    /** Apply all registered hooks to the given ClassLoader. */
    public void apply(ClassLoader loader) {
        for (Map.Entry<String, List<HookElement>> entry : hooks.entrySet()) {
            Class<?> clazz;
            try {
                clazz = Class.forName(entry.getKey(), true, loader);
            } catch (ClassNotFoundException ex) {
                // Optional class — may not exist in this ROM/version
                Log.d(TAG, "Class not found (skip): " + entry.getKey());
                continue;
            }

            var executables = Utils.getAllExecutables(clazz);
            for (HookElement element : entry.getValue()) {
                long count = Stream.of(executables)
                    .filter(Utils.filter(element.pattern()))
                    .peek(executable -> {
                        if (BuildConfig.DEBUG) {
                            Log.i(TAG, "Hooking: " + executable);
                        }
                        Hooks.hook(executable,
                            Hooks.EntryPointType.DIRECT,
                            element.impl(),
                            Hooks.EntryPointType.DIRECT);
                    })
                    .count();

                if (count == 0) {
                    Log.d(TAG, "No method matched in " + entry.getKey()
                        + " pattern=" + element.pattern());
                }
            }
        }
    }
}
