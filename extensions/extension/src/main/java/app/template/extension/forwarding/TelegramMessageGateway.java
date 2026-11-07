package app.template.extension.forwarding;

/** Adapter implemented by version-specific hooks into Telegram's already-authenticated account. */
public interface TelegramMessageGateway {
    interface ResultCallback {
        /** Native Telegram accepted the send operation; this is not a server-delivery receipt. */
        void onSuccess();
        void onFailure(Throwable error);
    }

    void forward(int accountId, ForwardingMessage source, long destinationChatId,
                 ResultCallback callback);

    /** Sends a native MessageObject copy without forwarded-message attribution. */
    void copy(int accountId, ForwardingMessage source, long destinationChatId,
              ResultCallback callback);
}
