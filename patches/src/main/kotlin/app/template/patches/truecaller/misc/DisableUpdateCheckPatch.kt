package app.template.patches.truecaller.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TRUECALLER_COMPATIBILITY

val disableUpdateCheckPatch = bytecodePatch(
    name = "Disable in-app update prompts",
    description = "Returns a benign completed update-check result from the verified 26.39.6 Activity/UpdateTrigger entry point.",
) {
    compatibleWith(TRUECALLER_COMPATIBILITY)
    execute {
        UpdateTriggerEntryFingerprint.method.addInstructions(0, """
            new-instance v0, Lyd60;
            const/4 v1, 0x0
            const-wide/16 v2, 0x0
            const/4 v4, 0x0
            invoke-direct {v0, p2, v1, v2, v4}, Lyd60;-><init>(Lcom/truecaller/inappupdate/UpdateTrigger;IJI)V
            invoke-static {v0}, Ljava/util/concurrent/CompletableFuture;->completedFuture(Ljava/lang/Object;)Ljava/util/concurrent/CompletableFuture;
            move-result-object v0
            return-object v0
        """.trimIndent())
    }
}
