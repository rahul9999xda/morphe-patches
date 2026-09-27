package app.template.patches.netmirror

import app.morphe.patcher.PatchException
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_NETMIRROR

private fun blankStringMethodPatch(
    name: String,
    description: String,
    fingerprintProvider: () -> app.morphe.patcher.Fingerprint,
) = bytecodePatch(
    name = name,
    description = description,
    default = true,
) {
    compatibleWith(COMPATIBILITY_NETMIRROR)
    execute {
        try {
            fingerprintProvider().method.addInstructions(
                0,
                """
                    const-string v0, ""
                    return-object v0
                """.trimIndent(),
            )
        } catch (e: PatchException) {
            println("[$name] fingerprint not applied: ${e.message}")
        }
    }
}

private fun zeroLongMethodPatch(
    name: String,
    description: String,
    fingerprintProvider: () -> app.morphe.patcher.Fingerprint,
) = bytecodePatch(
    name = name,
    description = description,
    default = true,
) {
    compatibleWith(COMPATIBILITY_NETMIRROR)
    execute {
        try {
            fingerprintProvider().method.addInstructions(
                0,
                """
                    const-wide/16 v0, 0x0
                    return-wide v0
                """.trimIndent(),
            )
        } catch (e: PatchException) {
            println("[$name] fingerprint not applied: ${e.message}")
        }
    }
}

// RNDeviceInfo's asynchronous methods in this APK delegate to their corresponding Sync methods,
// so patching these sync implementations also covers the Promise-based JS APIs.

@Suppress("unused")
val netMirrorDisableAndroidIdPatch = blankStringMethodPatch(
    "NetMirror: Disable Android ID",
    "Stops the RNDeviceInfo Android-ID surface from returning a device identifier.",
) { getAndroidIdSyncFingerprint }

@Suppress("unused")
val netMirrorDisableUniqueIdPatch = blankStringMethodPatch(
    "NetMirror: Disable unique ID",
    "Stops RNDeviceInfo's persistent unique-ID surface.",
) { getUniqueIdSyncFingerprint }

@Suppress("unused")
val netMirrorDisableInstanceIdPatch = blankStringMethodPatch(
    "NetMirror: Disable instance ID",
    "Stops the RNDeviceInfo instance-ID/installation attribution surface.",
) { getInstanceIdSyncFingerprint }

@Suppress("unused")
val netMirrorDisableInstallReferrerPatch = blankStringMethodPatch(
    "NetMirror: Disable install referrer",
    "Stops install-referrer attribution from being exposed to JavaScript.",
) { getInstallReferrerSyncFingerprint }

@Suppress("unused")
val netMirrorDisableInstallerPatch = blankStringMethodPatch(
    "NetMirror: Disable installer attribution",
    "Stops the installer package attribution surface.",
) { getInstallerPackageNameSyncFingerprint }

@Suppress("unused")
val netMirrorDisableIpPatch = blankStringMethodPatch(
    "NetMirror: Disable IP telemetry",
    "Stops RNDeviceInfo from exposing the local IP-address telemetry surface.",
) { getIpAddressSyncFingerprint }

@Suppress("unused")
val netMirrorDisableMacPatch = blankStringMethodPatch(
    "NetMirror: Disable MAC telemetry",
    "Stops RNDeviceInfo from exposing MAC-address telemetry.",
) { getMacAddressSyncFingerprint }

@Suppress("unused")
val netMirrorDisableCarrierPatch = blankStringMethodPatch(
    "NetMirror: Disable carrier telemetry",
    "Stops cellular-carrier information from being exposed by RNDeviceInfo.",
) { getCarrierSyncFingerprint }

@Suppress("unused")
val netMirrorDisableBuildFingerprintPatch = blankStringMethodPatch(
    "NetMirror: Disable build fingerprint",
    "Stops Android build-fingerprint collection through RNDeviceInfo.",
) { getFingerprintSyncFingerprint }

@Suppress("unused")
val netMirrorDisableSerialPatch = blankStringMethodPatch(
    "NetMirror: Disable serial telemetry",
    "Stops Android serial-number collection through RNDeviceInfo.",
) { getSerialNumberSyncFingerprint }

@Suppress("unused")
val netMirrorDisableInstallTimePatch = zeroLongMethodPatch(
    "NetMirror: Disable first-install telemetry",
    "Stops first-install timestamp exposure through RNDeviceInfo.",
) { getFirstInstallTimeSyncFingerprint }

@Suppress("unused")
val netMirrorDisableUpdateTimePatch = zeroLongMethodPatch(
    "NetMirror: Disable update-time telemetry",
    "Stops last-update timestamp exposure through RNDeviceInfo.",
) { getLastUpdateTimeSyncFingerprint }
