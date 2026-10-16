package app.template.patches.telegram.ads

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency

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

/** Retained UI fallback: Plus 13.0.2.0 still declares UpdateButton.update(Z)V. */
private val plusUpdateButtonUpdateFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/Components/UpdateButton;",
    name = "update",
    returnType = "V",
    parameters = listOf("Z"),
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
        var patchedTargets = 0

        sharedConfigIsAppUpdateAvailableFingerprint.methodOrNull?.let { method ->
            method.addInstructions(0, """
                const/4 v0, 0x0
                return v0
            """)
            patchedTargets++
        }

        sharedConfigSetNewAppVersionAvailableFingerprint.methodOrNull?.let { method ->
            method.addInstructions(0, """
                const/4 v0, 0x0
                return v0
            """)
            patchedTargets++
        }

        messagesControllerCheckPromoInfoInternalFingerprint.methodOrNull?.let { method ->
            method.addInstructions(0, "return-void")
            patchedTargets++
        }

        // The three SharedConfig/MessagesController targets have local method bodies in all
        // supplied 13.0.2 APKs. Keep the verified Plus UpdateButton hook as an additional
        // fallback for Plus UI flows that reach the update prompt.
        plusUpdateButtonUpdateFingerprint.methodOrNull?.let { method ->
            method.addInstructions(0, "return-void")
            patchedTargets++
        }

        if (patchedTargets == 0) {
            error("No verified auto-update target found in this APK; refusing a silent no-op patch")
        }
    }
}