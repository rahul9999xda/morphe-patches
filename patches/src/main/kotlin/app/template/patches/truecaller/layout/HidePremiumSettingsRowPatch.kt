package app.template.patches.truecaller.layout

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TRUECALLER_COMPATIBILITY
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/** Hides only the Get Premium row in Settings > Blocking. */
val hidePremiumSettingsRowPatch = bytecodePatch(
    name = "Hide Premium option from Settings",
    description = "Hides the Get Premium row on Truecaller's Blocking settings screen without changing entitlements.",
) {
    compatibleWith(TRUECALLER_COMPATIBILITY)
    execute {
        val method = HidePremiumSettingsRowFingerprint.method
        val instructions = method.implementation?.instructions
            ?: error("BlockSettingsFragment.onResume has no implementation")
        val listenerConstructor = instructions.indexOfFirst { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.definingClass == "Lcd6;" && reference.name == "<init>" &&
                reference.parameterTypes.map { it.toString() } ==
                    listOf("Lcom/truecaller/settings/impl/ui/block/BlockSettingsFragment;", "B")
        }
        check(listenerConstructor > 0) { "Premium Settings row listener constructor anchor not found" }
        // Insert before the new-instance immediately preceding Lcd6.<init>,
        // after the Premium row's null check. v0 is the row view and v1 is free.
        method.addInstructions(listenerConstructor - 1, """
            const/4 v1, 0x8
            invoke-virtual {v0, v1}, Landroid/view/View;->setVisibility(I)V
        """.trimIndent())
    }
}
