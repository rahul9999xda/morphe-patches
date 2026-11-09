package app.template.patches.telegram.content

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.SendScreenshotMessageSecretFingerprint
import app.template.patches.telegram.SendScreenshotMessageUserFingerprint
import app.template.patches.telegram.TelegramR8SendScreenshotNotificationNormalFingerprint
import app.template.patches.telegram.TelegramR8SendScreenshotNotificationWebFingerprint

@Suppress("unused")
val telegramAntiScreenshotNotificationPatch = bytecodePatch(
    name = "Anti-screenshot notification",
    description = "Blocks screenshot notifications from being sent to the other user.",
    default = true,
) {
    compatibleWith(TELEGRAM_COMPATIBILITY, TELEGRAM_WEB_COMPATIBILITY, TELEGRAM_PLUS_COMPATIBILITY)
    dependsOn(telegramSpoofDependency())

    execute {
        // Telegram/Web 13.2.1 merged the user screenshot-notification helper into
        // the boolean dispatcher that constructs TL_messages_sendScreenshotNotification.
        // It must return false (not return-void) to preserve its current descriptor.
        val currentDispatcher =
            TelegramR8SendScreenshotNotificationNormalFingerprint.methodOrNull
                ?: TelegramR8SendScreenshotNotificationWebFingerprint.methodOrNull
        currentDispatcher?.addInstructions(0, """
            const/4 v0, 0x0
            return v0
        """)

        // Plus and legacy builds retain the named void methods.
        SendScreenshotMessageUserFingerprint.methodOrNull?.addInstructions(0, "return-void")
        SendScreenshotMessageSecretFingerprint.methodOrNull?.addInstructions(0, "return-void")

        require(currentDispatcher != null || SendScreenshotMessageUserFingerprint.methodOrNull != null) {
            "No audited screenshot-notification dispatcher matched this APK"
        }
    }
}
