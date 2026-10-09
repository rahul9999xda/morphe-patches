package app.template.patches.truecaller.voicemail

import app.morphe.patcher.Fingerprint

/** Verified against clean Truecaller 26.39.6 / build 2639006. */
internal val VoicemailActiveOrPendingFingerprint = Fingerprint(
    definingClass = "Lcom/truecaller/voicemail/api/internal/data/models/status/VoicemailStatus;",
    name = "isActiveOrPending",
    returnType = "Z",
    parameters = emptyList(),
    custom = { method, _ ->
        val text = method.implementation?.instructions?.joinToString("\n") ?: ""
        text.contains("VoicemailStatus;->ACTIVE") &&
            text.contains("VoicemailStatus;->PENDING") &&
            text.contains("Ljava/lang/Boolean;") == false
    },
)
