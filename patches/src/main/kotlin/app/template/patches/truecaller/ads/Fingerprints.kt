package app.template.patches.truecaller.ads

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/** Verified against clean Truecaller 26.39.6 / build 2639006. */
internal val CentralAdRequestFingerprint = Fingerprint(
    definingClass = "Lb0q;",
    name = "a",
    returnType = "Ljava/lang/Object;",
    parameters = listOf(
        "Lcom/truecaller/ads/api/model/ad/AdScreen;",
        "Lcom/truecaller/ads/api/model/ad/AdPlacement;",
        "Z",
        "Lqrc;",
    ),
    custom = { method, _ ->
        val instructions = method.implementation?.instructions ?: emptyList()
        val hasListSource = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass?.contains("DlRequestSourceType") == true && reference.name == "List"
        }
        val hasAdSourceGetter = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.definingClass?.contains("DlRequestSource") == true && reference.name == "getAdRequestSource"
        }
        val hasUuid = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.definingClass == "Ljava/util/UUID;" && reference.name == "randomUUID"
        }
        hasListSource && hasAdSourceGetter && hasUuid
    },
)
