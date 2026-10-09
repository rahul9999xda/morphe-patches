package app.template.patches.truecaller.blocking

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TRUECALLER_COMPATIBILITY

/**
 * Forces the six concrete blocking-filter settings used by the 26.39.6 filter
 * layer to enabled. This is the current setting-read boundary, not an enum/UI guess.
 */
val enableMaxProtectionPatch = bytecodePatch(
    name = "Enable Max Protection filters",
    description = "Enables the current Truecaller 26.39.6 top-spammer, non-phonebook, foreign-number, neighbour-spoofing, unknown-number and spam-score filters.",
) {
    compatibleWith(TRUECALLER_COMPATIBILITY)
    execute {
        TopSpammersFilterFingerprint.method.addInstructions(0, "const/4 p0, 0x1\nreturn p0")
        NonPhonebookFilterFingerprint.method.addInstructions(0, "const/4 p0, 0x1\nreturn p0")
        ForeignNumbersFilterFingerprint.method.addInstructions(0, "const/4 p0, 0x1\nreturn p0")
        NeighbourSpoofingFilterFingerprint.method.addInstructions(0, "const/4 p0, 0x1\nreturn p0")
        UnknownNumbersFilterFingerprint.method.addInstructions(0, "const/4 p0, 0x1\nreturn p0")
        SpamScoreFilterFingerprint.method.addInstructions(0, "const/4 p0, 0x1\nreturn p0")
    }
}
