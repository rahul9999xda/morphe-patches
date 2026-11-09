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
 * Preserve Telegram's native Rich HTML clipboard parser. Copy mode separately
 * retains MessageObject.messageText as a styled CharSequence and passes it to
 * Telegram's native text-send helper, which converts spans to message entities.
 *
 * Only normalise the collapsed flag on parsed blockquote entities. Do not
 * intercept onTextContextMenuItem(ACTION_PASTE): doing so bypasses getHtmlText()
 * and can discard embedded URL/TextUrl spans.
 */
private val richHtmlBlockquoteFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "addEntitiesToText",
    returnType = "Z",
    parameters = listOf(
        "Ljava/lang/CharSequence;",
        "Ljava/util/ArrayList;",
        "Z", "Z", "Z", "Z", "I",
    ),
    filters = listOf(
        fieldAccess(
            definingClass = "Lorg/telegram/tgnet/TLRPC\$MessageEntity;",
            name = "collapsed",
            type = "Z",
            opcode = Opcode.IGET_BOOLEAN,
        ),
    ),
)

@Suppress("unused")
val telegramDisableRichHtmlPastePatch = bytecodePatch(
    name = "Fix Rich HTML copy and paste",
    description = "Preserves Telegram's native Rich HTML clipboard and embedded links; normalises collapsible blockquote entities for safe sending.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )
    dependsOn(telegramSpoofDependency())

    execute {
        // `instructionMatches` contains instruction/index records, not method-bearing
        // fingerprint matches. Resolve the containing method once, then edit by index.
        val targetMethod = richHtmlBlockquoteFingerprint.methodOrNull
            ?: error("Rich HTML entity normalisation target not found: MessageObject.addEntitiesToText()")
        val matches = richHtmlBlockquoteFingerprint.instructionMatches
        require(matches.isNotEmpty()) {
            "Collapsed blockquote field access not found in MessageObject.addEntitiesToText()"
        }
        matches.distinctBy { it.index }.sortedByDescending { it.index }.forEach { match ->
            val instruction = match.instruction as? TwoRegisterInstruction
                ?: error("Unexpected collapsed-field instruction at index ${match.index} in addEntitiesToText()")
            targetMethod.replaceInstruction(
                match.index,
                "const/4 v${instruction.registerA}, 0x0",
            )
        }
    }
}
