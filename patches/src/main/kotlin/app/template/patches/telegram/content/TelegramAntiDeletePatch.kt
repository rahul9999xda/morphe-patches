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
import app.template.patches.telegram.DeleteMessagesByPushFingerprint
import app.template.patches.telegram.MarkMessagesAsDeletedFingerprint2
import app.template.patches.telegram.NotificationsControllerRemoveDeletedMessagesFingerprint
import app.template.patches.telegram.NotificationsControllerRemoveDeletedMessagesPlusFingerprint
import app.template.patches.telegram.NotificationsControllerRemoveDeletedMessagesPlusLegacyFingerprint
import app.template.patches.telegram.MessagesControllerDeletePushMessagesPlusFingerprint
import app.template.patches.telegram.signature.telegramSpoofDependency
import app.template.patches.telegram.TelegramR8ProcessUpdateArrayNormalFingerprint
import app.template.patches.telegram.TelegramR8ProcessUpdateArrayWebFingerprint
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private val deletedMessageUiFilters = listOf(
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
)

// Current UI workers: Telegram/Web 13.1.1 share the lambda signature; Plus 13.1.1.0
// uses the static synthetic m0 worker with the androidx.collection/k parameters.
private val deletedMessageUiFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    returnType = "V",
    filters = deletedMessageUiFilters,
    custom = { method, _ ->
        val parameters = method.parameterTypes.map { it.toString() }
        (method.name == "lambda\$processUpdateArray\$419" && parameters == listOf(
            "Lorg/telegram/messenger/support/LongSparseIntArray;",
            "Lorg/telegram/messenger/support/LongSparseIntArray;",
            "Landroid/util/SparseIntArray;",
            "Lz/f;", "Lz/f;", "Lz/f;", "Lz/f;", "Lz/f;",
            "Lorg/telegram/messenger/support/LongSparseIntArray;",
        )) || (method.name == "m0" && parameters == listOf(
            "Lorg/telegram/messenger/MessagesController;",
            "Lorg/telegram/messenger/support/LongSparseIntArray;",
            "Lorg/telegram/messenger/support/LongSparseIntArray;",
            "Landroid/util/SparseIntArray;",
            "Landroidx/collection/k;", "Landroidx/collection/k;", "Landroidx/collection/k;",
            "Landroidx/collection/k;", "Landroidx/collection/k;",
            "Lorg/telegram/messenger/support/LongSparseIntArray;",
            "Ljava/util/ArrayList;",
        ))
    },
)

// Unconstrained historical matcher retained separately for older DEX layouts; it is not used
// by the current patch path because current target signatures above are explicitly audited.
private val deletedMessageUiLegacyFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    returnType = "V",
    filters = deletedMessageUiFilters,
)

/*
 * The first parameter is build-specific:
 *   Telegram / Telegram Web 13.1.1 -> Lz/f;
 *   Telegram Plus 13.1.1.0 build 22621 -> Landroidx/collection/k;
 *   older Plus builds may use Landroidx/collection/h;.
 *
 * Resolve the exact known variant at patch time rather than using a stale
 * LongSparseArray descriptor or silently skipping the notification hook.
 */
// Backward-compatible descriptor retained for earlier Plus releases.
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
        val currentR8UpdateMethod =
            TelegramR8ProcessUpdateArrayNormalFingerprint.methodOrNull
                ?: TelegramR8ProcessUpdateArrayWebFingerprint.methodOrNull

        if (currentR8UpdateMethod != null) {
            // In Telegram/Web 13.2.1, the old deleteMessagesByPush entry point is
            // folded into MessagesController.N3. Do not suppress the whole update
            // (which would stall pts); suppress only the two message-id list appends
            // for TL_updateDeleteMessages and TL_updateDeleteChannelMessages.
            // Scheduled, ephemeral and group-call delete updates remain untouched.
            val instructions = requireNotNull(currentR8UpdateMethod.implementation) {
                "R8 MessagesController update-array method has no implementation"
            }.instructions.toList()
            val deleteAppendIndexes = instructions.indices.filter { index ->
                if (index == 0) return@filter false
                val invoke = (instructions[index] as? ReferenceInstruction)?.reference as? MethodReference
                if (invoke?.definingClass != "Ljava/util/ArrayList;" ||
                    invoke.name != "addAll" ||
                    invoke.parameterTypes != listOf("Ljava/util/Collection;") ||
                    invoke.returnType != "Z"
                ) return@filter false
                val field = (instructions[index - 1] as? ReferenceInstruction)?.reference as? FieldReference
                field?.name == "messages" && field.type == "Ljava/util/ArrayList;" &&
                    (field.definingClass == "Lorg/telegram/tgnet/tl/TL_update\$TL_updateDeleteMessages;" ||
                        field.definingClass == "Lorg/telegram/tgnet/tl/TL_update\$TL_updateDeleteChannelMessages;")
            }
            require(deleteAppendIndexes.size == 2) {
                "Expected exactly two remote-delete list appends in R8 update processing, found ${deleteAppendIndexes.size}"
            }
            deleteAppendIndexes.sortedDescending().forEach { index ->
                currentR8UpdateMethod.replaceInstruction(index, "nop")
            }
        } else {
            // Historical named-class and Plus variants retain their original
            // independently audited delete worker and storage hook.
            val deleteMessagesByPushMethod =
                DeleteMessagesByPushFingerprint.methodOrNull
                    ?: MessagesControllerDeletePushMessagesPlusFingerprint.methodOrNull
            requireNotNull(deleteMessagesByPushMethod) {
                "Failed to match deleteMessagesByPush for legacy/Plus APK"
            }.addInstructions(0, "return-void")

            val markMessagesAsDeletedMethod =
                MarkMessagesAsDeletedFingerprint2.methodOrNull
            requireNotNull(markMessagesAsDeletedMethod) {
                "Failed to match markMessagesAsDeleted(J, ArrayList, Z, Z, I, I)"
            }.addInstructions(
                0,
                """
                    if-nez p4, :morphe_anti_delete_continue_original
                    new-instance v0, Ljava/util/ArrayList;
                    invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
                    return-object v0
                    :morphe_anti_delete_continue_original
                    nop
                """,
            )

            val deletedMessageUiMatches = deletedMessageUiFingerprint.matchAllOrNull().orEmpty()
            deletedMessageUiMatches.forEach { match ->
                match.instructionMatches.map { it.index }.distinct().sortedDescending().forEach { index ->
                    match.method.replaceInstruction(index, "nop\nnop")
                }
            }

            val removeDeletedMessagesMethod =
                NotificationsControllerRemoveDeletedMessagesFingerprint.methodOrNull
                    ?: NotificationsControllerRemoveDeletedMessagesPlusFingerprint.methodOrNull
                    ?: NotificationsControllerRemoveDeletedMessagesPlusLegacyFingerprint.methodOrNull
            removeDeletedMessagesMethod?.addInstructions(0, "return-void")
        }
    }
}
