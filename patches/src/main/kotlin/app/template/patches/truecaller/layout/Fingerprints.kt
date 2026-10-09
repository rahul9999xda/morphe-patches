package app.template.patches.truecaller.layout

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.Opcode

/** Verified against clean Truecaller 26.39.6 / build 2639006. */
internal val ScamFeedFeatureGateFingerprint = Fingerprint(
    definingClass = "Ley00;", name = "a", returnType = "Z", parameters = emptyList(),
    strings = listOf("featureScamFeedBottomTab")
)

internal val BottomBarRebuildFingerprint = Fingerprint(
    definingClass = "Lxm6;",
    name = "g",
    returnType = "V",
    parameters = listOf("Landroid/util/SparseIntArray;"),
    custom = { method, _ ->
        val instructions = method.implementation?.instructions ?: emptyList()
        val hasPremiumField = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "Lum6;" && reference.name == "d" && reference.type == "Lpsw;"
        }
        val hasVoicemailField = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "Lum6;" && reference.name == "f" && reference.type == "Lgf3;"
        }
        val hasListFilter = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.definingClass == "Lmb3;" && reference.name == "F"
        }
        hasPremiumField && hasVoicemailField && hasListFilter
    },
)

/**
 * The Get Premium settings row is a dedicated Lt77 view obtained from
 * BlockSettingsFragment.I and wired by bd6.invoke() in build 2639006.
 */
internal val HidePremiumSettingsRowFingerprint = Fingerprint(
    definingClass = "Lbd6;",
    name = "invoke",
    returnType = "Ljava/lang/Object;",
    parameters = emptyList(),
    custom = { method, _ ->
        val instructions = method.implementation?.instructions ?: emptyList()
        val hasGetPremiumLazy = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "Lcom/truecaller/settings/impl/ui/block/BlockSettingsFragment;" &&
                reference.name == "I" && reference.type == "Lkotlin/Lazy;"
        }
        val hasGetPremiumViewCast = instructions.any { instruction ->
            instruction.opcode == Opcode.CHECK_CAST &&
                ((instruction as? ReferenceInstruction)?.reference as? com.android.tools.smali.dexlib2.iface.reference.TypeReference)?.type == "Lt77;"
        }
        val hasClickListenerConstructor = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.definingClass == "Lcd6;" && reference.name == "<init>" &&
                reference.parameterTypes.map { it.toString() } ==
                listOf("Lcom/truecaller/settings/impl/ui/block/BlockSettingsFragment;", "B")
        }
        hasGetPremiumLazy && hasGetPremiumViewCast && hasClickListenerConstructor
    },
)
