package app.template.patches.telegram.content

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.CheckCanOpenChat2NormalFingerprint
import app.template.patches.telegram.CheckCanOpenChat3NormalFingerprint
import app.template.patches.telegram.CheckCanOpenChat4NormalFingerprint
import app.template.patches.telegram.CheckCanOpenChat4NormalCurrentFingerprint
import app.template.patches.telegram.CheckCanOpenChat4WebCurrentFingerprint
import app.template.patches.telegram.CheckCanOpenChat2PlusFingerprint
import app.template.patches.telegram.CheckCanOpenChat3PlusFingerprint
import app.template.patches.telegram.CheckCanOpenChat4PlusFingerprint
import app.template.patches.telegram.CheckCanOpenChat4Plus1302Fingerprint
import app.template.patches.telegram.CheckSensitiveNormalFingerprint
import app.template.patches.telegram.CheckSensitivePlusFingerprint
import app.template.patches.telegram.ShowCantOpenAlertNormalFingerprint
import app.template.patches.telegram.ShowCantOpenAlertPlusFingerprint
import app.template.patches.telegram.CheckCanOpenChat4Web1303Fingerprint
import app.template.patches.telegram.signature.telegramSpoofDependency
import app.template.patches.telegram.TelegramR8GetRestrictionReasonNormalFingerprint
import app.template.patches.telegram.TelegramR8GetRestrictionReasonWebFingerprint
import app.template.patches.telegram.TelegramR8IsSensitiveNormalFingerprint
import app.template.patches.telegram.TelegramR8IsSensitiveWebFingerprint
import app.template.patches.telegram.TelegramR8ShowSensitiveContentNormalFingerprint
import app.template.patches.telegram.TelegramR8ShowSensitiveContentWebFingerprint
import app.template.patches.telegram.TelegramR8MessageObjectIsSensitiveNormalFingerprint
import app.template.patches.telegram.TelegramR8MessageObjectIsSensitiveWebFingerprint
import app.template.patches.telegram.TelegramR8MessageObjectIsHiddenSensitiveNormalFingerprint
import app.template.patches.telegram.TelegramR8MessageObjectIsHiddenSensitiveWebFingerprint
import app.template.patches.telegram.TelegramR8ShowCantOpenAlertNormalFingerprint
import app.template.patches.telegram.TelegramR8ShowCantOpenAlertWebFingerprint
import app.template.patches.telegram.TelegramR8CheckCanOpenChat4NormalFingerprint
import app.template.patches.telegram.TelegramR8CheckCanOpenChat4WebFingerprint
import app.template.patches.telegram.TelegramPlusR8GetRestrictionReasonFingerprint
import app.template.patches.telegram.TelegramPlusR8IsSensitiveFingerprint
import app.template.patches.telegram.TelegramPlusR8MessageObjectIsSensitiveFingerprint
import app.template.patches.telegram.TelegramPlusR8ShowSensitiveContentFingerprint
import app.template.patches.telegram.TelegramPlusR8CheckCanOpenChat4Fingerprint

private val getRestrictionReasonFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "getRestrictionReason",
    returnType = "Ljava/lang/String;",
    parameters = listOf("Ljava/util/ArrayList;"),
)

private val setContentSettingsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "setContentSettings",
    returnType = "V",
    parameters = listOf("Z"),
)

private val messagesControllerIsSensitiveFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "isSensitive",
    returnType = "Z",
    parameters = listOf("Ljava/util/ArrayList;"),
)

private val messageObjectIsSensitiveFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isSensitive",
    returnType = "Z",
    parameters = emptyList(),
)

private val messageObjectIsHiddenSensitiveFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessageObject;",
    name = "isHiddenSensitive",
    returnType = "Z",
    parameters = emptyList(),
)

private val showSensitiveContentFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "showSensitiveContent",
    returnType = "Z",
    parameters = emptyList(),
)

// Telegram Web 13.0.2 legacy/current descriptor; Web 13.0.3 keeps s2.
private val showCantOpenAlertWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "showCantOpenAlert",
    returnType = "V",
    parameters = listOf(
        "Lorg/telegram/ui/ActionBar/s2;",
        "Ljava/lang/String;",
    ),
)

// Telegram 13.0.2 legacy descriptor; Telegram 13.0.3 uses ActionBar/r2.
private val showCantOpenAlertNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "showCantOpenAlert",
    returnType = "V",
    parameters = listOf(
        "Lorg/telegram/ui/ActionBar/s2;",
        "Ljava/lang/String;",
    ),
)

private val showCantOpenAlertPlusFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "showCantOpenAlert",
    returnType = "V",
    parameters = listOf(
        "Lorg/telegram/ui/ActionBar/i2;",
        "Ljava/lang/String;",
    ),
)

private val checkChannelErrorFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkChannelError",
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "J"),
)

private val checkSensitiveWebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkSensitive",
    returnType = "V",
    parameters = listOf(
        "Lorg/telegram/ui/ActionBar/s2;",
        "J",
        "Ljava/lang/Runnable;",
        "Ljava/lang/Runnable;",
    ),
)

// Telegram 13.0.2 legacy descriptor; Telegram 13.0.3 uses ActionBar/r2.
private val checkSensitiveNormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkSensitive",
    returnType = "V",
    parameters = listOf(
        "Lorg/telegram/ui/ActionBar/s2;",
        "J",
        "Ljava/lang/Runnable;",
        "Ljava/lang/Runnable;",
    ),
)

private val checkSensitivePlusFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkSensitive",
    returnType = "V",
    parameters = listOf(
        "Lorg/telegram/ui/ActionBar/i2;",
        "J",
        "Ljava/lang/Runnable;",
        "Ljava/lang/Runnable;",
    ),
)

private val checkCanOpenChat2WebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf(
        "Landroid/os/Bundle;",
        "Lorg/telegram/ui/ActionBar/s2;",
    ),
)

// Telegram 13.0.2 legacy descriptor; Telegram 13.0.3 uses ActionBar/r2.
private val checkCanOpenChat2NormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf(
        "Landroid/os/Bundle;",
        "Lorg/telegram/ui/ActionBar/s2;",
    ),
)

private val checkCanOpenChat2PlusFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf(
        "Landroid/os/Bundle;",
        "Lorg/telegram/ui/ActionBar/i2;",
    ),
)

private val checkCanOpenChat3WebFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf(
        "Landroid/os/Bundle;",
        "Lorg/telegram/ui/ActionBar/s2;",
        "Lorg/telegram/messenger/MessageObject;",
    ),
)

// Telegram 13.0.2 legacy descriptor; Telegram 13.0.3 uses ActionBar/r2.
private val checkCanOpenChat3NormalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf(
        "Landroid/os/Bundle;",
        "Lorg/telegram/ui/ActionBar/s2;",
        "Lorg/telegram/messenger/MessageObject;",
    ),
)

private val checkCanOpenChat3PlusFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf(
        "Landroid/os/Bundle;",
        "Lorg/telegram/ui/ActionBar/i2;",
        "Lorg/telegram/messenger/MessageObject;",
    ),
)

// Telegram Web 13.0.2 legacy descriptor; Web 13.0.3 uses Lje/e;.
private val checkCanOpenChat4WebFingerprint = Fingerprint(
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

// Legacy Telegram 13.0.2 descriptor; Telegram 13.1.1 uses Lge/e;.
private val checkCanOpenChat4NormalFingerprint = Fingerprint(
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


@Suppress("unused")
val telegramBypassChannelRestrictionsPatch = bytecodePatch(
    name = "Bypass channel restrictions",
    description = "Allows opening and viewing content from restricted, sensitive, and copyright-restricted channels.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )
    dependsOn(telegramSpoofDependency())

    execute {
        fun resolveVariantMethod(
            vararg fingerprints: Fingerprint,
            methodName: String,
        ) = requireNotNull(
            fingerprints.firstNotNullOfOrNull { it.methodOrNull },
        ) {
            "Failed to match Telegram/Plus fingerprint for $methodName"
        }

        // Telegram/Web 13.2.2 R8 owners and method names are recovered from the
        // local DEX bodies. The previous named-class hooks are kept below for legacy targets; Plus has separate current R8 hooks
        // and legacy APKs, but must not be used as fallbacks for these R8 builds.
        val currentGetRestrictionReason =
            TelegramR8GetRestrictionReasonNormalFingerprint.methodOrNull
                ?: TelegramR8GetRestrictionReasonWebFingerprint.methodOrNull
                ?: TelegramPlusR8GetRestrictionReasonFingerprint.methodOrNull
        if (currentGetRestrictionReason != null) {
            currentGetRestrictionReason.addInstructions(0, """
                const/4 v0, 0x0
                return-object v0
            """)
            val currentIsSensitive =
                TelegramR8IsSensitiveNormalFingerprint.methodOrNull
                    ?: TelegramR8IsSensitiveWebFingerprint.methodOrNull
                    ?: TelegramPlusR8IsSensitiveFingerprint.methodOrNull
            requireNotNull(currentIsSensitive) { "Missing R8 MessagesController.isSensitive(ArrayList)Z" }
                .addInstructions(0, """
                    const/4 v0, 0x0
                    return v0
                """)
            val currentMessageIsSensitive =
                TelegramR8MessageObjectIsSensitiveNormalFingerprint.methodOrNull
                    ?: TelegramR8MessageObjectIsSensitiveWebFingerprint.methodOrNull
                    ?: TelegramPlusR8MessageObjectIsSensitiveFingerprint.methodOrNull
            requireNotNull(currentMessageIsSensitive) { "Missing R8 MessageObject.isSensitive()Z" }
                .addInstructions(0, """
                    const/4 v0, 0x0
                    return v0
                """)
            val currentMessageIsHiddenSensitive =
                TelegramR8MessageObjectIsHiddenSensitiveNormalFingerprint.methodOrNull
                    ?: TelegramR8MessageObjectIsHiddenSensitiveWebFingerprint.methodOrNull
            // Plus folds the sensitive and hidden-sensitive checks into x20.E6().
            currentMessageIsHiddenSensitive?.addInstructions(0, """
                const/4 v0, 0x0
                return v0
            """)
            val currentShowSensitiveContent =
                TelegramR8ShowSensitiveContentNormalFingerprint.methodOrNull
                    ?: TelegramR8ShowSensitiveContentWebFingerprint.methodOrNull
                    ?: TelegramPlusR8ShowSensitiveContentFingerprint.methodOrNull
            requireNotNull(currentShowSensitiveContent) { "Missing R8 showSensitiveContent()Z" }
                .addInstructions(0, """
                    const/4 v0, 0x1
                    return v0
                """)
            (TelegramR8ShowCantOpenAlertNormalFingerprint.methodOrNull
                ?: TelegramR8ShowCantOpenAlertWebFingerprint.methodOrNull)
                ?.addInstructions(0, "return-void")
            (TelegramR8CheckCanOpenChat4NormalFingerprint.methodOrNull
                ?: TelegramR8CheckCanOpenChat4WebFingerprint.methodOrNull
                ?: TelegramPlusR8CheckCanOpenChat4Fingerprint.methodOrNull)
                ?.addInstructions(0, """
                    const/4 v0, 0x1
                    return v0
                """)
            return@execute
        }

        getRestrictionReasonFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """,
        )

        setContentSettingsFingerprint.method.addInstructions(0, "const/4 p1, 0x1")

        messagesControllerIsSensitiveFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )
        messageObjectIsSensitiveFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )
        messageObjectIsHiddenSensitiveFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """,
        )
        showSensitiveContentFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """,
        )

        resolveVariantMethod(
            showCantOpenAlertWebFingerprint,
            showCantOpenAlertNormalFingerprint,
            ShowCantOpenAlertNormalFingerprint,
            ShowCantOpenAlertPlusFingerprint,
            showCantOpenAlertPlusFingerprint,
            methodName = "showCantOpenAlert",
        ).addInstructions(0, "return-void")

        checkChannelErrorFingerprint.method.addInstructions(0, "return-void")

        resolveVariantMethod(
            checkSensitiveWebFingerprint,
            checkSensitiveNormalFingerprint,
            CheckSensitiveNormalFingerprint,
            CheckSensitivePlusFingerprint,
            checkSensitivePlusFingerprint,
            methodName = "checkSensitive",
        ).addInstructions(
            0,
            """
                if-eqz p4, :morphe_check_sensitive_skip
                invoke-interface {p4}, Ljava/lang/Runnable;->run()V
                :morphe_check_sensitive_skip
                return-void
            """,
        )

        listOf(
            resolveVariantMethod(
                checkCanOpenChat2WebFingerprint,
                checkCanOpenChat2NormalFingerprint,
                CheckCanOpenChat2NormalFingerprint,
                CheckCanOpenChat2PlusFingerprint,
                checkCanOpenChat2PlusFingerprint,
                methodName = "checkCanOpenChat/2",
            ),
            resolveVariantMethod(
                checkCanOpenChat3WebFingerprint,
                checkCanOpenChat3NormalFingerprint,
                CheckCanOpenChat3NormalFingerprint,
                CheckCanOpenChat3PlusFingerprint,
                checkCanOpenChat3PlusFingerprint,
                methodName = "checkCanOpenChat/3",
            ),
            resolveVariantMethod(
                checkCanOpenChat4WebFingerprint,
                checkCanOpenChat4NormalFingerprint,
                CheckCanOpenChat4NormalCurrentFingerprint,
                CheckCanOpenChat4WebCurrentFingerprint,
                CheckCanOpenChat4PlusFingerprint,
                CheckCanOpenChat4Web1303Fingerprint,
                CheckCanOpenChat4Plus1302Fingerprint,
                methodName = "checkCanOpenChat/4",
            ),
        ).forEach { method ->
            method.addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """,
            )
        }
    }
}
