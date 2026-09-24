package app.template.patches.telegram.content

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency

/**
 * Telegram 12.10.4 / Telegram Plus 12.10.3.0 / Telegram Web 12.10.4
 * DEX-verified stable deletion entry points.
 *
 * We deliberately do not patch markMessagesAsDeleted(): its two boolean
 * parameters are used by both local and remote deletion paths. Blocking it
 * globally can break the user's own delete operation. The server-push entry
 * point is the safer boundary.
 */
private val deleteMessagesByPushFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "deleteMessagesByPush",
    returnType = "V",
    parameters = listOf("J", "Ljava/util/ArrayList;", "J"),
)

private val removeDeletedMessagesFromNotificationsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/NotificationsController;",
    name = "removeDeletedMessagesFromNotifications",
    returnType = "V",
    custom = { method, _ -> method.parameterTypes.size == 2 },
)

@Suppress("unused")
val telegramAntiDeletePatch = bytecodePatch(
    name = "Anti-delete messages",
    description = "Prevents messages deleted by other users from being removed locally.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )
    dependsOn(telegramSpoofDependency())

    execute {
        deleteMessagesByPushFingerprint.method.addInstructions(0, "return-void")
        removeDeletedMessagesFromNotificationsFingerprint.method.addInstructions(0, "return-void")
    }
}