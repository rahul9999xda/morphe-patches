package app.template.patches.telegram.content

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
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
        // The Rich HTML parser lives in the superclass handler itself, not in the
        // composer subclass. Patching the child and calling invoke-super simply
        // re-enters that parser (the previous implementation's core bug).
        val currentTargets = listOf(
            TelegramR8RichHtmlPasteNormalFingerprint to "Lorg/telegram/ui/Components/jt;",
            TelegramR8RichHtmlPasteWebFingerprint to "Lorg/telegram/ui/Components/ft;",
            TelegramPlus13111RichHtmlPasteFingerprint to "Lorg/telegram/ui/Components/rh0;",
        )
        val matchedTargets = currentTargets.mapNotNull { (fingerprint, parentClass) ->
            fingerprint.methodOrNull?.let { parentClass }
        }
        require(matchedTargets.size <= 1) {
            "Ambiguous Rich HTML paste target: matched ${matchedTargets.size} composer variants"
        }
        val parentClass = matchedTargets.singleOrNull()
        if (parentClass != null) {
            var patched = false
            classDefForEach { classDef ->
                if (classDef.type != parentClass) return@classDefForEach
                val mutableClass = mutableClassDefBy(classDef)
                val handlers = mutableClass.methods.filter {
                    it.name == "onTextContextMenuItem" &&
                        it.parameterTypes.map { type -> type.toString() } == listOf("I") &&
                        it.returnType == "Z"
                }
                val handler = handlers.singleOrNull()
                    ?: error("Expected one parent Rich HTML handler $parentClass->onTextContextMenuItem(I)Z, found ${handlers.size}")
                handler.addInstructions(
                    0,
                    """
                        const v0, 0x1020022
                        if-ne p1, v0, :morphe_rich_html_paste_continue
                        invoke-super {p0, p1}, Lorg/telegram/ui/Components/EditTextBoldCursor;->onTextContextMenuItem(I)Z
                        move-result v0
                        return v0
                        :morphe_rich_html_paste_continue
                    """,
                )
                patched = true
            }
            check(patched) { "Could not locate parent Rich HTML handler $parentClass" }
        } else {
            // Historical named helper: returning false selects its caller's
            // existing plain-text paste fallback.
            val legacyMethod = ChatActivityEnterViewHandleRichHtmlPasteFingerprint.methodOrNull
                ?: throw IllegalStateException(
                    "Rich HTML paste target not found: no current composer or legacy handleRichHtmlPaste()Z method matched."
                )
            legacyMethod.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
        }
    }
}
