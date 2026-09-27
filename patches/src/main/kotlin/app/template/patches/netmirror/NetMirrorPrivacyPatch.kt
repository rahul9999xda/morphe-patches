package app.template.patches.netmirror

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
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

        // These two methods contain try/catch blocks. Inserting a return at
        // instruction 0 can move a catch handler to the entry point and make
        // ART reject the method. Preserve the existing control-flow graph and
        // replace only the normal-path return. The catch-path already returns
        // -1.0 in the stock RNDeviceInfo implementation.
        for (fingerprint in listOf(
            getFirstInstallTimeSyncFingerprint,
            getLastUpdateTimeSyncFingerprint,
        )) {
            val method = fingerprint.method
            val returnIndex = method.implementation!!.instructions.indexOfFirst {
                it.opcode == Opcode.RETURN_WIDE
            }
            check(returnIndex >= 0) { "Could not locate normal return in ${method.name}" }
            method.replaceInstruction(
                returnIndex,
                """
                    const-wide/high16 v0, 0xbff0
                    return-wide v0
                """.trimIndent(),
            )
        }
    }
}
