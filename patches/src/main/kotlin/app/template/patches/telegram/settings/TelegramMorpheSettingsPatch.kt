package app.template.patches.telegram.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency

private val telegramSettingsRowsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/p91;",
    name = "b0",
    returnType = "V",
    parameters = listOf("Lorg/telegram/ui/p91;", "Ljava/util/ArrayList;"),
)
private val telegramWebSettingsRowsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/t91;",
    name = "b0",
    returnType = "V",
    parameters = listOf("Lorg/telegram/ui/t91;", "Ljava/util/ArrayList;"),
)
private val postInitApplicationFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/ApplicationLoader;",
    name = "postInitApplication",
    returnType = "V",
    parameters = emptyList(),
)

@Suppress("unused")
val telegramMorpheSettingsPatch = bytecodePatch(
    name = "Morphe Settings",
    description = "Adds a dedicated Morphe Settings entry and automatic forwarding using Telegram's existing signed-in account.",
) {
    compatibleWith(TELEGRAM_COMPATIBILITY, TELEGRAM_WEB_COMPATIBILITY)
    dependsOn(telegramSpoofDependency())

    execute {
        val telegramMethod = telegramSettingsRowsFingerprint.methodOrNull
        val webMethod = telegramWebSettingsRowsFingerprint.methodOrNull

        // In both Telegram 13.1.1 and Telegram Web 13.1.1 the ArrayList is
        // already held in v1 at the end of the builder. Do not overwrite it.
        // v2/v3 are scratch locals at this point in the method.
        val telegramRowInstructions = """
            sget-object v0, Lorg/telegram/messenger/ApplicationLoader;->applicationContext:Landroid/content/Context;
            invoke-static {v0}, Lapp/template/extension/telegram/MorpheSettingsEntry;->createSettingsRow(Landroid/content/Context;)Landroid/view/View;
            move-result-object v0
            new-instance v2, Lorg/telegram/ui/Components/r71;
            const/4 v3, -0x4
            invoke-direct {v2, v3}, Lorg/telegram/ui/Components/r71;-><init>(I)V
            iput-object v0, v2, Lorg/telegram/ui/Components/r71;->c:Landroid/view/View;
            invoke-static {v0}, Lapp/template/extension/telegram/MorpheSettingsEntry;->getRowHeightPx(Landroid/content/Context;)I
            move-result v3
            iput v3, v2, Lorg/telegram/ui/Components/r71;->z:I
            invoke-virtual {v1, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
        """.trimIndent()

        when {
            telegramMethod != null -> telegramMethod.addInstructions(
                telegramMethod.implementation!!.instructions.size - 1, telegramRowInstructions,
            )
            webMethod != null -> webMethod.addInstructions(
                webMethod.implementation!!.instructions.size - 1, telegramRowInstructions,
            )
            else -> error("Could not resolve current Telegram Settings list builder (p91/t91/da4)")
        }

        val startupMethod = requireNotNull(postInitApplicationFingerprint.methodOrNull) {
            "Missing ApplicationLoader.postInitApplication()V"
        }
        startupMethod.addInstructions(
            startupMethod.implementation!!.instructions.size - 1,
            "invoke-static {}, Lapp/template/extension/telegram/MorpheForwardingRuntime;->install()V",
        )
    }
}
