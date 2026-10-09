package app.template.patches.truecaller.misc

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/** Verified against clean Truecaller 26.39.6 / build 2639006. */
internal val UpdateTriggerEntryFingerprint = Fingerprint(
    definingClass = "La8n;",
    name = "j",
    returnType = "Ljava/util/concurrent/CompletableFuture;",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Lcom/truecaller/inappupdate/UpdateTrigger;",
    ),
    custom = { method, _ ->
        val instructions = method.implementation?.instructions ?: emptyList()
        val hasCoroutineCall = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.definingClass == "Ljvb;" && reference.name == "k"
        }
        val hasCoroutineResultConstructor = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.definingClass == "Lzll;" && reference.name == "<init>"
        }
        hasCoroutineCall && hasCoroutineResultConstructor
    },
)

internal val OtpSelectorFingerprint = Fingerprint(
    definingClass = "Lco70;",
    name = "invoke",
    returnType = "Ljava/lang/Object;",
    parameters = emptyList(),
    strings = listOf("verificationOtpSmsApi_19731"),
    custom = { method, _ ->
        val instructions = method.implementation?.instructions ?: emptyList()
        val hasSms = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "Lcom/truecaller/wizard/verification/otp/sms/OtpSmsApi;" && reference.name == "SMS"
        }
        val hasGoogle = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "Lcom/truecaller/wizard/verification/otp/sms/OtpSmsApi;" && reference.name == "GOOGLE"
        }
        hasSms && hasGoogle
    },
)

/** Verified in the clean 26.39.6 APKM. */
internal val TelemetryEnableTrackingFingerprint = Fingerprint(
    definingClass = "Lcom/truecaller/analytics/technical/AppStartTracker;",
    name = "enableTracking",
    returnType = "V",
    parameters = emptyList(),
    custom = { method, _ ->
        val instructions = method.implementation?.instructions ?: emptyList()
        val hasEnabledField = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "Lcom/truecaller/analytics/technical/AppStartTracker;" && reference.name == "isEnabled" && reference.type == "Z"
        }
        val hasSputBoolean = instructions.any { instruction -> instruction.opcode == Opcode.SPUT_BOOLEAN }
        hasEnabledField && hasSputBoolean
    },
)
