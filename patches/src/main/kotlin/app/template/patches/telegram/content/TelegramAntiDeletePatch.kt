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
import app.template.patches.telegram.MessagesControllerProcessUpdateArrayMarkDeletedFingerprint
import app.template.patches.telegram.MessagesControllerDeletePushMessagesPlusFingerprint
import app.template.patches.telegram.signature.telegramSpoofDependency

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
    description = "Prevents remote deletion updates from removing local messages while preserving user-initiated deletion.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )
    dependsOn(telegramSpoofDependency())

    execute {
        // Current Plus 13.1.1.0 has no locally declared deleteMessagesByPush(J, ArrayList, J)V.
        // Use the verified static synthetic m3 worker for the current Plus descriptor.
        val deleteMessagesByPushMethod =
            DeleteMessagesByPushFingerprint.methodOrNull
                ?: MessagesControllerDeletePushMessagesPlusFingerprint.methodOrNull
                ?: error("Failed to match deleteMessagesByPush for Telegram/Web/Plus")

        deleteMessagesByPushMethod.addInstructions(0, "return-void")

        // Do not globally intercept MessagesStorage.markMessagesAsDeleted: it is also
        // used by local/user-initiated deletion flows. The incoming server-update call
        // below is the narrow hook for remote deletion events.


        // processUpdateArray uses the asynchronous markMessagesAsDeleted path (p4 = true)
        // for incoming server deletion updates. Do not globally block that boolean branch:
        // user-initiated deletes also use it. Instead, remove only the audited call site in
        // processUpdateArray so remote delete updates never reach local storage deletion.
        val incomingDeleteUpdateMatches =
            MessagesControllerProcessUpdateArrayMarkDeletedFingerprint.matchAllOrNull().orEmpty()
        require(incomingDeleteUpdateMatches.size == 1) {
            "Expected exactly one incoming-delete update path, found ${incomingDeleteUpdateMatches.size}"
        }
        incomingDeleteUpdateMatches.forEach { match ->
            val callIndices = match.instructionMatches.map { it.index }.distinct().reversed()
            require(callIndices.size == 1) {
                "Expected exactly one markMessagesAsDeleted call in processUpdateArray, found ${callIndices.size}"
            }
            callIndices.forEach { index ->
                match.method.replaceInstruction(index, "nop")
            }
        }

        val deletedMessageUiMatches = deletedMessageUiFingerprint.matchAllOrNull().orEmpty()
        require(deletedMessageUiMatches.size == 1) {
            "Expected exactly one deleted-message UI hook, found ${deletedMessageUiMatches.size}"
        }
        deletedMessageUiMatches.forEach { match ->
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

        // Do not globally suppress removeDeletedMessagesFromNotifications: doing so
        // can leave notifications for messages the user deliberately deleted. The
        // incoming-update UI worker above is the narrowly scoped remote-delete hook.
    }
}
