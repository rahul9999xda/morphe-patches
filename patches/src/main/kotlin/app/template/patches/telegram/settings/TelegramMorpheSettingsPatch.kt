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
        val telegramRowInstructions = """
            sget-object v4, Lorg/telegram/messenger/ApplicationLoader;->applicationContext:Landroid/content/Context;
            invoke-static {v4}, Lapp/template/extension/telegram/MorpheSettingsEntry;->createSettingsRow(Landroid/content/Context;)Landroid/view/View;
            move-result-object v0
            invoke-static {v0}, Lorg/telegram/ui/Components/r71;->l(Landroid/view/View;)Lorg/telegram/ui/Components/r71;
            move-result-object v2
            invoke-virtual {v1, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
        """.trimIndent()
        val webRowInstructions = """
            sget-object v4, Lorg/telegram/messenger/ApplicationLoader;->applicationContext:Landroid/content/Context;
            invoke-static {v4}, Lapp/template/extension/telegram/MorpheSettingsEntry;->createSettingsRow(Landroid/content/Context;)Landroid/view/View;
            move-result-object v0
            invoke-static {v0}, Lorg/telegram/ui/Components/g71;->l(Landroid/view/View;)Lorg/telegram/ui/Components/g71;
            move-result-object v2
            invoke-virtual {v1, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
        """.trimIndent()
        val plusRowInstructions = """
            sget-object v4, Lorg/telegram/messenger/ApplicationLoader;->applicationContext:Landroid/content/Context;
            invoke-static {v4}, Lapp/template/extension/telegram/MorpheSettingsEntry;->createSettingsRow(Landroid/content/Context;)Landroid/view/View;
            move-result-object v0
            invoke-static {v0}, Lorg/telegram/ui/Components/qb2;->C(Landroid/view/View;)Lorg/telegram/ui/Components/qb2;
            move-result-object v2
            invoke-virtual {p1, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
        """.trimIndent()
        when {
            telegramMethod != null -> telegramMethod.addInstructions(
                telegramMethod.implementation!!.instructions.size - 1, telegramRowInstructions,
            )
            webMethod != null -> webMethod.addInstructions(
                webMethod.implementation!!.instructions.size - 1, webRowInstructions,
            )
            plusMethod != null -> {
                // Plus 13.1.1.0's Z0(ArrayList, mc2) has two distinct return paths.
                // The previous end-only injection was skipped by the early-return path.
                // Add the row before BOTH returns, processing descending indices so the
                // earlier instruction positions remain stable after each insertion.
                val instructions = plusMethod.implementation!!.instructions
                val returnIndexes = instructions.mapIndexedNotNull { index, instruction ->
                    index.takeIf { instruction.opcode.name == "RETURN_VOID" }
                }.sortedDescending()
                require(returnIndexes.size == 2) {
                    "Unexpected Plus da4.Z0() control flow: expected 2 RETURN_VOID sites, found ${returnIndexes.size}"
                }
                returnIndexes.forEach { index ->
                    plusMethod.addInstructions(index, plusRowInstructions)
                }
            }
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
