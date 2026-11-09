package app.template.patches.telegram.content

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency
import app.template.patches.telegram.TelegramR8IsChatNoForwardsNormalFingerprint
import app.template.patches.telegram.TelegramR8IsChatNoForwardsWebFingerprint
import app.template.patches.telegram.TelegramR8IsUserNoForwardsUserFullNormalFingerprint
import app.template.patches.telegram.TelegramR8IsUserNoForwardsUserFullWebFingerprint
import app.template.patches.telegram.TelegramR8IsPeerNoForwardsNormalFingerprint
import app.template.patches.telegram.TelegramR8IsPeerNoForwardsWebFingerprint
import app.template.patches.telegram.TelegramR8CanForwardMessageNormalFingerprint
import app.template.patches.telegram.TelegramR8CanForwardMessageWebFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

private val isChatNoForwardsLongFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "isChatNoForwards",
    returnType = "Z",
    parameters = listOf("J"),
)

private val isChatNoForwardsChatFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "isChatNoForwards",
    returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$Chat;"),
)

private val isUserNoForwardsLongFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "isUserNoForwards",
    returnType = "Z",
    parameters = listOf("J"),
)

private val isUserNoForwardsUserFullFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "isUserNoForwards",
    returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$UserFull;"),
)

private val isPeerNoForwardsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "isPeerNoForwards",
    returnType = "Z",
    parameters = listOf("J"),
)

private val canForwardMessageFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "canForwardMessage",
    returnType = "Z",
    parameters = emptyList(),
)

private val messageNoForwardsReadFingerprint = Fingerprint(
    filters = listOf(
        fieldAccess(
            definingClass = "Lorg/telegram/tgnet/TLRPC\$Message;",
            name = "noforwards",
            type = "Z",
            opcode = Opcode.IGET_BOOLEAN,
        ),
    ),
)

private val chatNoForwardsReadFingerprint = Fingerprint(
    filters = listOf(
        fieldAccess(
            definingClass = "Lorg/telegram/tgnet/TLRPC\$Chat;",
            name = "noforwards",
            type = "Z",
            opcode = Opcode.IGET_BOOLEAN,
        ),
    ),
)

@Suppress("unused")
val telegramBypassContentRestrictionsPatch = bytecodePatch(
    name = "Bypass content restrictions",
    description = "Allows saving and forwarding content from restricted channels, chats, and users.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )
    dependsOn(telegramSpoofDependency())

    execute {
        val currentR8Methods = listOf(
            TelegramR8IsChatNoForwardsNormalFingerprint,
            TelegramR8IsChatNoForwardsWebFingerprint,
            TelegramR8IsUserNoForwardsUserFullNormalFingerprint,
            TelegramR8IsUserNoForwardsUserFullWebFingerprint,
            TelegramR8IsPeerNoForwardsNormalFingerprint,
            TelegramR8IsPeerNoForwardsWebFingerprint,
        ).mapNotNull { it.methodOrNull }
        val namedMethods = listOf(
            isChatNoForwardsLongFingerprint,
            isChatNoForwardsChatFingerprint,
            isUserNoForwardsLongFingerprint,
            isUserNoForwardsUserFullFingerprint,
            isPeerNoForwardsFingerprint,
        ).mapNotNull { it.methodOrNull }
        val noForwardMethods = (currentR8Methods + namedMethods).distinct()
        require(noForwardMethods.isNotEmpty()) { "No no-forwards controller methods matched" }
        noForwardMethods.forEach { method ->
            method.addInstructions(0, """
                const/4 v0, 0x0
                return v0
            """)
        }

        val currentCanForward =
            TelegramR8CanForwardMessageNormalFingerprint.methodOrNull
                ?: TelegramR8CanForwardMessageWebFingerprint.methodOrNull
        (currentCanForward ?: canForwardMessageFingerprint.methodOrNull)?.addInstructions(0, """
            const/4 v0, 0x1
            return v0
        """)

        listOf(messageNoForwardsReadFingerprint, chatNoForwardsReadFingerprint).forEach { fingerprint ->
            val matches = fingerprint.matchAllOrNull().orEmpty()
            require(matches.isNotEmpty()) {
                "No no-forwards field-read sites matched ${fingerprint}"
            }
            matches.forEach { match ->
                match.method.apply {
                    match.instructionMatches.map { it.index }.reversed().forEach { index ->
                        val register = getInstruction<TwoRegisterInstruction>(index).registerA
                        // IGET_BOOLEAN uses the 22c format: registerA is encoded in
                        // four bits, so this destination is guaranteed to be v0-v15.
                        val replacement = "const/4 v$register, 0x0"
                        replaceInstruction(index, replacement)
                    }
                }
            }
        }
    }
}