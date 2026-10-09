package app.template.patches.truecaller.layout

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

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


/** Fingerprint for the Get Premium row on Settings > Blocking. */
internal val HidePremiumSettingsRowFingerprint = Fingerprint(
    definingClass = "Lcom/truecaller/settings/impl/ui/block/BlockSettingsFragment;",
    name = "onResume",
    returnType = "V",
    parameters = emptyList(),
    custom = { method, _ ->
        val instructions = method.implementation?.instructions ?: emptyList()
        val hasPremiumRowField = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "Lcom/truecaller/settings/impl/ui/block/BlockSettingsFragment;" &&
                reference.name == "I" && reference.type == "Lkotlin/Lazy;"
        }
        val hasPremiumRowListener = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.definingClass == "Lcd6;" && reference.name == "<init>" &&
                reference.parameterTypes.map { it.toString() } ==
                    listOf("Lcom/truecaller/settings/impl/ui/block/BlockSettingsFragment;", "B")
        }
        hasPremiumRowField && hasPremiumRowListener
    },
)
