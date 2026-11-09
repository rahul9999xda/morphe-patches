package app.template.extension.telegram;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import app.template.extension.forwarding.ForwardingCoordinator;
import app.template.extension.forwarding.ForwardingMessage;
import app.template.extension.forwarding.ForwardingRule;
import app.template.extension.forwarding.TelegramMessageGateway;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Connects Morphe forwarding rules to Telegram's already-authenticated account stack.
 * Current Telegram 13.2.1 Android/Web R8 names are selected by the patch's startup hook.
 * The no-argument entry point is retained for older, unobfuscated targets.
 */
public final class MorpheForwardingRuntime {
    private static final int PROFILE_UNSET = -2;
    private static final int PROFILE_LEGACY = -1;
    private static final int PROFILE_TELEGRAM_ANDROID_1321 = 0;
    private static final int PROFILE_TELEGRAM_WEB_1321 = 1;

    private static final ArrayList<Object> OBSERVERS = new ArrayList<>();
    private static final Set<Integer> REGISTERED_ACCOUNTS = new HashSet<>();
    private static volatile boolean installed;
    private static int activeProfile = PROFILE_UNSET;
    private static int installRetryCount;
    private static final int MAX_INSTALL_RETRIES = 20;
    private static ForwardingCoordinator coordinator;
    private static SharedPreferencesForwardingRuleStore store;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private MorpheForwardingRuntime() { }

    /** Legacy entry point kept for previously supported Telegram/Plus builds. */
    public static synchronized void install() {
        installInternal(PROFILE_LEGACY);
    }

    /** Called by the 13.2.1 Telegram Android/Web patch with its exact R8 profile. */
    public static synchronized void install(int profile) {
        if (profile != PROFILE_TELEGRAM_ANDROID_1321 && profile != PROFILE_TELEGRAM_WEB_1321) {
            installInternal(PROFILE_LEGACY);
            return;
        }
        installInternal(profile);
    }

    private static synchronized void installInternal(int profile) {
        if (installed && activeProfile == profile) return;
        if (activeProfile != profile) {
            // A process only contains one Telegram application variant. Ignore stale retry
            // callbacks from a previously selected profile rather than mixing mappings.
            if (activeProfile != PROFILE_UNSET) return;
            activeProfile = profile;
        }
        try {
            Context context = applicationContext(profile);
            if (context == null) {
                scheduleInstallRetry(profile);
                return;
            }
            if (store == null) store = new SharedPreferencesForwardingRuleStore(context);
            if (coordinator == null) {
                Executor executor = Executors.newSingleThreadExecutor(command -> {
                    Thread thread = new Thread(command, "MorpheForwarding");
                    thread.setPriority(Thread.NORM_PRIORITY - 1);
                    return thread;
                });
                coordinator = new ForwardingCoordinator(store, new NativeGateway(profile), executor);
                coordinator.start();
            }

            RuntimeProfile mapping = RuntimeProfile.forId(profile);
            ClassLoader loader = context.getClassLoader();
            Class<?> notificationClass = Class.forName(mapping.notificationClass, false, loader);
            Class<?> delegateClass = Class.forName(mapping.delegateClass, false, loader);
            Method getInstance = notificationClass.getMethod(mapping.notificationGetInstance, int.class);
            Method addObserver = notificationClass.getMethod(mapping.notificationAddObserver, delegateClass, int.class);
            int maxAccounts = mapping.maxAccounts;

            for (int account = 0; account < maxAccounts; account++) {
                if (REGISTERED_ACCOUNTS.contains(account)) continue;
                try {
                    Object center = getInstance.invoke(null, account);
                    Object observer = Proxy.newProxyInstance(loader, new Class<?>[]{delegateClass},
                            new NotificationHandler(account, mapping.eventId, mapping));
                    addObserver.invoke(center, observer, mapping.eventId);
                    OBSERVERS.add(observer);
                    REGISTERED_ACCOUNTS.add(account);
                } catch (Throwable accountError) {
                    // Keep successful account registrations and retry missing slots only.
                }
            }
            installed = REGISTERED_ACCOUNTS.size() >= maxAccounts;
            if (!installed) scheduleInstallRetry(profile);
            else installRetryCount = 0;
        } catch (Throwable error) {
            // A version mismatch must never be allowed to interrupt Telegram startup.
            installed = false;
            scheduleInstallRetry(profile);
        }
    }

    private static void scheduleInstallRetry(final int profile) {
        if (installRetryCount >= MAX_INSTALL_RETRIES) return;
        installRetryCount++;
        MAIN.postDelayed(new Runnable() {
            @Override public void run() { MorpheForwardingRuntime.installInternal(profile); }
        }, 15000L);
    }

    private static Context applicationContext(int profile) {
        RuntimeProfile mapping = RuntimeProfile.forId(profile);
        try {
            Class<?> appClass = Class.forName(mapping.applicationClass);
            Field field = appClass.getField(mapping.applicationContextField);
            Object value = field.get(null);
            return value instanceof Context ? (Context) value : null;
        } catch (Throwable ignored) { return null; }
    }

    private static boolean isSecretDialog(long dialogId, RuntimeProfile mapping, ClassLoader loader) {
        try {
            Class<?> type = Class.forName(mapping.dialogIdClass, false, loader);
            Method method = type.getMethod(mapping.dialogIdMethod, long.class);
            return Boolean.TRUE.equals(method.invoke(null, dialogId));
        } catch (Throwable ignored) {
            // Fail closed: if this client variant's secret-chat predicate cannot be resolved,
            // do not pass the message to the automation engine.
            return true;
        }
    }

    private static final class NotificationHandler implements InvocationHandler {
        private final int accountId;
        private final int eventId;
        private final RuntimeProfile mapping;

        NotificationHandler(int accountId, int eventId, RuntimeProfile mapping) {
            this.accountId = accountId;
            this.eventId = eventId;
            this.mapping = mapping;
        }

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
                if (payload.length > 2 && Boolean.TRUE.equals(payload[2])) return null;
                long dialogId = ((Number) payload[0]).longValue();
                List<?> messages = (List<?>) payload[1];
                if (messages.isEmpty()) return null;
                ClassLoader loader = messages.get(0).getClass().getClassLoader();
                if (isSecretDialog(dialogId, mapping, loader)) return null;
                for (Object nativeMessage : messages) {
                    ForwardingMessage message = toForwardingMessage(accountId, dialogId, nativeMessage, mapping);
                    if (message != null && coordinator != null) coordinator.onIncomingMessage(message);
                }
            } catch (Throwable ignored) { }
            return null;
        }
    }

    private static ForwardingMessage toForwardingMessage(int accountId, long dialogId,
                                                           Object messageObject, RuntimeProfile mapping) {
        if (messageObject == null) return null;
        try {
            Class<?> messageType = messageObject.getClass();
            Object owner = messageType.getField(mapping.messageOwnerField).get(messageObject);
            if (owner == null) return null;
            Class<?> ownerType = owner.getClass();
            if (ownerType.getField("out").getBoolean(owner)) return null;
            if (ownerType.getField("action").get(owner) != null) return null;
            boolean protectedContent = ownerType.getField("noforwards").getBoolean(owner);
            if (isSecretDialog(dialogId, mapping, messageType.getClassLoader())) return null;

            Object idValue = ownerType.getField("id").get(owner);
            int messageId = ((Number) idValue).intValue();
            CharSequence richText = null;
            try {
                Object value = messageType.getField(mapping.messageTextField).get(messageObject);
                if (value instanceof CharSequence) richText = (CharSequence) value;
            } catch (Throwable ignored) { }
            String text = richText == null ? "" : richText.toString();
            if (text.isEmpty()) {
                Object raw = ownerType.getField("message").get(owner);
                if (raw != null) text = String.valueOf(raw);
            }
            // A protected message may still enter COPY mode when the user's separate
            // content-restriction patch allows copying. FORWARD is always rejected.
            return new ForwardingMessage(accountId, dialogId, messageId, text,
                    false, false, false, protectedContent, !protectedContent, messageObject);
        } catch (Throwable ignored) { return null; }
    }

    private static final class NativeGateway implements TelegramMessageGateway {
        private final RuntimeProfile mapping;
        NativeGateway(int profile) { mapping = RuntimeProfile.forId(profile); }

        @Override public void forward(final int accountId, final ForwardingMessage source,
                                      final long destinationChatId, final boolean showSenderName,
                                      final ResultCallback callback) {
            dispatchOnMain(new Runnable() {
                @Override public void run() {
                    try {
                        Object nativeMessage = source.getNativeMessage();
                        if (nativeMessage == null) throw new IllegalStateException("Original Telegram message is unavailable");
                        ClassLoader loader = nativeMessage.getClass().getClassLoader();
                        Class<?> helperClass = Class.forName(mapping.sendHelperClass, false, loader);
                        Object helper = mapping.currentR8
                                ? helperClass.getMethod(mapping.sendHelperInstance, int.class).invoke(null, accountId)
                                : helperClass.getMethod("getInstance", int.class).invoke(null, accountId);
                        Method method = mapping.currentR8
                                ? currentForwardMethod(helperClass, nativeMessage.getClass(), mapping)
                                : legacyForwardMethod(helperClass, nativeMessage.getClass());
                        ArrayList<Object> messages = new ArrayList<>(Collections.singletonList(nativeMessage));
                        method.invoke(helper, messages, destinationChatId,
                                !showSenderName, false, false, 0, 0, null, -1, 0L, 0L, null);
                        if (callback != null) callback.onSuccess();
                    } catch (Throwable error) {
                        if (callback != null) callback.onFailure(unwrap(error));
                    }
                }
            });
        }

        @Override public void copy(final int accountId, final ForwardingMessage source,
                                   final long destinationChatId, final boolean showSenderName,
                                   final ResultCallback callback) {
            dispatchOnMain(new Runnable() {
                @Override public void run() {
                    try {
                        Object nativeMessage = source.getNativeMessage();
                        if (nativeMessage == null) throw new IllegalStateException("Original Telegram message is unavailable");
                        if (source.getText().trim().isEmpty()) throw new IllegalArgumentException("Only text messages can be copied");
                        ClassLoader loader = nativeMessage.getClass().getClassLoader();
                        if (mapping.currentR8) {
                            // Current 13.2.1 clients expose the native text-send path as
                            // SendMessagesHelper.a0(AccountInstance, CharSequence, topicId,
                            // dialogId, ..., flags). It converts Telegram spans to entities
                            // before enqueueing, preserving rich text and embedded links.
                            Class<?> accountClass = Class.forName(mapping.accountInstanceClass, false, loader);
                            Object accountInstance = accountClass.getMethod(mapping.accountInstanceFactory, int.class)
                                    .invoke(null, accountId);
                            Class<?> helperClass = Class.forName(mapping.sendHelperClass, false, loader);
                            Method copyMethod = helperClass.getMethod(mapping.copyMethod, accountClass,
                                    CharSequence.class, long.class, long.class, boolean.class, int.class, int.class);
                            Object richText;
                            try { richText = nativeMessage.getClass().getField(mapping.messageTextField).get(nativeMessage); }
                            catch (Throwable missingTextField) { richText = null; }
                            if (!(richText instanceof CharSequence)) richText = source.getText();
                            copyMethod.invoke(null, accountInstance, richText, 0L, destinationChatId, false, 0, 0);
                        } else {
                            Class<?> helperClass = Class.forName(mapping.sendHelperClass, false, loader);
                            Class<?> paramsClass = Class.forName(mapping.sendParamsClass, false, loader);
                            Object params = paramsClass.getMethod("of", String.class, long.class)
                                    .invoke(null, source.getText(), destinationChatId);
                            Object owner = nativeMessage.getClass().getField(mapping.messageOwnerField).get(nativeMessage);
                            Object entities = null;
                            try { entities = nativeMessage.getClass().getMethod("getEntities").invoke(nativeMessage); }
                            catch (Throwable ignored) { }
                            if (!(entities instanceof List)) entities = owner.getClass().getField("entities").get(owner);
                            if (entities instanceof List) paramsClass.getField("entities").set(params, new ArrayList<>((List<?>) entities));
                            Object helper = helperClass.getMethod("getInstance", int.class).invoke(null, accountId);
                            helperClass.getMethod("sendMessage", paramsClass).invoke(helper, params);
                        }
                        if (callback != null) callback.onSuccess();
                    } catch (Throwable error) {
                        if (callback != null) callback.onFailure(unwrap(error));
                    }
                }
            });
        }

        private static Method currentForwardMethod(Class<?> helperClass, Class<?> messageClass,
                                                   RuntimeProfile mapping) throws NoSuchMethodException, ClassNotFoundException {
            ClassLoader loader = helperClass.getClassLoader();
            Class<?> suggestionClass = Class.forName(mapping.suggestionParamsClass, false, loader);
            Method method = helperClass.getMethod(mapping.forwardMethod, ArrayList.class, long.class,
                    boolean.class, boolean.class, boolean.class, int.class, int.class,
                    messageClass, int.class, long.class, long.class, suggestionClass);
            method.setAccessible(true);
            return method;
        }

        private static Method legacyForwardMethod(Class<?> helperClass, Class<?> messageClass) throws NoSuchMethodException {
            for (Method method : helperClass.getMethods()) {
                Class<?>[] p = method.getParameterTypes();
                if (!"sendMessage".equals(method.getName()) || p.length != 12) continue;
                if (p[0] == ArrayList.class && p[1] == long.class && p[2] == boolean.class
                        && p[3] == boolean.class && p[4] == boolean.class && p[5] == int.class
                        && p[6] == int.class && p[7].isAssignableFrom(messageClass)
                        && p[8] == int.class && p[9] == long.class && p[10] == long.class) {
                    method.setAccessible(true);
                    return method;
                }
            }
            throw new NoSuchMethodException("Legacy native SendMessagesHelper forward overload not found");
        }

        private static void dispatchOnMain(Runnable runnable) {
            if (Looper.myLooper() == Looper.getMainLooper()) runnable.run();
            else MAIN.post(runnable);
        }
    }

    private static Throwable unwrap(Throwable error) {
        return error instanceof InvocationTargetException && ((InvocationTargetException) error).getCause() != null
                ? ((InvocationTargetException) error).getCause() : error;
    }

    private static final class RuntimeProfile {
        final boolean currentR8;
        final String applicationClass, applicationContextField;
        final String notificationClass, delegateClass, notificationGetInstance, notificationAddObserver;
        final int eventId, maxAccounts;
        final String messageOwnerField, messageTextField, dialogIdClass, dialogIdMethod;
        final String sendHelperClass, sendHelperInstance, forwardMethod, suggestionParamsClass;
        final String accountInstanceClass, accountInstanceFactory, copyMethod, sendParamsClass;

        RuntimeProfile(boolean currentR8, String applicationClass, String applicationContextField,
                       String notificationClass, String delegateClass, String notificationGetInstance,
                       String notificationAddObserver, int eventId, int maxAccounts,
                       String messageOwnerField, String messageTextField, String dialogIdClass,
                       String dialogIdMethod, String sendHelperClass,
                       String sendHelperInstance, String forwardMethod, String suggestionParamsClass,
                       String accountInstanceClass, String accountInstanceFactory, String copyMethod,
                       String sendParamsClass) {
            this.currentR8 = currentR8;
            this.applicationClass = applicationClass;
            this.applicationContextField = applicationContextField;
            this.notificationClass = notificationClass;
            this.delegateClass = delegateClass;
            this.notificationGetInstance = notificationGetInstance;
            this.notificationAddObserver = notificationAddObserver;
            this.eventId = eventId;
            this.maxAccounts = maxAccounts;
            this.messageOwnerField = messageOwnerField;
            this.messageTextField = messageTextField;
            this.dialogIdClass = dialogIdClass;
            this.dialogIdMethod = dialogIdMethod;
            this.sendHelperClass = sendHelperClass;
            this.sendHelperInstance = sendHelperInstance;
            this.forwardMethod = forwardMethod;
            this.suggestionParamsClass = suggestionParamsClass;
            this.accountInstanceClass = accountInstanceClass;
            this.accountInstanceFactory = accountInstanceFactory;
            this.copyMethod = copyMethod;
            this.sendParamsClass = sendParamsClass;
        }

        static RuntimeProfile forId(int id) {
            if (id == PROFILE_TELEGRAM_ANDROID_1321) return new RuntimeProfile(true,
                    "org.telegram.messenger.u0", "b", "org.telegram.messenger.ys",
                    "org.telegram.messenger.ts", "f", "b", 1, 4, "k", "v",
                    "org.telegram.messenger.i5", "C", "org.telegram.messenger.qz", "C", "q0", "org.telegram.messenger.qj",
                    "org.telegram.messenger.a", "a", "a0", null);
            if (id == PROFILE_TELEGRAM_WEB_1321) return new RuntimeProfile(true,
                    "org.telegram.messenger.t0", "b", "org.telegram.messenger.us",
                    "org.telegram.messenger.ps", "f", "b", 1, 4, "k", "v",
                    "org.telegram.messenger.h5", "C", "org.telegram.messenger.pz", "C", "q0", "org.telegram.messenger.lj",
                    "org.telegram.messenger.a", "a", "a0", null);
            return new RuntimeProfile(false,
                    "org.telegram.messenger.ApplicationLoader", "applicationContext",
                    "org.telegram.messenger.NotificationCenter",
                    "org.telegram.messenger.NotificationCenter$NotificationCenterDelegate",
                    "getInstance", "addObserver", 1, 4, "messageOwner", "messageText",
                    "org.telegram.messenger.DialogObject", "isEncryptedDialog",
                    "org.telegram.messenger.SendMessagesHelper", "getInstance", "sendMessage", "",
                    "", "", "", "org.telegram.messenger.SendMessagesHelper$SendMessageParams");
        }
    }
}
