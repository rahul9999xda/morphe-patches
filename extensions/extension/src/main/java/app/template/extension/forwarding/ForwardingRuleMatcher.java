package app.template.extension.forwarding;

import java.util.List;
import java.util.Locale;

/** Pure matching logic; deliberately has no Telegram or Android dependencies. */
public final class ForwardingRuleMatcher {
    private ForwardingRuleMatcher() { }

    public static boolean matches(ForwardingRule rule, ForwardingMessage message) {
        if (rule == null || message == null || !rule.isEnabled()) return false;
        if (!rule.getSourceChatIds().contains(message.getSourceChatId())) return false;
        if (message.isOutgoing() || message.isSecretChat() || message.isServiceMessage()
                || message.isProtectedContent()) return false;
        // Native forwardability is a FORWARD-only constraint. COPY uses Telegram's
        // SendMessageParams.of(MessageObject) path and must not be rejected merely
        // because canForwardMessage() is false for the original post.
        if (rule.getDeliveryMode() == ForwardingRule.DeliveryMode.FORWARD
                && !message.isForwardable()) return false;

        String body = normalize(message.getText(), rule.isCaseSensitive());
        for (String excluded : rule.getExcludeKeywords()) {
            if (contains(body, normalize(excluded, rule.isCaseSensitive()))) return false;
        }

        List<String> included = rule.getIncludeKeywords();
        // Empty-text/media-only posts match rules with no include-keyword condition.
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
