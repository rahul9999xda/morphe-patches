package app.template.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TRUECALLER_COMPATIBILITY
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.Opcode

val hidePremiumSettingsRowPatch = bytecodePatch(
    name = "Hide Get Premium row in Block settings",
    description = "Sets the verified Get Premium Lt77 row to GONE in the BlockSettingsFragment binding path.",
) {
    compatibleWith(TRUECALLER_COMPATIBILITY)
    execute {
        val method = HidePremiumSettingsRowFingerprint.method
        val instructions = method.implementation?.instructions
            ?: error("Block-settings binding method has no implementation")

        // Locate the exact Get Premium view cast. The null guard immediately follows it;
        // insert at the subsequent Get Premium click-listener allocation, on the non-null path.
        val castIndex = instructions.indexOfFirst { instruction ->
            instruction.opcode == Opcode.CHECK_CAST &&
                ((instruction as? ReferenceInstruction)?.reference as? com.android.tools.smali.dexlib2.iface.reference.TypeReference)?.type == "Lt77;"
        }
        check(castIndex >= 0) { "Get Premium row view cast not found" }

        val constructorIndex = (castIndex + 1 until instructions.size).firstOrNull { index ->
            val reference = (instructions[index] as? ReferenceInstruction)?.reference as? MethodReference
            reference?.definingClass == "Lcd6;" && reference.name == "<init>" &&
                reference.parameterTypes.map { it.toString() } ==
                listOf("Lcom/truecaller/settings/impl/ui/block/BlockSettingsFragment;", "B") &&
                ((instructions.getOrNull(index + 1) as? ReferenceInstruction)?.reference as? MethodReference)
                    ?.let { it.definingClass == "Landroid/view/View;" && it.name == "setOnClickListener" } == true
        } ?: -1
        check(constructorIndex >= 0) { "Get Premium row listener anchor not found" }

        // v0 is the non-null Lt77 view here; v1 is scratch and is immediately reused
        // for the Lcd6 listener allocation. GONE removes layout space as well as visibility.
        method.addInstructions(constructorIndex - 1, """
            const/4 v1, 0x8
            invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
        """.trimIndent())
    }
}
