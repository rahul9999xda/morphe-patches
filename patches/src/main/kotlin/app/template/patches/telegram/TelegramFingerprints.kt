package app.template.patches.telegram

import app.morphe.patcher.Fingerprint

/**
 * Exact local-method targets recovered from the supplied Telegram 13.2.1 APKs.
 * Telegram Android and Telegram Web use different R8 class maps, while the
 * affected MessageObject/controller method names and descriptors below are
 * shared between those two APKs. These declarations are deliberately separate
 * from the historical named-class fingerprints in TelegramFingerprints.kt.
 */

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
    definingClass = "Lorg/telegram/messenger/u0;", name = "onCreate", returnType = "V", parameters = emptyList(),
)
val TelegramR8ApplicationOnCreateWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/t0;", name = "onCreate", returnType = "V", parameters = emptyList(),
)
