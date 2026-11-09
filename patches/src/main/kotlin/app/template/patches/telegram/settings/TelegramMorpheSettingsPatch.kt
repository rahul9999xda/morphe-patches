package app.template.patches.telegram.settings

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency
import com.android.tools.smali.dexlib2.Opcode
import org.w3c.dom.Element

// Current 13.2.1 row builders, independently resolved for Android and Web.
private val telegramSettingsRowsCurrentFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/c51;", name = "e0", returnType = "V",
    parameters = listOf("Lorg/telegram/ui/c51;", "Ljava/util/ArrayList;"),
)
private val telegramWebSettingsRowsCurrentFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/h51;", name = "f0", returnType = "V",
    parameters = listOf("Lorg/telegram/ui/h51;", "Ljava/util/ArrayList;"),
)
// Preserve historical fingerprints as an independent fallback.
private val telegramSettingsRowsLegacyFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/p91;", name = "b0", returnType = "V",
    parameters = listOf("Lorg/telegram/ui/p91;", "Ljava/util/ArrayList;"),
)
private val telegramWebSettingsRowsLegacyFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/ui/t91;", name = "b0", returnType = "V",
    parameters = listOf("Lorg/telegram/ui/t91;", "Ljava/util/ArrayList;"),
)
private val postInitApplicationFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/ApplicationLoader;", name = "postInitApplication",
    returnType = "V", parameters = emptyList(),
)
private val telegramApplicationOnCreateFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/u0;", name = "onCreate", returnType = "V", parameters = emptyList(),
)
private val telegramWebApplicationOnCreateFingerprint = Fingerprint(
    definingClass = "Lorg/telegram/messenger/t0;", name = "onCreate", returnType = "V", parameters = emptyList(),
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

private fun settingsRowInstructions(componentClass: String, factoryMethod: String, contextField: String): String = """
    sget-object v4, $contextField:Landroid/content/Context;
    invoke-static {v4}, Lapp/template/extension/telegram/MorpheSettingsEntry;->createSettingsRow(Landroid/content/Context;)Landroid/view/View;
    move-result-object v0
    invoke-static {v0}, Lorg/telegram/ui/Components/$componentClass;->$factoryMethod(Landroid/view/View;)Lorg/telegram/ui/Components/$componentClass;
    move-result-object v2
    invoke-virtual {p1, v2}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
""".trimIndent()

/**
 * Add the row on every method exit. These builders are static methods whose
 * parameters are (settingsController, ArrayList); therefore p0 is the controller
 * and p1 is the actual ArrayList. There are two return sites in the audited 13.2.1
 * builders. Insert in reverse index order so earlier indices remain stable.
 */

@Suppress("unused")
val telegramMorpheSettingsPatch = bytecodePatch(
    name = "Morphe Settings",
    description = "Adds Morphe Settings to Telegram Android and Telegram Web and initializes forwarding at startup.",
) {
    compatibleWith(TELEGRAM_COMPATIBILITY, TELEGRAM_WEB_COMPATIBILITY)
    dependsOn(telegramSpoofDependency(), morpheSettingsActivityManifestPatch)

    execute {
/**
 * Resolve the editable method through the patcher's mutable class API. Fingerprint.methodOrNull
 * returns a read-only Method in this Morphe Patcher version, so it must only be used to
 * establish that the target exists; mutations are performed on mutableClassDefBy(...).
 */
fun patchSettingsBuilder(
    targetClass: String,
    targetMethod: String,
    componentClass: String,
    factoryMethod: String,
    contextField: String,
    minRegisters: Int,
) {
    classDefForEach { classDef ->
        if (classDef.type != targetClass) return@classDefForEach

        val mutableMethod = mutableClassDefBy(classDef).methods.firstOrNull { method ->
            method.name == targetMethod &&
                method.parameterTypes.map { it.toString() } == listOf(targetClass, "Ljava/util/ArrayList;") &&
                method.returnType == "V"
        } ?: error("Could not resolve editable settings builder $targetClass->$targetMethod")

        val implementation = requireNotNull(mutableMethod.instructionsOrNull) {
            "Settings builder $targetClass->$targetMethod has no implementation"
        }
        val returnIndexes = implementation.mapIndexedNotNull { index, instruction ->
            index.takeIf { instruction.opcode == Opcode.RETURN_VOID }
        }
        require(returnIndexes.isNotEmpty()) {
            "Unexpected $targetClass->$targetMethod control flow: no RETURN_VOID site found"
        }
        val registerCount = mutableMethod.implementation!!.registerCount
        require(registerCount >= minRegisters) {
            "Unexpected $targetClass->$targetMethod register layout: expected at least $minRegisters registers, found $registerCount"
        }
        // A static method with two parameters maps them to p0 and p1. Validate that
        // p1 exists and is the ArrayList parameter before emitting the append call.
        require((mutableMethod.accessFlags and 0x8) != 0) {
            "Unexpected $targetClass->$targetMethod: expected static settings builder"
        }
        require(mutableMethod.parameterTypes.map { it.toString() } == listOf(targetClass, "Ljava/util/ArrayList;")) {
            "Unexpected parameter layout for $targetClass->$targetMethod"
        }
        val instructions = settingsRowInstructions(componentClass, factoryMethod, contextField)
        returnIndexes.asReversed().forEach { index ->
            mutableMethod.addInstructions(index, instructions)
        }
    }
}


        val currentAndroid = telegramSettingsRowsCurrentFingerprint.methodOrNull
        val currentWeb = telegramWebSettingsRowsCurrentFingerprint.methodOrNull
        val legacyAndroid = telegramSettingsRowsLegacyFingerprint.methodOrNull
        val legacyWeb = telegramWebSettingsRowsLegacyFingerprint.methodOrNull

        when {
            currentAndroid != null && currentWeb == null -> patchSettingsBuilder(
                "Lorg/telegram/ui/c51;", "e0", "t41", "m", "Lorg/telegram/messenger/u0;->b", 26,
            )
            currentWeb != null && currentAndroid == null -> patchSettingsBuilder(
                "Lorg/telegram/ui/h51;", "f0", "m41", "m", "Lorg/telegram/messenger/t0;->b", 24,
            )
            currentAndroid == null && currentWeb == null && legacyAndroid != null && legacyWeb == null -> patchSettingsBuilder(
                "Lorg/telegram/ui/p91;", "b0", "r71", "l", "Lorg/telegram/messenger/ApplicationLoader;->applicationContext", 22,
            )
            currentAndroid == null && currentWeb == null && legacyWeb != null && legacyAndroid == null -> patchSettingsBuilder(
                "Lorg/telegram/ui/t91;", "b0", "g71", "l", "Lorg/telegram/messenger/ApplicationLoader;->applicationContext", 22,
            )
            else -> error("Could not uniquely resolve the Telegram Android or Telegram Web Morphe Settings row builder")
        }

        when {
            currentAndroid != null -> requireNotNull(telegramApplicationOnCreateFingerprint.methodOrNull) {
                "Missing Telegram Android Application.onCreate()V"
            }
            currentWeb != null -> requireNotNull(telegramWebApplicationOnCreateFingerprint.methodOrNull) {
                "Missing Telegram Web Application.onCreate()V"
            }
            else -> requireNotNull(postInitApplicationFingerprint.methodOrNull) {
                "Missing legacy ApplicationLoader.postInitApplication()V"
            }
        }
        val startupClass = when {
            currentAndroid != null -> "Lorg/telegram/messenger/u0;"
            currentWeb != null -> "Lorg/telegram/messenger/t0;"
            else -> "Lorg/telegram/messenger/ApplicationLoader;"
        }
        val startupName = if (currentAndroid != null || currentWeb != null) "onCreate" else "postInitApplication"
        classDefForEach { classDef ->
            if (classDef.type != startupClass) return@classDefForEach
            val startupMethod = mutableClassDefBy(classDef).methods.firstOrNull {
                it.name == startupName && it.parameterTypes.isEmpty() && it.returnType == "V"
            } ?: error("Could not resolve editable startup method $startupClass->$startupName()V")
            val startupInstructions = requireNotNull(startupMethod.instructionsOrNull) {
                "Startup method $startupClass->$startupName()V has no implementation"
            }
            val startupReturnIndexes = startupInstructions.mapIndexedNotNull { index, instruction ->
                index.takeIf { instruction.opcode == Opcode.RETURN_VOID }
            }
            require(startupReturnIndexes.size == 1) {
                "Unexpected startup control flow: expected 1 RETURN_VOID site, found ${startupReturnIndexes.size}"
            }
            val runtimeInstallInstructions = when {
                currentAndroid != null -> """
                    const/4 v0, 0x0
                    invoke-static {v0}, Lapp/template/extension/telegram/MorpheForwardingRuntime;->install(I)V
                """.trimIndent()
                currentWeb != null -> """
                    const/4 v0, 0x1
                    invoke-static {v0}, Lapp/template/extension/telegram/MorpheForwardingRuntime;->install(I)V
                """.trimIndent()
                else -> "invoke-static {}, Lapp/template/extension/telegram/MorpheForwardingRuntime;->install()V"
            }
            // Insert immediately before the actual normal return, never before the
            // final instruction blindly: obfuscated methods can end in a throw path.
            startupMethod.addInstructions(startupReturnIndexes.single(), runtimeInstallInstructions)
        }
    }
}
