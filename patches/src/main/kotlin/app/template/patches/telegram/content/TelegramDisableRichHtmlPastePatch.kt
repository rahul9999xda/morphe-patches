package app.template.patches.telegram.content

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.methodCall
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/**
 * Telegram 12.10.x performs Rich HTML paste handling directly inside
 * onTextContextMenuItem(I)Z.
 *
 * The patched 12.10.4 DEX was inspected directly. The matched methods all
 * contain an existing invoke-super call to onTextContextMenuItem(I)Z. Rather
 * than resolving the obfuscated superclass through classDefBy(), we reuse
 * that exact method reference from the DEX instruction.
 *
 * When ACTION_PASTE is requested, the method immediately delegates to the
 * original superclass implementation. Other context-menu actions continue
 * through Telegram's original implementation unchanged.
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
            val superCall = match.method.implementation?.instructions
                ?.firstOrNull { instruction ->
                    instruction.opcode == Opcode.INVOKE_SUPER &&
                        instruction is ReferenceInstruction &&
                        instruction.reference is MethodReference
                }
                ?: return@forEach

            val superMethod = (superCall as ReferenceInstruction).reference as MethodReference

            val superMethodDescriptor = buildString {
                append(superMethod.definingClass)
                append("->")
                append(superMethod.name)
                append("(")
                append(superMethod.parameterTypes.joinToString(""))
                append(")")
                append(superMethod.returnType)
            }

            match.method.addInstructions(
                0,
                """
                    const v0, 0x1020022
                    if-ne p1, v0, :normal
                    invoke-super {p0, p1}, $superMethodDescriptor
                    move-result v0
                    return v0
                    :normal
                    nop
                """.trimIndent(),
            )
        }
    }
}
