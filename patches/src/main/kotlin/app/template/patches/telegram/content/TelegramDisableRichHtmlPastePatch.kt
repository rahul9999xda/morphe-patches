package app.template.patches.telegram.content

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

/**
 * Keeps Telegram's native Rich HTML paste path enabled.
 *
 * The previous "Use normal paste" implementation bypassed Telegram's native
 * HTML clipboard handling and caused embedded links to be lost.
 *
 * This patch leaves the native HTML paste path intact and only forces the
 * MessageEntity.collapsed field read in MessageObject.addEntitiesToText()
 * to false. This prevents pasted collapsible Rich HTML blocks from carrying
 * the collapsed state into the outgoing message while preserving URL/TextUrl
 * entities and normal formatting.
 */
private val richHtmlBlockquoteFingerprint = Fingerprint(
    name = "addEntitiesToText",
    returnType = "Z",
    parameters = listOf(
        "Ljava/lang/CharSequence;",
        "Ljava/util/ArrayList;",
        "Z",
        "Z",
        "Z",
        "Z",
        "I",
    ),
    filters = listOf(
        fieldAccess(
            definingClass = "Lorg/telegram/tgnet/TLRPC\$MessageEntity;",
            name = "collapsed",
            type = "Z",
        ),
    ),
)

@Suppress("unused")
val telegramFixRichHtmlPastePatch = bytecodePatch(
    name = "Fix Rich HTML paste sending",
    description = "Keeps native Rich HTML paste and embedded links while disabling the collapsed state read for pasted blockquote entities.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )

    dependsOn(telegramSpoofDependency())

    execute {
        val match = richHtmlBlockquoteFingerprint.instructionMatches.first()
        val index = match.index
        val instruction = match.instruction as TwoRegisterInstruction

        // Original:
        // iget-boolean vA, vB, Lorg/telegram/tgnet/TLRPC$MessageEntity;->collapsed:Z
        //
        // Keep the destination register and replace only the field read:
        // const/4 vA, 0x0
        richHtmlBlockquoteFingerprint.method.replaceInstruction(
            index,
            "const/4 v${instruction.registerA}, 0x0",
        )
    }
}
