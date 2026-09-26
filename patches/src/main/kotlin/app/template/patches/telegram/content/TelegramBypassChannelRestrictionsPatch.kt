package app.template.patches.telegram.content

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency

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

private val showCantOpenAlertTelegramFingerprint = Fingerprint(
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

private val checkSensitiveTelegramFingerprint = Fingerprint(
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

private val checkCanOpenChat2TelegramFingerprint = Fingerprint(
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

private val checkCanOpenChat3TelegramFingerprint = Fingerprint(
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

private val checkCanOpenChat4TelegramFingerprint = Fingerprint(
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

private val checkCanOpenChat4PlusFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkCanOpenChat",
    returnType = "Z",
    parameters = listOf(
        "Landroid/os/Bundle;",
        "Lorg/telegram/ui/ActionBar/i2;",
        "Lorg/telegram/messenger/MessageObject;",
        "Lej/e$c;",
    ),
)

private fun resolveVariantMethod(
    telegramFingerprint: Fingerprint,
    plusFingerprint: Fingerprint,
    methodName: String,
) = requireNotNull(
    telegramFingerprint.methodOrNull ?: plusFingerprint.methodOrNull,
) {
    "Failed to match Telegram/Plus fingerprint for $methodName"
}


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
            showCantOpenAlertTelegramFingerprint,
            showCantOpenAlertPlusFingerprint,
            "showCantOpenAlert",
        ).addInstructions(0, "return-void")

        checkChannelErrorFingerprint.method.addInstructions(0, "return-void")

        resolveVariantMethod(
            checkSensitiveTelegramFingerprint,
            checkSensitivePlusFingerprint,
            "checkSensitive",
        ).addInstructions(
            0,
            """
                if-eqz p4, :skip
                invoke-interface {p4}, Ljava/lang/Runnable;->run()V
                :skip
                return-void
            """,
        )

        listOf(
            resolveVariantMethod(
                checkCanOpenChat2TelegramFingerprint,
                checkCanOpenChat2PlusFingerprint,
                "checkCanOpenChat/2",
            ),
            resolveVariantMethod(
                checkCanOpenChat3TelegramFingerprint,
                checkCanOpenChat3PlusFingerprint,
                "checkCanOpenChat/3",
            ),
            resolveVariantMethod(
                checkCanOpenChat4TelegramFingerprint,
                checkCanOpenChat4PlusFingerprint,
                "checkCanOpenChat/4",
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
