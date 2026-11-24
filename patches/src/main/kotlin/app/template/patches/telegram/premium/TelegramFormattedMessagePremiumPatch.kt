package app.template.patches.telegram.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.Telegram1331FormattedForwardCallbackNormalFingerprint
import app.template.patches.telegram.Telegram1331FormattedForwardCallbackWebFingerprint

/**
 * Telegram's server rejects drop_author for non-Premium accounts. When that exact
 * response is returned, this patch falls back to sending the original text as a
 * new message. It is deliberately limited to text-only messages with formatting
 * entities; media, plain text, other errors, and normal forwards retain Telegram's
 * native error/forwarding behavior.
 *
 * The existing MessageObject CharSequence contains Telegram's formatting spans.
 * SendMessagesHelper.a0() parses those spans back into Telegram message entities.
 */
@Suppress("unused")
val telegramFormattedMessagePremiumPatch = bytecodePatch(
    name = "Copy formatted messages without sender",
    description = "On PREMIUM_ACCOUNT_REQUIRED for drop-author forwarding, sends eligible formatted text as new messages instead of forwarding.",
) {
    compatibleWith(TELEGRAM_COMPATIBILITY, TELEGRAM_WEB_COMPATIBILITY)

    execute {
        val androidMethod = Telegram1331FormattedForwardCallbackNormalFingerprint.methodOrNull
        val webMethod = Telegram1331FormattedForwardCallbackWebFingerprint.methodOrNull

        require((androidMethod != null) xor (webMethod != null)) {
            "Expected exactly one Telegram 13.3.1 forward callback (Android or Web)"
        }

        if (androidMethod != null) {
            androidMethod.addInstructions(
                0,
                formattedCopyFallbackInstructions(
                    callbackClass = "Lorg/telegram/messenger/uy;",
                    controllerBaseClass = "Lorg/telegram/messenger/c1;",
                    wrapperClass = "Lorg/telegram/messenger/jj;",
                    sendHelperClass = "Lorg/telegram/messenger/nz;",
                ),
            )
        } else {
            requireNotNull(webMethod).addInstructions(
                0,
                formattedCopyFallbackInstructions(
                    callbackClass = "Lorg/telegram/messenger/vy;",
                    controllerBaseClass = "Lorg/telegram/messenger/e1;",
                    wrapperClass = "Lorg/telegram/messenger/fj;",
                    sendHelperClass = "Lorg/telegram/messenger/pz;",
                ),
            )
        }
    }
}

private fun formattedCopyFallbackInstructions(
    callbackClass: String,
    controllerBaseClass: String,
    wrapperClass: String,
    sendHelperClass: String,
): String = """
    # Run only for the exact server entitlement error.
    move-object/from16 v0, p2
    if-eqz v0, :morphe_formatted_copy_continue
    iget-object v1, v0, Lorg/telegram/tgnet/TLRPC${'$'}TL_error;->text:Ljava/lang/String;
    if-eqz v1, :morphe_formatted_copy_continue
    const-string v0, "PREMIUM_ACCOUNT_REQUIRED"
    invoke-virtual {v0, v1}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
    move-result v0
    if-eqz v0, :morphe_formatted_copy_continue

    # The original forward must have explicitly requested drop_author.
    move-object/from16 v2, p0
    iget-object v3, v2, $callbackClass->k:Lorg/telegram/tgnet/TLRPC${'$'}TL_messages_forwardMessages;
    if-eqz v3, :morphe_formatted_copy_continue
    iget-boolean v0, v3, Lorg/telegram/tgnet/TLRPC${'$'}TL_messages_forwardMessages;->drop_author:Z
    if-eqz v0, :morphe_formatted_copy_continue

    # Do not alter scheduled, silent, reply, send-as, quick-reply, paid, or special forwards.
    iget v0, v3, Lorg/telegram/tgnet/TLRPC${'$'}TL_messages_forwardMessages;->schedule_date:I
    if-nez v0, :morphe_formatted_copy_continue
    iget-boolean v0, v3, Lorg/telegram/tgnet/TLRPC${'$'}TL_messages_forwardMessages;->silent:Z
    if-nez v0, :morphe_formatted_copy_continue
    iget-boolean v0, v3, Lorg/telegram/tgnet/TLRPC${'$'}TL_messages_forwardMessages;->background:Z
    if-nez v0, :morphe_formatted_copy_continue
    iget-boolean v0, v3, Lorg/telegram/tgnet/TLRPC${'$'}TL_messages_forwardMessages;->from_ephemeral:Z
    if-nez v0, :morphe_formatted_copy_continue
    iget-boolean v0, v3, Lorg/telegram/tgnet/TLRPC${'$'}TL_messages_forwardMessages;->noforwards:Z
    if-nez v0, :morphe_formatted_copy_continue
    iget-object v0, v3, Lorg/telegram/tgnet/TLRPC${'$'}TL_messages_forwardMessages;->reply_to:Lorg/telegram/tgnet/TLRPC${'$'}InputReplyTo;
    if-eqz v0, :morphe_formatted_copy_no_reply
    goto :morphe_formatted_copy_continue
    :morphe_formatted_copy_no_reply
    iget-object v0, v3, Lorg/telegram/tgnet/TLRPC${'$'}TL_messages_forwardMessages;->send_as:Lorg/telegram/tgnet/TLRPC${'$'}InputPeer;
    if-eqz v0, :morphe_formatted_copy_no_send_as
    goto :morphe_formatted_copy_continue
    :morphe_formatted_copy_no_send_as
    iget-object v0, v3, Lorg/telegram/tgnet/TLRPC${'$'}TL_messages_forwardMessages;->quick_reply_shortcut:Lorg/telegram/tgnet/TLRPC${'$'}InputQuickReplyShortcut;
    if-eqz v0, :morphe_formatted_copy_no_quick_reply
    goto :morphe_formatted_copy_continue
    :morphe_formatted_copy_no_quick_reply
    iget-object v0, v3, Lorg/telegram/tgnet/TLRPC${'$'}TL_messages_forwardMessages;->suggested_post:Lorg/telegram/tgnet/TLRPC${'$'}SuggestedPost;
    if-eqz v0, :morphe_formatted_copy_no_suggested_post
    goto :morphe_formatted_copy_continue
    :morphe_formatted_copy_no_suggested_post
    iget-wide v0, v3, Lorg/telegram/tgnet/TLRPC${'$'}TL_messages_forwardMessages;->effect:J
    const-wide/16 v10, 0x0
    cmp-long v0, v0, v10
    if-nez v0, :morphe_formatted_copy_continue
    iget-wide v0, v3, Lorg/telegram/tgnet/TLRPC${'$'}TL_messages_forwardMessages;->allow_paid_stars:J
    cmp-long v0, v0, v10
    if-nez v0, :morphe_formatted_copy_continue
    iget-boolean v0, v3, Lorg/telegram/tgnet/TLRPC${'$'}TL_messages_forwardMessages;->allow_paid_floodskip:Z
    if-nez v0, :morphe_formatted_copy_continue

    # Validate the complete batch before sending anything, avoiding partial fallback.
    iget-object v4, v2, $callbackClass->h:Ljava/util/ArrayList;
    if-eqz v4, :morphe_formatted_copy_continue
    invoke-virtual {v4}, Ljava/util/ArrayList;->size()I
    move-result v5
    if-lez v5, :morphe_formatted_copy_continue
    iget-object v0, v2, $callbackClass->g:Ljava/util/ArrayList;
    if-eqz v0, :morphe_formatted_copy_continue
    invoke-virtual {v0}, Ljava/util/ArrayList;->size()I
    move-result v1
    if-ne v1, v5, :morphe_formatted_copy_continue
    const/4 v6, 0x0

    :morphe_formatted_copy_validate_loop
    if-ge v6, v5, :morphe_formatted_copy_send_start
    invoke-virtual {v4, v6}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;
    move-result-object v7
    check-cast v7, $wrapperClass;
    iget-object v8, v7, $wrapperClass->k:Lorg/telegram/tgnet/TLRPC${'$'}Message;
    if-eqz v8, :morphe_formatted_copy_continue

    # Accept only empty/no media; do not convert photos, documents, albums or previews.
    iget-object v9, v8, Lorg/telegram/tgnet/TLRPC${'$'}Message;->media:Lorg/telegram/tgnet/TLRPC${'$'}MessageMedia;
    if-eqz v9, :morphe_formatted_copy_media_ok
    instance-of v10, v9, Lorg/telegram/tgnet/TLRPC${'$'}TL_messageMediaEmpty;
    if-eqz v10, :morphe_formatted_copy_continue
    :morphe_formatted_copy_media_ok

    # Require explicit formatting entities and a non-empty rendered CharSequence.
    iget-object v9, v8, Lorg/telegram/tgnet/TLRPC${'$'}Message;->entities:Ljava/util/ArrayList;
    if-eqz v9, :morphe_formatted_copy_continue
    invoke-virtual {v9}, Ljava/util/ArrayList;->isEmpty()Z
    move-result v10
    if-nez v10, :morphe_formatted_copy_continue
    iget-object v9, v7, $wrapperClass->v:Ljava/lang/CharSequence;
    if-eqz v9, :morphe_formatted_copy_continue
    invoke-interface {v9}, Ljava/lang/CharSequence;->length()I
    move-result v10
    if-lez v10, :morphe_formatted_copy_continue

    add-int/lit8 v6, v6, 0x1
    goto :morphe_formatted_copy_validate_loop

    :morphe_formatted_copy_send_start
    const/4 v6, 0x0

    :morphe_formatted_copy_send_loop
    if-ge v6, v5, :morphe_formatted_copy_return
    invoke-virtual {v4, v6}, Ljava/util/ArrayList;->get(I)Ljava/lang/Object;
    move-result-object v7
    check-cast v7, $wrapperClass;
    iget-object v8, v7, $wrapperClass->v:Ljava/lang/CharSequence;

    # Send the existing formatted CharSequence as a new message to the same dialog.
    invoke-virtual {v2}, $controllerBaseClass->getAccountInstance()Lorg/telegram/messenger/a;
    move-result-object v7
    # SendMessagesHelper.a0 expects (topicId, dialogId), in that order.
    iget v9, v3, Lorg/telegram/tgnet/TLRPC${'$'}TL_messages_forwardMessages;->top_msg_id:I
    int-to-long v9, v9
    iget-wide v11, v2, $callbackClass->b:J
    const/4 v13, 0x0
    const/4 v14, 0x0
    const/4 v15, 0x0
    invoke-static/range {v7 .. v15}, $sendHelperClass->a0(Lorg/telegram/messenger/a;Ljava/lang/CharSequence;JJZII)V

    add-int/lit8 v6, v6, 0x1
    goto :morphe_formatted_copy_send_loop

    :morphe_formatted_copy_return
    return-void

    :morphe_formatted_copy_continue
    nop
""".trimIndent()
