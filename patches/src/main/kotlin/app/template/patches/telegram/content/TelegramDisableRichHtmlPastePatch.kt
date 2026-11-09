package app.template.patches.telegram.content

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.ChatActivityEnterViewHandleRichHtmlPasteFingerprint
import app.template.patches.telegram.ChatActivityEnterViewRichHtmlPastePlusFingerprint
import app.template.patches.telegram.ChatActivityEnterViewRichHtmlPasteCurrentFingerprint
import app.template.patches.telegram.ChatActivityEnterViewRichHtmlPasteTelegram1303Fingerprint
import app.template.patches.telegram.TelegramR8RichHtmlPasteNormalFingerprint
import app.template.patches.telegram.TelegramR8RichHtmlPasteWebFingerprint
import app.template.patches.telegram.TelegramR8RichHtmlPasteNormalBaseFingerprint
import app.template.patches.telegram.TelegramR8RichHtmlPasteWebBaseFingerprint
import app.template.patches.telegram.TelegramPlus13111RichHtmlPasteFingerprint

@Suppress("unused")
val telegramDisableRichHtmlPastePatch = bytecodePatch(
    name = "Use normal paste for Rich HTML clipboard content",
    description = "Bypasses Telegram's custom Rich HTML clipboard handling and delegates paste to the normal superclass implementation.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
    )

    execute {
        // The custom Rich HTML clipboard parser is implemented by the base editor
        // classes (`ht` for Android, `ft` for Web), not by the thin composer wrappers
        // (`yf`/`ag`). Calling the wrapper's immediate superclass re-enters that parser.
        // Patch the actual parser owner and delegate to EditTextBoldCursor's inherited
        // normal text paste implementation instead.
        val baseTargets = listOf(
            TelegramR8RichHtmlPasteNormalBaseFingerprint to "Lorg/telegram/ui/Components/EditTextBoldCursor;",
            TelegramR8RichHtmlPasteWebBaseFingerprint to "Lorg/telegram/ui/Components/EditTextBoldCursor;",
        )
        val matchedBaseTargets = baseTargets.mapNotNull { (fingerprint, superClass) ->
            fingerprint.methodOrNull?.let { it to superClass }
        }
        require(matchedBaseTargets.size <= 1) {
            "Ambiguous Rich HTML base editor target: ${matchedBaseTargets.size} current base handlers matched"
        }
        val baseTarget = matchedBaseTargets.singleOrNull()

        val currentTargets = listOf(
            TelegramR8RichHtmlPasteNormalFingerprint to "Lorg/telegram/ui/Components/ht;",
            TelegramR8RichHtmlPasteWebFingerprint to "Lorg/telegram/ui/Components/ft;",
            TelegramPlus13111RichHtmlPasteFingerprint to "Lorg/telegram/ui/Components/xh0;",
            ChatActivityEnterViewRichHtmlPasteCurrentFingerprint to "Lorg/telegram/ui/Components/mg;",
            ChatActivityEnterViewRichHtmlPastePlusFingerprint to "Landroid/widget/EditText;",
            ChatActivityEnterViewRichHtmlPasteTelegram1303Fingerprint to "Lorg/telegram/ui/Components/lg;",
        )
        val matchedCurrentTargets = currentTargets.mapNotNull { (fingerprint, superClass) ->
            fingerprint.methodOrNull?.let { it to superClass }
        }
        require(matchedCurrentTargets.size <= 1) {
            "Ambiguous Rich HTML wrapper target: ${matchedCurrentTargets.size} current handlers matched"
        }
        val currentTarget = matchedCurrentTargets.singleOrNull()

        if (baseTarget != null) {
            val (method, superClass) = baseTarget
            method.addInstructions(
                0,
                """
                    const v0, 0x1020022
                    if-ne p1, v0, :morphe_rich_html_paste_continue
                    invoke-super {p0, p1}, $superClass->onTextContextMenuItem(I)Z
                    move-result v0
                    return v0
                    :morphe_rich_html_paste_continue
                """,
            )
        } else if (currentTarget != null) {
            val (method, superClass) = currentTarget
            method.addInstructions(
                0,
                """
                    const v0, 0x1020022
                    if-ne p1, v0, :morphe_rich_html_paste_continue
                    invoke-super {p0, p1}, $superClass->onTextContextMenuItem(I)Z
                    move-result v0
                    return v0
                    :morphe_rich_html_paste_continue
                """,
            )
        } else {
            // Legacy releases expose the original named helper. Returning false
            // from that helper lets the caller proceed through its plain-text path.
            val legacyMethod = ChatActivityEnterViewHandleRichHtmlPasteFingerprint.methodOrNull
                ?: throw IllegalStateException(
                    "Rich HTML paste target not found: no current Telegram/Web/Plus handler or legacy handleRichHtmlPaste()Z method matched."
                )
            legacyMethod.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )
        }
    }
}
