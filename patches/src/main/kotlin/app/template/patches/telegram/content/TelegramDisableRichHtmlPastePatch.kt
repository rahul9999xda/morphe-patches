package app.template.patches.telegram.content

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.ChatActivityEnterViewHandleRichHtmlPasteFingerprint
import app.template.patches.telegram.ChatActivityEnterViewRichHtmlPasteCurrentFingerprint
import app.template.patches.telegram.ChatActivityEnterViewRichHtmlPastePlusFingerprint
import app.template.patches.telegram.ChatActivityEnterViewRichHtmlPasteTelegram1303Fingerprint
import app.template.patches.telegram.TelegramR8RichHtmlPasteNormalFingerprint
import app.template.patches.telegram.TelegramPlus13111RichHtmlPasteFingerprint

@Suppress("unused")
val telegramDisableRichHtmlPastePatch = bytecodePatch(
    name = "Use normal paste for Rich HTML clipboard content",
    description = "Bypasses custom rich-content paste handling and delegates paste to the standard EditText implementation.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
    )

    execute {
        // Restore the legacy behavior: intercept only Paste / Paste as plain text,
        // then delegate to the actual EditText superclass. The verified current
        // Telegram/Web m.s and Plus AppCompat l classes both extend EditText.
        // This bypasses AndroidX rich-content receive handling without flattening
        // clipboard CharSequences in a separate helper, and leaves every other
        // context-menu action on the original method path.
        val currentPasteMethods = listOf(
            TelegramR8RichHtmlPasteNormalFingerprint,
            TelegramPlus13111RichHtmlPasteFingerprint,
        ).mapNotNull { it.methodOrNull }
        require(currentPasteMethods.size <= 1) {
            "Multiple current EditText paste handlers matched; refusing an ambiguous Rich HTML paste patch"
        }

        val currentPasteMethod = currentPasteMethods.singleOrNull()
        if (currentPasteMethod != null) {
            currentPasteMethod.addInstructions(
                0,
                """
                    const v0, 0x1020022
                    if-eq p1, v0, :morphe_rich_paste_super
                    const v0, 0x1020031
                    if-ne p1, v0, :morphe_rich_paste_continue
                    :morphe_rich_paste_super
                    invoke-super {p0, p1}, Landroid/widget/EditText;->onTextContextMenuItem(I)Z
                    move-result v0
                    return v0
                    :morphe_rich_paste_continue
                """,
            )
        } else {
            // Preserve support for older named releases whose composer still
            // exposes a dedicated handleRichHtmlPaste() helper.
            val legacyMethod = ChatActivityEnterViewHandleRichHtmlPasteFingerprint.methodOrNull
            if (legacyMethod != null) {
                legacyMethod.addInstructions(
                    0,
                    """
                        const/4 v0, 0x0
                        return v0
                    """,
                )
            } else {
                val legacyTargets = listOf(
                    ChatActivityEnterViewRichHtmlPasteCurrentFingerprint to "Lorg/telegram/ui/Components/mg;",
                    ChatActivityEnterViewRichHtmlPastePlusFingerprint to "Lorg/telegram/ui/Components/qh0;",
                    ChatActivityEnterViewRichHtmlPasteTelegram1303Fingerprint to "Lorg/telegram/ui/Components/lg;",
                )
                val parentClasses = legacyTargets.mapNotNull { (fingerprint, parent) ->
                    fingerprint.methodOrNull?.let { parent }
                }.distinct()
                require(parentClasses.size == 1) {
                    "Rich HTML paste handler not found or ambiguous in current/legacy APK"
                }
                var patched = false
                classDefForEach { classDef ->
                    if (classDef.type != parentClasses.single()) return@classDefForEach
                    val candidates = mutableClassDefBy(classDef).methods.filter {
                        it.name == "onTextContextMenuItem" &&
                            it.parameterTypes.map { type -> type.toString() } == listOf("I") &&
                            it.returnType == "Z"
                    }
                    val method = candidates.singleOrNull()
                        ?: error("Expected one legacy paste handler in ${parentClasses.single()}, found ${candidates.size}")
                    method.addInstructions(
                        0,
                        """
                            const v0, 0x1020022
                            if-eq p1, v0, :morphe_legacy_paste_super
                            const v0, 0x1020031
                            if-ne p1, v0, :morphe_legacy_paste_continue
                            :morphe_legacy_paste_super
                            invoke-super {p0, p1}, Landroid/widget/EditText;->onTextContextMenuItem(I)Z
                            move-result v0
                            return v0
                            :morphe_legacy_paste_continue
                        """,
                    )
                    patched = true
                }
                check(patched) { "Could not patch the legacy Rich HTML paste handler" }
            }
        }
    }
}
