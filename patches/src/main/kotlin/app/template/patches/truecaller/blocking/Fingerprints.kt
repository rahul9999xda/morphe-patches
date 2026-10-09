package app.template.patches.truecaller.blocking

import app.morphe.patcher.Fingerprint

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
        val text = method.implementation?.instructions?.joinToString("\n") ?: ""
        text.contains("filter_filteringVerifiedBusinesses") &&
            text.contains("Contact;->u0") &&
            text.contains("FilterMatch;->t")
    },
)

/** Current call-filter entry point used by the clean 26.39.6 blocking pipeline. */
internal val NumberSeriesFilterEntryFingerprint = Fingerprint(
    definingClass = "Lgqj;",
    name = "a",
    returnType = "Lcom/truecaller/blocking/FilterMatch;",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Z"),
    custom = { method, _ ->
        val text = method.implementation?.instructions?.joinToString("\n") ?: ""
        text.contains("FilterMatch;->s") &&
            text.contains("Lgqj;->w") &&
            text.contains("Lgqj;->F")
    },
)
