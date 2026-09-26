package org.lsposed.corepatch.hook

import android.annotation.SuppressLint
import android.os.Build
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.XposedHelper.findClassIfExists
import org.lsposed.corepatch.XposedHelper.hookBefore
import org.lsposed.corepatch.XposedHelper.log

/**
 * RestrictedSettingsHook
 *
 * Bypasses Android's "Restricted Settings" mechanism globally for all apps.
 * Targets two enforcement layers:
 *
 * - Android 13-14 (API 33-34): AppOpsService.isOperationRestricted()
 * - Android 15-17 (API 35-37): EnhancedConfirmationManagerService.isRestricted()
 *
 * When active, sideloaded apps can freely access Accessibility Services,
 * Notification Listeners, and other sensitive settings without the
 * "Restricted Settings" dialog.
 *
 * Compatible with PixelOS 17.0 / Android 17 (API 37).
 */
@SuppressLint("PrivateApi")
object RestrictedSettingsHook : BaseHook() {
    override val name = "RestrictedSettingsHook"

    override fun hook() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            log("[$name] Skipped: API ${Build.VERSION.SDK_INT} < 33, Restricted Settings not present")
            return
        }

        log("[$name] Initializing for API ${Build.VERSION.SDK_INT}")

        // Layer 1: Android 13-34 via AppOpsService
        hookAppOpsRestriction()

        // Layer 2: Android 35+ via EnhancedConfirmationManager (ECM)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            hookEnhancedConfirmationManager()
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Layer 1: AppOpsService (Android 13 / API 33+)
    // https://cs.android.com/android/platform/superproject/+/android-13.0.0_r1:
    // frameworks/base/services/core/java/com/android/server/appop/AppOpsService.java
    // ─────────────────────────────────────────────────────────────────────
    private fun hookAppOpsRestriction() {
        val appOpsServiceClazz = findClassIfExists("com.android.server.appop.AppOpsService")
            ?: run {
                log("[$name] AppOpsService not found — skipping Layer 1")
                return
            }

        var hookedCount = 0

        // isOperationRestricted(int uid, String packageName, int op) — Android 13-14
        appOpsServiceClazz.declaredMethods
            .filter { m -> m.name == "isOperationRestricted" }
            .forEach { method ->
                hookBefore(method) { callback ->
                    if (Config.isBypassRestrictedSettingsEnabled()) {
                        callback.returnAndSkip(false)
                    }
                }
                hookedCount++
            }

        // checkOperation for OP_RESTRICTED_SETTINGS (op code 120 in AOSP API 33+)
        // Fallback for ROMs that renamed isOperationRestricted
        if (hookedCount == 0) {
            appOpsServiceClazz.declaredMethods
                .filter { m -> m.name == "checkOperation" || m.name == "noteOperation" }
                .forEach { method ->
                    hookBefore(method) { callback ->
                        if (!Config.isBypassRestrictedSettingsEnabled()) return@hookBefore
                        // OP_RESTRICTED_SETTINGS = 120 in AOSP Android 13+
                        val opArg = callback.args.firstOrNull { it is Int } as? Int
                        if (opArg == 120) {
                            callback.returnAndSkip(0) // MODE_ALLOWED = 0
                        }
                    }
                    hookedCount++
                }
        }

        log("[$name] Layer 1 (AppOpsService): $hookedCount method(s) hooked")
    }

    // ─────────────────────────────────────────────────────────────────────
    // Layer 2: EnhancedConfirmationManager / ECM (Android 15 / API 35+)
    // https://cs.android.com/android/platform/superproject/+/android-15.0.0_r1:
    // frameworks/base/services/core/java/com/android/server/ecm/
    // ─────────────────────────────────────────────────────────────────────
    private fun hookEnhancedConfirmationManager() {
        // Try both known class names — AOSP and OEM-specific variants
        val ecmClazz = findClassIfExists(
            "com.android.server.ecm.EnhancedConfirmationManagerService"
        ) ?: findClassIfExists(
            "com.android.server.ecm.EnhancedConfirmationManagerServiceImpl"
        ) ?: findClassIfExists(
            // Fallback for OEM restructured packages (e.g. PixelOS, GrapheneOS)
            "com.android.server.pm.EnhancedConfirmationManager"
        ) ?: run {
            log("[$name] EnhancedConfirmationManager not found — skipping Layer 2")
            return
        }

        var hookedCount = 0

        // isRestricted(String packageName, String settingIdentifier) -> Boolean
        ecmClazz.declaredMethods
            .filter { m -> m.name == "isRestricted" }
            .forEach { method ->
                hookBefore(method) { callback ->
                    if (Config.isBypassRestrictedSettingsEnabled()) {
                        callback.returnAndSkip(false)
                    }
                }
                hookedCount++
            }

        // isClearRestrictionAllowed(String packageName) -> Boolean
        // Allows the user to manually clear restrictions without extra auth
        ecmClazz.declaredMethods
            .filter { m -> m.name == "isClearRestrictionAllowed" }
            .forEach { method ->
                hookBefore(method) { callback ->
                    if (Config.isBypassRestrictedSettingsEnabled()) {
                        callback.returnAndSkip(true)
                    }
                }
                hookedCount++
            }

        // clearRestriction(String packageName) — proactively clear on demand
        // Hook this to ensure it always succeeds (no auth required)
        ecmClazz.declaredMethods
            .filter { m -> m.name == "clearRestriction" }
            .forEach { method ->
                hookBefore(method) { _ ->
                    // Allow without restriction — no return value needed (void)
                }
                hookedCount++
            }

        log("[$name] Layer 2 (ECM): $hookedCount method(s) hooked on ${ecmClazz.name}")
    }
}
