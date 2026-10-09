package app.template.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TRUECALLER_COMPATIBILITY

val hideScamFeedTabPatch = bytecodePatch(
    name = "Hide Scam Feed tab",
    description = "Disables the current featureScamFeedBottomTab gate before the bottom-bar builder can create the Scam Feed path.",
) {
    compatibleWith(TRUECALLER_COMPATIBILITY)
    execute {
        ScamFeedFeatureGateFingerprint.method.addInstructions(0, "const/4 p0, 0x0\nreturn p0")
    }
}
