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
        // The composer subclasses (yf / hh.j1 / iv.l2) inherit the actual Rich HTML
        // parser from their immediate EditText parent (jt / ft / rh0). Calling
        // invoke-super from the composer therefore re-enters the parser and is not
        // a plain-text fallback. Patch the parser-owning parent method itself so
        // Paste goes directly to its Android EditText fallback before any MIME
        // inspection or HTML conversion takes place.
        val currentTargets = listOf(
            TelegramR8RichHtmlPasteNormalFingerprint to "Lorg/telegram/ui/Components/jt;",
            TelegramR8RichHtmlPasteWebFingerprint to "Lorg/telegram/ui/Components/ft;",
            TelegramPlus13111RichHtmlPasteFingerprint to "Lorg/telegram/ui/Components/rh0;",
            // Earlier named owners are retained with their independently verified
            // parser-owning superclasses.
            ChatActivityEnterViewRichHtmlPasteCurrentFingerprint to "Lorg/telegram/ui/Components/mg;",
            ChatActivityEnterViewRichHtmlPastePlusFingerprint to "Lorg/telegram/ui/Components/qh0;",
            ChatActivityEnterViewRichHtmlPasteTelegram1303Fingerprint to "Lorg/telegram/ui/Components/lg;",
        )
        val matchedTargets = currentTargets.mapNotNull { (fingerprint, parentClass) ->
            fingerprint.methodOrNull?.let { parentClass }
        }.distinct()
        require(matchedTargets.size <= 1) {
            "Ambiguous Rich HTML paste owners: matched ${matchedTargets.size} parser parents; refusing to patch an arbitrary class"
        }
        val parserParent = matchedTargets.singleOrNull()

        if (parserParent != null) {
            var patched = false
            classDefForEach { classDef ->
                if (classDef.type != parserParent) return@classDefForEach
                val candidates = mutableClassDefBy(classDef).methods.filter {
                    it.name == "onTextContextMenuItem" &&
                        it.parameterTypes.map { type -> type.toString() } == listOf("I") &&
                        it.returnType == "Z"
                }
                val mutableMethod = candidates.singleOrNull()
                    ?: error("Expected one parser-owning paste handler $parserParent, found ${candidates.size}")
                mutableMethod.addInstructions(
                    0,
                    """
                        const v0, 0x1020022
                        if-ne p1, v0, :morphe_rich_html_paste_continue
                        invoke-super {p0, p1}, Landroid/widget/EditText;->onTextContextMenuItem(I)Z
                        move-result v0
                        return v0
                        :morphe_rich_html_paste_continue
                    """,
                )
                patched = true
            }
            check(patched) { "Could not find parser-owning Rich HTML paste class $parserParent" }
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
