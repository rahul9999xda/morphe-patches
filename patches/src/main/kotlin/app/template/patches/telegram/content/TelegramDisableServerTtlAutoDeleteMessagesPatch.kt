package app.template.patches.telegram.content

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.MessagesControllerServerTtlChannelDeleteFingerprints
import app.template.patches.telegram.MessagesControllerServerTtlRegularDeleteFingerprints
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

/** Updated independent server-TTL component for Telegram 12.10.6 / 71122, Web 71129, Plus 22588. */
@Suppress("unused")
val telegramDisableServerTtlAutoDeleteMessagesPatch_12_10_6 = bytecodePatch(
    name = "Disable server TTL auto-delete (12.10.6)",
    description = "Preserves locally stored messages from server deletion updates when their own Telegram TTL has expired.",
) {
    compatibleWith(TELEGRAM_COMPATIBILITY, TELEGRAM_PLUS_COMPATIBILITY, TELEGRAM_WEB_COMPATIBILITY)
    execute {
        val regular = MessagesControllerServerTtlRegularDeleteFingerprints.flatMap { it.matchAllOrNull() ?: emptyList() }
        val channel = MessagesControllerServerTtlChannelDeleteFingerprints.flatMap { it.matchAllOrNull() ?: emptyList() }
        require(regular.size == 1) { "Expected exactly one regular server TTL hook, found ${regular.size}." }
        require(channel.size == 1) { "Expected exactly one channel server TTL hook, found ${channel.size}." }
        val allMatches = regular + channel
        val controllerClass = mutableClassDefBy(allMatches.first().method.definingClass)
        val helperName = "patch_filterExpiredServerTtlDeletes"
        val helperParameters = listOf(
            ImmutableMethodParameter("Lorg/telegram/messenger/MessagesStorage;", null, "storage"),
            ImmutableMethodParameter("J", null, "dialogId"),
            ImmutableMethodParameter("Ljava/util/ArrayList;", null, "messageIds"),
        )
        require(controllerClass.methods.none { it.name == helperName && it.parameterTypes == helperParameters.map { p -> p.type } }) {
            "${controllerClass.type}->$helperName already exists."
        }
        val helperMethod = ImmutableMethod(
            controllerClass.type, helperName, helperParameters, "Ljava/util/ArrayList;",
            AccessFlags.PRIVATE.value or AccessFlags.STATIC.value, null, null,
            MutableMethodImplementation(12),
        ).toMutable().apply {
            addInstructions(0, """
                invoke-virtual { p3 }, Ljava/util/ArrayList;->iterator()Ljava/util/Iterator;
                move-result-object v0
                invoke-virtual { p0 }, Lorg/telegram/messenger/BaseController;->getConnectionsManager()Lorg/telegram/tgnet/ConnectionsManager;
                move-result-object v1
                invoke-virtual { v1 }, Lorg/telegram/tgnet/ConnectionsManager;->getCurrentTime()I
                move-result v2
                :ttl_filter_loop
                invoke-interface { v0 }, Ljava/util/Iterator;->hasNext()Z
                move-result v3
                if-eqz v3, :ttl_filter_done
                invoke-interface { v0 }, Ljava/util/Iterator;->next()Ljava/lang/Object;
                move-result-object v4
                check-cast v4, Ljava/lang/Integer;
                invoke-virtual { v4 }, Ljava/lang/Integer;->intValue()I
                move-result v5
                int-to-long v5, v5
                invoke-virtual { p0, p1, p2, v5, v6 }, Lorg/telegram/messenger/MessagesStorage;->getMessage(JJ)Lorg/telegram/tgnet/TLRPC${'$'}Message;
                move-result-object v4
                if-eqz v4, :ttl_filter_loop
                iget v5, v4, Lorg/telegram/tgnet/TLRPC${'$'}Message;->ttl_period:I
                if-lez v5, :ttl_filter_loop
                iget v7, v4, Lorg/telegram/tgnet/TLRPC${'$'}Message;->date:I
                add-int v5, v7, v5
                if-le v5, v2, :ttl_filter_loop
                invoke-interface { v0 }, Ljava/util/Iterator;->remove()V
                goto :ttl_filter_loop
                :ttl_filter_done
                return-object p3
            """.trimIndent())
        }
        controllerClass.methods.add(helperMethod)
        allMatches.forEach { match ->
            require(match.instructionMatches.size == 1) { "Expected exactly one markMessagesAsDeleted() anchor in ${match.method.name}." }
            match.method.addInstructions(match.instructionMatches.single().index, """
                invoke-static { v0, v1, v2, v3 }, ${controllerClass.type}->$helperName(Lorg/telegram/messenger/MessagesStorage;JLjava/util/ArrayList;)Ljava/util/ArrayList;
                move-result-object v3
            """.trimIndent())
        }
    }
}
