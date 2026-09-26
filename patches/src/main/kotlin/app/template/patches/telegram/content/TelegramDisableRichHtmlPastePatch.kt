package app.template.patches.telegram.content

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction

/**
 * Unified Rich HTML paste fix for Telegram 12.10.5, Telegram Web 12.10.5
 * and Telegram Plus 12.10.3.0.
 *
 * IMPORTANT:
 * Do not bypass onTextContextMenuItem(ACTION_PASTE). Telegram's native HTML
 * clipboard parser is the path that preserves embedded URL/TextUrl spans.
 *
 * The stable cross-build problem is the collapsed flag read from
 * TLRPC.MessageEntity while MessageObject.addEntitiesToText(...) normalises
 * pasted entities. Force only that read to false. This keeps Telegram's
 * native Rich HTML parser and embedded links intact, while removing the
 * collapsed state from pasted blockquote/details entities.
 *
 * DEX-verified target in all three supplied APKs:
 * MessageObject.addEntitiesToText(
 *     CharSequence,
 *     ArrayList,
 *     Z, Z, Z, Z, I
 * ): Z
 *
 * The target method contains an IGET_BOOLEAN read of:
 * TLRPC$MessageEntity.collapsed:Z
 */
private val richHtmlCollapsedFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
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
            definingClass = "Lorg/telegram/tgnet/TLRPC${'$'}MessageEntity;",
            name = "collapsed",
            type = "Z",
            opcode = Opcode.IGET_BOOLEAN,
        ),
    ),
)

@Suppress("unused")
val telegramDisableRichHtmlPastePatch = bytecodePatch(
    name = "Fix Rich HTML paste",
    description = "Keeps native Rich HTML paste and embedded links, while flattening pasted collapsible blockquote state.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )

    dependsOn(telegramSpoofDependency())

    execute {
        val match = richHtmlCollapsedFingerprint.instructionMatches.first()
        val instruction = match.instruction as TwoRegisterInstruction

        richHtmlCollapsedFingerprint.method.replaceInstruction(
            match.index,
            "const/4 v${instruction.registerA}, 0x0",
        )
    }
}
