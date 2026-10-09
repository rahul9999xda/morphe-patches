package app.template.patches.truecaller.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TRUECALLER_COMPATIBILITY

val gmsSigninBypassPatch = bytecodePatch(
    name = "Use SMS OTP instead of Google OTP API",
    description = "Forces the verified Truecaller OTP selector to return OtpSmsApi.SMS. This does not bypass account authentication.",
) {
    compatibleWith(TRUECALLER_COMPATIBILITY)
    execute {
        OtpSelectorFingerprint.method.addInstructions(0, """
            sget-object v0, Lcom/truecaller/wizard/verification/otp/sms/OtpSmsApi;->SMS:Lcom/truecaller/wizard/verification/otp/sms/OtpSmsApi;
            return-object v0
        """.trimIndent())
    }
}
