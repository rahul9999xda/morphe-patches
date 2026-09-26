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
 * Preserves Telegram's native Rich HTML clipboard path and disables only the
 * collapsed-state read used when applying blockquote formatting.
 *
 * DEX verification for Telegram/Web 12.10.5 and Plus 12.10.3.0 confirms:
 * - native paste checks ClipboardManager / ClipDescription for text/html
 * - reads ClipData.Item.getHtmlText()
 * - parses the HTML into a Spannable/SpannableStringBuilder
 * - URLSpan.getURL() is converted into Telegram URL entities
 *
 * Therefore this patch does NOT replace or bypass the native HTML paste path.
 * It only forces MessageEntity.collapsed to false in the shared
 * MessageObject.addEntitiesToText(..., I):Z method.
 */
private val richHtmlBlockquoteFingerprint = Fingerprint(
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
val telegramFixRichHtmlPastePatch = bytecodePatch(
    name = "Fix Rich HTML paste sending",
    description = "Preserves native Rich HTML paste and embedded links while disabling collapsed blockquote state.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )

    dependsOn(telegramSpoofDependency())

    execute {
        val matches = richHtmlBlockquoteFingerprint.instructionMatches

        if (matches.isEmpty()) {
            throw IllegalStateException(
                "Rich HTML target matched but MessageEntity.collapsed read was not found."
            )
        }

        val match = matches.first()
        val instruction = match.instruction as TwoRegisterInstruction

        richHtmlBlockquoteFingerprint.method.replaceInstruction(
            match.index,
            "const/4 v${instruction.registerA}, 0x0",
        )
    }
}
