package org.lsposed.corepatch.zygisk;

import static android.os.Build.VERSION.SDK_INT;
import static com.v7878.unsafe.access.AccessLinker.FieldAccessKind.INSTANCE_GETTER;
import static com.v7878.unsafe.invoke.EmulatedStackFrame.RETURN_VALUE_IDX;

import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.access.AccessLinker;
import com.v7878.unsafe.access.AccessLinker.FieldAccess;
import com.v7878.unsafe.invoke.Transformers;
import com.v7878.vmtools.HookTransformer;

import java.security.Signature;

public class HookList {

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class AccessI {
        @FieldAccess(kind = INSTANCE_GETTER, klass = "java.security.Signature", name = "state")
        abstract int state(Signature instance);

        static final AccessI INSTANCE = AccessLinker.generateImpl(AccessI.class);
    }

    /**
     * Common cryptographic hooks applied in all processes (apps + system_server).
     * Bypasses low-level APK signature verification (V2/V3/V4 schemes).
     */
    public static void initCommon(BulkHooker hooks) {
        HookTransformer verifyImpl = (original, frame) -> {
            try {
                var accessor = frame.accessor();
                Signature thiz = accessor.getReference(0);
                if (thiz != null) {
                    String algo = thiz.getAlgorithm();
                    if (algo != null) {
                        switch (algo.toLowerCase()) {
                            case "rsa-sha1", "sha1withrsa", "sha256withdsa", "sha256withrsa" -> {
                                int state = 3;
                                try {
                                    state = AccessI.INSTANCE.state(thiz);
                                } catch (Throwable ignored) {
                                }
                                if (state == 3 /* Signature.VERIFY */) {
                                    frame.accessor().setBoolean(RETURN_VALUE_IDX, true);
                                    return;
                                }
                            }
                        }
                    }
                }
            } catch (Throwable th) {
                Logger.e("verifyImpl error", th);
            }
            Transformers.invokeExact(original, frame);
        };

        hooks.addExact(verifyImpl, "java.security.Signature", "verify", "boolean", "byte[]");
        hooks.addExact(verifyImpl, "java.security.Signature", "verify", "boolean", "byte[]", "int", "int");
        hooks.addExact(HTF.TRUE, "com.android.org.conscrypt.OpenSSLSignature", "engineVerify", "boolean", "byte[]");
        hooks.addExact(HTF.TRUE, "java.security.MessageDigest", "isEqual", "boolean", "byte[]", "byte[]");
    }

    /**
     * Register all system_server hooks into the given BulkHooker.
     * Called from EntryPoint.initSystemServer().
     */
    public static void initSystem(BulkHooker hooks) {
        // ──────────────────────────────────────────────────────────────────
        // Feature 1: verifySignatures → false
        // ──────────────────────────────────────────────────────────────────
        hooks.addAll(HTF.FALSE,
            "com.android.server.pm.PackageManagerServiceUtils", "verifySignatures");

        // ──────────────────────────────────────────────────────────────────
        // Feature 2: Downgrade bypass
        // 1) isDowngradePermitted -> true (Android 14/15/16/17 skips checkDowngrade!)
        // 2) checkDowngrade -> NOP (all overloads across all PMS helper classes)
        // ──────────────────────────────────────────────────────────────────
        if (SDK_INT >= 33) {
            hooks.addAll(HTF.TRUE,
                "com.android.server.pm.PackageManagerServiceUtils", "isDowngradePermitted");
            hooks.addAll(HTF.NOP,
                "com.android.server.pm.InstallPackageHelper", "checkDowngrade");
            hooks.addAll(HTF.NOP,
                "com.android.server.pm.PackageManagerServiceUtils", "checkDowngrade");
        }
        hooks.addAll(HTF.NOP,
            "com.android.server.pm.PackageManagerService", "checkDowngrade");

        // ──────────────────────────────────────────────────────────────────
        // Feature 3: isVerificationEnabled → false
        // ──────────────────────────────────────────────────────────────────
        hooks.addAll(HTF.FALSE,
            "com.android.server.pm.PackageManagerService", "isVerificationEnabled");

        // ──────────────────────────────────────────────────────────────────
        // Feature 4: checkCapability → true (conditional)
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
        // ──────────────────────────────────────────────────────────────────
        if (SDK_INT >= 33) {
            hooks.addAll(HTF.FALSE,
                "com.android.server.appop.AppOpsService", "isOperationRestricted");
        }

        // ──────────────────────────────────────────────────────────────────
        // Feature 13: Restricted Settings — ECM layer (API 35+ / Android 15+)
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
     */
    public static void initStaticFields(ClassLoader loader) {
        if (SDK_INT >= 30) {
            FieldUtils.setStaticBoolean(loader,
                "com.android.server.pm.ReconcilePackageUtils",
                "ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS");
        }
    }
}
