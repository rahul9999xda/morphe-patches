package app.template.patches.telegram.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.Telegram1331FormattingToolsFingerprint
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

@Suppress("unused")
val telegramFormattingToolsPatch = bytecodePatch(
    name = "Enable Formatting Tools",
    description = "Unlocks Telegram's RichMessage formatting editor without changing the account's Premium entitlement.",
) {
    compatibleWith(TELEGRAM_COMPATIBILITY, TELEGRAM_WEB_COMPATIBILITY)

    execute {
        val method = requireNotNull(Telegram1331FormattingToolsFingerprint.methodOrNull) {
            "Telegram 13.3.1 RichMessage editor ChatActivityEnterView.u2()V was not found"
        }
        val implementation = requireNotNull(method.implementation) {
            "ChatActivityEnterView.u2()V has no implementation"
        }

        val lockCalls = implementation.instructions.mapIndexedNotNull { index, instruction ->
            val reference = (instruction as? ReferenceInstruction)?.reference as? MethodReference
            if (
                instruction.opcode == Opcode.INVOKE_VIRTUAL &&
                reference?.name == "setLocked" &&
                reference.parameterTypes.size == 1 &&
                reference.parameterTypes[0].toString() == "Z" &&
                reference.returnType == "V"
            ) {
                val invoke = instruction as? FiveRegisterInstruction
                    ?: throw IllegalStateException("RichMessage setLocked call is not a 35c invoke")
                index to invoke
            } else null
        }

        require(lockCalls.size == 2) {
            "Expected exactly two RichMessage setLocked(Z)V calls in u2(), found ${lockCalls.size}"
        }

        val (premiumLockIndex, premiumLockInvoke) = lockCalls[0]
        val (normalPathIndex, normalPathInvoke) = lockCalls[1]

        require(
            premiumLockInvoke.registerCount == 2 &&
                premiumLockInvoke.registerC == 5 &&
                premiumLockInvoke.registerD == 0
        ) {
            "Unexpected Premium lock registers in RichMessage editor: expected {v5, v0}"
        }
        require(
            normalPathInvoke.registerCount == 2 &&
                normalPathInvoke.registerC == 5 &&
                normalPathInvoke.registerD == 4 &&
                normalPathIndex > premiumLockIndex
        ) {
            "Unexpected non-RichMessage lock call; refusing to alter the wrong branch"
        }

        method.addInstructions(premiumLockIndex, "const/4 v0, 0x0")
    }
}
