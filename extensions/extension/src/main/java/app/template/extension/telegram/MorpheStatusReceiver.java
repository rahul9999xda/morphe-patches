package app.template.extension.telegram;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Handles the notification action without exposing an exported broadcast endpoint. */
public final class MorpheStatusReceiver extends BroadcastReceiver {
    public static final String ACTION_TOGGLE = "app.template.extension.telegram.action.TOGGLE_AUTOMATION";

    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_TOGGLE.equals(intent.getAction())) return;
        SharedPreferencesForwardingRuleStore store = new SharedPreferencesForwardingRuleStore(context);
        boolean enabled = !store.isEnabled();
        store.setEnabled(enabled);
        MorpheStatusNotification.update(context, enabled);
    }
}
