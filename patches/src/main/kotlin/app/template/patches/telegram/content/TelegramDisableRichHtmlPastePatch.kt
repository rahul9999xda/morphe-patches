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
 * Telegram 12.10.x can copy Rich HTML containing collapsible 
<details>
 * sections. Native HTML paste must remain enabled because it is also the
 * path that restores embedded URL spans correctly.
 *
 * The native paste parser turns collapsible rich blocks into Telegram
 * blockquote entities. When that entity is generated with collapsed=true,
 * the resulting pasted text can fail when sent to a normal chat. We keep the
 * blockquote entity, but force its `collapsed` flag to false. This preserves
 * the text, formatting and embedded links while removing only the
 * collapsible state that is problematic for pasted content.
 */
private val richHtmlEntityBuilderFingerprint = Fingerprint(
    name = "getEntities",
    returnType = "Ljava/util/ArrayList;",
    parameters = listOf("[Ljava/lang/CharSequence;", "Z", "Z"),
    filters = listOf(
        fieldAccess(
            definingClass = "Lorg/telegram/tgnet/TLRPC$TL_messageEntityBlockquote;",
            name = "collapsed",
            type = "Z",
        ),
    ),
)

@Suppress("unused")
val telegramFixRichHtmlPastePatch = bytecodePatch(
    name = "Fix Rich HTML paste sending",
    description = "Keeps Telegram's native Rich HTML paste and embedded links, but removes the collapsed state from pasted blockquotes so copied Rich HTML can be sent normally.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )

    dependsOn(telegramSpoofDependency())

    execute {
        richHtmlEntityBuilderFingerprint.instructionMatchesOrNull()?.forEach { match ->
            val instruction = match.instruction as? TwoRegisterInstruction ?: return@forEach
            val sourceRegister = instruction.registerA

            // Replace `iput-boolean vA, vB, ...->collapsed:Z` with
            // `const/4 vA, 0`. The newly-created MessageEntityBlockquote
            // therefore retains the default collapsed=false value.
            match.method.replaceInstruction(
                match.index,
                "const/4 v$sourceRegister, 0x0",
            )
        }
    }
}
