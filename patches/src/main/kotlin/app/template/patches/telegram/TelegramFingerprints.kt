package app.template.patches.telegram

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.methodCall
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

// ════════════════════════════════════════════════════════════════════════════════
// Telegram shared fingerprints
// Updated using DEX method-signature audit: Telegram 12.10.6 (71122), Telegram Web 12.10.6 (71129), Plus Messenger 12.10.6.0 (22588)
// All class names non-obfuscated — stable across Telegram forks.
// AccessFlags omitted where Web/Plus differ (e.g. protected vs public).
// ════════════════════════════════════════════════════════════════════════════════

// ─── Premium ──────────────────────────────────────────────────────────────────

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

val StoriesControllerIsPremiumFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Stories/StoriesController;",
    name = "isPremium",
    returnType = "Z",
    parameters = listOf("J"),
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
    ),
)

// ─── Ads ──────────────────────────────────────────────────────────────────────

val ChatActivityAddSponsoredMessagesFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ChatActivity;",
    name = "addSponsoredMessages",
    returnType = "V",
    parameters = listOf("Z"),
)

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

// ─── No-forwards ──────────────────────────────────────────────────────────────

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

val CanForwardMessageFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "canForwardMessage",
    returnType = "Z",
    parameters = listOf(),
)

val ChatActivityHasSelectedNoforwardsMessageFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ChatActivity;",
    name = "hasSelectedNoforwardsMessage",
    returnType = "Z",
)

// ─── Channel restrictions ──────────────────────────────────────────────────────

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

// Web: public static; Plus: public (not static). Omit accessFlags for compat.
val ShowCantOpenAlertFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "showCantOpenAlert",
    returnType = "V",
)

val CheckChannelErrorFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkChannelError",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "J"),
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

// Current 12.10.6 four-argument overloads; callback descriptors differ by fork.
val CheckCanOpenChat4NormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf(
        "Landroid/os/Bundle;",
        "Lorg/telegram/ui/ActionBar/r2;",
        "Lorg/telegram/messenger/MessageObject;",
        "Lee/f;",
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
        "Lhe/e;",
    ),
)

val CheckCanOpenChat4PlusFingerprint = Fingerprint(
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

// Legacy Plus descriptor retained for older Plus builds only.
val CheckCanOpenChat4PlusLegacyFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf(
        "Landroid/os/Bundle;",
        "Lorg/telegram/ui/ActionBar/i2;",
        "Lorg/telegram/messenger/MessageObject;",
        "Lej/e\$c;",
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

// Web: protected; Plus: public. Omit accessFlags.
val DeleteMessagesByPushFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "deleteMessagesByPush",
    returnType = "V",
    parameters = listOf("J", "Ljava/util/ArrayList;", "J"),
)

// Telegram standard and Telegram Web 12.10.6 use Lz/f;.
val NotificationsControllerRemoveDeletedMessagesFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/NotificationsController;",
    name = "removeDeletedMessagesFromNotifications",
    returnType = "V",
    parameters = listOf("Lz/f;", "Z"),
)

// Plus Messenger 12.10.6.0 current descriptor.
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

val SecretMediaViewerClosePhotoFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/SecretMediaViewer;",
    name = "closePhoto",
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC),
    parameters = listOf("Z", "Z"),
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

val StoriesControllerHasStoriesFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Stories/StoriesController;",
    name = "hasStories",
    returnType = "Z",
    parameters = listOf(),
)

// Use the 2-param overload (J, StoryItem) — present in both variants
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

// MessageObject.needDrawBluredPreview()Z — blurs self-destructing media previews.
// Present in both Web and Plus. Returning false prevents the blurred overlay.
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

// ─── Channel switching (Killergram / NoAds) ───────────────────────────────────

// Web: getNextUnreadDialog()Dialog (no params)
// Plus: getNextUnreadDialog(JIIZ[I)Dialog (5 params — different signature)
// Omit parameters for cross-variant compat; method name + returnType + PUBLIC STATIC is unique
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

// ─── Sponsored messages count (NoAds) ────────────────────────────────────────

val ChatActivityGetSponsoredMessagesCountFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ChatActivity;",
    name = "getSponsoredMessagesCount",
    returnType = "I",
    // Web=private, Plus=public final — omit accessFlags for cross-variant compat
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

// MessagesController.premiumFeaturesBlocked()Z
// Present only in org.telegram.plus — gates "Get Premium" nag dialogs.
// Using methodOrNull makes this safe to reference in shared patches.
val PremiumFeaturesBlockedFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "premiumFeaturesBlocked",
    returnType = "Z",
)

// org.telegram.plus.update.PlusUpdater.checkAppUpdate(RequestDelegate)V
val PlusUpdaterCheckAppUpdateFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/plus/update/PlusUpdater;",
    name = "checkAppUpdate",
    returnType = "V",
)

// PlusSettings.isUpdateEnabled()Z
val PlusSettingsIsUpdateEnabledFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ActionBar/PlusSettings;",
    name = "isUpdateEnabled",
    returnType = "Z",
    accessFlags = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC),
)

// org.telegram.plus.ads.AdsController.adsDisabled()Z
val AdsControllerAdsDisabledFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/plus/ads/AdsController;",
    name = "adsDisabled",
    returnType = "Z",
)

// org.telegram.plus.ads.AdsInstance.loadAds()V
val AdsInstanceLoadAdsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/plus/ads/AdsInstance;",
    name = "loadAds",
    returnType = "V",
)

// org.telegram.plus.ads.AdsInstance.loadNativeAd(...) — returns Z in 12.9.0.1
val AdsInstanceLoadNativeAdFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/plus/ads/AdsInstance;",
    name = "loadNativeAd",
    parameters = listOf(
        "Landroid/content/Context;",
        "Z",
        "Lorg/telegram/plus/ads/AdsInstance\$AdsInstanceInterface;",
    ),
)

// MessagesController.sendTyping(JJII)Z — Plus-specific signature
val PlusSendTypingFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "sendTyping",
    returnType = "Z",
    parameters = listOf("J", "J", "I", "I"),
)

// org.telegram.plus.helpers.AnalyticsHelper fingerprints
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

// ─── Disable message auto-delete ──────────────────────────────────────────────
/**
 * Telegram 12.10.6 unified Auto-Delete fingerprints.
 *
 * Audited APKs:
 * - Telegram 12.10.6 / 71122
 * - Telegram Web 12.10.6 / 71129
 * - Plus Messenger 12.10.6.0 / 22588
 *
 * The normal Telegram/Web builds retain the readable synthetic lambda names.
 * Plus is obfuscated and therefore uses separate exact method-name/signature
 * fingerprints. The sink shape is nevertheless identical: one
 * MessagesStorage.markMessagesAsDeleted(...) call in each server-delete
 * lambda.
 */

private val deleteMessagesCall = methodCall(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "deleteMessages",
    returnType = "V",
)

val MessagesControllerAutoDeleteTaskTelegramFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "lambda${'$'}checkDeletingTask${'$'}86",
    returnType = "V",
    parameters = listOf("Lz/f;", "Lz/f;"),
    filters = listOf(deleteMessagesCall),
)

val MessagesControllerAutoDeleteTaskTelegramWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "lambda${'$'}checkDeletingTask${'$'}86",
    returnType = "V",
    parameters = listOf("Lz/f;", "Lz/f;"),
    filters = listOf(deleteMessagesCall),
)

/** Plus 12.10.6.0 / 22588 local TTL worker. */
val MessagesControllerAutoDeleteTaskPlusFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "f2",
    returnType = "V",
    parameters = listOf(
        "Lorg/telegram/messenger/MessagesController;",
        "Landroidx/collection/k;",
        "Landroidx/collection/k;",
    ),
    filters = listOf(deleteMessagesCall),
)

val MessagesControllerAutoDeleteTaskFingerprints = listOf(
    MessagesControllerAutoDeleteTaskTelegramWebFingerprint,
    MessagesControllerAutoDeleteTaskPlusFingerprint,
)

private val markMessagesAsDeletedCall = methodCall(
    definingClass = "Lorg/telegram/messenger/MessagesStorage;",
    name = "markMessagesAsDeleted",
    returnType = "Ljava/util/ArrayList;",
)

val MessagesControllerServerTtlRegularDeleteTelegramFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "lambda${'$'}processUpdateArray${'$'}418",
    returnType = "V",
    parameters = listOf("J", "Ljava/util/ArrayList;"),
    filters = listOf(markMessagesAsDeletedCall),
)

val MessagesControllerServerTtlChannelDeleteTelegramFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "lambda${'$'}processUpdateArray${'$'}419",
    returnType = "V",
    parameters = listOf("J", "Ljava/util/ArrayList;", "J"),
    filters = listOf(markMessagesAsDeletedCall),
)

// Web uses the same synthetic names/signatures as Telegram 12.10.6.
val MessagesControllerServerTtlRegularDeleteWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "lambda${'$'}processUpdateArray${'$'}418",
    returnType = "V",
    parameters = listOf("J", "Ljava/util/ArrayList;"),
    filters = listOf(markMessagesAsDeletedCall),
)

val MessagesControllerServerTtlChannelDeleteWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "lambda${'$'}processUpdateArray${'$'}419",
    returnType = "V",
    parameters = listOf("J", "Ljava/util/ArrayList;", "J"),
    filters = listOf(markMessagesAsDeletedCall),
)

// Plus 12.10.6.0 / 22588 obfuscates these lambdas and captures the controller
// as the first static-lambda parameter.
val MessagesControllerServerTtlRegularDeletePlusFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "M1",
    returnType = "V",
    parameters = listOf(
        "Lorg/telegram/messenger/MessagesController;",
        "J",
        "Ljava/util/ArrayList;",
    ),
    filters = listOf(markMessagesAsDeletedCall),
)

val MessagesControllerServerTtlChannelDeletePlusFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "J8",
    returnType = "V",
    parameters = listOf(
        "Lorg/telegram/messenger/MessagesController;",
        "J",
        "Ljava/util/ArrayList;",
        "J",
    ),
    filters = listOf(markMessagesAsDeletedCall),
)

val MessagesControllerServerTtlRegularDeleteFingerprints = listOf(
    MessagesControllerServerTtlRegularDeleteTelegramFingerprint,
    MessagesControllerServerTtlRegularDeletePlusFingerprint,
)

val MessagesControllerServerTtlChannelDeleteFingerprints = listOf(
    MessagesControllerServerTtlChannelDeleteTelegramFingerprint,
    MessagesControllerServerTtlChannelDeletePlusFingerprint,
)

// ─── Rich HTML paste ──────────────────────────────────────────────────────────
val ChatActivityEnterViewHandleRichHtmlPasteFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Components/ChatActivityEnterView;",
    name = "handleRichHtmlPaste",
    returnType = "Z",
    parameters = listOf(),
)

// ─── Rich-message forwarding / Hide Sender Name ───────────────────────────────
val ChatActivityForwardMessagesFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/ChatActivity;",
    name = "forwardMessages",
    returnType = "V",
    parameters = listOf(
        "Ljava/util/ArrayList;",
        "Z",
        "Z",
        "Z",
        "I",
        "J",
    ),
)