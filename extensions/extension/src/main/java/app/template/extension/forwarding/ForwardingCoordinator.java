package app.template.extension.forwarding;

import java.util.Collections;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Account-scoped event coordinator for the Telegram 13.1.1 extension foundation.
 * Events are serialized by the supplied executor; a dispatch is claimed atomically so
 * duplicate NotificationCenter callbacks cannot race between the recent/in-flight checks.
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

    /** Stops accepting/starting queued work. In-flight native sends are allowed to finish. */
    public void stop() { running = false; }

    public boolean isRunning() { return running; }

    public void onIncomingMessage(final ForwardingMessage message) {
        if (!running || message == null || message.isOutgoing() || message.isSecretChat()
                || message.isServiceMessage() || message.isProtectedContent()) return;
        try {
            executor.execute(new Runnable() {
                @Override public void run() {
                    if (!running) return;
                    final List<ForwardingRule> rules;
                    try {
                        rules = store.loadRules();
                    } catch (Throwable ignored) {
                        // A broken preference read must not terminate the executor thread.
                        return;
                    }
                    if (rules == null) return;
                    for (ForwardingRule rule : rules) {
                        if (!running) return;
                        if (rule == null) continue;
                        final boolean matches;
                        try {
                            matches = ForwardingRuleMatcher.matches(rule, message);
                        } catch (Throwable ignored) {
                            // Isolate a malformed rule; continue evaluating the remaining rules.
                            continue;
                        }
                        if (!matches) continue;
                        Set<Long> destinations = rule.getDestinationChatIds();
                        if (destinations == null) continue;
                        for (Long destination : destinations) {
                            if (!running) return;
                            if (destination == null || destination.longValue() == message.getSourceChatId()) continue;
                            dispatch(rule, message, destination.longValue());
                        }
                    }
                }
            });
        } catch (RuntimeException rejected) {
            // Executor shutdown/rejection is contained; the next native update can retry.
        }
    }

    private void dispatch(final ForwardingRule rule, final ForwardingMessage message, final long destination) {
        final String key = message.getAccountId() + ":" + rule.getId() + ":" + message.getSourceChatId()
                + ":" + message.getMessageId() + ":" + destination;

        // The recent-key check, in-flight claim and recent-key insertion form one atomic action.
        synchronized (recentlyDispatched) {
            if (recentlyDispatched.contains(key) || !inFlight.add(key)) return;
            recentlyDispatched.add(key);
            while (recentlyDispatched.size() > MAX_RECENT_KEYS) {
                String oldest = recentlyDispatched.iterator().next();
                recentlyDispatched.remove(oldest);
            }
        }

        final AtomicBoolean completed = new AtomicBoolean(false);
        TelegramMessageGateway.ResultCallback callback = new TelegramMessageGateway.ResultCallback() {
            @Override public void onSuccess() {
                if (!completed.compareAndSet(false, true)) return;
                inFlight.remove(key);
                // Keep the recent key after success to suppress duplicate native update delivery.
            }
            @Override public void onFailure(Throwable error) {
                if (!completed.compareAndSet(false, true)) return;
                inFlight.remove(key);
                synchronized (recentlyDispatched) { recentlyDispatched.remove(key); }
            }
        };
        try {
            if (rule.getDeliveryMode() == ForwardingRule.DeliveryMode.COPY) {
                gateway.copy(message.getAccountId(), message, destination, callback);
            } else {
                gateway.forward(message.getAccountId(), message.getSourceChatId(),
                        message.getMessageId(), destination, callback);
            }
        } catch (Throwable error) {
            callback.onFailure(error);
        }
    }
}
