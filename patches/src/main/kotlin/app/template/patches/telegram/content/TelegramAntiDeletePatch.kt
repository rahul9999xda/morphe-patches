package app.template.patches.telegram.content

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import com.android.tools.smali.dexlib2.Opcode
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency

/**
 * Telegram 12.10.x anti-delete, DEX-verified.
 *
 * Telegram has two deletion paths:
 *  1. MESSAGE_DELETED push -> MessagesController.deleteMessagesByPush()
 *  2. Normal online update -> MessagesController.processUpdateArray(), which
 *     eventually calls MessagesStorage.markMessagesAsDeleted() and marks the
 *     in-memory MessageObject.deleted field.
 *
 * The old patch only blocked the push path, so deleting a message while the
 * client is online still removed it through processUpdateArray().
 */
private val deleteMessagesByPushFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "deleteMessagesByPush",
    returnType = "V",
    parameters = listOf("J", "Ljava/util/ArrayList;", "J"),
)

/**
 * This is the storage deletion entry used by normal MESSAGE_DELETED updates.
 * The fourth parameter is useQueue:
 *   p4 == false -> remote/server deletion path
 *   p4 == true  -> normal local/user deletion path
 *
 * We return an empty dialog-id list only for the remote path, preserving the
 * normal local deletion behavior.
 */
private val markMessagesAsDeletedFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesStorage;",
    name = "markMessagesAsDeleted",
    returnType = "Ljava/util/ArrayList;",
    parameters = listOf("J", "Ljava/util/ArrayList;", "Z", "Z", "I", "I"),
)

/**
 * Both Telegram Web 12.10.4 and Plus 12.10.3.0 contain exactly one
 * MessagesController method which both:
 *  - writes MessageObject.deleted (Z), and
 *  - calls NotificationsController.removeDeletedMessagesFromNotifications().
 *
 * The method name is obfuscated/compiler-generated differently between builds
 * (Web: lambda$processUpdateArray$416, Plus: k5), so we intentionally anchor
 * on the two stable instructions instead of the generated method name.
 */
private val deletedMessageUiFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    returnType = "V",
    filters = listOf(
        fieldAccess(
            definingClass = "Lorg/telegram/messenger/MessageObject;",
            name = "deleted",
            type = "Z",
            opcode = Opcode.IPUT_BOOLEAN,
        ),
        methodCall(
            definingClass = "Lorg/telegram/messenger/NotificationsController;",
            name = "removeDeletedMessagesFromNotifications",
            returnType = "V",
        ),
    ),
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
        // 1. Push deletion path. This is the path used when Telegram receives
        // a MESSAGE_DELETED push notification while the app is not processing
        // the normal update flow.
        deleteMessagesByPushFingerprint.method.addInstructions(
            0,
            "return-void",
        )

        // 2. Normal online deletion path. Telegram calls the six-parameter
        // markMessagesAsDeleted(J, ArrayList, useQueue, deleteFiles, mode, topicId).
        // Remote deletion uses useQueue=false; local/user deletion uses true.
        // Preserve local deletion, but return an empty result for remote deletion.
        markMessagesAsDeletedFingerprint.method.addInstructions(
            0,
            """
                if-nez p4, :continue_original
                new-instance v0, Ljava/util/ArrayList;
                invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
                return-object v0
                :continue_original
                nop
            """,
        )

        // 3. The normal update path also marks the in-memory MessageObject as
        // deleted before/alongside storage deletion. Remove those two writes,
        // otherwise the message can still disappear from the current chat even
        // when the database deletion is blocked.
        deletedMessageUiFingerprint.matchAllOrNull()?.forEach { match ->
            match.instructionMatches
                .map { it.index }
                .reversed()
                .forEach { index ->
                    match.method.replaceInstruction(
                        index,
                        "nop\nnop",
                    )
                }
        }

        // 4. Keep notification cleanup disabled as well. This is retained from
        // the previous implementation and is harmless when the method exists.
        removeDeletedMessagesFromNotificationsFingerprint.methodOrNull?.addInstructions(
            0,
            "return-void",
        )
    }
}
