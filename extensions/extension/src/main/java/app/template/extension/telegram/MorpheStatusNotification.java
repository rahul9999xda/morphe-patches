package app.template.extension.telegram;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

/** Persistent notification-panel control for Morphe message automation. */
public final class MorpheStatusNotification {
    public static final String CHANNEL_ID = "morphe_message_automation";
    public static final int NOTIFICATION_ID = 0x4d50;
    private static final int REQUEST_TOGGLE = 0x4d51;
    private static final int REQUEST_OPEN = 0x4d52;

    private MorpheStatusNotification() { }

    public static boolean hasPermission(Context context) {
        return Build.VERSION.SDK_INT < 33
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    public static void update(Context context, boolean enabled) {
        if (context == null || !hasPermission(context)) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Morphe message automation", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Status and quick control for Morphe automatic message forwarding");
            channel.setShowBadge(false);
            manager.createNotificationChannel(channel);
        }

        Intent open = new Intent(context, MorpheSettingsActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(context, REQUEST_OPEN, open, pendingFlags());

        Intent toggle = new Intent(context, MorpheStatusReceiver.class);
        toggle.setAction(MorpheStatusReceiver.ACTION_TOGGLE);
        PendingIntent action = PendingIntent.getBroadcast(context, REQUEST_TOGGLE, toggle, pendingFlags());

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL_ID) : new Notification.Builder(context);
        builder.setSmallIcon(android.R.drawable.stat_notify_sync)
                .setContentTitle("Morphe – Message Automation")
                .setContentText(enabled ? "Automatic forwarding is active" : "Automatic forwarding is paused")
                .setContentIntent(content)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(0, enabled ? "Pause" : "Enable", action);
        if (Build.VERSION.SDK_INT < 26) builder.setPriority(Notification.PRIORITY_LOW);
        try { manager.notify(NOTIFICATION_ID, builder.build()); }
        catch (SecurityException ignored) { }
    }

    public static void remove(Context context) {
        if (context == null) return;
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(NOTIFICATION_ID);
    }

    private static int pendingFlags() {
        return PendingIntent.FLAG_UPDATE_CURRENT
                | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);
    }
}
