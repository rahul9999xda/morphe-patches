package app.template.patches.truecaller.voicemail

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TRUECALLER_COMPATIBILITY

val disableVoicemailPatch = bytecodePatch(
    name = "Disable Voicemail",
    description = "Forces the new 26.39.6 VoicemailStatus.isActiveOrPending() gate to false. It does not delete the enum, routing, call-log models, onboarding, or cloud-telephony infrastructure.",
) {
    compatibleWith(TRUECALLER_COMPATIBILITY)
    execute {
        VoicemailActiveOrPendingFingerprint.method.addInstructions(0, "const/4 p0, 0x0\nreturn p0")
    }
}
