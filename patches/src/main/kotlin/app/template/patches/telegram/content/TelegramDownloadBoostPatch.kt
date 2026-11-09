package app.template.patches.telegram.content

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.telegram.signature.telegramSpoofDependency
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.FileLoadOperationUpdateParamsFingerprint
import app.template.patches.telegram.TelegramR8FileLoadOperationUpdateParamsNormalFingerprint
import app.template.patches.telegram.TelegramR8FileLoadOperationUpdateParamsWebFingerprint
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val telegramDownloadBoostPatch = bytecodePatch(
    name = "Download speed boost",
    description = "Increases download chunk size to 1 MB and max concurrent requests to 12.",
) {
    compatibleWith(TELEGRAM_COMPATIBILITY, TELEGRAM_WEB_COMPATIBILITY, TELEGRAM_PLUS_COMPATIBILITY)
    dependsOn(telegramSpoofDependency())

    execute {
        val currentR8Method =
            TelegramR8FileLoadOperationUpdateParamsNormalFingerprint.methodOrNull
                ?: TelegramR8FileLoadOperationUpdateParamsWebFingerprint.methodOrNull

        if (currentR8Method != null) {
            // 13.2.1 renamed FileLoadOperation and its fields. Preserve the stock
            // updateParams decision tree (preload/forceSmallChunk/experiment flags),
            // then override only the final chunk and concurrency values. The stock
            // method computes its CDN-part cap from the final chunk size afterwards.
            val implementation = requireNotNull(currentR8Method.implementation) {
                "R8 FileLoadOperation.updateParams has no implementation"
            }
            val returnIndex = implementation.instructions.indexOfLast {
                it.opcode == Opcode.RETURN_VOID
            }
            require(returnIndex >= 0) { "R8 FileLoadOperation.updateParams has no RETURN_VOID" }
            val owner = currentR8Method.definingClass.toString()
            currentR8Method.addInstructions(
                returnIndex,
                """
                    const/high16 v0, 0x100000
                    iput v0, p0, $owner->n:I
                    const/16 v0, 0xC
                    iput v0, p0, $owner->o:I
                    iput v0, p0, $owner->p:I
                    const/16 v0, 0x7D0
                    iput v0, p0, $owner->q:I
                """,
            )
        } else {
            // Named Telegram Plus / legacy owner. Kept separate from the R8 path.
            val method = requireNotNull(FileLoadOperationUpdateParamsFingerprint.methodOrNull) {
                "Missing FileLoadOperation.updateParams()V"
            }
            method.addInstructions(0, """
                const/high16 v0, 0x100000
                iput v0, p0, Lorg/telegram/messenger/FileLoadOperation;->downloadChunkSizeBig:I
                const/16 v0, 0xC
                iput v0, p0, Lorg/telegram/messenger/FileLoadOperation;->maxDownloadRequests:I
                iput v0, p0, Lorg/telegram/messenger/FileLoadOperation;->maxDownloadRequestsBig:I
                const/16 v0, 0x7D0
                iput v0, p0, Lorg/telegram/messenger/FileLoadOperation;->maxCdnParts:I
                return-void
            """)
        }
    }
}
