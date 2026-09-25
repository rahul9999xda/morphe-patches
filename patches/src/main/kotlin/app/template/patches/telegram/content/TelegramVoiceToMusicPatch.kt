package app.template.patches.telegram.content

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.MessageObjectIsMusicFingerprint
import app.template.patches.telegram.signature.telegramSpoofDependency

// Voice-to-Music must not override MessageObject.isVoice().
//
// Telegram's Raise-to-Listen implementation uses MessageObject.isVoice()
// directly in MediaController to decide whether a currently playing voice
// message is eligible for proximity/headset listening. Returning false from
// isVoice() therefore disables Raise-to-Listen and leaves playback on the
// normal loudspeaker.
//
// We keep the original isVoice() semantics and only make voice messages
// satisfy the music-player classification through isMusic().
//
// This preserves the native Raise-to-Listen voice detection while still
// allowing the message to enter the music-player path.

@Suppress("unused")
val telegramVoiceToMusicPatch = bytecodePatch(
    name = "Voice to music",
    description = "Plays voice notes through the music-player path while preserving Raise-to-Listen.",
    default = true,
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
    )

    dependsOn(telegramSpoofDependency())

    execute {
        // Do NOT modify MessageObject.isVoice().
        //
        // Raise-to-Listen checks this method from MediaController. Keeping
        // the native result allows Telegram to detect voice messages through
        // the proximity sensor and switch to the earpiece.

        // Make voice messages qualify as music for the music-player path.
        // Inject at the beginning so a voice message is treated as music
        // before the original document/music classification is evaluated.
        MessageObjectIsMusicFingerprint.method.addInstructions(
            0,
            """
                iget-object v0, p0, Lorg/telegram/messenger/MessageObject;->messageOwner:Lorg/telegram/tgnet/TLRPC${'$'}Message
                invoke-static { v0 }, Lorg/telegram/messenger/MessageObject;->isVoiceMessage(Lorg/telegram/tgnet/TLRPC${'$'}Message;)Z
                move-result v0
                if-eqz v0, :not_voice
                const/4 v0, 0x1
                return v0
                :not_voice
                nop
            """,
        )
    }
}
