package app.template.patches.telegram.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency
import org.w3c.dom.Element

// Telegram 13.1.1 build 72371 and Telegram Web 13.1.1 build 72379.
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

private val morpheSettingsActivityManifestPatch = resourcePatch(
    name = "Register Morphe Settings Activity",
    description = "Registers the in-app Morphe settings Activity in the target Telegram manifest.",
) {
    execute {
        document("AndroidManifest.xml").use { doc ->
            val applications = doc.getElementsByTagName("application")
            require(applications.length > 0) { "AndroidManifest.xml has no application element" }
            val application = applications.item(0) as Element
            val androidNs = "http://schemas.android.com/apk/res/android"
            val activityName = "app.template.extension.telegram.MorpheSettingsActivity"
            var found = false
            val children = application.childNodes
            for (i in 0 until children.length) {
                val element = children.item(i) as? Element ?: continue
                if (element.tagName == "activity" &&
                    element.getAttributeNS(androidNs, "name") == activityName) {
                    element.setAttributeNS(androidNs, "android:exported", "false")
                    found = true
                    break
                }
            }
            if (!found) {
                val activity = doc.createElement("activity")
                activity.setAttributeNS(androidNs, "android:name", activityName)
                activity.setAttributeNS(androidNs, "android:exported", "false")
                application.appendChild(activity)
            }
        }
    }
}

@Suppress("unused")
val telegramMorpheSettingsPatch = bytecodePatch(
    name = "Morphe Settings",
    description = "Adds the original Morphe settings row and initializes forwarding at Telegram startup.",
) {
    compatibleWith(TELEGRAM_COMPATIBILITY, TELEGRAM_WEB_COMPATIBILITY, TELEGRAM_PLUS_COMPATIBILITY)
    dependsOn(telegramSpoofDependency(), morpheSettingsActivityManifestPatch)

    execute {
        val telegramMethod = telegramSettingsRowsFingerprint.methodOrNull
        val webMethod = telegramWebSettingsRowsFingerprint.methodOrNull
        val plusMethod = plusSettingsRowsFingerprint.methodOrNull
        when {
            // Telegram 13.1.1: the View wrapper is r71.l(View), not g71.l(View).
            // This distinction was checked against the supplied 72371 DEX.
            telegramMethod != null -> telegramMethod.addInstructions(
                telegramMethod.implementation!!.instructions.size - 1,
                """
                    sget-object v4, Lorg/telegram/messenger/ApplicationLoader;->applicationContext:Landroid/content/Context;
                    invoke-static {v4}, Lapp/template/extension/telegram/MorpheSettingsEntry;->createSettingsRow(Landroid/content/Context;)Landroid/view/View;
                    move-result-object v0
                    invoke-static {v0}, Lorg/telegram/ui/Components/r71;->l(Landroid/view/View;)Lorg/telegram/ui/Components/r71;
                    move-result-object v2
                    invoke-virtual {v1, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                """.trimIndent(),
            )
            // Telegram Web 13.1.1: g71.l(View) is the verified adapter factory.
            webMethod != null -> webMethod.addInstructions(
                webMethod.implementation!!.instructions.size - 1,
                """
                    sget-object v4, Lorg/telegram/messenger/ApplicationLoader;->applicationContext:Landroid/content/Context;
                    invoke-static {v4}, Lapp/template/extension/telegram/MorpheSettingsEntry;->createSettingsRow(Landroid/content/Context;)Landroid/view/View;
                    move-result-object v0
                    invoke-static {v0}, Lorg/telegram/ui/Components/g71;->l(Landroid/view/View;)Lorg/telegram/ui/Components/g71;
                    move-result-object v2
                    invoke-virtual {v1, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                """.trimIndent(),
            )
            // Plus Messenger 13.1.1.0: qb2.C(View) is the verified adapter factory.
            plusMethod != null -> plusMethod.addInstructions(
                plusMethod.implementation!!.instructions.size - 1,
                """
                    sget-object v4, Lorg/telegram/messenger/ApplicationLoader;->applicationContext:Landroid/content/Context;
                    invoke-static {v4}, Lapp/template/extension/telegram/MorpheSettingsEntry;->createSettingsRow(Landroid/content/Context;)Landroid/view/View;
                    move-result-object v0
                    invoke-static {v0}, Lorg/telegram/ui/Components/qb2;->C(Landroid/view/View;)Lorg/telegram/ui/Components/qb2;
                    move-result-object v2
                    invoke-virtual {p1, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                """.trimIndent(),
            )
            else -> error("Could not resolve Telegram 13.1.1 Settings builder (p91/t91/da4)")
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
