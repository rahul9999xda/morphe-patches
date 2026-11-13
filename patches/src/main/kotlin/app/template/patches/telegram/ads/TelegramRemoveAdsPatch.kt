package app.template.patches.telegram.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.PlusApplicationLoaderLoadAdsFingerprint
import app.template.patches.telegram.PlusApplicationLoaderLoadAdsLegacyFingerprint
import app.template.patches.telegram.TelegramR8MessageObjectIsSponsoredNormalFingerprint
import app.template.patches.telegram.TelegramR8MessageObjectIsSponsoredWebFingerprint
import app.template.patches.telegram.TelegramR8MessagesControllerIsSponsoredDisabledNormalFingerprint
import app.template.patches.telegram.TelegramR8MessagesControllerIsSponsoredDisabledWebFingerprint
import app.template.patches.telegram.TelegramPlusR8GetSponsoredMessagesFingerprint
import app.template.patches.telegram.TelegramPlusR8IsSponsoredDisabledFingerprint
import app.template.patches.telegram.signature.telegramSpoofDependency

private val messageObjectIsSponsoredFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isSponsored",
    returnType = "Z",
)

private val messagesControllerIsSponsoredDisabledFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "isSponsoredDisabled",
    returnType = "Z",
)

private val messagesControllerGetSponsoredMessagesFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "getSponsoredMessages",
    returnType = "Lorg/telegram/messenger/MessagesController\$SponsoredMessagesInfo;",
    parameters = listOf("J"),
)

private val videoAdsLoadFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/video/VideoAds;",
    name = "load",
    returnType = "V",
)

@Suppress("unused")
val telegramRemoveAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Removes sponsored messages and video ads. On Telegram Plus also blocks current and legacy Plus ad-loader entry points.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )
    dependsOn(telegramSpoofDependency())

    execute {
        val currentMessageObjectMethod =
            TelegramR8MessageObjectIsSponsoredNormalFingerprint.methodOrNull
                ?: TelegramR8MessageObjectIsSponsoredWebFingerprint.methodOrNull
        val sponsoredMethod = currentMessageObjectMethod
            ?: messageObjectIsSponsoredFingerprint.methodOrNull
        if (sponsoredMethod != null) {
            sponsoredMethod.addInstructions(0, """
                const/4 v0, 0x0
                return v0
            """)
        } else {
            // Plus Messenger has no standalone MessageObject.isSponsored() method.
            // Its controller owns the sponsored-message cache/request path instead.
            val plusSponsoredLoader = TelegramPlusR8GetSponsoredMessagesFingerprint.methodOrNull
            requireNotNull(plusSponsoredLoader) {
                "Missing sponsored-message classifier or audited Plus sponsored loader"
            }.addInstructions(0, """
                const/4 v0, 0x0
                return-object v0
            """)
        }

        val currentControllerMethod =
            TelegramR8MessagesControllerIsSponsoredDisabledNormalFingerprint.methodOrNull
                ?: TelegramR8MessagesControllerIsSponsoredDisabledWebFingerprint.methodOrNull
                ?: TelegramPlusR8IsSponsoredDisabledFingerprint.methodOrNull
        (currentControllerMethod ?: messagesControllerIsSponsoredDisabledFingerprint.methodOrNull)
            ?.addInstructions(0, """
                const/4 v0, 0x1
                return v0
            """)

        // Some current R8 builds inline the sponsored-message cache path into the
        // controller; retain the named cache hook only when the exact local method exists.
        messagesControllerGetSponsoredMessagesFingerprint.methodOrNull?.addInstructions(0, """
            const/4 v0, 0x0
            return-object v0
        """)

        videoAdsLoadFingerprint.methodOrNull?.addInstructions(0, "return-void")

        // Plus Messenger's current R8 controller also owns the sponsored-message request cache.
        // Keep the historical no-argument signature as a separate fallback for older Plus builds.
        listOf(
            PlusApplicationLoaderLoadAdsFingerprint.methodOrNull,
            PlusApplicationLoaderLoadAdsLegacyFingerprint.methodOrNull,
        ).filterNotNull().distinct().forEach { method ->
            method.addInstructions(0, "return-void")
        }
    }
}