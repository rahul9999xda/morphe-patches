package app.template.patches.netmirror

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.NETMIRROR_COMPATIBILITY

private const val UNKNOWN = "unknown"

@Suppress("unused")
val netMirrorPrivacyDeviceTelemetryPatch = bytecodePatch(
    name = "NetMirror: Neutralize device telemetry",
    description = "Neutralizes the confirmed NetMirror 3.1 RNDeviceInfo identifier/referrer/IP/MAC/carrier/fingerprint/serial values and the two persistent install/update timestamps without changing unrelated device-information APIs.",
    default = true,
) {
    compatibleWith(NETMIRROR_COMPATIBILITY)
    execute {
        // High-confidence identifier / tracking inputs.
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

        // Persistent installation/update timestamps are also device-history signals.
        // Use a simple zero-wide return rather than the previous high16 construction.
        // The async Promise methods delegate to these Sync implementations in RNDeviceInfo.
        getFirstInstallTimeSyncFingerprint.method.addInstructions(0, """
            const-wide/16 v0, 0x0
            return-wide v0
        """.trimIndent())

        getLastUpdateTimeSyncFingerprint.method.addInstructions(0, """
            const-wide/16 v0, 0x0
            return-wide v0
        """.trimIndent())
    }
}
