package app.template.patches.telegram

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

// ════════════════════════════════════════════════════════════════════════════════
// Telegram shared fingerprints
// Historical named-class and older obfuscation fingerprints retained for backward compatibility.
// Earlier audit targets include Telegram 13.1.1 build 72371, Telegram Web 13.1.1 build 72379,
// and Plus Messenger 13.1.1.0 build 22621. Current 13.2.1 R8 targets are additive in the current-R8 section below. Earlier descriptors remain separately named legacy
// fingerprints; Constants.kt exposes only the three current APK targets.
// Distinct legacy descriptors remain available where their signatures differ. A descriptor found only
// in a reference table is not considered a patchable method unless a local method body exists.
// Several old UI owners are R8-renamed in these builds; their old named fingerprints stay below
// for compatibility but must not be treated as current verified targets. AccessFlags are
// omitted where APK variants differ.
// ════════════════════════════════════════════════════════════════════════════════

// ─── Premium ──────────────────────────────────────────────────────────────────
// StoriesControllerIsPremiumFingerprint is a legacy named-class candidate: controller return
// descriptors differ across Telegram, Web, and Plus.

val UserConfigIsPremiumFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/UserConfig;",
    name = "isPremium",
    returnType = "Z",
)

val MessagesControllerIsPremiumUserFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "isPremiumUser",
    returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$User;"),
)

// Plus Messenger 13.1.1.0: verified update UI method. The core premium and translation
// gates are separately verified in the current Plus APK; this remains an optional UI hook.
val PlusUpdateButtonUpdateFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Components/UpdateButton;",
    name = "update",
    returnType = "V",
    parameters = listOf("Z"),
)


// ─── Premium translation feature gates (Telegram/Web 13.1.1, Plus 13.1.1.0) ──
// Instruction audit: both methods return translations_*_enabled != "disabled".
// Their values are populated from app config; these hooks force the client-side
// feature gates open. Server-side eligibility / translation service responses are
// not changed by this patch.
val MessagesControllerIsTranslationsManualEnabledFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "isTranslationsManualEnabled",
    returnType = "Z",
    parameters = listOf(),
)

val MessagesControllerIsTranslationsAutoEnabledFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "isTranslationsAutoEnabled",
    returnType = "Z",
    parameters = listOf(),
)

// ─── Integrity bypass ─────────────────────────────────────────────────────────

val AndroidUtilitiesGetCertFingerprintFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/AndroidUtilities;",
    name = "getCertificateSHA256Fingerprint",
    returnType = "Ljava/lang/String;",
)

val SafetyNetCheckFingerprint = Fingerprint(
    filters = listOf(
        string("basicIntegrity"),
        string("ctsProfileMatch"),
        methodCall(
            definingClass = "Lorg/json/JSONObject;",
            name = "optBoolean",
            returnType = "Z",
        ),
    ),
    // Match the actual boolean reads, not a method that merely logs these words.
    // All three supplied builds have a key const-string immediately followed by
    // JSONObject.optBoolean(String)Z and move-result.
    custom = { method, _ ->
        val instructions = method.implementation?.instructions?.toList() ?: emptyList()
        val keys = instructions.mapIndexedNotNull { index, instruction ->
            if (instruction.opcode != Opcode.CONST_STRING) return@mapIndexedNotNull null
            val string = (instruction as? ReferenceInstruction)?.reference as? StringReference
            val invoke = instructions.getOrNull(index + 1)
            val methodRef = (invoke as? ReferenceInstruction)?.reference as? MethodReference
            val result = instructions.getOrNull(index + 2)
            if (string?.string in setOf("basicIntegrity", "ctsProfileMatch") &&
                invoke?.opcode == Opcode.INVOKE_VIRTUAL &&
                methodRef != null &&
                methodRef.definingClass == "Lorg/json/JSONObject;" &&
                methodRef.name == "optBoolean" &&
                result?.opcode == Opcode.MOVE_RESULT
            ) string?.string else null
        }
        keys.toSet() == setOf("basicIntegrity", "ctsProfileMatch")
    },
)

// ─── Ads ──────────────────────────────────────────────────────────────────────
// UI ad fingerprints are retained candidates; named-method matches need body/caller validation.

val MessagesControllerIsSponsoredDisabledFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "isSponsoredDisabled",
    returnType = "Z",
)

val MessageObjectIsSponsoredFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isSponsored",
    returnType = "Z",
)

val VideoAdsLoadFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/video/VideoAds;",
    name = "load",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PRIVATE),
)

// ─── Auto-update ──────────────────────────────────────────────────────────────
// Legacy UI/update names must not be redirected to obfuscated methods without body-level proof.

val SharedConfigIsAppUpdateAvailableFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/SharedConfig;",
    name = "isAppUpdateAvailable",
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
)

// Web: setNewAppVersionAvailable(TL_help_appUpdate)Z static
// Plus: same sig — both return Z so returnType = "Z" is safe
val SharedConfigSetNewAppVersionAvailableFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/SharedConfig;",
    name = "setNewAppVersionAvailable",
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$TL_help_appUpdate;"),
)

val MessagesControllerCheckPromoInfoInternalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkPromoInfoInternal",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PRIVATE),
    parameters = listOf("Z"),
)

// ─── No-forwards controller/message targets ───────────────────────────────────

val MessagesControllerIsChatNoForwardsLongFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "isChatNoForwards",
    returnType = "Z",
    parameters = listOf("J"),
)

val MessagesControllerIsChatNoForwardsChatFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "isChatNoForwards",
    returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$Chat;"),
)

val MessagesControllerIsPeerNoForwardsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "isPeerNoForwards",
    returnType = "Z",
    parameters = listOf("J"),
)

val CanForwardMessageFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "canForwardMessage",
    returnType = "Z",
    parameters = listOf(),
)

// ─── Channel restriction controller/message targets ───────────────────────────

val GetRestrictionReasonFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "getRestrictionReason",
    returnType = "Ljava/lang/String;",
    parameters = listOf("Ljava/util/ArrayList;"),
)

val MessagesControllerIsSensitiveFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "isSensitive",
    returnType = "Z",
    parameters = listOf("Ljava/util/ArrayList;"),
)

val CheckChannelErrorFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkChannelError",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "J"),
)

val ShowSensitiveContentFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "showSensitiveContent",
    returnType = "Z",
    parameters = listOf(),
)

val MessageObjectIsSensitiveFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isSensitive",
    returnType = "Z",
    parameters = listOf(),
)

val MessageObjectIsHiddenSensitiveFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isHiddenSensitive",
    returnType = "Z",
    parameters = listOf(),
)

val CreateNoAccessAlertFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Components/AlertsCreator;",
    name = "createNoAccessAlert",
    returnType = "Lorg/telegram/ui/ActionBar/AlertDialog\$Builder;",
)

val LoadFullChatErrorFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    returnType = "V",
    // 12.9.2: private synthetic lambda, params = (TL_error;J)
    // 12.10.0: R8 promoted to public static synthetic, added MessagesController as first param
    // Omit parameters so both generations match.
    filters = listOf(
        methodCall(
            definingClass = "Lorg/telegram/messenger/MessagesController;",
            name = "checkChannelError",
        ),
    ),
    // R8 name is unstable — exclude the NotificationCenter variant (GetChannelDiff)
    // by ensuring HashSet.remove is present in the body
    // LoadFullChat lambda calls HashSet.remove; GetChannelDiff calls postNotificationName.
    // This distinguishes the two without relying on unstable R8-generated method names.
    custom = { method, _ ->
        method.implementation?.instructions?.any { instr ->
            val ref = (instr as? ReferenceInstruction)?.reference as? MethodReference
            ref?.definingClass == "Ljava/util/HashSet;" && ref.name == "remove"
        } == true
    },
)

val GetChannelDiffErrorFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    returnType = "V",
    // 12.9.2: private synthetic lambda, params = (TL_error;J)
    // 12.10.0: R8 promoted to public static synthetic; params = (MessagesController;TL_error;J)
    // Omit parameters so both generations match.
    filters = listOf(
        methodCall(
            definingClass = "Lorg/telegram/messenger/MessagesController;",
            name = "checkChannelError",
        ),
        methodCall(
            definingClass = "Lorg/telegram/messenger/NotificationCenter;",
            name = "postNotificationName",
        ),
    ),
)

val SetContentSettingsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "setContentSettings",
    returnType = "V",
    parameters = listOf("Z"),
)

// Legacy 13.0.2 four-argument overloads (kept for older APKs): Telegram uses Lfe/e;, Web uses Lie/e;, and Plus uses Ldj/e$c;.
// Each descriptor is confirmed in its corresponding clean APK; the old Plus Lej/e$c; stays legacy-only.
val CheckCanOpenChat4NormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf(
        "Landroid/os/Bundle;",
        "Lorg/telegram/ui/ActionBar/s2;",
        "Lorg/telegram/messenger/MessageObject;",
        "Lfe/e;",
    ),
)

val CheckCanOpenChat4WebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf(
        "Landroid/os/Bundle;",
        "Lorg/telegram/ui/ActionBar/s2;",
        "Lorg/telegram/messenger/MessageObject;",
        "Lie/e;",
    ),
)

val CheckCanOpenChat4Plus1302Fingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf(
        "Landroid/os/Bundle;",
        "Lorg/telegram/ui/ActionBar/i2;",
        "Lorg/telegram/messenger/MessageObject;",
        "Ldj/e\$c;",
    ),
)

// Telegram/Web channel-restriction targets. Identical historical/current descriptors are consolidated.
val ShowCantOpenAlertNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "showCantOpenAlert",
    returnType = "V",
    parameters = listOf("Lorg/telegram/ui/ActionBar/r2;", "Ljava/lang/String;"),
)

val CheckSensitiveNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkSensitive",
    returnType = "V",
    parameters = listOf("Lorg/telegram/ui/ActionBar/r2;", "J", "Ljava/lang/Runnable;", "Ljava/lang/Runnable;"),
)

val CheckCanOpenChat2NormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf("Landroid/os/Bundle;", "Lorg/telegram/ui/ActionBar/r2;"),
)

val CheckCanOpenChat3NormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf("Landroid/os/Bundle;", "Lorg/telegram/ui/ActionBar/r2;", "Lorg/telegram/messenger/MessageObject;"),
)

val CheckCanOpenChat4NormalCurrentFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf("Landroid/os/Bundle;", "Lorg/telegram/ui/ActionBar/r2;", "Lorg/telegram/messenger/MessageObject;", "Lge/e;"),
)

val CheckCanOpenChat4WebCurrentFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf("Landroid/os/Bundle;", "Lorg/telegram/ui/ActionBar/r2;", "Lorg/telegram/messenger/MessageObject;", "Lje/e;"),
)


// Plus Messenger descriptor shared by the current and historical builds.
val ShowCantOpenAlertPlusFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "showCantOpenAlert",
    returnType = "V",
    parameters = listOf("Lorg/telegram/ui/ActionBar/i2;", "Ljava/lang/String;"),
)


// Plus Messenger descriptor shared by the current and historical builds.
val CheckSensitivePlusFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkSensitive",
    returnType = "V",
    parameters = listOf("Lorg/telegram/ui/ActionBar/i2;", "J", "Ljava/lang/Runnable;", "Ljava/lang/Runnable;"),
)


// Plus Messenger descriptor shared by the current and historical builds.
val CheckCanOpenChat2PlusFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf("Landroid/os/Bundle;", "Lorg/telegram/ui/ActionBar/i2;"),
)


// Plus Messenger descriptor shared by the current and historical builds.
val CheckCanOpenChat3PlusFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf("Landroid/os/Bundle;", "Lorg/telegram/ui/ActionBar/i2;", "Lorg/telegram/messenger/MessageObject;"),
)

// Legacy Plus Messenger 13.0.3.0 build 22608 uses Lej/e$c; for the four-argument overload.
// The older 13.0.2 descriptor Ldj/e$c; is retained separately below.

// Plus Messenger descriptor shared by the current and historical builds.
val CheckCanOpenChat4PlusFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf("Landroid/os/Bundle;", "Lorg/telegram/ui/ActionBar/i2;", "Lorg/telegram/messenger/MessageObject;", "Lej/e\$c;"),
)

// Telegram 13.0.3 build 71831 changed the standard UI fragment from ActionBar/s2 to ActionBar/r2.
// Telegram Web 13.0.3 build 71839 retains ActionBar/s2 but changed its final callback type from ie/e to je/e.





val CheckCanOpenChat4Web1303Fingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf(
        "Landroid/os/Bundle;", "Lorg/telegram/ui/ActionBar/s2;", "Lorg/telegram/messenger/MessageObject;", "Lje/e;",
    ),
)


// ─── Anti-delete ──────────────────────────────────────────────────────────────

val MarkMessagesAsDeletedFingerprint1 = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesStorage;",
    name = "markMessagesAsDeleted",
    returnType = "Ljava/util/ArrayList;",
    parameters = listOf("J", "I", "Z", "Z"),
)

val MarkMessagesAsDeletedFingerprint2 = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesStorage;",
    name = "markMessagesAsDeleted",
    returnType = "Ljava/util/ArrayList;",
    parameters = listOf("J", "Ljava/util/ArrayList;", "Z", "Z", "I", "I"),
)

// Telegram/Web body signatures are confirmed. The Plus descriptor is reference-level only in
// the supplied APK; omit accessFlags but do not treat this as a confirmed Plus patch target.
val DeleteMessagesByPushFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "deleteMessagesByPush",
    returnType = "V",
    parameters = listOf("J", "Ljava/util/ArrayList;", "J"),
)

// Current Plus Messenger 13.1.1.0 (build 22621) has no locally declared
// deleteMessagesByPush(J, ArrayList, J)V method. The equivalent push-deletion
// worker is the verified synthetic method below: its body schedules a UI runnable,
// calls MessagesStorage.deletePushMessages(J, ArrayList), then
// MessagesStorage.markMessagesAsDeleted(J, ArrayList, Z, Z, I, I). Keep the
// named target above for Telegram/Web and any legacy build where its local body exists; use this exact
// current Plus descriptor only as the Plus fallback.
val MessagesControllerDeletePushMessagesPlusFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "m3",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
    parameters = listOf(
        "Lorg/telegram/messenger/MessagesController;",
        "Ljava/util/ArrayList;",
        "J",
        "J",
    ),
)


// Telegram and Telegram Web 13.0.2 declare this method locally with Lz/f;.
val NotificationsControllerRemoveDeletedMessagesFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/NotificationsController;",
    name = "removeDeletedMessagesFromNotifications",
    returnType = "V",
    parameters = listOf("Lz/f;", "Z"),
)

// Plus Messenger 13.1.1.0 declares the current method locally with Landroidx/collection/k;.
val NotificationsControllerRemoveDeletedMessagesPlusFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/NotificationsController;",
    name = "removeDeletedMessagesFromNotifications",
    returnType = "V",
    parameters = listOf("Landroidx/collection/k;", "Z"),
)


// Backward-compatible fallback for older Plus builds.
val NotificationsControllerRemoveDeletedMessagesPlusLegacyFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/NotificationsController;",
    name = "removeDeletedMessagesFromNotifications",
    returnType = "V",
    parameters = listOf("Landroidx/collection/h;", "Z"),
)

// ─── Anti-disappearing media ──────────────────────────────────────────────────

val IsSecretMediaInstanceFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isSecretMedia",
    returnType = "Z",
    parameters = listOf(),
)

val IsSecretMediaStaticFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isSecretMedia",
    returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$Message;"),
)

val IsSecretPhotoOrVideoFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isSecretPhotoOrVideo",
    returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$Message;"),
)

// Static overload (I, Message)Z — present in both Web and Plus.
// Paresh uses this overload. The instance ()Z delegates to this,
// but targeting the static ensures we hit the actual implementation.
val ShouldEncryptPhotoOrVideoFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "shouldEncryptPhotoOrVideo",
    returnType = "Z",
    parameters = listOf("I", "Lorg/telegram/tgnet/TLRPC\$Message;"),
)

val IsVoiceOnceFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isVoiceOnce",
    returnType = "Z",
)

val IsRoundOnceFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isRoundOnce",
    returnType = "Z",
)

// Web: private; Plus: public final. Omit accessFlags.
val SendSecretMediaDeleteFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ChatActivity;",
    name = "sendSecretMediaDelete",
    returnType = "Ljava/lang/Runnable;",
    parameters = listOf("Lorg/telegram/messenger/MessageObject;"),
)

val SendSecretMessageReadFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ChatActivity;",
    name = "sendSecretMessageRead",
    returnType = "Ljava/lang/Runnable;",
    parameters = listOf("Lorg/telegram/messenger/MessageObject;", "Z"),
)

// ─── Download boost ───────────────────────────────────────────────────────────

val FileLoadOperationUpdateParamsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/FileLoadOperation;",
    name = "updateParams",
    returnType = "V",
    parameters = listOf(),
)

// ─── Stories / translate / misc premium UI ────────────────────────────────────

val ApplicationLoaderOnCreateFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/ApplicationLoader;",
    name = "onCreate",
    returnType = "V",
    accessFlags = listOf(AccessFlags.PUBLIC),
)

val MessagesControllerStoriesEnabledFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "storiesEnabled",
    returnType = "Z",
)

val MessagesControllerStoryEntitiesAllowedFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "storyEntitiesAllowed",
    returnType = "Z",
    parameters = listOf(),
)

val MessagesControllerStoryEntitiesAllowedUserFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "storyEntitiesAllowed",
    returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$User;"),
)

val TranslateControllerIsTranslateDialogHiddenFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/TranslateController;",
    name = "isTranslateDialogHidden",
    returnType = "Z",
    parameters = listOf("J"),
)

val ProfileActivityIsSwipeBackEnabledFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ProfileActivity;",
    name = "isSwipeBackEnabled",
    returnType = "Z",
    parameters = listOf("Landroid/view/MotionEvent;"),
)

val ChatActivityIsSwipeBackEnabledFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ChatActivity;",
    name = "isSwipeBackEnabled",
    returnType = "Z",
    parameters = listOf("Landroid/view/MotionEvent;"),
)

val MediaDataControllerLoadPinnedMessagesFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MediaDataController;",
    name = "loadPinnedMessages",
    returnType = "V",
    parameters = listOf("J", "I", "I"),
)

// ─── VoiceToMusic ─────────────────────────────────────────────────────────────

val MessageObjectIsVoiceFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isVoice",
    returnType = "Z",
    parameters = listOf(),
)

val MessageObjectIsMusicFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isMusic",
    returnType = "Z",
    parameters = listOf(),
    filters = listOf(
        methodCall(
            definingClass = "Lorg/telegram/messenger/MessageObject;",
            name = "isMusicMessage",
        ),
    ),
)

// ─── Anti-disappearing media (additional) ────────────────────────────────────

// MessageObject.needDrawBluredPreview()Z — current method body verified in Telegram,
// Telegram Web, and Plus Messenger 13.1.1.0. Retained as a shared direct target.
val MessageObjectNeedDrawBluredPreviewFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "needDrawBluredPreview",
    returnType = "Z",
    parameters = listOf(),
)

// ─── Anti-screenshot notification ─────────────────────────────────────────────

// SendMessagesHelper.sendScreenshotMessage(User,I,Message)V
// Notifies the other user when you screenshot a conversation.
val SendScreenshotMessageUserFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/SendMessagesHelper;",
    name = "sendScreenshotMessage",
    returnType = "V",
    parameters = listOf(
        "Lorg/telegram/tgnet/TLRPC\$User;",
        "I",
        "Lorg/telegram/tgnet/TLRPC\$Message;",
    ),
)

// SecretChatHelper.sendScreenshotMessage(EncryptedChat,ArrayList,Message)V
val SendScreenshotMessageSecretFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/SecretChatHelper;",
    name = "sendScreenshotMessage",
    returnType = "V",
    parameters = listOf(
        "Lorg/telegram/tgnet/TLRPC\$EncryptedChat;",
        "Ljava/util/ArrayList;",
        "Lorg/telegram/tgnet/TLRPC\$Message;",
    ),
)

// ─── Stories hide-read receipt flow: current targets ─────────────────────────
// Instruction sequence verified in Telegram 13.1.1 build 72371, Web 13.1.1 build 72379,
// and Plus Messenger 13.1.1.0 build 22621. Current owner names are isolated from legacy owners.
// Each method accepts (PeerStories, StoryItem, boolean), returns boolean, then calls
// processReadStories(JI)V, constructs TL_stories_readStories, and sends it via sendRequest.
val StoriesReadReceiptFlowFingerprint = Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = "Lorg/telegram/messenger/NotificationsController;",
            name = "processReadStories",
            returnType = "V",
        ),
        methodCall(
            definingClass = "Lorg/telegram/tgnet/tl/TL_stories\$TL_stories_readStories;",
            name = "<init>",
            returnType = "V",
        ),
        methodCall(
            definingClass = "Lorg/telegram/tgnet/ConnectionsManager;",
            name = "sendRequest",
            returnType = "I",
        ),
    ),
    custom = { method, _ ->
        method.returnType.toString() == "Z" &&
            method.parameterTypes.map { it.toString() } == listOf(
                "Lorg/telegram/tgnet/tl/TL_stories\$PeerStories;",
                "Lorg/telegram/tgnet/tl/TL_stories\$StoryItem;",
                "Z",
            ) &&
            method.definingClass.toString() in setOf(
                "Lug/s5;", // Telegram 13.1.1 build 72371
                "Lxg/o5;", // Telegram Web 13.1.1 build 72379
                "Lorg/telegram/ui/Stories/yc;", // Plus Messenger 13.1.1.0 build 22621
            )
    },
)

// Legacy Stories owners are kept as a separate fingerprint and are not part of the current
// compatibility targets. This avoids silently treating an older obfuscated owner as current.
val StoriesReadReceiptFlowLegacyFingerprint = Fingerprint(
    filters = listOf(
        methodCall(
            definingClass = "Lorg/telegram/messenger/NotificationsController;",
            name = "processReadStories",
            returnType = "V",
        ),
        methodCall(
            definingClass = "Lorg/telegram/tgnet/tl/TL_stories\$TL_stories_readStories;",
            name = "<init>",
            returnType = "V",
        ),
        methodCall(
            definingClass = "Lorg/telegram/tgnet/ConnectionsManager;",
            name = "sendRequest",
            returnType = "I",
        ),
    ),
    custom = { method, _ ->
        method.returnType.toString() == "Z" &&
            method.parameterTypes.map { it.toString() } == listOf(
                "Lorg/telegram/tgnet/tl/TL_stories\$PeerStories;",
                "Lorg/telegram/tgnet/tl/TL_stories\$StoryItem;",
                "Z",
            ) &&
            method.definingClass.toString() in setOf(
                "Ltg/p5;", // Telegram/Web 13.0.3 legacy owners
                "Lwg/m5;",
                "Lsg/q5;", // Telegram/Web 13.0.2 legacy owners
                "Lvg/o5;",
            )
    },
)

// ─── User no-forwards (missing from original set) ─────────────────────────────

val MessagesControllerIsUserNoForwardsLongFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "isUserNoForwards",
    returnType = "Z",
    parameters = listOf("J"),
)

val MessagesControllerIsUserNoForwardsUserFullFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "isUserNoForwards",
    returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$UserFull;"),
)

// ─── Premium account count / device class (Killergram) ───────────────────────

val UserConfigGetMaxAccountCountFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/UserConfig;",
    name = "getMaxAccountCount",
    returnType = "I",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
)

val UserConfigHasPremiumOnAccountsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/UserConfig;",
    name = "hasPremiumOnAccounts",
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
)

val SharedConfigGetDevicePerformanceClassFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/SharedConfig;",
    name = "getDevicePerformanceClass",
    returnType = "I",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
)

// Plus Messenger 13.1.1.0 current ad-loader owner. The older no-argument signature is
// retained below as a legacy fallback; do not confuse the two method descriptors.
val PlusApplicationLoaderLoadAdsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/ApplicationLoader;",
    name = "loadAds",
    returnType = "V",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Lorg/telegram/ui/Cells/m3;",
    ),
)


// Legacy Plus builds used ApplicationLoader.loadAds()V.
val PlusApplicationLoaderLoadAdsLegacyFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/ApplicationLoader;",
    name = "loadAds",
    returnType = "V",
    parameters = emptyList(),
)

// MessagesController.getSponsoredMessages(J) — fetches sponsored msg cache entry
val MessagesControllerGetSponsoredMessagesFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "getSponsoredMessages",
    returnType = "Lorg/telegram/messenger/MessagesController\$SponsoredMessagesInfo;",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("J"),
)

// ─── Copyright / restriction message bypass ───────────────────────────────────

// MessageObject.updateMessageText() — called at construction and on refresh.
// When getRestrictionReason returns non-null, this sets isRestrictedMessage=true
// and replaces messageText with the copyright string.
// Patching getRestrictionReason→null already prevents this, but patching
// updateMessageText directly gives belt-and-suspenders protection.
val MessageObjectUpdateMessageTextFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "updateMessageText",
    returnType = "V",
    parameters = listOf(),
    accessFlags = listOf(AccessFlags.PUBLIC),
)

// DialogCell.buildLayout()V — calls getRestrictionReason twice; we use matchAll on the
// getRestrictionReason methodCall filter to find and neutralise both result registers.
val DialogCellBuildLayoutFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Cells/DialogCell;",
    name = "buildLayout",
    returnType = "V",
    filters = listOf(
        methodCall(
            definingClass = "Lorg/telegram/messenger/MessagesController;",
            name = "getRestrictionReason",
        ),
    ),
)

// DialogCell.updateMessageThumbs()V — also calls getRestrictionReason
val DialogCellUpdateMessageThumbsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Cells/DialogCell;",
    name = "updateMessageThumbs",
    returnType = "V",
    filters = listOf(
        methodCall(
            definingClass = "Lorg/telegram/messenger/MessagesController;",
            name = "getRestrictionReason",
        ),
    ),
)

// ─── Plus-specific (safe to probe via methodOrNull on non-Plus builds) ────────

// MessagesController.premiumFeaturesBlocked()Z — legacy catalogue entry; current Plus
// MessagesController is present in the 13.0.2.0 APK. Keep this declaration separate from
// active paid-entitlement behavior; signature presence alone does not authorize patching it.
val PremiumFeaturesBlockedFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "premiumFeaturesBlocked",
    returnType = "Z",
)

// MessagesController.sendTyping(JJII)Z — Plus-specific signature
val PlusSendTypingFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "sendTyping",
    returnType = "Z",
    parameters = listOf("J", "J", "I", "I"),
)

// ─── Legacy declarations restored from the original catalogue ──────────────────
// Kept to avoid breaking downstream imports. The instruction-level audit found that these old
// named UI/Plus targets are not declared under those names in the supplied 13.0.2 DEX sets.
// Do not wire them into active patches until a body-level replacement is confirmed.

val StoriesControllerIsPremiumFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Stories/StoriesController;",
    name = "isPremium",
    returnType = "Z",
    parameters = listOf("J"),
)

val ChatActivityAddSponsoredMessagesFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ChatActivity;",
    name = "addSponsoredMessages",
    returnType = "V",
    parameters = listOf("Z"),
)

val CheckAppUpdateFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/LaunchActivity;",
    name = "checkAppUpdate",
    returnType = "V",
    parameters = listOf("Z", "Lorg/telegram/messenger/browser/Browser\$Progress;"),
)

val BlockingUpdateViewShowFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Components/BlockingUpdateView;",
    name = "show",
    returnType = "V",
    parameters = listOf("I", "Lorg/telegram/tgnet/TLRPC\$TL_help_appUpdate;", "Z"),
)

val ChatActivityIsPeerNoForwardsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ChatActivity;",
    name = "isPeerNoForwards",
    returnType = "Z",
)

val ProfileActivityIsPeerNoForwardsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ProfileActivity;",
    name = "isPeerNoForwards",
    returnType = "Z",
)

val ChatActivityHasSelectedNoforwardsMessageFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ChatActivity;",
    name = "hasSelectedNoforwardsMessage",
    returnType = "Z",
)

val ShowCantOpenAlertFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "showCantOpenAlert",
    returnType = "V",
)

val CheckSensitiveFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkSensitive",
    returnType = "V",
    parameters = listOf(
        "Lorg/telegram/ui/ActionBar/BaseFragment;",
        "J",
        "Ljava/lang/Runnable;",
        "Ljava/lang/Runnable;",
    ),
)

val CheckCanOpenChat2Fingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf("Landroid/os/Bundle;", "Lorg/telegram/ui/ActionBar/BaseFragment;"),
)

val CheckCanOpenChat3Fingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf(
        "Landroid/os/Bundle;",
        "Lorg/telegram/ui/ActionBar/BaseFragment;",
        "Lorg/telegram/messenger/MessageObject;",
    ),
)

val CheckCanOpenChat4Fingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf(
        "Landroid/os/Bundle;",
        "Lorg/telegram/ui/ActionBar/BaseFragment;",
        "Lorg/telegram/messenger/MessageObject;",
        "Lorg/telegram/messenger/browser/Browser\$Progress;",
    ),
)

val SecretMediaViewerClosePhotoFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/SecretMediaViewer;",
    name = "closePhoto",
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Z", "Z"),
)

val StoriesControllerHasStoriesFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Stories/StoriesController;",
    name = "hasStories",
    returnType = "Z",
    parameters = listOf(),
)

val StoriesControllerMarkStoryAsReadFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Stories/StoriesController;",
    name = "markStoryAsRead",
    returnType = "Z",
    parameters = listOf("J", "Lorg/telegram/tgnet/tl/TL_stories\$StoryItem;"),
)

val PeerStoriesViewAllowScreenshotsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Stories/PeerStoriesView\$StoryItemHolder;",
    name = "allowScreenshots",
    returnType = "Z",
)

val ChatPullingDownDrawableGetNextFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ChatPullingDownDrawable;",
    name = "getNextUnreadDialog",
    returnType = "Lorg/telegram/tgnet/TLRPC\$Dialog;",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
)

val ChatPullingDownDrawableDrawBottomPanelFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ChatPullingDownDrawable;",
    name = "drawBottomPanel",
    returnType = "V",
)

val ChatPullingDownDrawableNeedDrawBottomPanelFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ChatPullingDownDrawable;",
    name = "needDrawBottomPanel",
    returnType = "Z",
)

val ChatActivityGetSponsoredMessagesCountFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ChatActivity;",
    name = "getSponsoredMessagesCount",
    returnType = "I",
    // Web=private, Plus=public final — omit accessFlags for cross-variant compat
)

val PlusUpdaterCheckAppUpdateFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/plus/update/PlusUpdater;",
    name = "checkAppUpdate",
    returnType = "V",
)

val PlusSettingsIsUpdateEnabledFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ActionBar/PlusSettings;",
    name = "isUpdateEnabled",
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
)

val AdsControllerAdsDisabledFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/plus/ads/AdsController;",
    name = "adsDisabled",
    returnType = "Z",
)

val AdsInstanceLoadAdsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/plus/ads/AdsInstance;",
    name = "loadAds",
    returnType = "V",
)

val AdsInstanceLoadNativeAdFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/plus/ads/AdsInstance;",
    name = "loadNativeAd",
    parameters = listOf(
        "Landroid/content/Context;",
        "Z",
        "Lorg/telegram/plus/ads/AdsInstance\$AdsInstanceInterface;",
    ),
)

val AnalyticsEnableFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/plus/helpers/AnalyticsHelper;",
    name = "enableAnalytics",
    returnType = "V",
    parameters = listOf("Landroid/app/Application;"),
)

val AnalyticsTrackEventFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/plus/helpers/AnalyticsHelper;",
    name = "trackEvent",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

val AnalyticsTrackEventMapFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/plus/helpers/AnalyticsHelper;",
    name = "trackEvent",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/util/HashMap;"),
)

// ════════════════════════════════════════════════════════════════════════════════
// Privacy / telemetry fingerprints (merged from the 2026-10-04 instruction audit)
// ════════════════════════════════════════════════════════════════════════════════

/**
 * Instruction-audited privacy/telemetry fingerprints for Telegram/Web 13.1.1
 * and Plus Messenger 13.1.1.0.
 *
 * Telegram/Web do not bundle Firebase Analytics or Crashlytics in their standard
 * DEX files. Their ApplicationLoader App Center entry points are no-op stubs in
 * the audited builds. Plus bundles Firebase Analytics and Crashlytics under the
 * exact descriptors below.
 */

val TelegramPrivacyAppCenterLogFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/ApplicationLoader;",
    name = "appCenterLogInternal",
    returnType = "V",
    parameters = listOf("Ljava/lang/Throwable;"),
)

val TelegramPrivacyAppCenterStartFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/ApplicationLoader;",
    name = "startAppCenterInternal",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;"),
)

// Plus Messenger 13.1.1.0 build 22621: FirebaseAnalytics.smali.
val TelegramPrivacyFirebaseLogEventFingerprint = Fingerprint(
    definingClass = "Lcom/google/firebase/analytics/FirebaseAnalytics;",
    name = "a",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
)

val TelegramPrivacyFirebaseSetCollectionFingerprint = Fingerprint(
    definingClass = "Lcom/google/firebase/analytics/FirebaseAnalytics;",
    name = "b",
    returnType = "V",
    parameters = listOf("Z"),
)

val TelegramPrivacyFirebaseSetUserPropertyFingerprint = Fingerprint(
    definingClass = "Lcom/google/firebase/analytics/FirebaseAnalytics;",
    name = "c",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
)

val TelegramPrivacyFirebaseSetCurrentScreenFingerprint = Fingerprint(
    definingClass = "Lcom/google/firebase/analytics/FirebaseAnalytics;",
    name = "setCurrentScreen",
    returnType = "V",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
    ),
)

// Plus Messenger 13.1.1.0: Firebase Crashlytics public wrapper is R8-obfuscated.
// c(Z) forwards collection state, d(String,String) records a custom key, and
// e(String) records a log line. Exact descriptors and forwarding bodies were inspected.
val TelegramPrivacyCrashlyticsSetCollectionFingerprint = Fingerprint(
    definingClass = "Lcom/google/firebase/crashlytics/a;",
    name = "c",
    returnType = "V",
    parameters = listOf("Z"),
)

val TelegramPrivacyCrashlyticsSetCustomKeyFingerprint = Fingerprint(
    definingClass = "Lcom/google/firebase/crashlytics/a;",
    name = "d",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;"),
)

val TelegramPrivacyCrashlyticsLogFingerprint = Fingerprint(
    definingClass = "Lcom/google/firebase/crashlytics/a;",
    name = "e",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

// ─── Rich HTML paste ──────────────────────────────────────────────────────────

// Rich HTML clipboard paste handlers verified in clean APKs. These are separate
// per-build/fork because R8 changes the input-view owner and superclass.
// Current Telegram/Web and historical Web builds share this handler descriptor and superclass.
val ChatActivityEnterViewRichHtmlPasteCurrentFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Components/of;",
    name = "onTextContextMenuItem",
    returnType = "Z",
    parameters = listOf("I"),
)

// Plus Messenger handler; identical current and historical descriptors share this fingerprint.
val ChatActivityEnterViewRichHtmlPastePlusFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Components/qh0;",
    name = "onTextContextMenuItem",
    returnType = "Z",
    parameters = listOf("I"),
)


val ChatActivityEnterViewRichHtmlPasteTelegram1303Fingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Components/nf;",
    name = "onTextContextMenuItem",
    returnType = "Z",
    parameters = listOf("I"),
)

// Telegram Web 13.0.3 build 71839: of extends Components/mg.


// Legacy-only target retained for older Telegram/Web releases that still expose the
// named helper. It is absent from the current Telegram/Web 13.1.1 and Plus 13.1.1.0 APKs.
// Do not use this as the current target; those builds use the onTextContextMenuItem
// fingerprints above.
val ChatActivityEnterViewHandleRichHtmlPasteFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Components/ChatActivityEnterView;",
    name = "handleRichHtmlPaste",
    returnType = "Z",
    parameters = listOf(),
)

// ════════════════════════════════════════════════════════════════════════════════
// CURRENT TELEGRAM 13.2.1 R8 FINGERPRINTS (ADDITIVE)
// These exact R8 targets are preferred by revised patches; legacy fingerprints above
// remain available as fallbacks for earlier Telegram/Web/Plus builds.
// Do not delete or rename the legacy declarations when updating current R8 targets.
// ════════════════════════════════════════════════════════════════════════════════

// MessageObject: Telegram Android = lj; Telegram Web = gj; Plus remains named.
private const val MESSAGE_OBJECT_NORMAL = "Lorg/telegram/messenger/lj;"
private const val MESSAGE_OBJECT_WEB = "Lorg/telegram/messenger/gj;"

val TelegramR8MessageObjectIsSecretMediaNormalFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_NORMAL, name = "m4", returnType = "Z", parameters = emptyList(),
)
val TelegramR8MessageObjectIsSecretMediaWebFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_WEB, name = "m4", returnType = "Z", parameters = emptyList(),
)
val TelegramR8MessageObjectIsSecretMediaStaticNormalFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_NORMAL, name = "n4", returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$Message;"),
)
val TelegramR8MessageObjectIsSecretMediaStaticWebFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_WEB, name = "n4", returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$Message;"),
)
val TelegramR8MessageObjectIsSecretPhotoOrVideoNormalFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_NORMAL, name = "S5", returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$Message;"),
)
val TelegramR8MessageObjectIsSecretPhotoOrVideoWebFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_WEB, name = "S5", returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$Message;"),
)
val TelegramR8MessageObjectIsVoiceOnceNormalFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_NORMAL, name = "h4", returnType = "Z", parameters = emptyList(),
)
val TelegramR8MessageObjectIsVoiceOnceWebFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_WEB, name = "h4", returnType = "Z", parameters = emptyList(),
)
val TelegramR8MessageObjectIsRoundOnceNormalFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_NORMAL, name = "T4", returnType = "Z", parameters = emptyList(),
)
val TelegramR8MessageObjectIsRoundOnceWebFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_WEB, name = "T4", returnType = "Z", parameters = emptyList(),
)
val TelegramR8MessageObjectNeedDrawBlurredPreviewNormalFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_NORMAL, name = "n5", returnType = "Z", parameters = emptyList(),
)
val TelegramR8MessageObjectNeedDrawBlurredPreviewWebFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_WEB, name = "n5", returnType = "Z", parameters = emptyList(),
)
val TelegramR8MessageObjectIsSponsoredNormalFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_NORMAL, name = "s4", returnType = "Z", parameters = emptyList(),
)
val TelegramR8MessageObjectIsSponsoredWebFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_WEB, name = "s4", returnType = "Z", parameters = emptyList(),
)
val TelegramR8MessageObjectIsMusicNormalFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_NORMAL, name = "J3", returnType = "Z", parameters = emptyList(),
)
val TelegramR8MessageObjectIsMusicWebFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_WEB, name = "J3", returnType = "Z", parameters = emptyList(),
)

// Current screenshot-notification dispatcher: pz; (Web), qz; (Android).
val TelegramR8SendScreenshotNotificationNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/qz;", name = "k0", returnType = "Z",
    parameters = listOf("J", "Lorg/telegram/messenger/lj;", "Z"),
)
val TelegramR8SendScreenshotNotificationWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/pz;", name = "k0", returnType = "Z",
    parameters = listOf("J", "Lorg/telegram/messenger/gj;", "Z"),
)

// Stories read receipt method: wg/e5 (Android), zg/e5 (Web).
val TelegramR8StoriesReadReceiptNormalFingerprint = Fingerprint(
    definingClass = "Lwg/e5;", name = "Y", returnType = "Z",
    parameters = listOf(
        "Lorg/telegram/tgnet/tl/TL_stories\$PeerStories;",
        "Lorg/telegram/tgnet/tl/TL_stories\$StoryItem;",
        "Z",
    ),
)
val TelegramR8StoriesReadReceiptWebFingerprint = Fingerprint(
    definingClass = "Lzg/e5;", name = "Y", returnType = "Z",
    parameters = listOf(
        "Lorg/telegram/tgnet/tl/TL_stories\$PeerStories;",
        "Lorg/telegram/tgnet/tl/TL_stories\$StoryItem;",
        "Z",
    ),
)

// Current controller methods: vp; (Android), rp; (Web).
val TelegramR8GetRestrictionReasonNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/vp;", name = "F1", returnType = "Ljava/lang/String;",
    parameters = listOf("Ljava/util/ArrayList;"),
)
val TelegramR8GetRestrictionReasonWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/rp;", name = "F1", returnType = "Ljava/lang/String;",
    parameters = listOf("Ljava/util/ArrayList;"),
)
val TelegramR8IsSensitiveNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/vp;", name = "s", returnType = "Z",
    parameters = listOf("Ljava/util/ArrayList;"),
)
val TelegramR8IsSensitiveWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/rp;", name = "s", returnType = "Z",
    parameters = listOf("Ljava/util/ArrayList;"),
)
val TelegramR8ShowSensitiveContentNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/vp;", name = "T4", returnType = "Z", parameters = emptyList(),
)
val TelegramR8ShowSensitiveContentWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/rp;", name = "T4", returnType = "Z", parameters = emptyList(),
)
val TelegramR8CheckCanOpenChat4NormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/vp;", name = "E", returnType = "Z",
    parameters = listOf(
        "Landroid/os/Bundle;", "Lorg/telegram/ui/ActionBar/j2;",
        "Lorg/telegram/messenger/lj;", "Lorg/telegram/ui/yi;",
    ),
)
val TelegramR8CheckCanOpenChat4WebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/rp;", name = "E", returnType = "Z",
    parameters = listOf(
        "Landroid/os/Bundle;", "Lorg/telegram/ui/ActionBar/h2;",
        "Lorg/telegram/messenger/gj;", "Lorg/telegram/ui/yi;",
    ),
)
val TelegramR8CheckPromoInfoInternalNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/vp;", name = "N", returnType = "V", parameters = listOf("Z"),
)
val TelegramR8CheckPromoInfoInternalWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/rp;", name = "N", returnType = "V", parameters = listOf("Z"),
)
val TelegramR8IsChatNoForwardsNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/vp;", name = "o2", returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$Chat;"),
)
val TelegramR8IsChatNoForwardsWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/rp;", name = "o2", returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$Chat;"),
)
val TelegramR8IsPeerNoForwardsNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/vp;", name = "A2", returnType = "Z", parameters = listOf("J"),
)
val TelegramR8IsPeerNoForwardsWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/rp;", name = "A2", returnType = "Z", parameters = listOf("J"),
)
val TelegramR8CanForwardMessageNormalFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_NORMAL, name = "w", returnType = "Z", parameters = emptyList(),
)
val TelegramR8CanForwardMessageWebFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_WEB, name = "w", returnType = "Z", parameters = emptyList(),
)

// Current shared configuration, user configuration and file-load operation methods.
val TelegramR8IsAppUpdateAvailableWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/sz;", name = "s", returnType = "Z", parameters = emptyList(),
)
val TelegramR8UserConfigIsPremiumFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/c20;", name = "q", returnType = "Z", parameters = emptyList(),
)
val TelegramR8UserConfigHasPremiumOnAccountsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/c20;", name = "o", returnType = "Z", parameters = emptyList(),
)
val TelegramR8UserConfigGetMaxAccountCountFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/c20;", name = "l", returnType = "I", parameters = emptyList(),
)
val TelegramR8FileLoadOperationUpdateParamsNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/g7;", name = "t", returnType = "V", parameters = emptyList(),
)
val TelegramR8FileLoadOperationUpdateParamsWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/d7;", name = "t", returnType = "V", parameters = emptyList(),
)

val TelegramR8MessagesControllerIsSponsoredDisabledNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/vp;", name = "E2", returnType = "Z", parameters = emptyList(),
)
val TelegramR8MessagesControllerIsSponsoredDisabledWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/rp;", name = "E2", returnType = "Z", parameters = emptyList(),
)

val TelegramR8MessagesControllerIsPremiumUserNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/vp;", name = "B2", returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$User;"),
)
val TelegramR8MessagesControllerIsPremiumUserWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/rp;", name = "B2", returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$User;"),
)
val TelegramR8TranslationsManualNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/vp;", name = "G2", returnType = "Z", parameters = emptyList(),
)
val TelegramR8TranslationsManualWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/rp;", name = "G2", returnType = "Z", parameters = emptyList(),
)
val TelegramR8TranslationsAutoNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/vp;", name = "H2", returnType = "Z", parameters = emptyList(),
)
val TelegramR8TranslationsAutoWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/rp;", name = "H2", returnType = "Z", parameters = emptyList(),
)
val TelegramR8DevicePerformanceClassNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/tz;", name = "o", returnType = "I", parameters = emptyList(),
)
val TelegramR8DevicePerformanceClassWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/sz;", name = "o", returnType = "I", parameters = emptyList(),
)
val TelegramR8IsUserNoForwardsUserFullNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/vp;", name = "K2", returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$UserFull;"),
)
val TelegramR8IsUserNoForwardsUserFullWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/rp;", name = "K2", returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$UserFull;"),
)
val TelegramR8MessageObjectIsSensitiveNormalFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_NORMAL, name = "q4", returnType = "Z", parameters = emptyList(),
)
val TelegramR8MessageObjectIsSensitiveWebFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_WEB, name = "q4", returnType = "Z", parameters = emptyList(),
)
val TelegramR8MessageObjectIsHiddenSensitiveNormalFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_NORMAL, name = "y3", returnType = "Z", parameters = emptyList(),
)
val TelegramR8MessageObjectIsHiddenSensitiveWebFingerprint = Fingerprint(
    definingClass = MESSAGE_OBJECT_WEB, name = "y3", returnType = "Z", parameters = emptyList(),
)
val TelegramR8ShowCantOpenAlertNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/vp;", name = "S4", returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Lorg/telegram/ui/ActionBar/j2;"),
)
val TelegramR8ShowCantOpenAlertWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/rp;", name = "S4", returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Lorg/telegram/ui/ActionBar/h2;"),
)

// The current R8 MessagesController.update-array dispatcher. Its two
// delete-update list appends are handled separately from scheduled/ephemeral deletes.
val TelegramR8ProcessUpdateArrayNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/vp;", name = "N3", returnType = "Z",
    parameters = listOf("Ljava/util/ArrayList;", "Ljava/util/ArrayList;", "Ljava/util/ArrayList;", "Z", "I"),
)
val TelegramR8ProcessUpdateArrayWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/rp;", name = "N3", returnType = "Z",
    parameters = listOf("Ljava/util/ArrayList;", "Ljava/util/ArrayList;", "Ljava/util/ArrayList;", "Z", "I"),
)

val TelegramR8RichHtmlPasteNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Components/yf;", name = "onTextContextMenuItem",
    returnType = "Z", parameters = listOf("I"),
)
val TelegramR8RichHtmlPasteWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Components/ag;", name = "onTextContextMenuItem",
    returnType = "Z", parameters = listOf("I"),
)
// The Rich HTML clipboard implementation is in the Web base editor `ft`, not its
// wrapper subclass `ag`. Patching `ag` and invoking `ft` re-enters the custom handler.
val TelegramR8RichHtmlPasteNormalBaseFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Components/ht;", name = "onTextContextMenuItem",
    returnType = "Z", parameters = listOf("I"),
)
val TelegramR8RichHtmlPasteWebBaseFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Components/ft;", name = "onTextContextMenuItem",
    returnType = "Z", parameters = listOf("I"),
)
val TelegramPlus13111RichHtmlPasteFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Components/ChatActivityEnterView\$r2;",
    name = "onTextContextMenuItem", returnType = "Z", parameters = listOf("I"),
)

val TelegramR8SendTypingNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/vp;", name = "D4", returnType = "Z",
    parameters = listOf("I", "I", "J", "J", "Ljava/lang/String;"),
)
val TelegramR8SendTypingWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/rp;", name = "D4", returnType = "Z",
    parameters = listOf("I", "I", "J", "J", "Ljava/lang/String;"),
)

// Current 13.2.1 computes the certificate hash inline in ConnectionsManager.<init>(I).
val TelegramR8ConnectionManagerCertificateFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/tgnet/ConnectionsManager;",
    name = "<init>", returnType = "V", parameters = listOf("I"),
    filters = listOf(
        app.morphe.patcher.methodCall(
            definingClass = "Lorg/telegram/messenger/Utilities;",
            name = "f", returnType = "Ljava/lang/String;",
        ),
    ),
    custom = { method, _ ->
        val instructions = method.implementation?.instructions?.toList() ?: emptyList()
        instructions.indices.any { index ->
            val reference = (instructions[index] as? com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction)
                ?.reference as? com.android.tools.smali.dexlib2.iface.reference.MethodReference
            reference?.definingClass == "Lorg/telegram/messenger/Utilities;" &&
                reference.name == "f" && reference.parameterTypes == listOf("[B") &&
                reference.returnType == "Ljava/lang/String;" &&
                instructions.getOrNull(index + 1)?.opcode == com.android.tools.smali.dexlib2.Opcode.MOVE_RESULT_OBJECT
        }
    },
)

// Morphe Settings row builders and application startup hooks (Telegram 13.2.1).
val TelegramR8MorpheSettingsRowsNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/c51;", name = "e0", returnType = "V",
    parameters = listOf("Lorg/telegram/ui/c51;", "Ljava/util/ArrayList;"),
)
val TelegramR8MorpheSettingsRowsWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/h51;", name = "f0", returnType = "V",
    parameters = listOf("Lorg/telegram/ui/h51;", "Ljava/util/ArrayList;"),
)
val TelegramR8ApplicationOnCreateNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/u0;", name = "onCreate", returnType = "V",
    parameters = emptyList(),
)
val TelegramR8ApplicationOnCreateWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/t0;", name = "onCreate", returnType = "V",
    parameters = emptyList(),
)
