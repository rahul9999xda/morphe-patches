package app.template.patches.telegram.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.MutableMethod
import org.w3c.dom.Element

// Verified against the supplied Telegram 13.1.1 build 72371 APK.
private val telegramSettingsRowsFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/p91;",
    name = "b0",
    returnType = "V",
    parameters = listOf("Lorg/telegram/ui/p91;", "Ljava/util/ArrayList;"),
)

// Verified against the supplied Telegram Web 13.1.1 build 72379 APK.
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

private fun settingsRowInstructions(componentClass: String): String = """
    sget-object v4, Lorg/telegram/messenger/ApplicationLoader;->applicationContext:Landroid/content/Context;
    invoke-static {v4}, Lapp/template/extension/telegram/MorpheSettingsEntry;->createSettingsRow(Landroid/content/Context;)Landroid/view/View;
    move-result-object v0
    invoke-static {v0}, Lorg/telegram/ui/Components/$componentClass;->l(Landroid/view/View;)Lorg/telegram/ui/Components/$componentClass;
    move-result-object v2
    invoke-virtual {v1, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
""".trimIndent()

/**
 * Add the row on every normal method exit. The audited 13.1.1 methods each contain
 * two RETURN_VOID sites: one early exit and one normal completion. Insert in reverse
 * index order so earlier instruction indices remain stable.
 */
private fun addRowAtEveryReturn(method: MutableMethod, componentClass: String) {
    val implementation = requireNotNull(method.implementation) {
        "Settings builder ${method.definingClass}->${method.name} has no implementation"
    }
    val returnIndexes = implementation.instructions.mapIndexedNotNull { index, instruction ->
        index.takeIf { instruction.opcode == Opcode.RETURN_VOID }
    }
    require(returnIndexes.size == 2) {
        "Unexpected ${method.definingClass}->${method.name} control flow: expected 2 RETURN_VOID sites, found ${returnIndexes.size}"
    }
    require(implementation.registerCount >= 22) {
        "Unexpected ${method.definingClass}->${method.name} register layout: expected at least 22 registers, found ${implementation.registerCount}"
    }
    val instructions = settingsRowInstructions(componentClass)
    returnIndexes.asReversed().forEach { index -> method.addInstructions(index, instructions) }
}

@Suppress("unused")
val telegramMorpheSettingsPatch = bytecodePatch(
    name = "Morphe Settings",
    description = "Adds Morphe Settings to Telegram Android and Telegram Web and initializes forwarding at startup.",
) {
    compatibleWith(TELEGRAM_COMPATIBILITY, TELEGRAM_WEB_COMPATIBILITY)
    dependsOn(telegramSpoofDependency(), morpheSettingsActivityManifestPatch)

    execute {
        val telegramMethod = telegramSettingsRowsFingerprint.methodOrNull
        val webMethod = telegramWebSettingsRowsFingerprint.methodOrNull

        when {
            telegramMethod != null && webMethod == null -> addRowAtEveryReturn(telegramMethod, "r71")
            webMethod != null && telegramMethod == null -> addRowAtEveryReturn(webMethod, "g71")
            telegramMethod == null && webMethod == null -> error(
                "Could not resolve Telegram 13.1.1 Settings builder (p91/t91). Check APK version and fingerprints."
            )
            else -> error(
                "Ambiguous Morphe Settings target: both Telegram Android and Telegram Web settings builders matched."
            )
        }

        val startupMethod = requireNotNull(postInitApplicationFingerprint.methodOrNull) {
            "Missing ApplicationLoader.postInitApplication()V"
        }
        val startupImplementation = requireNotNull(startupMethod.implementation) {
            "ApplicationLoader.postInitApplication()V has no implementation"
        }
        val startupReturns = startupImplementation.instructions.count { it.opcode == Opcode.RETURN_VOID }
        require(startupReturns == 1) {
            "Unexpected ApplicationLoader.postInitApplication()V control flow: expected 1 RETURN_VOID site, found $startupReturns"
        }
        startupMethod.addInstructions(
            startupImplementation.instructions.size - 1,
            "invoke-static {}, Lapp/template/extension/telegram/MorpheForwardingRuntime;->install()V",
        )
    }
}
