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
        // Current Telegram/Web 13.2.2 and Plus 13.2.1.0: patch the Rich HTML superclass for the Android Paste menu item
        // (0x01020022), then delegate directly to the same superclass that the
        // original method uses for its non-rich-HTML fallback. All other menu
        // actions continue through the original method unchanged.
        val currentTargets = listOf(
            // The child fingerprints locate the composer. The HTML parser itself is
            // implemented by these parent classes, which must be patched directly.
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
            val (matchedMethod, richPasteOwner) = currentTarget
            // The Rich HTML parser is NOT in the composer subclass matched above.
            // It lives in that class's immediate superclass (jt / ft / rh0).
            // Calling invoke-super from the child therefore re-enters the HTML
            // parser and was the reason the old patch still pasted formatted HTML.
            // Patch the superclass implementation itself and route only Paste to
            // its ordinary EditText fallback. That fallback is already the exact
            // terminal branch used by the unmodified Rich HTML handler.
            var patchedCount = 0
            classDefForEach { classDef ->
                if (classDef.type != richPasteOwner) return@classDefForEach
                val mutableClass = mutableClassDefBy(classDef)
                val candidates = mutableClass.methods.filter {
                    it.name == "onTextContextMenuItem" &&
                        it.parameterTypes.map { type -> type.toString() } == listOf("I") &&
                        it.returnType == "Z"
                }
                val mutableMethod = candidates.singleOrNull()
                    ?: error("Expected one Rich HTML superclass handler $richPasteOwner->onTextContextMenuItem(I)Z, found ${candidates.size}")
                mutableMethod.addInstructions(
                    0,
                    """
                        const v0, 0x1020022
                        if-ne p1, v0, :morphe_rich_html_super_continue
                        invoke-super {p0, p1}, Landroid/widget/EditText;->onTextContextMenuItem(I)Z
                        move-result v0
                        return v0
                        :morphe_rich_html_super_continue
                    """,
                )
                patchedCount++
            }
            check(patchedCount == 1) {
                "Expected exactly one editable Rich HTML superclass $richPasteOwner for ${matchedMethod.definingClass}, patched $patchedCount"
            }
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
