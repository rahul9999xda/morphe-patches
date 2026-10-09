package app.template.patches.truecaller.blocking

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

/** Verified against clean Truecaller 26.39.6 / build 2639006. */
internal val TopSpammersFilterFingerprint = Fingerprint(
    definingClass = "Lhrj;", name = "g", returnType = "Z", parameters = emptyList(),
    strings = listOf("filter_filteringTopSpammers")
)

internal val NonPhonebookFilterFingerprint = Fingerprint(
    definingClass = "Lhrj;", name = "e", returnType = "Z", parameters = emptyList(),
    strings = listOf("filter_filteringNonPhonebook")
)

internal val ForeignNumbersFilterFingerprint = Fingerprint(
    definingClass = "Lhrj;", name = "c", returnType = "Z", parameters = emptyList(),
    strings = listOf("filter_filteringForeignNumbers")
)

internal val NeighbourSpoofingFilterFingerprint = Fingerprint(
    definingClass = "Lhrj;", name = "d", returnType = "Z", parameters = emptyList(),
    strings = listOf("filter_filteringNeighbourSpoofing")
)

internal val UnknownNumbersFilterFingerprint = Fingerprint(
    definingClass = "Lhrj;", name = "h", returnType = "Z", parameters = emptyList(),
    strings = listOf("filter_filteringUnknown")
)

internal val SpamScoreFilterFingerprint = Fingerprint(
    definingClass = "Lhrj;", name = "f", returnType = "Z", parameters = emptyList(),
    strings = listOf("filter_filteringSpamScore")
)

/** Current verified-business enforcement gate. */
internal val VerifiedBusinessFilterFingerprint = Fingerprint(
    definingClass = "Lgqj;",
    name = "A",
    returnType = "Lcom/truecaller/blocking/FilterMatch;",
    parameters = listOf(
        "Ljava/lang/String;",
        "Lcom/truecaller/blocking/FilterMatch;",
        "Lkotlin/Lazy;",
    ),
    custom = { method, _ ->
        val instructions = method.implementation?.instructions ?: emptyList()
        val hasPreferenceKey = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? com.android.tools.smali.dexlib2.iface.reference.StringReference
            reference?.string == "filter_filteringVerifiedBusinesses"
        }
        val hasContactGate = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.definingClass == "Lcom/truecaller/data/entity/Contact;" && reference.name == "u0"
        }
        val hasVerifiedResult = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "Lcom/truecaller/blocking/FilterMatch;" && reference.name == "t"
        }
        hasPreferenceKey && hasContactGate && hasVerifiedResult
    },
)

/** Current call-filter entry point used by the clean 26.39.6 blocking pipeline. */
internal val NumberSeriesFilterEntryFingerprint = Fingerprint(
    definingClass = "Lgqj;",
    name = "a",
    returnType = "Lcom/truecaller/blocking/FilterMatch;",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Z"),
    custom = { method, _ ->
        val instructions = method.implementation?.instructions ?: emptyList()
        val hasNumberSeriesSentinel = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "Lcom/truecaller/blocking/FilterMatch;" &&
                reference.name == "s" &&
                reference.type == "Lcom/truecaller/blocking/FilterMatch;"
        }
        val hasWCall = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.definingClass == "Lgqj;" && reference.name == "w"
        }
        val hasFCall = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            reference?.definingClass == "Lgqj;" && reference.name == "F"
        }
        hasNumberSeriesSentinel && hasWCall && hasFCall
    },
)
