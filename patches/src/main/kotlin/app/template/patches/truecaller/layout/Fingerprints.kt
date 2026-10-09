package app.template.patches.truecaller.layout

import app.morphe.patcher.Fingerprint

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
        val text = method.implementation?.instructions?.joinToString("\n") ?: ""
        text.contains("Lum6;->d:Lpsw;") &&
            text.contains("Lum6;->f:Lgf3;") &&
            text.contains("mb3;->F")
    },
)
