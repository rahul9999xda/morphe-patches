package app.template.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TRUECALLER_COMPATIBILITY
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

val hidePremiumTabPatch = bytecodePatch(
    name = "Hide Premium tab",
    description = "Removes the current Premium bottom-bar item from the Lxm6.g() list while leaving the rest of the navigation builder intact.",
) {
    compatibleWith(TRUECALLER_COMPATIBILITY)
    execute {
        val method = BottomBarRebuildFingerprint.method
        val instructions = method.implementation?.instructions
            ?: error("Bottom-bar rebuild method has no implementation")
        val index = instructions.indexOfFirst { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "Lum6;" && reference.name == "d" && reference.type == "Lpsw;"
        }
        check(index >= 0) { "Premium bottom-bar owner not found" }
        method.addInstructions(index + 1, "const/4 v4, 0x0")
    }
}
