package app.template.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TRUECALLER_COMPATIBILITY
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

val hideVoicemailTabPatch = bytecodePatch(
    name = "Hide Voicemail tab",
    description = "Removes the current Voicemail bottom-bar item from the Lxm6.g() list while leaving the voicemail service itself untouched.",
) {
    compatibleWith(TRUECALLER_COMPATIBILITY)
    execute {
        val method = BottomBarRebuildFingerprint.method
        val instructions = method.implementation?.instructions
            ?: error("Bottom-bar rebuild method has no implementation")
        val index = instructions.indexOfFirst { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "Lum6;" && reference.name == "f" && reference.type == "Lgf3;"
        }
        check(index >= 0) { "Voicemail bottom-bar owner not found" }
        method.addInstructions(index + 1, "const/4 v5, 0x0")
    }
}
