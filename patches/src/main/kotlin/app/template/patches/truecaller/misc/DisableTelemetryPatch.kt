package app.template.patches.truecaller.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TRUECALLER_COMPATIBILITY

val disableTelemetryPatch = bytecodePatch(
    name = "Disable startup telemetry",
    description = "Disables the verified AppStartTracker startup-tracking enable switch in Truecaller 26.39.6.",
) {
    compatibleWith(TRUECALLER_COMPATIBILITY)
    execute {
        TelemetryEnableTrackingFingerprint.method.addInstructions(0, "return-void")
    }
}
