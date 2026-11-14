package app.template.extension.forwarding;

import java.util.List;
import java.util.Locale;

/** Pure matching logic; deliberately has no Telegram or Android dependencies. */
public final class ForwardingRuleMatcher {
    private ForwardingRuleMatcher() { }

    public static boolean matches(ForwardingRule rule, ForwardingMessage message) {
        if (rule == null || message == null || !rule.isEnabled()) return false;
        if (!rule.getSourceChatIds().contains(message.getSourceChatId())) return false;
        if (message.isOutgoing() || message.isSecretChat() || message.isServiceMessage()) return false;
        // Protected text can be copied when the user's separate content-restriction
        // patch permits it. The sticker/emoji preference is applied only in FORWARD mode below.
        if (rule.getDeliveryMode() == ForwardingRule.DeliveryMode.FORWARD) {
            // Sticker/emoji filtering is a forwarding preference only. COPY mode must
            // remain independent so a user can intentionally copy an emoji or sticker
            // caption/message without this forwarding-only filter suppressing it.
            if (rule.isIgnoreStickerEmojiOnly() && message.isStickerOrEmojiOnly()) return false;
            // Do not pre-filter with MessageObject.canForwardMessage() here. In 13.2.2
            // that UI predicate rejects several channel-post shapes even when Telegram's
            // native SendMessagesHelper can enqueue them. The native helper remains the
            // final authority and reports a zero result when it declines the request.
            // Secret chats, service messages and outgoing messages are still excluded above.

        }
        if (rule.getDeliveryMode() == ForwardingRule.DeliveryMode.COPY && message.getText().trim().isEmpty()) return false;

        String body = normalize(message.getText(), rule.isCaseSensitive());
        for (String excluded : rule.getExcludeKeywords()) {
            if (contains(body, normalize(excluded, rule.isCaseSensitive()))) return false;
        }

        List<String> included = rule.getIncludeKeywords();
        if (included.isEmpty()) return true;
        if (rule.getMatchMode() == ForwardingRule.MatchMode.ALL) {
            for (String keyword : included) {
                if (!contains(body, normalize(keyword, rule.isCaseSensitive()))) return false;
            }
            return true;
        }
        for (String keyword : included) {
            if (contains(body, normalize(keyword, rule.isCaseSensitive()))) return true;
        }
        return false;
    }

    private static String normalize(String value, boolean caseSensitive) {
        String clean = value == null ? "" : value.trim();
        return caseSensitive ? clean : clean.toLowerCase(Locale.ROOT);
    }

    private static boolean contains(String body, String keyword) {
        return !keyword.isEmpty() && body.contains(keyword);
    }
}
