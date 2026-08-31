package app.template.patches.telegram

import app.morphe.patcher.Fingerprint

/**
 * The matched method is the forwarding overload in SendMessagesHelper.
 * Its p4 parameter is forwardFromMyName and the verified DEX writes that
 * value to TL_messages_forwardMessages.drop_author.
 */
internal object SendMessagesHelperRichForwardFingerprint : Fingerprint(
    definingClass = "Lorg/telegram/messenger/SendMessagesHelper;",
    name = "sendMessage",
    returnType = "I",
    parameters = listOf(
        "Ljava/util/ArrayList;",
        "J",
        "Z",
        "Z",
        "Z",
        "I",
        "I",
        "Lorg/telegram/messenger/MessageObject;",
        "I",
        "J",
        "J",
        "Lorg/telegram/messenger/MessageSuggestionParams;",
    ),
)