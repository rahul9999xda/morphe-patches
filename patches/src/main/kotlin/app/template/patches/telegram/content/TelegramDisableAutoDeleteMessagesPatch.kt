package app.template.patches.telegram.content

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.MessagesControllerAutoDeleteTaskPlusFingerprint
import app.template.patches.telegram.MessagesControllerAutoDeleteTaskTelegramWebFingerprint

/**
 * Disables Telegram's client-side message auto-delete timer.
 *
 * DEX-verified design for:
 *   - Telegram 12.10.5 / 71052
 *   - Telegram Web 12.10.5 / 71059
 *   - Plus Messenger 12.10.3.0
 *
 * Telegram stores expiring-message tasks in enc_tasks_v4 and eventually
 * reaches MessagesController.checkDeletingTask(), whose UI worker invokes:
 *
 *   deleteMessages(ArrayList, ArrayList, EncryptedChat, J, I, Z, I, Z)
 *
 * The exact synthetic worker names for the inspected builds are:
 *   - Telegram/Web 12.10.5: lambda$checkDeletingTask$86(Lz/f;, Lz/f;)V
 *   - Plus 12.10.3.0: e2(MessagesController, androidx.collection.h,
 *     androidx.collection.h)V
 *
 * The same worker also handles disappearing/show-once media through a
 * separate taskMedia path. We therefore NOP only the regular message
 * deleteMessages() invocation and leave the media-expiration path intact.
 * This protects regular messages with text, photos, videos, documents, or
 * other attachments from the timer-driven whole-message deletion. It does
 * not disable media self-destruct/show-once expiry, which remains a separate
 * taskMedia path handled by the Anti-disappearing-media patch.
 *
 * This is intentionally independent from TelegramAntiDisappearingMediaPatch.
 */
@Suppress("unused")
val telegramDisableAutoDeleteMessagesPatch = bytecodePatch(
    name = "Disable message auto-delete",
    description = "Prevents the client-side auto-delete timer from deleting regular messages while preserving disappearing-media handling.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )

    execute {
        val matches = (
            MessagesControllerAutoDeleteTaskTelegramWebFingerprint.matchAllOrNull()
                ?: emptyList()
        ) + (
            MessagesControllerAutoDeleteTaskPlusFingerprint.matchAllOrNull()
                ?: emptyList()
        )

        require(matches.size == 1) {
            "Failed to uniquely match Telegram message auto-delete worker. " +
                "Expected exactly one Telegram/Web or Plus DEX variant, found ${matches.size}."
        }

        val match = matches.single()

        require(match.instructionMatches.size == 1) {
            "Expected exactly one regular deleteMessages() call in the auto-delete worker, " +
                "found ${match.instructionMatches.size}."
        }

        // deleteMessages(...) is invoke-virtual/range (3 code units).
        // Keep the original instruction width with three NOPs so branch/layout
        // structure remains stable and only the deletion side effect is removed.
        match.method.replaceInstruction(
            match.instructionMatches.single().index,
            "nop\nnop\nnop",
        )
    }
}
