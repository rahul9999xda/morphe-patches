package app.template.patches.truecaller.voicemail

import app.morphe.patcher.Fingerprint
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference

/** Verified against clean Truecaller 26.39.6 / build 2639006. */
internal val VoicemailActiveOrPendingFingerprint = Fingerprint(
    definingClass = "Lcom/truecaller/voicemail/api/internal/data/models/status/VoicemailStatus;",
    name = "isActiveOrPending",
    returnType = "Z",
    parameters = emptyList(),
    custom = { method, _ ->
        val instructions = method.implementation?.instructions ?: emptyList()
        val hasActive = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "Lcom/truecaller/voicemail/api/internal/data/models/status/VoicemailStatus;" && reference.name == "ACTIVE"
        }
        val hasPending = instructions.any { instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? FieldReference
            reference?.definingClass == "Lcom/truecaller/voicemail/api/internal/data/models/status/VoicemailStatus;" && reference.name == "PENDING"
        }
        hasActive && hasPending
    },
)
