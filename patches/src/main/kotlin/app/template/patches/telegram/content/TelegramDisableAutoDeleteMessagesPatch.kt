package app.template.patches.telegram.content

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.MessagesControllerAutoDeleteTaskPlusFingerprint
import app.template.patches.telegram.MessagesControllerAutoDeleteTaskTelegramWebFingerprint

/**
 * Disables the app-local scheduled TTL worker. The worker contains both the
 * regular-message deleteMessages() loop and the SecretMediaViewer expiry branch,
 * so returning before either branch prevents local timer-driven message/media
 * expiry. This is intentionally separate from Anti-Delete and does not modify
 * their deletion hooks.
 *
 * Important boundary: server-synchronized delete updates can still remove a
 * message independently of this local timer worker. This patch does not claim
 * to bypass server TTL updates; that requires a separately verified filter that
 * distinguishes TTL expiry from ordinary user/admin deletion.
 */
@Suppress("unused")
val telegramDisableAutoDeleteMessagesPatch = bytecodePatch(
    name = "Disable message auto-delete",
    description = "Disables local timer-driven message and media expiry; server-synchronized TTL deletions are not intercepted.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )

    execute {
        // Fresh targets only: Telegram/Web 13.0.3 and Plus Messenger 13.0.2.0.
        val candidates = listOf(
            MessagesControllerAutoDeleteTaskTelegramWebFingerprint,
            MessagesControllerAutoDeleteTaskPlusFingerprint,
        )
        val matches = candidates.flatMap { fingerprint ->
            fingerprint.matchAllOrNull().orEmpty()
        }

        require(matches.size == 1) {
            "Failed to uniquely match the local auto-delete worker. " +
                "Expected exactly one current Telegram/Web or Plus target, found ${matches.size}."
        }

        val match = matches.single()
        require(match.instructionMatches.size == 1) {
            "Expected one deleteMessages() call in the local auto-delete worker, " +
                "found ${match.instructionMatches.size}."
        }

        // This worker is dedicated to timer expiry. An early return suppresses
        // both its regular-message deletion loop and its media-expiry branch.
        // No new registers are used, and existing instructions/branch offsets are
        // left intact behind the inserted return.
        match.method.addInstructions(0, "return-void")
    }
}
