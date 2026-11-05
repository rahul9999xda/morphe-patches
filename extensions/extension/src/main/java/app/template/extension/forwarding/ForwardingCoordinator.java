package app.template.extension.forwarding;

import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * Event-driven forwarding coordinator. It does not create a second Telegram login or service;
 * a native Telegram update hook supplies messages and the gateway uses the active account.
 */
public final class ForwardingCoordinator {
    private final ForwardingRuleStore store;
    private final TelegramMessageGateway gateway;
    private final Executor executor;
    private final Set<String> inFlight = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final LinkedHashSet<String> recentlyDispatched = new LinkedHashSet<>();
    private static final int MAX_RECENT_KEYS = 2048;
    private volatile boolean running;

    public ForwardingCoordinator(ForwardingRuleStore store, TelegramMessageGateway gateway, Executor executor) {
        if (store == null || gateway == null || executor == null) throw new IllegalArgumentException("Dependencies are required");
        this.store = store;
        this.gateway = gateway;
        this.executor = executor;
    }

    public void start() { running = true; }
    public void stop() { running = false; inFlight.clear(); }
    public boolean isRunning() { return running; }

    public void onIncomingMessage(final ForwardingMessage message) {
        if (!running || message == null || message.isOutgoing() || message.isSecretChat()
                || message.isServiceMessage() || message.isProtectedContent()) return;
        executor.execute(new Runnable() {
            @Override public void run() {
                if (!running) return;
                List<ForwardingRule> rules = store.loadRules();
                if (rules == null) return;
                for (ForwardingRule rule : rules) {
                    if (!ForwardingRuleMatcher.matches(rule, message)) continue;
                    for (Long destination : rule.getDestinationChatIds()) {
                        if (destination == null || destination.longValue() == message.getSourceChatId()) continue;
                        dispatch(rule, message, destination.longValue());
                    }
                }
            }
        });
    }

    private void dispatch(final ForwardingRule rule, final ForwardingMessage message, final long destination) {
        final String key = message.getAccountId() + ":" + rule.getId() + ":" + message.getSourceChatId()
                + ":" + message.getMessageId() + ":" + destination;
        synchronized (recentlyDispatched) {
            if (recentlyDispatched.contains(key)) return;
            recentlyDispatched.add(key);
            while (recentlyDispatched.size() > MAX_RECENT_KEYS) {
                String oldest = recentlyDispatched.iterator().next();
                recentlyDispatched.remove(oldest);
            }
        }
        if (!inFlight.add(key)) return;
        if (store instanceof app.template.extension.telegram.SharedPreferencesForwardingRuleStore) {
            ((app.template.extension.telegram.SharedPreferencesForwardingRuleStore) store).recordAutomation(rule.getId());
        }
        TelegramMessageGateway.ResultCallback callback = new TelegramMessageGateway.ResultCallback() {
            @Override public void onSuccess() {
                inFlight.remove(key);
                if (store instanceof app.template.extension.telegram.SharedPreferencesForwardingRuleStore) {
                    app.template.extension.telegram.SharedPreferencesForwardingRuleStore stats = (app.template.extension.telegram.SharedPreferencesForwardingRuleStore) store;
                    if (rule.getDeliveryMode() == ForwardingRule.DeliveryMode.COPY) stats.recordCopied(rule.getId());
                    else stats.recordForwarded(rule.getId());
                }
            }
            @Override public void onFailure(Throwable error) {
                inFlight.remove(key);
                synchronized (recentlyDispatched) { recentlyDispatched.remove(key); }
            }
        };
        try {
            if (rule.getDeliveryMode() == ForwardingRule.DeliveryMode.COPY) {
                gateway.copy(message.getAccountId(), message, destination, false, callback);
            } else {
                gateway.forward(message.getAccountId(), message, destination, false, callback);
            }
        } catch (Throwable error) {
            callback.onFailure(error);
        }
    }
}
