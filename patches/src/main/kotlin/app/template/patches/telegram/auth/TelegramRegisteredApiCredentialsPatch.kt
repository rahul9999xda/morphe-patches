package app.template.patches.telegram.auth

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

private const val TELEGRAM_BUILD_VARS = "Lorg/telegram/messenger/m2;"

/**
 * Telegram 13.3.1 and Telegram Web 13.3.1 both initialize the API credentials in
 * org.telegram.messenger.m2.<clinit>(). The API ID is assigned to static field d and
 * the API hash to static field e. Both login request paths (auth.sendCode and Firebase
 * phone-number verification) read these same fields, so changing the initialization
 * values is narrower and safer than patching individual login request constructors.
 */
private val TelegramBuildVarsStaticInitializerFingerprint = Fingerprint(
    accessFlags = listOf(AccessFlags.STATIC, AccessFlags.CONSTRUCTOR),
    parameters = emptyList(),
    custom = { method, classDef ->
        if (classDef.type != TELEGRAM_BUILD_VARS || method.name != "<clinit>") {
            false
        } else {
            val instructions = method.implementation?.instructions?.toList().orEmpty()
            instructions.indices.any { index ->
                val field = (instructions[index] as? ReferenceInstruction)?.reference as? FieldReference
                field?.definingClass == TELEGRAM_BUILD_VARS &&
                    field.name == "d" && field.type == "I" && index > 0 &&
                    instructions[index - 1] is OneRegisterInstruction
            } && instructions.indices.any { index ->
                val field = (instructions[index] as? ReferenceInstruction)?.reference as? FieldReference
                field?.definingClass == TELEGRAM_BUILD_VARS &&
                    field.name == "e" && field.type == "Ljava/lang/String;" && index > 0 &&
                    instructions[index - 1] is OneRegisterInstruction
            }
        }
    },
)

@Suppress("unused")
val telegramRegisteredApiCredentialsPatch = bytecodePatch(
    name = "Use registered Telegram API credentials",
    description = "Uses the API ID and API hash of an app registered at my.telegram.org instead of Telegram's bundled credentials. This can help when Telegram rejects the bundled API ID during sign-in. Telegram still chooses the available login-code delivery methods.",
    default = false,
) {
    compatibleWith(TELEGRAM_COMPATIBILITY, TELEGRAM_WEB_COMPATIBILITY)

    val apiIdOption by stringOption(
        key = "apiId",
        default = null,
        title = "API ID",
        description = "App api_id from https://my.telegram.org/apps. Leave both fields empty to keep Telegram's bundled credentials.",
        required = false,
    ) { value -> value == null || value.isBlank() || value.trim().toIntOrNull()?.let { it > 0 } == true }

    val apiHashOption by stringOption(
        key = "apiHash",
        default = null,
        title = "API hash",
        description = "App api_hash from https://my.telegram.org/apps. Keep this value private. Leave both fields empty to keep Telegram's bundled credentials.",
        required = false,
    ) { value -> value == null || value.isBlank() || value.trim().matches(Regex("^[0-9a-fA-F]{32}$")) }

    execute {
        val apiIdText = apiIdOption?.trim()?.takeIf(String::isNotEmpty)
        val apiHash = apiHashOption?.trim()?.takeIf(String::isNotEmpty)

        if (apiIdText == null && apiHash == null) {
            println("Registered Telegram API credentials: both options are empty; bundled credentials retained.")
            return@execute
        }
        if (apiIdText == null || apiHash == null) {
            throw PatchException("Both apiId and apiHash must be supplied together. Leave both empty to retain Telegram's bundled credentials.")
        }

        val apiId = apiIdText.toIntOrNull()
            ?: throw PatchException("apiId must be a positive 32-bit integer.")
        if (apiId <= 0) throw PatchException("apiId must be a positive 32-bit integer.")
        if (!apiHash.matches(Regex("^[0-9a-fA-F]{32}$"))) {
            throw PatchException("apiHash must contain exactly 32 hexadecimal characters.")
        }

        val instructions = TelegramBuildVarsStaticInitializerFingerprint.method
            .implementation?.instructions?.toList()
            ?: throw PatchException("Telegram BuildVars static initializer has no implementation.")

        val apiIdFieldIndex = instructions.indices.firstOrNull { index ->
            val field = (instructions[index] as? ReferenceInstruction)?.reference as? FieldReference
            field?.definingClass == TELEGRAM_BUILD_VARS && field.name == "d" && field.type == "I"
        } ?: throw PatchException("Could not locate the API ID assignment in Telegram BuildVars.")

        val apiHashFieldIndex = instructions.indices.firstOrNull { index ->
            val field = (instructions[index] as? ReferenceInstruction)?.reference as? FieldReference
            field?.definingClass == TELEGRAM_BUILD_VARS && field.name == "e" && field.type == "Ljava/lang/String;"
        } ?: throw PatchException("Could not locate the API hash assignment in Telegram BuildVars.")

        if (apiIdFieldIndex == 0 || apiHashFieldIndex == 0) {
            throw PatchException("Telegram BuildVars assignments do not have the expected constant producers.")
        }

        val apiIdProducer = instructions[apiIdFieldIndex - 1] as? OneRegisterInstruction
            ?: throw PatchException("API ID assignment is not preceded by a register constant.")
        val apiHashProducer = instructions[apiHashFieldIndex - 1] as? OneRegisterInstruction
            ?: throw PatchException("API hash assignment is not preceded by a register constant.")

        val apiIdOpcode = instructions[apiIdFieldIndex - 1].opcode
        if (apiIdOpcode != com.android.tools.smali.dexlib2.Opcode.CONST_4 &&
            apiIdOpcode != com.android.tools.smali.dexlib2.Opcode.CONST_16 &&
            apiIdOpcode != com.android.tools.smali.dexlib2.Opcode.CONST) {
            throw PatchException("Unexpected API ID constant instruction: $apiIdOpcode")
        }

        val apiHashOpcode = instructions[apiHashFieldIndex - 1].opcode
        if (apiHashOpcode != com.android.tools.smali.dexlib2.Opcode.CONST_STRING &&
            apiHashOpcode != com.android.tools.smali.dexlib2.Opcode.CONST_STRING_JUMBO) {
            throw PatchException("Unexpected API hash constant instruction: $apiHashOpcode")
        }

        val apiIdRegister = apiIdProducer.registerA
        val apiHashRegister = apiHashProducer.registerA
        val escapedHash = apiHash.replace("\\", "\\\\").replace("\"", "\\\"")

        TelegramBuildVarsStaticInitializerFingerprint.method.apply {
            replaceInstruction(apiIdFieldIndex - 1, "const v$apiIdRegister, 0x${apiId.toString(16)}")
            replaceInstruction(apiHashFieldIndex - 1, "const-string v$apiHashRegister, \"$escapedHash\"")
        }

        println("Registered Telegram API credentials applied to BuildVars (API ID $apiId; hash not logged).")
    }
}
