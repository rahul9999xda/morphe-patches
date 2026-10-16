package app.template.patches.telegram.content

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.StoriesReadReceiptFlowFingerprint

@Suppress("unused")
val telegramHideStoriesReadPatch = bytecodePatch(
    name = "Hide Stories read status",
    description = "Prevents the client from processing and sending Stories read receipts.",
    default = true,
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
    )

    execute {
        // The target method returns Z. Returning true preserves its success/handled
        // contract while skipping the local read update and TL_stories_readStories request.
        StoriesReadReceiptFlowFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x1
                return v0
            """,
        )
    }
}
