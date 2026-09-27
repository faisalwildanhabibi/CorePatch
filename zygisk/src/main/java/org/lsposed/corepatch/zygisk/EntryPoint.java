package org.lsposed.corepatch.zygisk;

public class EntryPoint {
    public static void mainCommon() {
        try {
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            Logger.i("Applying common cryptographic hooks via system ClassLoader: " + loader);
            BulkHooker hooks = new BulkHooker();
            HookList.initCommon(hooks);
            hooks.apply(loader);
            Logger.i("Common cryptographic hooks applied successfully");
        } catch (Throwable th) {
            Logger.e("Failed to apply common hooks", th);
        }
    }

    public static void initSystemServer(ClassLoader loader) {
        try {
            Logger.i("Applying system_server hooks via ClassLoader: " + loader);
            BulkHooker hooks = new BulkHooker();
            HookList.initSystem(hooks);
            hooks.apply(loader);

            // Apply static field patches
            HookList.initStaticFields(loader);

            Logger.i("All hooks applied to system_server successfully");
        } catch (Throwable th) {
            Logger.e("Failed to apply system_server hooks", th);
        }
    }
}
