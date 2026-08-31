package app.template.patches.telegram.content

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.SendMessagesHelperRichForwardFingerprint

/**
 * Removes the forwarded channel attribution for Rich Messages.
 *
 * Telegram's normal forwarding implementation already maps forwardFromMyName
 * to TL_messages_forwardMessages.drop_author. This patch only changes that
 * parameter when the first forwarded MessageObject contains rich_message.
 * The original message is not rebuilt or converted to plain text.
 */
@Suppress("unused")
val telegramHideRichMessageForwardedSenderPatch = bytecodePatch(
    name = "Hide sender name for Rich Message forwards",
    description = "Removes forwarded channel attribution from Rich Messages while preserving the original message.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
    )

    execute {
        SendMessagesHelperRichForwardFingerprint.method.addInstructions(
            0,
            """
                if-eqz p0, :rich_forward_done
                invoke-virtual {p0}, Ljava/util/ArrayList;->isEmpty()Z
                move-result v0
                if-nez v0, :rich_forward_done
                const/4 v0, 0x0
                invoke-virtual {p0, v0}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;
                move-result-object v0
                check-cast v0, Lorg/telegram/messenger/MessageObject;
                iget-object v0, v0, Lorg/telegram/messenger/MessageObject;->messageOwner:Lorg/telegram/tgnet/TLRPC${'$'}Message;
                if-eqz v0, :rich_forward_done
                iget-object v0, v0, Lorg/telegram/tgnet/TLRPC${'$'}Message;->rich_message:Lorg/telegram/tgnet/tl/TL_iv${'$'}RichMessage;
                if-eqz v0, :rich_forward_done
                const/4 p4, 0x1
                :rich_forward_done
            """,
        )
    }
}