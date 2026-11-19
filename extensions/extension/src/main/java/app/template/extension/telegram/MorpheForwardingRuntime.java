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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Connects Morphe forwarding rules to Telegram's already-authenticated account stack.
 * Current Telegram 13.2.2 Android/Web R8 names are selected by the patch's startup hook.
 * The no-argument entry point is retained for older, unobfuscated targets.
 */
public final class MorpheForwardingRuntime {
    private static final int PROFILE_UNSET = -2;
    private static final int PROFILE_LEGACY = -1;
    private static final int PROFILE_TELEGRAM_ANDROID_1322 = 0;
    private static final int PROFILE_TELEGRAM_WEB_1322 = 1;

    private static final ArrayList<Object> OBSERVERS = new ArrayList<>();
    private static final Set<Integer> REGISTERED_ACCOUNTS = new HashSet<>();
    private static volatile boolean installed;
    private static int activeProfile = PROFILE_UNSET;
    private static int installRetryCount;
    private static volatile String lastAccountRegistrationError = "";
    private static final int MAX_INSTALL_RETRIES = 20;
    private static volatile ForwardingCoordinator coordinator;
    private static volatile SharedPreferencesForwardingRuleStore store;
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean FIRST_INCOMING_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean PAYLOAD_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean MESSAGE_ADAPTER_ERROR_REPORTED = new AtomicBoolean(false);
    private static final AtomicBoolean SECRET_FILTER_ERROR_REPORTED = new AtomicBoolean(false);

    private MorpheForwardingRuntime() { }

    /** Legacy entry point kept for previously supported Telegram/Plus builds. */
    public static synchronized void install() {
        installInternal(PROFILE_LEGACY);
    }

    /** Called by the 13.2.2 Telegram Android/Web patch with its exact R8 profile. */
    public static synchronized void install(int profile) {
        if (profile != PROFILE_TELEGRAM_ANDROID_1322 && profile != PROFILE_TELEGRAM_WEB_1322) {
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
                updateRuntimeStatus("Waiting for Telegram application context");
                scheduleInstallRetry(profile);
                return;
            }
            MorpheRulesBackup.schedule(context);
            if (store == null) store = new SharedPreferencesForwardingRuleStore(context);
            updateRuntimeStatus("Registering Telegram message listener");
            if (coordinator == null) {
                // Keep event matching ordered, but isolate the two delivery modes. A slow
                // FORWARD operation must not stall COPY rules and vice versa. Every queue is
                // bounded; rejected work is counted by the coordinator instead of disappearing.
                ThreadPoolExecutor eventExecutor = createQueue("MorpheEvents", 256);
                ThreadPoolExecutor forwardExecutor = createQueue("MorpheForward", 128);
                ThreadPoolExecutor copyExecutor = createQueue("MorpheCopy", 128);
                coordinator = new ForwardingCoordinator(store, new NativeGateway(profile),
                        eventExecutor, forwardExecutor, copyExecutor);
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
                    // Surface the failure class: otherwise a broken account slot looks like
                    // a healthy listener with fewer accounts and is impossible to diagnose
                    // without ADB/logcat.
                    lastAccountRegistrationError = unwrap(accountError).getClass().getSimpleName();
                }
            }
            installed = REGISTERED_ACCOUNTS.size() >= maxAccounts;
            if (!installed) {
                String status = "Listener partially registered: " + REGISTERED_ACCOUNTS.size() + "/" + maxAccounts + " accounts";
                if (!lastAccountRegistrationError.isEmpty()) status += "; last error: " + lastAccountRegistrationError;
                updateRuntimeStatus(status);
                scheduleInstallRetry(profile);
            } else {
                installRetryCount = 0;
                lastAccountRegistrationError = "";
                updateRuntimeStatus("Ready — listening on " + REGISTERED_ACCOUNTS.size() + " Telegram accounts");
            }
        } catch (Throwable error) {
            // A version mismatch must never be allowed to interrupt Telegram startup.
            installed = false;
            updateRuntimeStatus("Listener setup failed: " + unwrap(error).getClass().getSimpleName());
            scheduleInstallRetry(profile);
        }
    }

    private static ThreadPoolExecutor createQueue(final String name, int capacity) {
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(capacity), command -> {
                    Thread thread = new Thread(command, name);
                    thread.setPriority(Thread.NORM_PRIORITY - 1);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    private static void scheduleInstallRetry(final int profile) {
        if (installRetryCount >= MAX_INSTALL_RETRIES) return;
        installRetryCount++;
        MAIN.postDelayed(new Runnable() {
            @Override public void run() { MorpheForwardingRuntime.installInternal(profile); }
        }, 15000L);
    }

    private static void updateRuntimeStatus(String status) {
        SharedPreferencesForwardingRuleStore current = store;
        if (current != null) current.setRuntimeStatus(status);
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
        } catch (Throwable error) {
            // Fail closed: if this client variant's secret-chat predicate cannot be resolved,
            // do not pass the message to the automation engine. Surface the reason in Settings.
            if (SECRET_FILTER_ERROR_REPORTED.compareAndSet(false, true)) {
                updateRuntimeStatus("Safety filter unavailable; automation paused: " + unwrap(error).getClass().getSimpleName());
            }
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
                if (payload.length < 2 || !(payload[0] instanceof Number)) {
                    if (PAYLOAD_ERROR_REPORTED.compareAndSet(false, true)) {
                        updateRuntimeStatus("Listener active but Telegram message payload was unexpected");
                    }
                    return null;
                }
                // NotificationCenter event 1 normally carries
                // [dialogId, ArrayList<MessageObject>, scheduled, ...]. Accept a single
                // MessageObject as well: some Telegram update paths emit one object rather
                // than a grouped list. Scheduled updates are deliberately not automated.
                if (payload.length > 2 && Boolean.TRUE.equals(payload[2])) return null;
                long dialogId = ((Number) payload[0]).longValue();
                List<?> messages;
                if (payload[1] instanceof List) {
                    messages = (List<?>) payload[1];
                } else if (isNativeMessageObject(payload[1], mapping)) {
                    messages = Collections.singletonList(payload[1]);
                } else {
                    if (PAYLOAD_ERROR_REPORTED.compareAndSet(false, true)) {
                        updateRuntimeStatus("Listener active but Telegram message object was unexpected");
                    }
                    return null;
                }
                if (messages.isEmpty()) return null;
                Object firstMessage = null;
                for (Object candidate : messages) if (candidate != null) { firstMessage = candidate; break; }
                if (firstMessage == null) return null;
                ClassLoader loader = firstMessage.getClass().getClassLoader();
                if (isSecretDialog(dialogId, mapping, loader)) return null;
                int converted = 0;
                for (Object nativeMessage : messages) {
                    ForwardingMessage message = toForwardingMessage(accountId, dialogId, nativeMessage, mapping);
                    if (message != null) {
                        converted++;
                        if (coordinator != null) coordinator.onIncomingMessage(message);
                    }
                }
                if (converted > 0 && FIRST_INCOMING_REPORTED.compareAndSet(false, true)) {
                    updateRuntimeStatus("Listener active — Telegram incoming messages detected");
                }
            } catch (Throwable error) {
                if (PAYLOAD_ERROR_REPORTED.compareAndSet(false, true)) {
                    updateRuntimeStatus("Listener update failed: " + unwrap(error).getClass().getSimpleName());
                }
            }
            return null;
        }
    }

    private static boolean isNativeMessageObject(Object value, RuntimeProfile mapping) {
        if (value == null) return false;
        try {
            value.getClass().getField(mapping.messageOwnerField);
            return true;
        } catch (Throwable ignored) { return false; }
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
            // FORWARD follows Telegram's own eligibility predicate. COPY remains a
            // separate text-resend operation and does not claim to be a native forward.
            boolean canForward = false;
            try {
                canForward = Boolean.TRUE.equals(messageType.getMethod(mapping.messageCanForwardMethod).invoke(messageObject));
            } catch (Throwable ignored) { /* fail closed for forward mode */ }
            boolean stickerOrEmojiOnly = (isStickerMessage(owner) && text.trim().isEmpty())
                    || (!text.trim().isEmpty() && isEmojiOnlyText(text) && !hasMedia(owner));
            // The Bypass content restrictions patch changes Telegram's native noforwards
            // predicates/field reads. Trust MessageObject.canForwardMessage() after that
            // patch: it retains Telegram's other eligibility checks while allowing
            // protected channel messages. Do not reapply the raw noforwards bit here.
            return new ForwardingMessage(accountId, dialogId, messageId, text,
                    false, false, false, protectedContent, canForward,
                    messageObject, stickerOrEmojiOnly);
        } catch (Throwable error) {
            if (MESSAGE_ADAPTER_ERROR_REPORTED.compareAndSet(false, true)) {
                updateRuntimeStatus("Message adapter failed: " + unwrap(error).getClass().getSimpleName());
            }
            return null;
        }
    }


    private static boolean isStickerMessage(Object owner) {
        try {
            Field mediaField = findField(owner.getClass(), "media");
            if (mediaField == null) return false;
            mediaField.setAccessible(true);
            Object media = mediaField.get(owner);
            if (media == null) return false;
            if (media.getClass().getName().toLowerCase(java.util.Locale.ROOT).contains("sticker")) return true;
            Field documentField = findField(media.getClass(), "document");
            if (documentField == null) return false;
            documentField.setAccessible(true);
            Object document = documentField.get(media);
            if (document == null) return false;
            Field attributesField = findField(document.getClass(), "attributes");
            if (attributesField == null) return false;
            attributesField.setAccessible(true);
            Object attributes = attributesField.get(document);
            if (!(attributes instanceof Iterable)) return false;
            for (Object attribute : (Iterable<?>) attributes) {
                if (attribute != null && (attribute.getClass().getName().toLowerCase(java.util.Locale.ROOT).contains("sticker")
                        || findField(attribute.getClass(), "stickerset") != null)) return true;
            }
        } catch (Throwable ignored) { }
        return false;
    }

    private static boolean hasMedia(Object owner) {
        try { Field f = findField(owner.getClass(), "media"); if (f == null) return false; f.setAccessible(true); return f.get(owner) != null; }
        catch (Throwable ignored) { return false; }
    }

    private static boolean isEmojiOnlyText(String value) {
        return MorpheMessageContentFilter.isEmojiOnlyText(value);
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try { return c.getDeclaredField(name); } catch (Throwable ignored) { }
        }
        return null;
    }

    private static final class NativeGateway implements TelegramMessageGateway {
        private final RuntimeProfile mapping;
        NativeGateway(int profile) { mapping = RuntimeProfile.forId(profile); }

        @Override public void forward(final int accountId, final ForwardingMessage source,
                                      final long destinationChatId, final boolean showSenderName,
                                      final ResultCallback callback) {
            // FORWARD alone uses Telegram's native forwardMessages RPC. The copy path
            // below never enters this method or the q0(...) overload.
            enqueueNativeForward(accountId, source, destinationChatId,
                    false, !showSenderName, callback);
        }

        @Override public void copy(final int accountId, final ForwardingMessage source,
                                   final long destinationChatId, final boolean showSenderName,
                                   final ResultCallback callback) {
            // COPY is a new outgoing message, not a forward with drop_author set.
            // On current R8 builds SendMessageParams.of(MessageObject) is preferred
            // because it retains the original entity/media model. If that factory is
            // unavailable, the verified a0(AccountInstance, CharSequence, topicId,
            // dialogId, ...) text-send path preserves Spanned URL entities and lets
            // Telegram resolve the webpage preview again from the original URL.
            dispatchOnMain(new Runnable() {
                @Override public void run() {
                    try {
                        Object nativeMessage = source.getNativeMessage();
                        if (nativeMessage == null) throw new IllegalStateException("Original Telegram message is unavailable");
                        ClassLoader loader = nativeMessage.getClass().getClassLoader();
                        Class<?> helperClass = Class.forName(mapping.sendHelperClass, false, loader);
                        Object helper = helperClass.getMethod(mapping.sendHelperInstance, int.class).invoke(null, accountId);
                        if (mapping.currentR8 && isTextOrWebPagePreview(nativeMessage, mapping)) {
                            // Use Telegram's composer text-send path for text and webpage
                            // previews. It consumes the original Spanned CharSequence,
                            // turns URLSpan/formatting spans into MessageEntity objects,
                            // and leaves webpage preview generation enabled. This mirrors
                            // the successful manual copy/paste behaviour.
                            CharSequence richText = readNativeRichText(nativeMessage, mapping);
                            if (richText == null || richText.length() == 0) {
                                throw new IllegalStateException("Original Telegram rich text is unavailable for COPY");
                            }
                            Class<?> accountClass = Class.forName(mapping.accountInstanceClass, false, loader);
                            Object account = accountClass.getMethod(mapping.accountInstanceFactory, int.class).invoke(null, accountId);
                            Method textCopy = helperClass.getDeclaredMethod(mapping.copyMethod, accountClass,
                                    CharSequence.class, long.class, long.class, boolean.class, int.class, int.class);
                            textCopy.setAccessible(true);
                            // Telegram a0 parameter order: AccountInstance, text, topicId,
                            // destination dialogId, disableWebPagePreview, scheduleDate, flags.
                            textCopy.invoke(helper, account, richText, 0L, destinationChatId, false, 0, 0);
                        } else if (mapping.currentR8) {
                            // Media and caption messages use Telegram's native new-message
                            // params factory. If it cannot be mapped, fail rather than
                            // dropping media or silently turning COPY into FORWARD.
                            if (!trySendParamsCopy(helperClass, helper, nativeMessage,
                                    destinationChatId, source.getSourceChatId())) {
                                throw new NoSuchMethodException("Native media SendMessageParams copy factory not found or ambiguous");
                            }
                        } else if (!tryLegacySendParamsCopy(helperClass, helper, nativeMessage, destinationChatId)) {
                            throw new NoSuchMethodException("Legacy SendMessageParams copy factory not found");
                        }
                        if (callback != null) callback.onQueued();
                    } catch (Throwable error) {
                        updateRuntimeStatus("Copy request failed: " + unwrap(error).getClass().getSimpleName());
                        if (callback != null) callback.onFailure(unwrap(error));
                    }
                }
            }, callback, "Copy");
        }

        private void enqueueNativeForward(final int accountId, final ForwardingMessage source,
                                          final long destinationChatId, final boolean dropMedia,
                                          final boolean dropAuthor, final ResultCallback callback) {
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
                                ? currentForwardMethod(helperClass, mapping)
                                : legacyForwardMethod(helperClass, nativeMessage.getClass());
                        ArrayList<Object> messages = new ArrayList<>(Collections.singletonList(nativeMessage));
                        // This method is FORWARD-only. The current R8 q0 overload has
                        // separate dropMedia and dropAuthor arguments; COPY never reaches it.
                        boolean firstFlag = mapping.currentR8 ? dropMedia : dropAuthor;
                        boolean secondFlag = mapping.currentR8 ? dropAuthor : false;
                        method.invoke(helper, messages, destinationChatId,
                                firstFlag, secondFlag, false, 0, 0, null, -1, 0L, 0L, null);
                        // q0 returns a local message identifier (Int), not a Boolean
                        // success flag. Do not reject zero/negative IDs: Telegram's send
                        // queue may legitimately use them. A thrown exception is the
                        // reliable indication that invocation failed before local enqueue.
                        if (callback != null) callback.onQueued();
                    } catch (Throwable error) {
                        updateRuntimeStatus("Forward request failed: " + unwrap(error).getClass().getSimpleName());
                        if (callback != null) callback.onFailure(unwrap(error));
                    }
                }
            }, callback, "Forward");
        }

        private static CharSequence readNativeRichText(Object nativeMessage, RuntimeProfile mapping) {
            try {
                Field field = findField(nativeMessage.getClass(), mapping.messageTextField);
                if (field == null) return null;
                field.setAccessible(true);
                Object value = field.get(nativeMessage);
                return value instanceof CharSequence ? (CharSequence) value : null;
            } catch (Throwable ignored) { return null; }
        }

        private static boolean isTextOrWebPagePreview(Object nativeMessage, RuntimeProfile mapping) {
            try {
                Field ownerField = findField(nativeMessage.getClass(), mapping.messageOwnerField);
                if (ownerField == null) return false;
                ownerField.setAccessible(true);
                Object owner = ownerField.get(nativeMessage);
                if (owner == null) return false;
                Field mediaField = findField(owner.getClass(), "media");
                if (mediaField == null) return true;
                mediaField.setAccessible(true);
                Object media = mediaField.get(owner);
                if (media == null) return true;
                String mediaName = media.getClass().getName().toLowerCase(java.util.Locale.ROOT);
                return mediaName.contains("webpage") || findField(media.getClass(), "webpage") != null;
            } catch (Throwable ignored) { return false; }
        }

        /** Finds the current R8-renamed SendMessageParams factory by its native input type. */
        private static boolean trySendParamsCopy(Class<?> helperClass, Object helper, Object nativeMessage,
                                                  long destinationChatId, long sourceDialogId) throws Exception {
            Class<?> messageClass = nativeMessage.getClass();
            Class<?> paramsClass = null;
            Method factory = null;
            for (Class<?> nested : helperClass.getDeclaredClasses()) {
                for (Method candidate : nested.getDeclaredMethods()) {
                    if (!java.lang.reflect.Modifier.isStatic(candidate.getModifiers())
                            || !"of".equals(candidate.getName()) || candidate.getParameterTypes().length != 1
                            || !candidate.getParameterTypes()[0].isAssignableFrom(messageClass)) continue;
                    if (factory != null) throw new IllegalStateException("Ambiguous native SendMessageParams factory");
                    factory = candidate;
                    paramsClass = nested;
                }
            }
            if (factory == null || paramsClass == null) return false;
            factory.setAccessible(true);
            Object params = factory.invoke(null, nativeMessage);
            Field peer = findField(paramsClass, "peer");
            if (peer != null && peer.getType() == long.class) {
                peer.setAccessible(true);
                peer.setLong(params, destinationChatId);
            } else {
                // Some builds rename the peer field. Only rewrite a unique long field
                // whose original value equals the source dialog; never guess among fields.
                Field candidate = null;
                for (Class<?> type = paramsClass; type != null && type != Object.class; type = type.getSuperclass()) {
                    for (Field field : type.getDeclaredFields()) {
                        if (field.getType() != long.class) continue;
                        field.setAccessible(true);
                        if (field.getLong(params) == sourceDialogId) {
                            if (candidate != null) throw new IllegalStateException("Ambiguous source dialog field in copy parameters");
                            candidate = field;
                        }
                    }
                }
                if (candidate == null) return false;
                candidate.setLong(params, destinationChatId);
            }
            Method send = null;
            for (Method candidate : helperClass.getMethods()) {
                if ("sendMessage".equals(candidate.getName())
                        && candidate.getParameterTypes().length == 1
                        && candidate.getParameterTypes()[0] == paramsClass) {
                    if (send != null) throw new IllegalStateException("Ambiguous native sendMessage(params) overload");
                    send = candidate;
                }
            }
            if (send == null) return false;
            send.setAccessible(true);
            send.invoke(helper, params);
            return true;
        }

        private static boolean tryLegacySendParamsCopy(Class<?> helperClass, Object helper,
                                                        Object nativeMessage, long destinationChatId) throws Exception {
            ClassLoader loader = nativeMessage.getClass().getClassLoader();
            Class<?> paramsClass = Class.forName("org.telegram.messenger.SendMessagesHelper$SendMessageParams", false, loader);
            Method factory = paramsClass.getMethod("of", nativeMessage.getClass());
            Object params = factory.invoke(null, nativeMessage);
            Field peer = findField(paramsClass, "peer");
            if (peer == null || peer.getType() != long.class) return false;
            peer.setAccessible(true);
            peer.setLong(params, destinationChatId);
            Method send = helperClass.getMethod("sendMessage", paramsClass);
            send.invoke(helper, params);
            return true;
        }

        private static Method currentForwardMethod(Class<?> helperClass,
                                                   RuntimeProfile mapping) throws NoSuchMethodException, ClassNotFoundException {
            ClassLoader loader = helperClass.getClassLoader();
            Class<?> forwardOptionsClass = Class.forName(mapping.forwardOptionsClass, false, loader);
            Class<?> suggestionClass = Class.forName(mapping.suggestionParamsClass, false, loader);
            // 13.2.2 Android uses SendMessagesHelper.q0(..., mj, ..., rj); Web
            // uses q0(..., hj, ..., mj). The intermediate parameter is a topic/reply
            // descriptor, not MessageObject. Matching MessageObject here made reflection
            // fail before Telegram could enqueue any channel post.
            Method method = helperClass.getMethod(mapping.forwardMethod, ArrayList.class, long.class,
                    boolean.class, boolean.class, boolean.class, int.class, int.class,
                    forwardOptionsClass, int.class, long.class, long.class, suggestionClass);
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

        private static void dispatchOnMain(Runnable runnable, ResultCallback callback, String operation) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                runnable.run();
            } else if (!MAIN.post(runnable)) {
                IllegalStateException rejected = new IllegalStateException(
                        "Telegram main-thread queue rejected the " + operation.toLowerCase(java.util.Locale.ROOT) + " request");
                updateRuntimeStatus(rejected.getMessage());
                if (callback != null) callback.onFailure(rejected);
            }
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
        final String messageOwnerField, messageTextField, messageCanForwardMethod, dialogIdClass, dialogIdMethod;
        final String sendHelperClass, sendHelperInstance, forwardMethod, forwardOptionsClass, suggestionParamsClass;
        final String accountInstanceClass, accountInstanceFactory, copyMethod, sendParamsClass;

        RuntimeProfile(boolean currentR8, String applicationClass, String applicationContextField,
                       String notificationClass, String delegateClass, String notificationGetInstance,
                       String notificationAddObserver, int eventId, int maxAccounts,
                       String messageOwnerField, String messageTextField, String messageCanForwardMethod,
                       String dialogIdClass, String dialogIdMethod, String sendHelperClass,
                       String sendHelperInstance, String forwardMethod, String forwardOptionsClass,
                       String suggestionParamsClass, String accountInstanceClass, String accountInstanceFactory, String copyMethod,
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
            this.messageCanForwardMethod = messageCanForwardMethod;
            this.dialogIdClass = dialogIdClass;
            this.dialogIdMethod = dialogIdMethod;
            this.sendHelperClass = sendHelperClass;
            this.sendHelperInstance = sendHelperInstance;
            this.forwardMethod = forwardMethod;
            this.forwardOptionsClass = forwardOptionsClass;
            this.suggestionParamsClass = suggestionParamsClass;
            this.accountInstanceClass = accountInstanceClass;
            this.accountInstanceFactory = accountInstanceFactory;
            this.copyMethod = copyMethod;
            this.sendParamsClass = sendParamsClass;
        }

        static RuntimeProfile forId(int id) {
            if (id == PROFILE_TELEGRAM_ANDROID_1322) return new RuntimeProfile(true,
                    "org.telegram.messenger.u0", "b", "org.telegram.messenger.zs",
                    "org.telegram.messenger.us", "f", "b", 1, 4, "k", "v", "w",
                    "org.telegram.messenger.i5", "D", "org.telegram.messenger.rz", "C", "q0", "org.telegram.messenger.mj", "org.telegram.messenger.rj",
                    "org.telegram.messenger.a", "a", "a0", null);
            if (id == PROFILE_TELEGRAM_WEB_1322) return new RuntimeProfile(true,
                    "org.telegram.messenger.t0", "b", "org.telegram.messenger.vs",
                    "org.telegram.messenger.qs", "f", "b", 1, 4, "k", "v", "w",
                    "org.telegram.messenger.h5", "D", "org.telegram.messenger.rz", "C", "q0", "org.telegram.messenger.hj", "org.telegram.messenger.mj",
                    "org.telegram.messenger.a", "a", "a0", null);
            return new RuntimeProfile(false,
                    "org.telegram.messenger.ApplicationLoader", "applicationContext",
                    "org.telegram.messenger.NotificationCenter",
                    "org.telegram.messenger.NotificationCenter$NotificationCenterDelegate",
                    "getInstance", "addObserver", 1, 4, "messageOwner", "messageText", "canForwardMessage",
                    "org.telegram.messenger.DialogObject", "isEncryptedDialog",
                    "org.telegram.messenger.SendMessagesHelper", "getInstance", "sendMessage", "", "",
                    "", "", "", "org.telegram.messenger.SendMessagesHelper$SendMessageParams");
        }
    }
}
