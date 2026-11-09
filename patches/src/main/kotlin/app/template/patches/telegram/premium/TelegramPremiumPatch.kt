package app.template.patches.telegram.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.MessagesControllerIsPremiumUserFingerprint
import app.template.patches.telegram.MessagesControllerIsTranslationsAutoEnabledFingerprint
import app.template.patches.telegram.MessagesControllerIsTranslationsManualEnabledFingerprint
import app.template.patches.telegram.SharedConfigGetDevicePerformanceClassFingerprint
import app.template.patches.telegram.UserConfigGetMaxAccountCountFingerprint
import app.template.patches.telegram.UserConfigHasPremiumOnAccountsFingerprint
import app.template.patches.telegram.UserConfigIsPremiumFingerprint
import app.template.patches.telegram.TelegramR8UserConfigIsPremiumFingerprint
import app.template.patches.telegram.TelegramR8UserConfigHasPremiumOnAccountsFingerprint
import app.template.patches.telegram.TelegramR8UserConfigGetMaxAccountCountFingerprint
import app.template.patches.telegram.TelegramR8MessagesControllerIsPremiumUserNormalFingerprint
import app.template.patches.telegram.TelegramR8MessagesControllerIsPremiumUserWebFingerprint
import app.template.patches.telegram.TelegramR8TranslationsManualNormalFingerprint
import app.template.patches.telegram.TelegramR8TranslationsManualWebFingerprint
import app.template.patches.telegram.TelegramR8TranslationsAutoNormalFingerprint
import app.template.patches.telegram.TelegramR8TranslationsAutoWebFingerprint
import app.template.patches.telegram.TelegramR8DevicePerformanceClassNormalFingerprint
import app.template.patches.telegram.TelegramR8DevicePerformanceClassWebFingerprint
import app.template.patches.telegram.signature.telegramSpoofDependency

@Suppress("unused")
val telegramPremiumPatch = bytecodePatch(
    name = "Unlock Premium",
    description = "Unlocks Telegram Premium features for the current account.",
) {
    // DEX-verified against Telegram 13.1.1 build 72371, Telegram Web 13.1.1
    // build 72379, and Plus Messenger 13.1.1.0 build 22621. Older
    // target descriptors remain separately declared. The core premium
    // and translation-gate methods below use identical descriptors in all three.
    // Existing legacy targets are unchanged; only builds whose exact fingerprints resolve
    // are eligible, so this does not broaden support by guessing renamed methods.
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
    )

    dependsOn(telegramSpoofDependency())

    execute {
        val userConfigPremium =
            TelegramR8UserConfigIsPremiumFingerprint.methodOrNull ?: UserConfigIsPremiumFingerprint.methodOrNull
        requireNotNull(userConfigPremium) { "Missing UserConfig.isPremium()Z" }.addInstructions(
            0, """
                const/4 v0, 0x1
                return v0
            """,
        )

        val premiumUserMethod =
            TelegramR8MessagesControllerIsPremiumUserNormalFingerprint.methodOrNull
                ?: TelegramR8MessagesControllerIsPremiumUserWebFingerprint.methodOrNull
                ?: MessagesControllerIsPremiumUserFingerprint.methodOrNull
        premiumUserMethod?.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """)

        val translationsManual =
            TelegramR8TranslationsManualNormalFingerprint.methodOrNull
                ?: TelegramR8TranslationsManualWebFingerprint.methodOrNull
                ?: MessagesControllerIsTranslationsManualEnabledFingerprint.methodOrNull
        translationsManual?.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """)

        val translationsAuto =
            TelegramR8TranslationsAutoNormalFingerprint.methodOrNull
                ?: TelegramR8TranslationsAutoWebFingerprint.methodOrNull
                ?: MessagesControllerIsTranslationsAutoEnabledFingerprint.methodOrNull
        translationsAuto?.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """)

        val hasPremiumOnAccounts =
            TelegramR8UserConfigHasPremiumOnAccountsFingerprint.methodOrNull
                ?: UserConfigHasPremiumOnAccountsFingerprint.methodOrNull
        hasPremiumOnAccounts?.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """)

        val maxAccountCount =
            TelegramR8UserConfigGetMaxAccountCountFingerprint.methodOrNull
                ?: UserConfigGetMaxAccountCountFingerprint.methodOrNull
        maxAccountCount?.addInstructions(0, """
            const/16 v0, 0x3E7
            return v0
        """)

        val devicePerformanceClass =
            TelegramR8DevicePerformanceClassNormalFingerprint.methodOrNull
                ?: TelegramR8DevicePerformanceClassWebFingerprint.methodOrNull
                ?: SharedConfigGetDevicePerformanceClassFingerprint.methodOrNull
        devicePerformanceClass?.addInstructions(0, """
            const/4 v0, 0x2
            return v0
        """)
    }
}
