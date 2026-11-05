package app.template.extension.telegram;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import app.template.extension.forwarding.ForwardingCoordinator;
import app.template.extension.forwarding.ForwardingMessage;
import app.template.extension.forwarding.ForwardingRule;
import app.template.extension.forwarding.ForwardingRuleStore;
import app.template.extension.forwarding.TelegramMessageGateway;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Connects the rule engine to Telegram's existing NotificationCenter and SendMessagesHelper.
 * No TDLib client, second login, or separate Telegram session is created.
 */
public final class MorpheForwardingRuntime {
    private static final String NOTIFICATION_CENTER = "org.telegram.messenger.NotificationCenter";
    private static final String USER_CONFIG = "org.telegram.messenger.UserConfig";
    private static final String MESSAGE_OBJECT = "org.telegram.messenger.MessageObject";
    private static final String APPLICATION_LOADER = "org.telegram.messenger.ApplicationLoader";
    private static final ArrayList<Object> OBSERVERS = new ArrayList<>();
    private static volatile boolean installed;
    private static ForwardingCoordinator coordinator;
    private static SharedPreferencesForwardingRuleStore store;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private MorpheForwardingRuntime() { }

    public static synchronized void install() {
        if (installed) return;
        try {
            Context context = applicationContext();
            if (context == null) return;
            store = new SharedPreferencesForwardingRuleStore(context);
            Executor executor = Executors.newSingleThreadExecutor(command -> new Thread(command, "MorpheForwarding"));
            coordinator = new ForwardingCoordinator(store, new NativeGateway(), executor);
            coordinator.start();

            ClassLoader loader = context.getClassLoader();
            Class<?> notificationClass = Class.forName(NOTIFICATION_CENTER, false, loader);
            Class<?> delegateClass = Class.forName(NOTIFICATION_CENTER + "$NotificationCenterDelegate", false, loader);
            int eventId = notificationClass.getField("didReceiveNewMessages").getInt(null);
            int maxAccounts = maxAccountCount(loader);
            Method getInstance = notificationClass.getMethod("getInstance", int.class);
            Method addObserver = notificationClass.getMethod("addObserver", delegateClass, int.class);

            for (int account = 0; account < maxAccounts; account++) {
                Object center = getInstance.invoke(null, account);
                final int accountId = account;
                Object observer = Proxy.newProxyInstance(loader, new Class<?>[]{delegateClass},
                        new NotificationHandler(accountId, eventId));
                addObserver.invoke(center, observer, eventId);
                OBSERVERS.add(observer);
            }
            installed = true;
        } catch (Throwable error) {
            // Keep Telegram startup safe if a future fork changes an internal API.
            installed = false;
        }
    }

    private static Context applicationContext() {
        try {
            Class<?> loader = Class.forName(APPLICATION_LOADER);
            Field field = loader.getField("applicationContext");
            Object value = field.get(null);
            return value instanceof Context ? (Context) value : null;
        } catch (Throwable ignored) { return null; }
    }

    private static int maxAccountCount(ClassLoader loader) {
        try { return Class.forName(USER_CONFIG, false, loader).getField("MAX_ACCOUNT_COUNT").getInt(null); }
        catch (Throwable ignored) { return 4; }
    }

    private static boolean isAccountActivated(ClassLoader loader, int account) {
        try {
            Class<?> userConfig = Class.forName(USER_CONFIG, false, loader);
            Object instance = userConfig.getMethod("getInstance", int.class).invoke(null, account);
            return Boolean.TRUE.equals(userConfig.getMethod("isClientActivated").invoke(instance));
        } catch (Throwable ignored) { return false; }
    }

    private static final class NotificationHandler implements InvocationHandler {
        private final int accountId;
        private final int eventId;
        NotificationHandler(int accountId, int eventId) { this.accountId = accountId; this.eventId = eventId; }

        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            if ("toString".equals(method.getName())) return "MorpheNotificationObserver";
            if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
            if ("equals".equals(method.getName())) return proxy == (args == null ? null : args[0]);
            if (!"didReceivedNotification".equals(method.getName()) || args == null || args.length < 3) return null;
            try {
                int id = ((Number) args[0]).intValue();
                if (id != eventId || !(args[2] instanceof Object[])) return null;
                Object[] payload = (Object[]) args[2];
                if (payload.length < 2 || !(payload[0] instanceof Number) || !(payload[1] instanceof List)) return null;
                if (payload.length > 2 && Boolean.TRUE.equals(payload[2])) return null; // scheduled messages
                long dialogId = ((Number) payload[0]).longValue();
                if (isSecretDialog(dialogId)) return null;
                List<?> messages = (List<?>) payload[1];
                for (Object message : messages) {
                    ForwardingMessage event = toForwardingMessage(accountId, dialogId, message);
                    if (event != null && coordinator != null) coordinator.onIncomingMessage(event);
                }
            } catch (Throwable ignored) { }
            return null;
        }
    }

    private static ForwardingMessage toForwardingMessage(int accountId, long dialogId, Object messageObject) {
        if (messageObject == null) return null;
        try {
            Class<?> type = messageObject.getClass();
            Object owner = type.getField("messageOwner").get(messageObject);
            if (owner == null) return null;
            Class<?> ownerType = owner.getClass();
            if (ownerType.getField("out").getBoolean(owner)) return null;
            if (ownerType.getField("action").get(owner) != null) return null; // service messages
            if (ownerType.getField("noforwards").getBoolean(owner)) return null;
            if (isSecretDialog(dialogId)) return null;
            if (!Boolean.TRUE.equals(type.getMethod("canForwardMessage").invoke(messageObject))) return null;
            int messageId = ((Number) type.getMethod("getId").invoke(messageObject)).intValue();
            String text = "";
            try {
                Object value = type.getField("messageText").get(messageObject);
                if (value != null) text = value.toString();
            } catch (Throwable ignored) { }
            if (text.isEmpty()) {
                Object value = ownerType.getField("message").get(owner);
                if (value != null) text = String.valueOf(value);
            }
            return new ForwardingMessage(accountId, dialogId, messageId, text,
                    false, false, false, false, messageObject);
        } catch (Throwable ignored) { return null; }
    }

    private static boolean isSecretDialog(long dialogId) {
        try {
            Class<?> dialogObject = Class.forName("org.telegram.messenger.DialogObject");
            return Boolean.TRUE.equals(dialogObject.getMethod("isEncryptedDialog", long.class).invoke(null, dialogId));
        } catch (Throwable ignored) { return false; }
    }

    private static final class NativeGateway implements TelegramMessageGateway {
        @Override public void forward(final int accountId, final ForwardingMessage source,
                                      final long destinationChatId, final ResultCallback callback) {
            dispatchOnMain(new Runnable() {
                @Override public void run() {
                    try {
                        Object messageObject = source.getNativeMessage();
                        if (messageObject == null) throw new IllegalStateException("Original Telegram message is unavailable");
                        ClassLoader loader = messageObject.getClass().getClassLoader();
                        Class<?> helperClass = Class.forName("org.telegram.messenger.SendMessagesHelper", false, loader);
                        Object helper = helperClass.getMethod("getInstance", int.class).invoke(null, accountId);
                        Method method = findForwardMethod(helperClass, messageObject.getClass());
                        ArrayList<Object> messages = new ArrayList<>(Collections.singletonList(messageObject));
                        Object result = method.invoke(helper, messages, destinationChatId,
                                false, false, false, 0, 0, null, -1, 0L, 0L, null);
                        if (callback != null) callback.onSuccess();
                    } catch (Throwable error) {
                        if (callback != null) callback.onFailure(unwrap(error));
                    }
                }
            });
        }

        @Override public void copy(final int accountId, final ForwardingMessage source,
                                   final long destinationChatId, final ResultCallback callback) {
            dispatchOnMain(new Runnable() {
                @Override public void run() {
                    try {
                        Object messageObject = source.getNativeMessage();
                        if (messageObject == null) throw new IllegalStateException("Original Telegram message is unavailable");
                        ClassLoader loader = messageObject.getClass().getClassLoader();
                        Class<?> helperClass = Class.forName("org.telegram.messenger.SendMessagesHelper", false, loader);
                        Class<?> paramsClass = Class.forName("org.telegram.messenger.SendMessagesHelper$SendMessageParams", false, loader);
                        Object params = paramsClass.getMethod("of", messageObject.getClass()).invoke(null, messageObject);
                        paramsClass.getField("peer").setLong(params, destinationChatId);
                        Object helper = helperClass.getMethod("getInstance", int.class).invoke(null, accountId);
                        helperClass.getMethod("sendMessage", paramsClass).invoke(helper, params);
                        if (callback != null) callback.onSuccess();
                    } catch (Throwable error) {
                        if (callback != null) callback.onFailure(unwrap(error));
                    }
                }
            });
        }

        private static Method findForwardMethod(Class<?> helperClass, Class<?> messageObjectClass) throws NoSuchMethodException {
            for (Method method : helperClass.getMethods()) {
                Class<?>[] p = method.getParameterTypes();
                if (!"sendMessage".equals(method.getName()) || p.length != 12) continue;
                if (p[0] == ArrayList.class && p[1] == long.class && p[2] == boolean.class
                        && p[3] == boolean.class && p[4] == boolean.class && p[5] == int.class
                        && p[6] == int.class && p[7].isAssignableFrom(messageObjectClass)
                        && p[8] == int.class && p[9] == long.class && p[10] == long.class) {
                    method.setAccessible(true);
                    return method;
                }
            }
            throw new NoSuchMethodException("Native SendMessagesHelper forward overload not found");
        }

        private static void dispatchOnMain(Runnable runnable) {
            if (Looper.myLooper() == Looper.getMainLooper()) runnable.run();
            else MAIN.post(runnable);
        }

        private static Throwable unwrap(Throwable error) {
            return error instanceof java.lang.reflect.InvocationTargetException
                    && ((java.lang.reflect.InvocationTargetException) error).getCause() != null
                    ? ((java.lang.reflect.InvocationTargetException) error).getCause() : error;
        }
    }
}
