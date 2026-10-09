package app.template.patches.truecaller.ads

import app.morphe.patcher.Fingerprint

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
        val text = method.implementation?.instructions?.joinToString("\n") ?: ""
        text.contains("DlRequestSourceType;->List") &&
            text.contains("DlRequestSource;->getAdRequestSource") &&
            text.contains("Ljava/util/UUID;->randomUUID")
    },
)
