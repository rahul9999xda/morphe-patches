package app.template.patches.telegram.content

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency

/**
 * 12.10.x inlines the rich-HTML paste handler into the actual
 * onTextContextMenuItem(I)Z implementation. The old
 * ChatActivityEnterView.handleRichHtmlPaste() method no longer exists.
 *
 * This fingerprint therefore follows the Android clipboard operations that
 * uniquely identify the rich-HTML branch instead of depending on R8 class or
 * method names. Returning false only for ACTION_PASTE lets the normal Android
 * TextView paste path handle the clipboard item.
 */
private val richHtmlPasteHandlerFingerprint = Fingerprint(
    name = "onTextContextMenuItem",
    returnType = "Z",
    parameters = listOf("I"),
    filters = listOf(
        methodCall(
            definingClass = "Landroid/content/ClipboardManager;",
            name = "getPrimaryClip",
            returnType = "Landroid/content/ClipData;",
        ),
        methodCall(
            definingClass = "Landroid/content/ClipData;",
            name = "getItemCount",
            returnType = "I",
        ),
        methodCall(
            definingClass = "Landroid/content/ClipData;",
            name = "getDescription",
            returnType = "Landroid/content/ClipDescription;",
        ),
        methodCall(
            definingClass = "Landroid/content/ClipDescription;",
            name = "hasMimeType",
            parameters = listOf("Ljava/lang/String;"),
            returnType = "Z",
        ),
        methodCall(
            definingClass = "Landroid/content/ClipData;",
            name = "getItemAt",
            parameters = listOf("I"),
            returnType = "Landroid/content/ClipData\$Item;",
        ),
        methodCall(
            definingClass = "Landroid/content/ClipData\$Item;",
            name = "getHtmlText",
            returnType = "Ljava/lang/String;",
        ),
    ),
)

@Suppress("unused")
val telegramDisableRichHtmlPastePatch = bytecodePatch(
    name = "Use normal paste",
    description = "Skips Telegram's Rich HTML paste handler and falls back to the normal paste path.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )
    dependsOn(telegramSpoofDependency())

    execute {
        richHtmlPasteHandlerFingerprint.matchAllOrNull()?.forEach { match ->
            match.method.addInstructions(0, """
                const v0, 0x1020022
                if-ne p1, v0, :normal
                const/4 v0, 0x0
                return v0
                :normal
                nop
            """)
        }
    }
}