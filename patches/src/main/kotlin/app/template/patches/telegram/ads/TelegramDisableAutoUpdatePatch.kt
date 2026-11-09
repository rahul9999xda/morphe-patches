package app.template.patches.telegram.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.PlusUpdateButtonUpdateFingerprint
import app.template.patches.telegram.TelegramR8IsAppUpdateAvailableWebFingerprint
import app.template.patches.telegram.TelegramR8CheckPromoInfoInternalNormalFingerprint
import app.template.patches.telegram.TelegramR8CheckPromoInfoInternalWebFingerprint
import app.template.patches.telegram.signature.telegramSpoofDependency
import com.android.tools.smali.dexlib2.Opcode

private val sharedConfigIsAppUpdateAvailableFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/SharedConfig;",
    name = "isAppUpdateAvailable",
    returnType = "Z",
)

private val sharedConfigSetNewAppVersionAvailableFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/SharedConfig;",
    name = "setNewAppVersionAvailable",
    returnType = "Z",
    parameters = listOf("Lorg/telegram/tgnet/TLRPC\$TL_help_appUpdate;"),
)

private val messagesControllerCheckPromoInfoInternalFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/MessagesController;",
    name = "checkPromoInfoInternal",
    returnType = "V",
    parameters = listOf("Z"),
)

// Telegram Android 13.2.1 inlines isAppUpdateAvailable(). Its update response
// writes the result into SharedConfig.tz.j1; suppress those stores without
// mutating the response object or unrelated local variables.
private val telegramR8UpdateObjectStoreNormalFingerprint = Fingerprint(
    filters = listOf(
        fieldAccess(
            definingClass = "Lorg/telegram/messenger/tz;",
            name = "j1",
            type = "Lorg/telegram/tgnet/TLRPC\$TL_help_appUpdate;",
            opcode = Opcode.SPUT_OBJECT,
        ),
    ),
)

@Suppress("unused")
val telegramDisableAutoUpdatePatch = bytecodePatch(
    name = "Disable auto-update",
    description = "Disables Telegram update availability, update-version storage and proxy sponsor-channel insertion.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )
    dependsOn(telegramSpoofDependency())

    execute {
        val updateAvailable =
            TelegramR8IsAppUpdateAvailableWebFingerprint.methodOrNull
                ?: sharedConfigIsAppUpdateAvailableFingerprint.methodOrNull
        if (updateAvailable != null) {
            updateAvailable.addInstructions(0, """
                const/4 v0, 0x0
                return v0
            """)
        } else {
            // Normal Telegram 13.2.1 has no standalone availability predicate;
            // prevent its update object from being published to the shared config.
            val updateStores = telegramR8UpdateObjectStoreNormalFingerprint.matchAllOrNull().orEmpty()
            require(updateStores.isNotEmpty()) {
                "Missing SharedConfig.isAppUpdateAvailable() and update-object store hook"
            }
            updateStores.forEach { match ->
                match.instructionMatches.map { it.index }.distinct().sortedDescending().forEach { index ->
                    match.method.replaceInstruction(index, "nop")
                }
            }
        }

        // setNewAppVersionAvailable(TL_help_appUpdate) was removed/inlined by R8 in
        // 13.2.1; keep the named hook only for Plus/legacy variants where it is local.
        sharedConfigSetNewAppVersionAvailableFingerprint.methodOrNull?.addInstructions(0, """
            const/4 v0, 0x0
            return v0
        """)

        val checkPromoInfo =
            TelegramR8CheckPromoInfoInternalNormalFingerprint.methodOrNull
                ?: TelegramR8CheckPromoInfoInternalWebFingerprint.methodOrNull
                ?: messagesControllerCheckPromoInfoInternalFingerprint.methodOrNull
        checkPromoInfo?.addInstructions(0, "return-void")

        PlusUpdateButtonUpdateFingerprint.methodOrNull?.addInstructions(0, "return-void")
    }
}