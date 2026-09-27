package org.lsposed.corepatch.zygisk;

import static android.os.Build.VERSION.SDK_INT;

import android.util.Log;

import com.v7878.vmtools.HookTransformer;

/**
 * HookList — Complete set of CorePatch N hooks translated to Zygisk/AndroidVMTools API.
 *
 * Covers ALL CorePatch N features (originally Xposed-based):
 *
 *  Feature 1:  verifySignatures → false
 *              Bypass APK signature verification during package install.
 *
 *  Feature 2:  checkDowngrade → void (NOP)
 *              Allow installing older versions over newer ones.
 *
 *  Feature 3:  isVerificationEnabled → false
 *              Disable Package Verification Agent (e.g. Play Protect install checks).
 *
 *  Feature 4:  checkCapability → true (conditional by call stack)
 *              Allow signature downgrade capabilities (V3 cert rotation).
 *              Only active during install paths, not reconcile paths.
 *
 *  Feature 5:  checkCapabilityRecover → true
 *              Allow capability recovery for cert rotation lineage.
 *
 *  Feature 6:  hasCommonAncestor → true
 *              Shared UID: allow packages with different signing lineages.
 *
 *  Feature 7:  signaturesMatchExactly → true
 *              Bypass exact signature match (split APKs with different certs).
 *
 *  Feature 8:  shouldCheckUpgradeKeySetLocked → true
 *  Feature 9:  checkUpgradeKeySetLocked → true
 *              V3 upgrade key set: bypass key set enforcement.
 *
 *  Feature 10: doesSignatureMatchForPermissions → true
 *              Allow apps with different signatures to share permissions.
 *
 *  Feature 11: canJoinSharedUserId → true (API 33+)
 *              Allow apps to join shared UIDs without signature match.
 *
 *  Feature 11b: ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS = true (static field)
 *              Allow non-preloaded apps to join system shared UIDs.
 *
 *  Feature 12: AppOpsService.isOperationRestricted → false (API 33–34)
 *              Disable Restricted Settings via AppOps layer.
 *
 *  Feature 13: EnhancedConfirmationManager.isRestricted → false (API 35+)
 *              Disable Restricted Settings via ECM layer (Android 15+).
 *
 *  Feature 13b: isClearRestrictionAllowed → true (API 35+)
 *              Allow clearing restrictions without extra authentication.
 *
 *  Bonus A:    ApkSignatureVerifier.getMinimumSignatureSchemeVersionForTargetSdk → false
 *              Disable minimum signing scheme version enforcement.
 *
 *  Bonus B:    ScanPackageUtils.assertMinSignatureSchemeIsValid → NOP (API 33+)
 *              Bypass minimum signature scheme assertion.
 */
public class HookList {

    /**
     * Register all system_server hooks into the given BulkHooker.
     * Called from EntryPoint.initSystemServer().
     */
    public static void initSystem(BulkHooker hooks) {
        // ──────────────────────────────────────────────────────────────────
        // Feature 1: verifySignatures → false
        // PackageManagerServiceUtils (API 28+) handles all signature checks.
        // Returning false means "verification succeeded without checking".
        // ──────────────────────────────────────────────────────────────────
        hooks.addAll(HTF.FALSE,
            "com.android.server.pm.PackageManagerServiceUtils", "verifySignatures");

        // ──────────────────────────────────────────────────────────────────
        // Feature 2: checkDowngrade → void (NOP)
        // ──────────────────────────────────────────────────────────────────
        if (SDK_INT <= 32) {
            hooks.addAll(HTF.NOP,
                "com.android.server.pm.PackageManagerService", "checkDowngrade");
        } else {
            hooks.addAll(HTF.NOP,
                "com.android.server.pm.PackageManagerServiceUtils", "checkDowngrade");
        }

        // ──────────────────────────────────────────────────────────────────
        // Feature 3: isVerificationEnabled → false
        // ──────────────────────────────────────────────────────────────────
        hooks.addAll(HTF.FALSE,
            "com.android.server.pm.PackageManagerService", "isVerificationEnabled");

        // ──────────────────────────────────────────────────────────────────
        // Feature 4: checkCapability → true (conditional)
        // Only return true when called from install paths, NOT from reconcile.
        // API < 33: always true (simpler ROM, no reconcilePackages issue).
        // API >= 33: conditional based on call stack.
        // ──────────────────────────────────────────────────────────────────
        HookTransformer checkCapImpl = SDK_INT < 33
            ? HTF.TRUE
            : HTF.constant(true,
                new String[]{"installPackagesLI", "preparePackageLI", "preparePackage"},
                new String[]{"reconcilePackages"});

        if (SDK_INT >= 28) {
            hooks.addAll(checkCapImpl,
                "android.content.pm.PackageParser$SigningDetails", "checkCapability");
        }
        if (SDK_INT >= 33) {
            hooks.addAll(checkCapImpl,
                "android.content.pm.SigningDetails", "checkCapability");
        }

        // ──────────────────────────────────────────────────────────────────
        // Feature 5: checkCapabilityRecover → true
        // ──────────────────────────────────────────────────────────────────
        if (SDK_INT >= 28) {
            hooks.addAll(HTF.TRUE,
                "android.content.pm.PackageParser$SigningDetails", "checkCapabilityRecover");
        }
        if (SDK_INT >= 33) {
            hooks.addAll(HTF.TRUE,
                "android.content.pm.SigningDetails", "checkCapabilityRecover");
        }

        // ──────────────────────────────────────────────────────────────────
        // Feature 6: hasCommonAncestor → true (API 30+)
        // ──────────────────────────────────────────────────────────────────
        if (SDK_INT >= 30) {
            hooks.addAll(HTF.TRUE,
                "android.content.pm.SigningDetails", "hasCommonAncestor");
        }

        // ──────────────────────────────────────────────────────────────────
        // Feature 7: signaturesMatchExactly → true
        // ──────────────────────────────────────────────────────────────────
        if (SDK_INT >= 28) {
            hooks.addAll(HTF.TRUE,
                "android.content.pm.PackageParser$SigningDetails", "signaturesMatchExactly");
        }
        if (SDK_INT >= 33) {
            hooks.addAll(HTF.TRUE,
                "android.content.pm.SigningDetails", "signaturesMatchExactly");
        }

        // ──────────────────────────────────────────────────────────────────
        // Feature 8-9: KeySetManagerService — V3 upgrade key bypass
        // ──────────────────────────────────────────────────────────────────
        hooks.addAll(HTF.TRUE,
            "com.android.server.pm.KeySetManagerService", "shouldCheckUpgradeKeySetLocked");
        hooks.addAll(HTF.TRUE,
            "com.android.server.pm.KeySetManagerService", "checkUpgradeKeySetLocked");

        // ──────────────────────────────────────────────────────────────────
        // Feature 10: doesSignatureMatchForPermissions → true
        // Location changed in API 33 (moved from PMS to InstallPackageHelper).
        // ──────────────────────────────────────────────────────────────────
        if (SDK_INT >= 31 && SDK_INT <= 32) {
            hooks.addAll(HTF.TRUE,
                "com.android.server.pm.PackageManagerService",
                "doesSignatureMatchForPermissions");
        } else if (SDK_INT >= 33) {
            hooks.addAll(HTF.TRUE,
                "com.android.server.pm.InstallPackageHelper",
                "doesSignatureMatchForPermissions");
        }

        // ──────────────────────────────────────────────────────────────────
        // Feature 11: canJoinSharedUserId → true (API 33+)
        // ──────────────────────────────────────────────────────────────────
        if (SDK_INT >= 33) {
            hooks.addAll(HTF.TRUE,
                "com.android.server.pm.PackageManagerServiceUtils", "canJoinSharedUserId");
        }

        // ──────────────────────────────────────────────────────────────────
        // Feature 12: Restricted Settings — AppOps layer (API 33-34)
        // AppOpsService.isOperationRestricted() → false
        // Prevents OP_RESTRICTED_SETTINGS from being triggered.
        // ──────────────────────────────────────────────────────────────────
        if (SDK_INT >= 33) {
            hooks.addAll(HTF.FALSE,
                "com.android.server.appop.AppOpsService", "isOperationRestricted");
        }

        // ──────────────────────────────────────────────────────────────────
        // Feature 13: Restricted Settings — ECM layer (API 35+ / Android 15+)
        // EnhancedConfirmationManager blocks sideloaded apps from accessing
        // sensitive settings (Accessibility, Notification Listeners, etc.).
        // We hook both known class names as fallback for OEM variants.
        // ──────────────────────────────────────────────────────────────────
        if (SDK_INT >= 35) {
            hooks.addAll(HTF.FALSE,
                "com.android.server.ecm.EnhancedConfirmationManagerServiceImpl", "isRestricted");
            hooks.addAll(HTF.FALSE,
                "com.android.server.ecm.EnhancedConfirmationManagerService",   "isRestricted");
            hooks.addAll(HTF.TRUE,
                "com.android.server.ecm.EnhancedConfirmationManagerServiceImpl",
                "isClearRestrictionAllowed");
            hooks.addAll(HTF.TRUE,
                "com.android.server.ecm.EnhancedConfirmationManagerService",
                "isClearRestrictionAllowed");
        }

        // ──────────────────────────────────────────────────────────────────
        // Bonus A: ApkSignatureVerifier minimum scheme bypass (API 30+)
        // Prevents rejection of APKs signed with deprecated V1 scheme.
        // ──────────────────────────────────────────────────────────────────
        if (SDK_INT >= 30) {
            hooks.addAll(HTF.FALSE,
                "android.util.apk.ApkSignatureVerifier",
                "getMinimumSignatureSchemeVersionForTargetSdk");
        }

        // ──────────────────────────────────────────────────────────────────
        // Bonus B: assertMinSignatureSchemeIsValid → NOP (API 33+)
        // ──────────────────────────────────────────────────────────────────
        if (SDK_INT >= 33) {
            hooks.addAll(HTF.NOP,
                "com.android.server.pm.ScanPackageUtils",
                "assertMinSignatureSchemeIsValid");
        }
    }

    /**
     * Apply static field patches that cannot be done via method hooks.
     * Called after hooks.apply() so classes are already loaded.
     *
     * @param loader system_server ClassLoader
     */
    public static void initStaticFields(ClassLoader loader) {
        // Feature 11b: ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS = true
        // Allows apps that weren’t pre-installed in /system to join system shared UIDs.
        // This is a static boolean field, not a method — must be set via reflection.
        if (SDK_INT >= 30) {
            FieldUtils.setStaticBoolean(loader,
                "com.android.server.pm.ReconcilePackageUtils",
                "ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS");
        }
    }
}
