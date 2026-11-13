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
import app.template.patches.telegram.PlusR8IsAppUpdateAvailableFingerprint
import app.template.patches.telegram.PlusR8HasPendingAppUpdateFingerprint
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

// Telegram 13.2.2 uses different SharedConfig owners in the two APK variants:
// Android stores the update in org.telegram.messenger.uz.j1; Web stores it in
// org.telegram.messenger.tz.j1. The exact static-field write is the hook, not a
// guessed method name. Keep the older named SharedConfig predicate below for legacy.
private val telegramR8UpdateObjectStoreAndroidFingerprint = Fingerprint(
    filters = listOf(
        fieldAccess(
            definingClass = "Lorg/telegram/messenger/uz;",
            name = "j1",
            type = "Lorg/telegram/tgnet/TLRPC\$TL_help_appUpdate;",
            opcode = Opcode.SPUT_OBJECT,
        ),
    ),
)

private val telegramR8UpdateObjectStoreWebFingerprint = Fingerprint(
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
                ?: PlusR8IsAppUpdateAvailableFingerprint.methodOrNull
                ?: sharedConfigIsAppUpdateAvailableFingerprint.methodOrNull
        if (updateAvailable != null) {
            updateAvailable.addInstructions(0, """
                const/4 v0, 0x0
                return v0
            """)
        } else {
            // Current Android and Web APKs both inline the update-availability gate.
            // Their actual update-object owners differ (uz.j1 vs tz.j1). Replace only
            // exact SPUT_OBJECT writes to those fields; leave all unrelated initialization
            // and update-response parsing intact. The legacy named predicate remains above.
            val updateStores = (
                telegramR8UpdateObjectStoreAndroidFingerprint.matchAllOrNull().orEmpty() +
                    telegramR8UpdateObjectStoreWebFingerprint.matchAllOrNull().orEmpty()
                ).distinctBy { match -> match.method to match.instructionMatches.map { it.index } }
            if (updateStores.isNotEmpty()) {
                updateStores.forEach { match ->
                    match.instructionMatches.map { it.index }.distinct().sortedDescending().forEach { index ->
                        match.method.replaceInstruction(index, "nop")
                    }
                }
            } else {
                require(
                    sharedConfigSetNewAppVersionAvailableFingerprint.methodOrNull != null ||
                        PlusR8IsAppUpdateAvailableFingerprint.methodOrNull != null ||
                        PlusUpdateButtonUpdateFingerprint.methodOrNull != null
                ) {
                    "Missing current uz.j1/tz.j1 update-store hooks and legacy/Plus update UI hooks"
                }
            }
        }

        // Plus has a second UI-facing predicate which checks the pending update object
        // directly. Disable it as well, so drawer/update-button surfaces stay hidden even
        // if an update response populated the cached object through another callback.
        PlusR8HasPendingAppUpdateFingerprint.methodOrNull?.addInstructions(0, """
            const/4 v0, 0x0
            return v0
        """)

        // setNewAppVersionAvailable(TL_help_appUpdate) was removed/inlined by R8 in
        // 13.2.2; keep the named hook only for Plus/legacy variants where it is local.
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
