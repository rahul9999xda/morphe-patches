package app.template.patches.truecaller.misc

import app.morphe.patcher.Fingerprint

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
        val text = method.implementation?.instructions?.joinToString("\n") ?: ""
        text.contains("Ljvb;->k") &&
            text.contains("Ljava/util/concurrent/CompletableFuture;")
    },
)

internal val OtpSelectorFingerprint = Fingerprint(
    definingClass = "Lco70;",
    name = "invoke",
    returnType = "Ljava/lang/Object;",
    parameters = emptyList(),
    strings = listOf("verificationOtpSmsApi_19731"),
    custom = { method, _ ->
        val text = method.implementation?.instructions?.joinToString("\n") ?: ""
        text.contains("OtpSmsApi;->SMS") && text.contains("OtpSmsApi;->GOOGLE")
    },
)

/** Verified in the clean 26.39.6 APKM. */
internal val TelemetryEnableTrackingFingerprint = Fingerprint(
    definingClass = "Lcom/truecaller/analytics/technical/AppStartTracker;",
    name = "enableTracking",
    returnType = "V",
    parameters = emptyList(),
    custom = { method, _ ->
        val text = method.implementation?.instructions?.joinToString("\n") ?: ""
        text.contains("AppStartTracker;->isEnabled:Z") && text.contains("sput-boolean")
    },
)
