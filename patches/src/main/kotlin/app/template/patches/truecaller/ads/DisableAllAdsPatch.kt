package app.template.patches.truecaller.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TRUECALLER_COMPATIBILITY

val disableAllAdsPatch = bytecodePatch(
    name = "Disable Truecaller ad delivery",
    description = "Short-circuits the verified 26.39.6 central ad-request engine with its existing nullable no-ad contract before source resolution, auction, or mediation. This targets the app-owned ad pipeline; it does not mutate paid entitlement state.",
) {
    compatibleWith(TRUECALLER_COMPATIBILITY)
    execute {
        CentralAdRequestFingerprint.method.addInstructions(0, """
            const/4 v14, 0x0
            return-object v14
        """.trimIndent())
    }
}
