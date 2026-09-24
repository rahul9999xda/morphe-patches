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
 * Telegram 12.10.4 preserves embedded URL spans only when the native
 * getHtmlText() -> Html.fromHtml() paste path is allowed to run. The old
 * "Use normal paste" patch bypassed that path and therefore lost embedded
 * links.
 *
 * The remaining Rich HTML problem is the collapsed flag carried by a
 * MessageEntityBlockquote generated from Telegram's 
<blockquote>/
    <details>
 * HTML. MessageObject.addEntitiesToText() reads MessageEntity.collapsed while
 * normalising the pasted text. Force that read to false so pasted rich
 * blocks remain ordinary blockquotes instead of carrying the collapsed state.
 *
 * This changes only the boolean value read from MessageEntity.collapsed; all
 * normal URL/TextUrl and formatting handling remains Telegram's original code.
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
    description = "Keeps native Rich HTML paste and embedded links, while disabling the collapsed state read for pasted blockquote entities.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )

    dependsOn(telegramSpoofDependency())

    execute {
        richHtmlBlockquoteFingerprint.matchAllOrNull()?.forEach { match ->
            val instruction = match.instruction as? TwoRegisterInstruction
                ?: return@forEach

            // The matched instruction is the boolean read:
            // iget-boolean vA, vB, Lorg/telegram/tgnet/TLRPC$MessageEntity;->collapsed:Z
            // Replace only the destination value with false. The original
            // receiver register is not modified.
            match.method.replaceInstruction(
                match.index,
                "const/4 v${instruction.registerA}, 0x0",
            )
        }
    }
}
