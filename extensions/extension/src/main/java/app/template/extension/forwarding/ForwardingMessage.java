package app.template.extension.forwarding;

/** Account-scoped event created from Telegram's native message-update pipeline. */
public final class ForwardingMessage {
    private final int accountId;
    private final long sourceChatId;
    private final int messageId;
    private final String text;
    // Keep Telegram's original styled CharSequence (Spans/entities) intact for native copy sending.
    private final CharSequence richText;
    private final boolean outgoing;
    private final boolean secretChat;
    private final boolean serviceMessage;
    private final boolean protectedContent;
    private final boolean forwardable;
    private final Object nativeMessage;

    public ForwardingMessage(int accountId, long sourceChatId, int messageId, String text,
                             boolean outgoing, boolean secretChat, boolean serviceMessage) {
        this(accountId, sourceChatId, messageId, text, outgoing, secretChat,
                serviceMessage, false, true, null, text);
    }

    public ForwardingMessage(int accountId, long sourceChatId, int messageId, String text,
                             boolean outgoing, boolean secretChat, boolean serviceMessage,
                             boolean protectedContent, Object nativeMessage) {
        this(accountId, sourceChatId, messageId, text, outgoing, secretChat, serviceMessage,
                protectedContent, true, nativeMessage, text);
    }

    public ForwardingMessage(int accountId, long sourceChatId, int messageId, String text,
                             boolean outgoing, boolean secretChat, boolean serviceMessage,
                             boolean protectedContent, boolean forwardable, Object nativeMessage) {
        this(accountId, sourceChatId, messageId, text, outgoing, secretChat, serviceMessage,
                protectedContent, forwardable, nativeMessage, text);
    }

    public ForwardingMessage(int accountId, long sourceChatId, int messageId, String text,
                             boolean outgoing, boolean secretChat, boolean serviceMessage,
                             boolean protectedContent, boolean forwardable, Object nativeMessage,
                             CharSequence richText) {
        this.accountId = accountId;
        this.sourceChatId = sourceChatId;
        this.messageId = messageId;
        this.text = text == null ? "" : text;
        this.richText = richText == null ? this.text : richText;
        this.outgoing = outgoing;
        this.secretChat = secretChat;
        this.serviceMessage = serviceMessage;
        this.protectedContent = protectedContent;
        this.forwardable = forwardable;
        this.nativeMessage = nativeMessage;
    }

    public int getAccountId() { return accountId; }
    public long getSourceChatId() { return sourceChatId; }
    public int getMessageId() { return messageId; }
    public String getText() { return text; }
    /** Original Telegram styled text; do not flatten before native copy conversion. */
    public CharSequence getRichText() { return richText; }
    public boolean isOutgoing() { return outgoing; }
    public boolean isSecretChat() { return secretChat; }
    public boolean isServiceMessage() { return serviceMessage; }
    public boolean isProtectedContent() { return protectedContent; }
    public boolean canForward() { return forwardable && !protectedContent; }
    /** Opaque original Telegram object; only a version adapter may interpret it. */
    public Object getNativeMessage() { return nativeMessage; }
}
