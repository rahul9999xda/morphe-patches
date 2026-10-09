package app.template.patches.truecaller.blocking

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TRUECALLER_COMPATIBILITY
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

/**
 * Uses Truecaller's existing NUMBER_SERIES FilterMatch sentinel. The check is
 * inserted after the method has normalized a null input to an empty string,
 * and before the downstream series/database matching.
 */
val block140160TelemarketersPatch = bytecodePatch(
    name = "Block 140/160 telemarketers",
    description = "Treats 140/160-prefixed numbers as the existing NUMBER_SERIES blocked result in the 26.39.6 call-filter entry point.",
) {
    compatibleWith(TRUECALLER_COMPATIBILITY)
    execute {
        val method = NumberSeriesFilterEntryFingerprint.method
        val instructions = method.implementation?.instructions
            ?: error("Number-series filter has no implementation")
        val anchor = instructions.indexOfFirst { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "Lgqj;" &&
                reference.name == "g" &&
                reference.type == "Lzk10;"
        }
        check(anchor >= 0) { "Call-filter database field anchor not found in Lgqj.a(String,String,boolean)" }
        method.addInstructions(anchor, """
            const-string v0, "140"
            invoke-virtual {v1, v0}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
            move-result v0
            if-eqz v0, :tc_check_160
            sget-object v0, Lcom/truecaller/blocking/FilterMatch;->s:Lcom/truecaller/blocking/FilterMatch;
            return-object v0
            :tc_check_160
            const-string v0, "160"
            invoke-virtual {v1, v0}, Ljava/lang/String;->startsWith(Ljava/lang/String;)Z
            move-result v0
            if-eqz v0, :tc_continue_filter
            sget-object v0, Lcom/truecaller/blocking/FilterMatch;->s:Lcom/truecaller/blocking/FilterMatch;
            return-object v0
            :tc_continue_filter
        """.trimIndent())
    }
}
