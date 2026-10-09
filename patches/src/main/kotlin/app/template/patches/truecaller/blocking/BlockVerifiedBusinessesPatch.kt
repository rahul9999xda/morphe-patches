package app.template.patches.truecaller.blocking

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TRUECALLER_COMPATIBILITY

/**
 * Enables the existing verified-business branch inside the current filter
 * enforcement method without replacing the surrounding contact/type checks.
 */
val blockVerifiedBusinessesPatch = bytecodePatch(
    name = "Block verified businesses",
    description = "Forces the current filter_filteringVerifiedBusinesses gate on inside Lgqj.A while preserving its contact and existing-match checks.",
) {
    compatibleWith(TRUECALLER_COMPATIBILITY)
    execute {
        val method = VerifiedBusinessFilterFingerprint.method
        val index = method.implementation?.instructions?.indexOfFirst {
            it.toString().contains("SharedPreferences;->getBoolean") &&
                it.toString().contains("getBoolean")
        } ?: -1
        check(index >= 0) { "Verified-business SharedPreferences read not found" }
        method.addInstructions(index + 2, "const/4 p2, 0x1")
    }
}
