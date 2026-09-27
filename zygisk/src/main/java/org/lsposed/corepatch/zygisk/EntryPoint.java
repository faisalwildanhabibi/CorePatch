package org.lsposed.corepatch.zygisk;

import android.util.Log;

/**
 * EntryPoint — Orchestrates hook application for each process context.
 *
 * Called from SystemServerInit when system_server is starting up.
 */
public class EntryPoint {

    /**
     * Apply all system_server hooks.
     * Called from SystemServerInit when com.android.server.SystemServer is detected.
     *
     * @param loader The ClassLoader for system_server classes.
     */
    public static void initSystemServer(ClassLoader loader) {
        if (BuildConfig.DEBUG) {
            Log.i(Main.TAG, "Applying system_server hooks via ClassLoader: " + loader);
        }

        BulkHooker hooks = new BulkHooker();
        HookList.initSystem(hooks);
        hooks.apply(loader);

        // Apply static field patches (cannot be done via method hooks)
        HookList.initStaticFields(loader);

        Log.i(Main.TAG, "All hooks applied to system_server");
    }
}
