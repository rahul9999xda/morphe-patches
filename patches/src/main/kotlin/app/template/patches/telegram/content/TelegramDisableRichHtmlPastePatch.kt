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
 * The old "Use normal paste" implementation modified onTextContextMenuItem()
 * and attempted to delegate ACTION_PASTE to the superclass. That approach is
 * unnecessary and can fail during inline-smali compilation because the
 * generated invoke-super descriptor depends on the obfuscated matched class.
 *
 * The native HTML clipboard path must remain intact because it preserves
 * embedded URL/TextUrl entities.
 *
 * The remaining Rich HTML problem is the collapsed state carried by
 * MessageEntityBlockquote entities. MessageObject.addEntitiesToText() reads
 * MessageEntity.collapsed while normalising the pasted rich content.
 *
 * We replace only that boolean read with false. The original HTML parser,
 * URL/entity extraction and paste insertion code remain untouched.
 *
 * Verified against:
 *   Telegram 12.10.5 / 71052
 *   Telegram Web 12.10.5 / 71059
 *   Telegram Plus 12.10.3.0
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
            definingClass = "Lorg/telegram/tgnet/TLRPC\$MessageEntity;",
            name = "collapsed",
            type = "Z",
        ),
    ),
)

@Suppress("unused")
val telegramDisableRichHtmlPastePatch = bytecodePatch(
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
        richHtmlBlockquoteFingerprint.matchAllOrNull()?.forEach { match ->
            val instruction = match.instruction as? TwoRegisterInstruction
                ?: return@forEach

            // Original:
            // iget-boolean vA, vB, TLRPC$MessageEntity->collapsed:Z
            //
            // Replacement:
            // const/4 vA, 0x0
            //
            // The destination register is preserved, so all following
            // control flow and entity processing remain unchanged.
            match.method.replaceInstruction(
                match.index,
                "const/4 v${instruction.registerA}, 0x0",
            )
        }
    }
}
