package app.template.patches.telegram.content

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.MessagesControllerAutoDeleteTaskFingerprints

/**
 * Safe local-timer-only auto-delete patch for Telegram 12.10.6.
 *
 * This intentionally changes only the local checkDeletingTask worker's
 * regular deleteMessages() call. It does NOT hook processUpdateArray() or
 * MessagesStorage.markMessagesAsDeleted(), because the previous server-TTL
 * filtering hook caused all message history to stop loading in the user's
 * patched Telegram Web build.
 *
 * Supported targets:
 * - Telegram 12.10.6 / build 71122
 * - Telegram Web 12.10.6 / build 71129
 * - Plus Messenger 12.10.6.0 / build 22588
 *
 * Limitation: server-synchronized deletion updates remain enabled. This is
 * deliberately safer than suppressing all server deletes or using an
 * unverified filter. A fully unified local + server TTL implementation needs
 * an instruction-level verified sink/register audit and runtime tests first.
 */
@Suppress("unused")
val telegramDisableAutoDeleteMessagesUnifiedPatch = bytecodePatch(
    name = "Disable Telegram auto-delete (safe local timer)",
    description = "Disables local timer deletion without modifying server-synchronized deletion handling.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )

    execute {
        val matches = MessagesControllerAutoDeleteTaskFingerprints
            .flatMap { it.matchAllOrNull() ?: emptyList() }

        require(matches.size == 1) {
            "Expected exactly one local Auto-Delete worker for this APK, found ${matches.size}."
        }

        val match = matches.single()

        require(match.instructionMatches.size == 1) {
            "Expected exactly one regular deleteMessages() call in the local TTL worker, " +
                "found ${match.instructionMatches.size}."
        }

        // The audited call is invoke-virtual/range (3 code units).
        // Three NOPs preserve instruction width and branch layout.
        match.method.replaceInstruction(
            match.instructionMatches.single().index,
            """
                nop
                nop
                nop
            """.trimIndent(),
        )
    }
}
