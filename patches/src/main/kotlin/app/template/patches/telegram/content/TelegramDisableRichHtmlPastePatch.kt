package app.template.patches.telegram.content

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.classDefBy
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency

/**
 * Telegram 12.10.x handles rich-HTML paste inside the app's own
 * onTextContextMenuItem(I)Z overrides.
 *
 * Returning false for ACTION_PASTE is not sufficient: Telegram's override
 * consumes the action and the normal Android TextView/EditText paste path is
 * never reached.
 *
 * For ACTION_PASTE we therefore call the matched class' immediate superclass
 * implementation and return its result. This preserves Android's normal paste
 * behaviour while skipping Telegram's rich-HTML paste implementation.
 *
 * The superclass is resolved from the actual matched DEX class, so this works
 * across the differently obfuscated Telegram / Telegram Plus / Telegram Web
 * builds instead of hard-coding an obfuscated class name.
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
    description = "Skips Telegram's Rich HTML paste handler and delegates paste to the normal Android superclass implementation.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )
    dependsOn(telegramSpoofDependency())

    execute {
        richHtmlPasteHandlerFingerprint.matchAllOrNull()?.forEach { match ->
            val superClass = classDefBy(match.originalMethod.definingClass).superclass
                ?: return@forEach

            match.method.addInstructions(0, """
                const v0, 0x1020022
                if-ne p1, v0, :normal
                invoke-super {p0, p1}, $superClass->onTextContextMenuItem(I)Z
                move-result v0
                return v0
                :normal
                nop
            """)
        }
    }
}
