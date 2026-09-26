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
 * Converts Telegram Rich HTML clipboard content into a normal editable message
 * while preserving standard embedded HTML links.
 *
 * The old Rich HTML handler builds Telegram-specific rich blocks. Those blocks
 * are what produce structures such as MediaInfo / Screenshots in the composer.
 *
 * Instead of bypassing paste completely, this patch handles ACTION_PASTE itself:
 *
 *   Clipboard HTML -> ClipData.Item.coerceToStyledText()
 *                  -> Android Spanned text
 *                  -> Editable.replace()
 *
 * Android's coerceToStyledText() uses Html.fromHtml() for HTML clipboard data.
 * Standard 
<a href="..."> links therefore become URLSpan instances and are
 * preserved when Editable.replace() inserts the Spanned text. Telegram-specific
 * rich-message tags are not interpreted as Telegram rich blocks, so media/
 * screenshot blocks are not inserted into the composer.
 *
 * The native Telegram Rich HTML parser is therefore bypassed only for the
 * ACTION_PASTE operation. Normal paste is implemented locally rather than
 * delegating to the superclass, because the latter previously lost embedded
 * link spans in functional testing.
 *
 * DEX-verified handler in all three supplied builds:
 *   Telegram 12.10.5 / 71052
 *   Telegram Web 12.10.5 / 71059
 *   Plus Messenger 12.10.3.0
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
            returnType = "Landroid/content/ClipData${'$'}Item;",
        ),
        methodCall(
            definingClass = "Landroid/content/ClipData${'$'}Item;",
            name = "getHtmlText",
            returnType = "Ljava/lang/String;",
        ),
    ),
)

@Suppress("unused")
val telegramDisableRichHtmlPastePatch = bytecodePatch(
    name = "Use normal paste with links",
    description = "Converts Rich HTML clipboard content to normal editable text while preserving standard embedded links and removing Telegram rich media blocks.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )

    dependsOn(telegramSpoofDependency())

    execute {
        richHtmlPasteHandlerFingerprint.matchAllOrNull()?.forEach { match ->
            match.method.addInstructions(
                0,
                """
                    const v4, 0x1020022
                    if-ne p1, v4, :normal_paste

                    # v4 = this.getContext()
                    invoke-virtual {p0}, Landroid/view/View;->getContext()Landroid/content/Context;
                    move-result-object v4

                    # v5 = clipboard service
                    const-string v5, "clipboard"
                    invoke-virtual {v4, v5}, Landroid/content/Context;->getSystemService(Ljava/lang/String;)Ljava/lang/Object;
                    move-result-object v5
                    check-cast v5, Landroid/content/ClipboardManager;

                    # v5 = primary ClipData
                    invoke-virtual {v5}, Landroid/content/ClipboardManager;->getPrimaryClip()Landroid/content/ClipData;
                    move-result-object v5
                    if-eqz v5, :normal_paste

                    # Clipboard must contain at least one item.
                    invoke-virtual {v5}, Landroid/content/ClipData;->getItemCount()I
                    move-result v6
                    if-lez v6, :normal_paste

                    # v5 = first ClipData.Item
                    const/4 v6, 0x0
                    invoke-virtual {v5, v6}, Landroid/content/ClipData;->getItemAt(I)Landroid/content/ClipData${'$'}Item;
                    move-result-object v5

                    # v5 = Android's styled representation of the clipboard HTML.
                    # coerceToStyledText() uses Html.fromHtml() for HTML data,
                    # preserving standard 
    <a href> links as URLSpan.
                    invoke-virtual {v5, v4}, Landroid/content/ClipData${'$'}Item;->coerceToStyledText(Landroid/content/Context;)Ljava/lang/CharSequence;
                    move-result-object v5
                    if-eqz v5, :normal_paste

                    # v2 = selection start, v3 = selection end.
                    invoke-virtual {p0}, Landroid/widget/TextView;->getSelectionStart()I
                    move-result v2
                    invoke-virtual {p0}, Landroid/widget/TextView;->getSelectionEnd()I
                    move-result v3

                    # Invalid selection: let the original Telegram handler run.
                    if-ltz v2, :normal_paste
                    if-ltz v3, :normal_paste

                    # Normalize reversed selections to min/max.
                    if-le v2, v3, :selection_ready
                    move v6, v2
                    move v2, v3
                    move v3, v6

                    :selection_ready
                    invoke-virtual {p0}, Landroid/widget/TextView;->getText()Ljava/lang/CharSequence;
                    move-result-object v6
                    check-cast v6, Landroid/text/Editable;

                    # Preserve spans (including URLSpan) while replacing the
                    # selected range with the styled clipboard text.
                    invoke-interface {v6, v2, v3, v5}, Landroid/text/Editable;->replace(IILjava/lang/CharSequence;)Landroid/text/Editable;

                    const/4 v0, 0x1
                    return v0

                    :normal_paste
                    nop
                """.trimIndent(),
            )
        }
    }
}
