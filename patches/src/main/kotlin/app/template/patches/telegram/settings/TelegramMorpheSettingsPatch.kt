package app.template.patches.telegram.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency

/**
 * Telegram Web 13.1.1 Settings integration.
 *
 * Important register rule: keep the Context in v4. createSettingsRow() returns
 * a View, so its result must never overwrite the Context register subsequently
 * passed to another Context-taking method. g71.l(View) creates Telegram's
 * custom-view settings item and initializes its height to -1 (view-driven).
 */
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
    description = "Adds the Morphe Settings row and initializes automatic forwarding.",
) {
    compatibleWith(TELEGRAM_WEB_COMPATIBILITY)
    dependsOn(telegramSpoofDependency())

    execute {
        val settingsMethod = requireNotNull(telegramWebSettingsRowsFingerprint.methodOrNull) {
            "Could not resolve Telegram Web Settings list builder t91.b0(t91, ArrayList)"
        }

        // At the end of this method v1 is the existing ArrayList. v4 is a
        // dead local at this insertion point and is used to preserve Context.
        settingsMethod.addInstructions(
            settingsMethod.implementation!!.instructions.size - 1,
            """
                sget-object v4, Lorg/telegram/messenger/ApplicationLoader;->applicationContext:Landroid/content/Context;
                invoke-static {v4}, Lapp/template/extension/telegram/MorpheSettingsEntry;->createSettingsRow(Landroid/content/Context;)Landroid/view/View;
                move-result-object v0
                invoke-static {v0}, Lorg/telegram/ui/Components/g71;->l(Landroid/view/View;)Lorg/telegram/ui/Components/g71;
                move-result-object v2
                invoke-virtual {v1, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
            """.trimIndent(),
        )

        val startupMethod = requireNotNull(postInitApplicationFingerprint.methodOrNull) {
            "Missing ApplicationLoader.postInitApplication()V"
        }
        startupMethod.addInstructions(
            startupMethod.implementation!!.instructions.size - 1,
            "invoke-static {}, Lapp/template/extension/telegram/MorpheForwardingRuntime;->install()V",
        )
    }
}
