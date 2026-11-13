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
        // Current Telegram/Web 13.1.1 and Plus 13.1.1.0: only intercept the Android Paste menu item
        // (0x01020022), then delegate directly to the same superclass that the
        // original method uses for its non-rich-HTML fallback. All other menu
        // actions continue through the original method unchanged.
        val currentTargets = listOf(
            // Telegram Android 13.2.2: yf extends jt; Telegram Web 13.2.2: hh.j1 extends Components/ft.
            // Invoke the immediate superclass implementation, not an ancestor or interface.
            TelegramR8RichHtmlPasteNormalFingerprint to "Lorg/telegram/ui/Components/jt;",
            TelegramR8RichHtmlPasteWebFingerprint to "Lorg/telegram/ui/Components/ft;",
            // Plus Messenger 13.2.1.0 build 22678: composer input view iv.l2 extends rh0.
            TelegramPlus13111RichHtmlPasteFingerprint to "Lorg/telegram/ui/Components/rh0;",
            // Historical named targets retained independently.
            ChatActivityEnterViewRichHtmlPasteCurrentFingerprint to
                "Lorg/telegram/ui/Components/mg;",
            ChatActivityEnterViewRichHtmlPastePlusFingerprint to
                "Landroid/widget/EditText;",
            // Telegram 13.0.3 uses a different owner and superclass.
            ChatActivityEnterViewRichHtmlPasteTelegram1303Fingerprint to
                "Lorg/telegram/ui/Components/lg;",
        )
        val matchedTargets = currentTargets.mapNotNull { (fingerprint, superClass) ->
            fingerprint.methodOrNull?.let { it to superClass }
        }
        require(matchedTargets.size <= 1) {
            "Ambiguous Rich HTML paste target: matched ${matchedTargets.size} current handlers; refusing to patch an arbitrary class"
        }
        val currentTarget = matchedTargets.singleOrNull()

        if (currentTarget != null) {
            val (matchedMethod, superClass) = currentTarget
            var patched = false
            classDefForEach { classDef ->
                if (classDef.type != matchedMethod.definingClass) return@classDefForEach
                val candidates = mutableClassDefBy(classDef).methods.filter {
                    it.name == matchedMethod.name &&
                        it.parameterTypes.map { type -> type.toString() } == listOf("I") &&
                        it.returnType == "Z"
                }
                val mutableMethod = candidates.singleOrNull()
                    ?: error("Expected one editable Rich HTML paste handler ${matchedMethod.definingClass}->${matchedMethod.name}, found ${candidates.size}")
                mutableMethod.addInstructions(
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
                patched = true
            }
            check(patched) { "Could not patch editable Rich HTML paste handler" }
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
