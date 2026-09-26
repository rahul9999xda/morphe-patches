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
 * Fixes Telegram's native Rich HTML paste send path.
 *
 * The native HTML clipboard path must remain enabled because it is the path
 * that preserves embedded URL/TextUrl entities.
 *
 * The send-side problem is in MediaDataController.getEntities(). When a
 * Rich HTML blockquote span is converted into a
 * TLRPC$TL_messageEntityBlockquote, Telegram copies the span's collapsed
 * boolean into MessageEntity.collapsed. The resulting outgoing entity can
 * cause the pasted Rich HTML message to fail server-side, leaving a red
 * failed-to-send indicator.
 *
 * We therefore force ONLY this outgoing blockquote entity's collapsed value
 * to false. URL/TextUrl entities and all other HTML formatting remain on
 * Telegram's native implementation.
 *
 * Verified on:
 *  - Telegram 12.10.5
 *  - Telegram Web 12.10.5
 *  - Plus Messenger 12.10.3.0
 */
private val richHtmlOutgoingEntityFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MediaDataController;",
    name = "getEntities",
    returnType = "Ljava/util/ArrayList;",
    parameters = listOf(
        "[Ljava/lang/CharSequence;",
        "Z",
        "Z",
    ),
    filters = listOf(
        fieldAccess(
            definingClass = "Lorg/telegram/tgnet/TLRPC\$MessageEntity;",
            name = "collapsed",
            type = "Z",
            opcode = Opcode.IPUT_BOOLEAN,
        ),
    ),
)

@Suppress("unused")
val telegramFixRichHtmlPastePatch = bytecodePatch(
    name = "Fix Rich HTML paste sending",
    description = "Preserves native Rich HTML paste and embedded links while preventing pasted blockquote entities from carrying the collapsed flag into outgoing messages.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )

    dependsOn(telegramSpoofDependency())

    execute {
        val match = richHtmlOutgoingEntityFingerprint.instructionMatches.first()
        val instruction = match.instruction as TwoRegisterInstruction

        // Original:
        // iput-boolean vA, vB,
        //     Lorg/telegram/tgnet/TLRPC$MessageEntity;->collapsed:Z
        //
        // Keep the same value register and force it to false immediately
        // before the entity is added to the outgoing entity list.
        richHtmlOutgoingEntityFingerprint.method.replaceInstruction(
            match.index,
            "const/4 v${instruction.registerA}, 0x0",
        )
    }
}
