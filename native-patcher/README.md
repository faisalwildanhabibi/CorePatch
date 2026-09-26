# CorePatch N — Native Patcher

Script untuk mem-patch `services.jar` secara native (tanpa Xposed) pada perangkat Android 13–17.

> **Target aktif:** PixelOS 17.0 / Android 17 (API 37) / Poco F3 alioth

## Fitur yang Di-patch

| # | Fitur | Target Class | Return |
|---|---|---|---|
| 1 | Signature verification bypass | `PackageManagerServiceUtils.verifySignatures` | `false` |
| 2 | Allow downgrade | `PackageManagerServiceUtils.checkDowngrade` | `void` |
| 3 | Disable verification agent | `PackageManagerService.isVerificationEnabled` | `false` |
| 4 | checkCapability bypass | `SigningDetails.checkCapability` | `true` |
| 5 | Exact signature match bypass | `SigningDetails.signaturesMatchExactly` | `true` |
| 6 | Shared UID bypass | `SigningDetails.hasCommonAncestor` | `true` |
| 7 | V3 signature bypass | `KeySetManagerService.*` | `true` |
| 8 | Permission signature bypass | `InstallPackageHelper.doesSignatureMatchForPermissions` | `true` |
| 9 | Shared UID join bypass | `PackageManagerServiceUtils.canJoinSharedUserId` | `true` |
| 10 | System shared UID | `ReconcilePackageUtils.ALLOW_NON_PRELOADS_SYSTEM_SHAREDUIDS` | `true` |
| 11 | checkCapabilityRecover | `SigningDetails.checkCapabilityRecover` | `true` |
| 12 | **Restricted Settings (ECM)** | `EnhancedConfirmationManagerServiceImpl.isRestricted` | `false` |
| 13 | **Restricted Settings (AppOps)** | `AppOpsService.isOperationRestricted` | `false` |
| 14 | **Clear restriction allowed** | `EnhancedConfirmationManagerServiceImpl.isClearRestrictionAllowed` | `true` |

## File

- `run.sh` — Orchestrator utama (jalankan ini)
- `patcher.py` — Python smali patcher (14 patches)
- `build_module.sh` — KernelSU/ReSukiSu module builder
- `apktool.jar` — Harus disalin manual ke `/sdcard/CorePatchN-Patcher/` via ADB

## Cara Pakai

```bash
# Di Termux dengan root
su
cd /sdcard/CorePatchN-Patcher
sh run.sh
```

## Environment yang Diuji

- Device: Poco F3 (alioth)
- ROM: PixelOS 17.0 (Android 17, API 37)
- Kernel: 4.19.325 NON-GKI
- Root: ReSukiSu (ksud 4.2.0-rc3) + ReSuSFS v2.3.0
- Zygisk: Zygisk Next 1.5.0
- Xposed: Vector v2.2 (JingMatrix)
