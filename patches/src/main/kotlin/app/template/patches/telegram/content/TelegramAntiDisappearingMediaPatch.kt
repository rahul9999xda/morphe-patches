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
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

private val isSecretMediaInstanceFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isSecretMedia",
    returnType = "Z",
    parameters = emptyList(),
)

private val isSecretMediaStaticFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isSecretMedia",
    returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$Message;"),
)

private val isSecretPhotoOrVideoFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isSecretPhotoOrVideo",
    returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$Message;"),
)

private val shouldEncryptPhotoOrVideoFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "shouldEncryptPhotoOrVideo",
    returnType = "Z",
    parameters = listOf("I", "Lorg/telegram/tgnet/TLRPC\$Message;"),
)

private val isVoiceOnceFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isVoiceOnce",
    returnType = "Z",
    parameters = emptyList(),
)

private val isRoundOnceFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isRoundOnce",
    returnType = "Z",
    parameters = emptyList(),
)

private val needDrawBluredPreviewFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "needDrawBluredPreview",
    returnType = "Z",
    parameters = emptyList(),
)

/**
 * Telegram/Web 12.10.4 renamed the old closePhoto() method to e(ZZ)Z.
 * Telegram Plus 12.10.3.0 uses o0(ZZ)Z.
 *
 * We identify the method semantically by its Runnable field read rather than
 * relying on the old method name or the obfuscated field name.
 */
private val secretMediaViewerCloseFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/SecretMediaViewer;",
    returnType = "Z",
    parameters = listOf("Z", "Z"),
    filters = listOf(
        fieldAccess(
            definingClass = "Lorg/telegram/ui/SecretMediaViewer;",
            type = "Ljava/lang/Runnable;",
            opcode = Opcode.IGET_OBJECT,
        ),
    ),
)

@Suppress("unused")
val telegramAntiDisappearingMediaPatch = bytecodePatch(
    name = "Anti-disappearing media",
    description = "Keeps view-once photos, videos and voice messages viewable indefinitely.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )

    dependsOn(telegramSpoofDependency())

    execute {
        listOf(
            isSecretMediaInstanceFingerprint,
            isSecretMediaStaticFingerprint,
            isSecretPhotoOrVideoFingerprint,
            shouldEncryptPhotoOrVideoFingerprint,
            isVoiceOnceFingerprint,
            isRoundOnceFingerprint,
            needDrawBluredPreviewFingerprint,
        ).forEach { fingerprint ->
            fingerprint.method.addInstructions(
                0,
                """
                const/4 v0, 0x0
                return v0
                """,
            )
        }

        secretMediaViewerCloseFingerprint.instructionMatches
            .map { it.index }
            .reversed()
            .forEach { index ->
                val register = secretMediaViewerCloseFingerprint.method
                    .getInstruction<OneRegisterInstruction>(index)
                    .registerA

                secretMediaViewerCloseFingerprint.method
                    .replaceInstruction(
                        index,
                        "const/4 v$register, 0x0",
                    )
            }
    }
}
