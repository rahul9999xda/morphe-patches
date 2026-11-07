package app.template.extension.forwarding;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/** Serial event processor. The native Telegram process owns the account and update lifecycle. */
public final class ForwardingCoordinator {
    private final ForwardingRuleStore store;
    private final TelegramMessageGateway gateway;
    private final Executor executor;
    private final Set<String> inFlight = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final LinkedHashSet<String> recentlyDispatched = new LinkedHashSet<>();
    private static final int MAX_RECENT_KEYS = 4096;
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
        try {
            executor.execute(new Runnable() {
                @Override public void run() {
                    if (!running) return;
                    final List<ForwardingRule> rules;
                    try { rules = store.loadRules(); }
                    catch (Throwable ignored) { return; }
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
        } catch (RejectedExecutionException ignored) {
            // Shutdown/rejection is contained; Telegram's notification thread must never crash.
        }
    }

    private void dispatch(final ForwardingRule rule, final ForwardingMessage message, final long destination) {
        final String key = message.getAccountId() + ":" + rule.getId() + ":" + message.getSourceChatId()
                + ":" + message.getMessageId() + ":" + destination;
        synchronized (recentlyDispatched) {
            if (recentlyDispatched.contains(key) || inFlight.contains(key)) return;
            inFlight.add(key);
        }
        TelegramMessageGateway.ResultCallback callback = new TelegramMessageGateway.ResultCallback() {
            @Override public void onSuccess() {
                synchronized (recentlyDispatched) {
                    inFlight.remove(key);
                    recentlyDispatched.add(key);
                    while (recentlyDispatched.size() > MAX_RECENT_KEYS) {
                        recentlyDispatched.remove(recentlyDispatched.iterator().next());
                    }
                }
            }
            @Override public void onFailure(Throwable error) {
                synchronized (recentlyDispatched) { inFlight.remove(key); }
            }
        };
        try {
            if (rule.getDeliveryMode() == ForwardingRule.DeliveryMode.COPY) {
                gateway.copy(message.getAccountId(), message, destination, callback);
            } else {
                gateway.forward(message.getAccountId(), message, destination, callback);
            }
        } catch (Throwable error) {
            callback.onFailure(error);
        }
    }
}
