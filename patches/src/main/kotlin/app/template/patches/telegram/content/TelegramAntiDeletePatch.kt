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
import app.template.patches.telegram.TelegramPlusR8ProcessUpdateArrayFingerprint
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
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
    description = "Ignores incoming remote deletion updates while leaving the local delete action path intact.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )
    dependsOn(telegramSpoofDependency())

    execute {
        val currentR8UpdateMethods = listOf(
            TelegramR8ProcessUpdateArrayNormalFingerprint,
            TelegramR8ProcessUpdateArrayWebFingerprint,
            TelegramPlusR8ProcessUpdateArrayFingerprint,
        ).mapNotNull { it.methodOrNull }
        require(currentR8UpdateMethods.size <= 1) {
            "Multiple current R8 update-array dispatchers matched; refusing an ambiguous Anti-delete patch"
        }
        val currentR8UpdateMethod = currentR8UpdateMethods.singleOrNull()

        if (currentR8UpdateMethod != null) {
            // In Telegram/Web 13.2.2 (Android build 73031 / Web build 73039), and Plus 13.2.1.0 build 22678,
            // the old deleteMessagesByPush entry point is folded into the independently fingerprinted update-array
            // dispatcher (N3 for Telegram/Web; Bh for Plus). Do not suppress the whole update
            // (which would stall pts); suppress only the two message-id list appends
            // for TL_updateDeleteMessages and TL_updateDeleteChannelMessages.
            // Scheduled, ephemeral and group-call delete updates remain untouched.
            // Locally initiated deletion uses separate callbacks: Android/Web
            // MessagesController wp/sp.b0 -> MessagesStorage is/ds.P0; Plus
            // MessagesController ko0.ob -> MessagesStorage vz0.K7. This hook only
            // removes remote update IDs from N3/Bh and does not patch those callbacks.
            // A deletion initiated on another device for an
            // outgoing message is not distinguishable from a peer deletion in the
            // TL_updateDelete* payload (IDs only), so author-aware filtering for that
            // cross-device case is deliberately not claimed.
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
            val expectedDeleteFields = setOf(
                "Lorg/telegram/tgnet/tl/TL_update\$TL_updateDeleteMessages;",
                "Lorg/telegram/tgnet/tl/TL_update\$TL_updateDeleteChannelMessages;",
            )
            val matchedDeleteFields = deleteAppendIndexes.mapNotNull { index ->
                ((instructions[index - 1] as? ReferenceInstruction)?.reference as? FieldReference)
                    ?.definingClass
            }
            require(deleteAppendIndexes.size == 2 && matchedDeleteFields.toSet() == expectedDeleteFields &&
                matchedDeleteFields.size == expectedDeleteFields.size) {
                "Expected one normal and one channel remote-delete ID append in R8 update processing; " +
                    "found ${deleteAppendIndexes.size} appends for ${matchedDeleteFields.joinToString()}"
            }
            // Removing only addAll leaves an empty chat-key entry in the delete map.
            // Telegram subsequently dispatches that entry to NotificationCenter and the
            // notification controller; some consumers interpret an empty ID list as a
            // chat-wide deletion refresh. Remove the key itself instead, so no remote
            // deletion event is emitted downstream at all. The nearest preceding map
            // lookup is the exact map/key pair used to obtain the ArrayList receiver.
            // Telegram/Web use La0/h (f/l, m/n); Plus uses Lu/k (f/l, n/q).
            val expectedMapClass = when (currentR8UpdateMethod.definingClass) {
                "Lorg/telegram/messenger/wp;", "Lorg/telegram/messenger/sp;" -> "La0/h;"
                "Lorg/telegram/messenger/ko0;" -> "Lu/k;"
                else -> error("Unrecognized current R8 delete dispatcher ${currentR8UpdateMethod.definingClass}")
            }
            var mapRemoveMethodFound = false
            classDefForEach { classDef ->
                if (classDef.type == expectedMapClass) {
                    mapRemoveMethodFound = classDef.methods.any { method ->
                        method.name == "l" && method.parameterTypes.map { it.toString() } == listOf("J") &&
                            method.returnType == "V"
                    }
                }
            }
            require(mapRemoveMethodFound) {
                "Expected verified $expectedMapClass->l(J)V map-key removal method for Anti-delete"
            }
            fun invokeRegisters(instruction: Instruction): List<Int>? = when (instruction) {
                is FiveRegisterInstruction -> when (instruction.registerCount) {
                    1 -> listOf(instruction.registerC)
                    2 -> listOf(instruction.registerC, instruction.registerD)
                    3 -> listOf(instruction.registerC, instruction.registerD, instruction.registerE)
                    4 -> listOf(instruction.registerC, instruction.registerD, instruction.registerE, instruction.registerF)
                    5 -> listOf(instruction.registerC, instruction.registerD, instruction.registerE, instruction.registerF, instruction.registerG)
                    else -> null
                }
                is RegisterRangeInstruction -> (0 until instruction.registerCount).map { instruction.startRegister + it }
                else -> null
            }
            deleteAppendIndexes.sortedDescending().forEach { index ->
                val mapLookupIndex = (index - 1 downTo 0).firstOrNull { candidate ->
                    val reference = (instructions[candidate] as? ReferenceInstruction)?.reference as? MethodReference
                    reference?.definingClass == expectedMapClass && reference.name == "f" &&
                        reference.parameterTypes == listOf("J") && reference.returnType == "Ljava/lang/Object;"
                } ?: error("Could not resolve the delete-map key preceding addAll at instruction $index")
                val mapReference = (instructions[mapLookupIndex] as ReferenceInstruction).reference as MethodReference
                val registers = invokeRegisters(instructions[mapLookupIndex])
                    ?: error("Could not read delete-map lookup registers at instruction $mapLookupIndex")
                require(registers.size == 3) {
                    "Expected map + wide-key registers for delete-map lookup; got $registers"
                }
                // Prove that this exact lookup supplies the ArrayList receiver being
                // appended to. Merely finding the nearest f(J) call is insufficient in
                // R8 methods containing several maps: a wrong match could remove an
                // unrelated entry and leave the deletion IDs untouched.
                val lookupResult = instructions.getOrNull(mapLookupIndex + 1)
                require(lookupResult?.opcode == Opcode.MOVE_RESULT_OBJECT &&
                    (lookupResult as? OneRegisterInstruction)?.registerA ==
                    (invokeRegisters(instructions[index])?.firstOrNull())) {
                    "Delete-map lookup at $mapLookupIndex does not feed the delete-ID ArrayList at $index"
                }
                require(instructions.getOrNull(index + 1)?.opcode !in setOf(
                    Opcode.MOVE_RESULT, Opcode.MOVE_RESULT_OBJECT, Opcode.MOVE_RESULT_WIDE,
                )) {
                    "Remote-delete addAll result is consumed; refusing an unsafe replacement"
                }
                currentR8UpdateMethod.replaceInstruction(
                    index,
                    "invoke-virtual {${registers.joinToString(", ") { "v$it" }}}, ${mapReference.definingClass}->l(J)V",
                )
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
