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
import app.template.patches.telegram.TelegramPlusSendScreenshotNotificationFingerprint

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
        val currentAndroid = TelegramR8SendScreenshotNotificationNormalFingerprint.methodOrNull
        val currentWeb = TelegramR8SendScreenshotNotificationWebFingerprint.methodOrNull
        val plusCurrent = TelegramPlusSendScreenshotNotificationFingerprint.methodOrNull

        require(listOf(currentAndroid, currentWeb, plusCurrent).count { it != null } <= 1) {
            "Ambiguous screenshot-notification dispatcher: more than one target profile matched"
        }

        when {
            currentAndroid != null -> currentAndroid.addInstructions(0, """
                const/4 v0, 0x0
                return v0
            """)
            currentWeb != null -> currentWeb.addInstructions(0, """
                const/4 v0, 0x0
                return v0
            """)
            plusCurrent != null -> plusCurrent.addInstructions(0, "return-void")
            else -> {
                // Older named builds retain the two historical notification helpers.
                val legacy = listOfNotNull(
                    SendScreenshotMessageUserFingerprint.methodOrNull,
                    SendScreenshotMessageSecretFingerprint.methodOrNull,
                )
                require(legacy.isNotEmpty()) {
                    "No audited screenshot-notification dispatcher matched this APK"
                }
                legacy.forEach { it.addInstructions(0, "return-void") }
            }
        }
    }
}
