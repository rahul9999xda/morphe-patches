package app.template.patches.telegram.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency

// R8-renamed Settings list builders resolved from the supplied clean 13.1.1 DEX files.
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
private val plusSettingsRowsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/da4;",
    name = "Z0",
    returnType = "V",
    parameters = listOf("Ljava/util/ArrayList;", "Lorg/telegram/ui/Components/mc2;"),
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
    compatibleWith(TELEGRAM_COMPATIBILITY, TELEGRAM_WEB_COMPATIBILITY, TELEGRAM_PLUS_COMPATIBILITY)
    dependsOn(telegramSpoofDependency())

    execute {
        val telegramMethod = telegramSettingsRowsFingerprint.methodOrNull
        val webMethod = telegramWebSettingsRowsFingerprint.methodOrNull
        val plusMethod = plusSettingsRowsFingerprint.methodOrNull

        val telegramRowInstructions = """
            sget-object v0, Lorg/telegram/messenger/ApplicationLoader;->applicationContext:Landroid/content/Context;
            invoke-static {v0}, Lapp/template/extension/telegram/MorpheSettingsEntry;->createSettingsRow(Landroid/content/Context;)Landroid/view/View;
            move-result-object v0
            new-instance v1, Lorg/telegram/ui/Components/r71;
            const/4 v2, -0x4
            invoke-direct {v1, v2}, Lorg/telegram/ui/Components/r71;-><init>(I)V
            iput-object v0, v1, Lorg/telegram/ui/Components/r71;->c:Landroid/view/View;
            invoke-static {v0}, Lapp/template/extension/telegram/MorpheSettingsEntry;->getRowHeightPx(Landroid/content/Context;)I
            move-result v2
            iput v2, v1, Lorg/telegram/ui/Components/r71;->z:I
            invoke-virtual {p1, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
            move-result v2
        """.trimIndent()

        when {
            telegramMethod != null -> telegramMethod.addInstructions(
                telegramMethod.implementation!!.instructions.size - 1, telegramRowInstructions,
            )
            webMethod != null -> webMethod.addInstructions(
                webMethod.implementation!!.instructions.size - 1, telegramRowInstructions,
            )
            plusMethod != null -> plusMethod.addInstructions(
                plusMethod.implementation!!.instructions.size - 1,
                """
                    sget-object v0, Lorg/telegram/messenger/ApplicationLoader;->applicationContext:Landroid/content/Context;
                    invoke-static {v0}, Lapp/template/extension/telegram/MorpheSettingsEntry;->createSettingsRow(Landroid/content/Context;)Landroid/view/View;
                    move-result-object v0
                    invoke-static {v0}, Lorg/telegram/ui/Components/qb2;->C(Landroid/view/View;)Lorg/telegram/ui/Components/qb2;
                    move-result-object v1
                    invoke-static {v0}, Lapp/template/extension/telegram/MorpheSettingsEntry;->getRowHeightPx(Landroid/content/Context;)I
                    move-result v2
                    iput v2, v1, Lorg/telegram/ui/Components/qb2;->B:I
                    invoke-virtual {p1, v1}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                    move-result v2
                """.trimIndent(),
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
