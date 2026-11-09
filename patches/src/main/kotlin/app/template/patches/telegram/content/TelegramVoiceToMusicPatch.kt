package app.template.patches.telegram.content

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.MessageObjectIsMusicFingerprint
import app.template.patches.telegram.TelegramR8MessageObjectIsMusicNormalFingerprint
import app.template.patches.telegram.TelegramR8MessageObjectIsMusicWebFingerprint
import app.template.patches.telegram.signature.telegramSpoofDependency

/**
 * Plays voice notes through the music-player path while preserving
 * Telegram's native Raise-to-Listen / proximity detection.
 *
 * IMPORTANT:
 * MessageObject.isVoice() must NOT be overridden. Telegram's
 * MediaController uses isVoice() to decide whether proximity mode
 * should switch playback to the earpiece.
 */
@Suppress("unused")
val telegramVoiceToMusicPatch = bytecodePatch(
    name = "Voice to music",
    description = "Plays voice notes through the music-player path while preserving Raise-to-Listen.",
    default = true,
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )

    dependsOn(telegramSpoofDependency())

    execute {
        // Keep MessageObject.isVoice() unchanged so Raise-to-Listen continues
        // to recognize voice messages.
        //
        // Only make voice messages qualify for the music-player path.
        val currentR8Method =
            TelegramR8MessageObjectIsMusicNormalFingerprint.methodOrNull
                ?: TelegramR8MessageObjectIsMusicWebFingerprint.methodOrNull

        if (currentR8Method != null) {
            // The current R8 MessageObject stores its message in field k; S4(Message)
            // is the verified isVoiceMessage predicate in both current APKs.
            val owner = currentR8Method.definingClass.toString()
            currentR8Method.addInstructions(0, """
                iget-object v0, p0, $owner->k:Lorg/telegram/tgnet/TLRPC${'$'}Message;
                invoke-static {v0}, $owner->S4(Lorg/telegram/tgnet/TLRPC${'$'}Message;)Z
                move-result v0
                if-eqz v0, :morphe_voice_to_music_not_voice
                const/4 v0, 0x1
                return v0
                :morphe_voice_to_music_not_voice
                nop
            """)
        } else {
            // Named Plus and legacy MessageObject path.
            MessageObjectIsMusicFingerprint.method.addInstructions(
                0,
                """
                    iget-object v0, p0, Lorg/telegram/messenger/MessageObject;->messageOwner:Lorg/telegram/tgnet/TLRPC${'$'}Message;
                    invoke-static { v0 }, Lorg/telegram/messenger/MessageObject;->isVoiceMessage(Lorg/telegram/tgnet/TLRPC${'$'}Message;)Z
                    move-result v0
                    if-eqz v0, :morphe_voice_to_music_not_voice
                    const/4 v0, 0x1
                    return v0
                    :morphe_voice_to_music_not_voice
                    nop
                """,
            )
        }
    }
}
