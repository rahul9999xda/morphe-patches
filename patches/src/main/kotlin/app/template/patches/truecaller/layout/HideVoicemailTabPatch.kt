package app.template.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TRUECALLER_COMPATIBILITY

val hideVoicemailTabPatch = bytecodePatch(
    name = "Hide Voicemail tab",
    description = "Removes the current Voicemail bottom-bar item from the Lxm6.g() list while leaving the voicemail service itself untouched.",
) {
    compatibleWith(TRUECALLER_COMPATIBILITY)
    execute {
        val method = BottomBarRebuildFingerprint.method
        val index = method.implementation?.instructions?.indexOfFirst {
            it.toString().contains("Lum6;->f:Lgf3;")
        } ?: -1
        check(index >= 0) { "Voicemail bottom-bar owner not found" }
        method.addInstructions(index + 1, "const/4 v5, 0x0")
    }
}
