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

private val deleteMessagesByPushFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "deleteMessagesByPush",
    returnType = "V",
    parameters = listOf("J", "Ljava/util/ArrayList;", "J"),
)

private val markMessagesAsDeletedFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesStorage;",
    name = "markMessagesAsDeleted",
    returnType = "Ljava/util/ArrayList;",
    parameters = listOf("J", "Ljava/util/ArrayList;", "Z", "Z", "I", "I"),
)

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

/*
 * Telegram 12.10.5 Web uses the obfuscated type Lz/f; for the first
 * parameter of removeDeletedMessagesFromNotifications().
 *
 * Do not use androidx.collection.LongSparseArray here. The clean 12.10.5
 * DEX resolves the method as:
 *
 * removeDeletedMessagesFromNotifications(Lz/f;, Z)V
 */
private val removeDeletedMessagesFromNotificationsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/NotificationsController;",
    name = "removeDeletedMessagesFromNotifications",
    returnType = "V",
    parameters = listOf(
        "Lz/f;",
        "Z",
    ),
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
        deleteMessagesByPushFingerprint.method.addInstructions(
            0,
            "return-void",
        )

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

        removeDeletedMessagesFromNotificationsFingerprint.methodOrNull?.addInstructions(
            0,
            "return-void",
        )
    }
}
