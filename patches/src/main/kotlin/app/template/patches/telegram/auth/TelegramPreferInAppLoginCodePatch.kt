package app.template.patches.telegram.auth

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val CODE_SETTINGS = "Lorg/telegram/tgnet/TLRPC\$TL_codeSettings;"
private const val ALLOW_FIREBASE = "allow_firebase"
private const val LOGOUT_TOKENS = "logout_tokens"

/**
 * Telegram 13.3.1: af0.h(String) builds the initial auth.sendCode CodeSettings.
 * Its stock logic sets allow_firebase from Google Play Services availability and
 * may clear it for other conditions. We replace only the value register used by the
 * first allow_firebase write with a known zero register. The original hasServices()
 * result remains intact for allow_app_hash, and logout_tokens remain untouched.
 */
private val Telegram1331LoginCodeSettingsFingerprint = Fingerprint(
    parameters = listOf("Ljava/lang/String;"),
    returnType = "V",
    custom = { method, classDef ->
        if (classDef.type != "Lorg/telegram/ui/af0;" || method.name != "h") {
            false
        } else {
            val instructions = method.implementation?.instructions?.toList().orEmpty()
            val firebaseWrites = instructions.mapIndexedNotNull { index, instruction ->
                val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference
                index.takeIf {
                    instruction.opcode == Opcode.IPUT_BOOLEAN &&
                        field?.definingClass == CODE_SETTINGS && field.name == ALLOW_FIREBASE && field.type == "Z"
                }
            }
            firebaseWrites.size == 2 && instructions.any { instruction ->
                val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference
                field?.definingClass == CODE_SETTINGS && field.name == LOGOUT_TOKENS
            }
        }
    },
)

/** Telegram Web 13.3.1: equivalent R8 method, independently verified in its DEX. */
private val TelegramWeb1331LoginCodeSettingsFingerprint = Fingerprint(
    parameters = listOf("Ljava/lang/String;"),
    returnType = "V",
    custom = { method, classDef ->
        if (classDef.type != "Lorg/telegram/ui/ye0;" || method.name != "h") {
            false
        } else {
            val instructions = method.implementation?.instructions?.toList().orEmpty()
            val firebaseWrites = instructions.mapIndexedNotNull { index, instruction ->
                val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference
                index.takeIf {
                    instruction.opcode == Opcode.IPUT_BOOLEAN &&
                        field?.definingClass == CODE_SETTINGS && field.name == ALLOW_FIREBASE && field.type == "Z"
                }
            }
            firebaseWrites.size == 2 && instructions.any { instruction ->
                val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference
                field?.definingClass == CODE_SETTINGS && field.name == LOGOUT_TOKENS
            }
        }
    },
)

private fun applyInAppPreference(fingerprint: Fingerprint, target: String, zeroRegister: Int) {
    val method = fingerprint.methodOrNull
        ?: throw PatchException("Could not locate the verified $target login CodeSettings builder.")
    val instructions = method.implementation?.instructions?.toList()
        ?: throw PatchException("$target login CodeSettings builder has no implementation.")

    val firebaseWriteIndexes = instructions.mapIndexedNotNull { index, instruction ->
        val field = (instruction as? ReferenceInstruction)?.reference as? FieldReference
        index.takeIf {
            instruction.opcode == Opcode.IPUT_BOOLEAN &&
                field?.definingClass == CODE_SETTINGS && field.name == ALLOW_FIREBASE && field.type == "Z"
        }
    }
    if (firebaseWriteIndexes.size != 2) {
        throw PatchException("Unexpected $target allow_firebase write count: ${firebaseWriteIndexes.size}.")
    }

    val firstFirebaseWrite = firebaseWriteIndexes.first()
    val firebaseInstruction = instructions[firstFirebaseWrite] as? TwoRegisterInstruction
        ?: throw PatchException("Unexpected $target allow_firebase instruction format.")
    val settingsRegister = firebaseInstruction.registerB

    // Each audited build initializes a dedicated zero register immediately before
    // constructing CodeSettings: v8 in Telegram, v9 in Telegram Web. It remains
    // untouched up to the first allow_firebase write. Reusing it changes only that
    // field; the hasServices() result remains available for allow_app_hash.
    val zeroInitialized = instructions.take(firstFirebaseWrite).any { instruction ->
        instruction.opcode == Opcode.CONST_4 &&
            (instruction as? OneRegisterInstruction)?.registerA == zeroRegister &&
            (instruction as? NarrowLiteralInstruction)?.narrowLiteral == 0L
    }
    if (!zeroInitialized) {
        throw PatchException("The verified zero register v$zeroRegister is not initialized in $target.")
    }
    if (firebaseInstruction.registerA == zeroRegister) {
        throw PatchException("Unexpected $target register alias between allow_firebase value and zero register.")
    }

    method.replaceInstruction(
        firstFirebaseWrite,
        "iput-boolean v$zeroRegister, v$settingsRegister, $CODE_SETTINGS->$ALLOW_FIREBASE:Z",
    )
}

@Suppress("unused")
val telegramPreferInAppLoginCodePatch = bytecodePatch(
    name = "Prefer in-app login codes",
    description = "Disables Firebase SMS eligibility in the initial login request so Telegram can select another supported method, including a code in an existing Telegram session when its servers permit it. Does not force server-side delivery or bypass email, 2-step verification, or other authentication requirements.",
    default = false,
) {
    compatibleWith(TELEGRAM_COMPATIBILITY, TELEGRAM_WEB_COMPATIBILITY)

    execute {
        val android = Telegram1331LoginCodeSettingsFingerprint.methodOrNull
        val web = TelegramWeb1331LoginCodeSettingsFingerprint.methodOrNull
        if ((android == null) == (web == null)) {
            throw PatchException("Expected exactly one Telegram 13.3.1 or Telegram Web 13.3.1 login fingerprint; found Android=${android != null}, Web=${web != null}.")
        }
        if (android != null) {
            applyInAppPreference(Telegram1331LoginCodeSettingsFingerprint, "Telegram 13.3.1", zeroRegister = 8)
        } else {
            applyInAppPreference(TelegramWeb1331LoginCodeSettingsFingerprint, "Telegram Web 13.3.1", zeroRegister = 9)
        }
    }
}
