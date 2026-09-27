# ZygiskCore

Conversion of **CorePatch N** (LSPosed/Xposed module) to a **Zygisk module** — no Xposed framework required.

Based on [PMPatch](https://github.com/vova7878-modules/PMPatch) architecture by vova7878, 
extended with all CorePatch N features.

## Features

| # | Feature | API Range | Target Class | Return |
|---|---|---|---|---|
| 1 | Signature verification bypass | 28+ | `PackageManagerServiceUtils.verifySignatures` | `false` |
| 2 | Allow downgrade | 28+ | `PackageManagerServiceUtils.checkDowngrade` | `void` |
| 3 | Disable verification agent | 28+ | `PackageManagerService.isVerificationEnabled` | `false` |
| 4 | checkCapability bypass | 28+ | `SigningDetails.checkCapability` | `true` (conditional) |
| 5 | checkCapabilityRecover | 28+ | `SigningDetails.checkCapabilityRecover` | `true` |
| 6 | Shared UID lineage bypass | 30+ | `SigningDetails.hasCommonAncestor` | `true` |
| 7 | Exact signature match bypass | 28+ | `SigningDetails.signaturesMatchExactly` | `true` |
| 8 | V3 key set check | 28+ | `KeySetManagerService.shouldCheckUpgradeKeySetLocked` | `true` |
| 9 | V3 key set validation | 28+ | `KeySetManagerService.checkUpgradeKeySetLocked` | `true` |
| 10 | Permission signature bypass | 31+ | `InstallPackageHelper.doesSignatureMatchForPermissions` | `true` |
| 11 | Shared UID join | 33+ | `PackageManagerServiceUtils.canJoinSharedUserId` | `true` |
| 11b | System shared UID | 30+ | `ReconcilePackageUtils.ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS` | `true` (field) |
| 12 | Restricted Settings (AppOps) | 33-34 | `AppOpsService.isOperationRestricted` | `false` |
| 13 | Restricted Settings (ECM) | 35+ | `EnhancedConfirmationManagerServiceImpl.isRestricted` | `false` |

## Requirements

- Android 8.1+ (API 28+)
- **Zygisk** enabled (Zygisk Next, or built-in Magisk/KernelSU Zygisk)
- Tested on: PixelOS 17.0 / Android 17 (API 37) / Poco F3
  - Root: ReSukiSu + ReSuSFS v2.3.0
  - Zygisk: Zygisk Next 1.5.0

## Architecture

```
Zygisk injection → Main.main()
  └─ SystemServerInit.init()        ← hooks RuntimeInit.findStaticMain()
       └─ EntryPoint.initSystemServer() ← when SystemServer detected
            ├─ BulkHooker.apply()         ← pattern-based method hooks
            └─ FieldUtils.setStaticBoolean ← static field patches
```

## Differences from CorePatch N (Xposed)

| Aspect | CorePatch N (Xposed) | ZygiskCore (Zygisk) |
|---|---|---|
| Framework | Xposed/LSPosed/Vector | Native Zygisk |
| Hook API | `io.github.libxposed.api` | `com.v7878.vmtools.Hooks` |
| UI/Config | Settings app | No UI (always-on) |
| Scope | system_server only | system_server (via RuntimeInit hook) |
| APK signing | Required | Not required (module .zip) |
| Install | APK install + Xposed enable | Flash module .zip |
