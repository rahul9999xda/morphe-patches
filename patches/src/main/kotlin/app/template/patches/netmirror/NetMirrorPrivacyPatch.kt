package app.template.patches.netmirror

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.NETMIRROR_COMPATIBILITY

private const val UNKNOWN = "unknown"

@Suppress("unused")
val netMirrorPrivacyDeviceTelemetryPatch = bytecodePatch(
    name = "NetMirror: Neutralize device telemetry",
    description = "Replaces confirmed RNDeviceInfo telemetry values in NetMirror 3.1 with non-device-specific placeholders while preserving method return types.",
    default = true,
) {
    compatibleWith(NETMIRROR_COMPATIBILITY)
    execute {
        getAndroidIdSyncFingerprint.method.addInstructions(0, """
            const-string v0, "$UNKNOWN"
            return-object v0
        """.trimIndent())

        getUniqueIdSyncFingerprint.method.addInstructions(0, """
            const-string v0, "$UNKNOWN"
            return-object v0
        """.trimIndent())

        getInstanceIdSyncFingerprint.method.addInstructions(0, """
            const-string v0, "$UNKNOWN"
            return-object v0
        """.trimIndent())

        getInstallReferrerSyncFingerprint.method.addInstructions(0, """
            const-string v0, "$UNKNOWN"
            return-object v0
        """.trimIndent())

        getInstallerPackageNameSyncFingerprint.method.addInstructions(0, """
            const-string v0, "$UNKNOWN"
            return-object v0
        """.trimIndent())

        getIpAddressSyncFingerprint.method.addInstructions(0, """
            const-string v0, "$UNKNOWN"
            return-object v0
        """.trimIndent())

        getMacAddressSyncFingerprint.method.addInstructions(0, """
            const-string v0, "$UNKNOWN"
            return-object v0
        """.trimIndent())

        getCarrierSyncFingerprint.method.addInstructions(0, """
            const-string v0, "$UNKNOWN"
            return-object v0
        """.trimIndent())

        getFingerprintSyncFingerprint.method.addInstructions(0, """
            const-string v0, "$UNKNOWN"
            return-object v0
        """.trimIndent())

        getSerialNumberSyncFingerprint.method.addInstructions(0, """
            const-string v0, "$UNKNOWN"
            return-object v0
        """.trimIndent())

        // Timestamp methods are intentionally left untouched in this diagnostic revision.
        // The prior revision used a wide-constant insertion that emitted return-wide before
        // initialization on v3.1, which can fail DEX verification during startup.
    }
}
