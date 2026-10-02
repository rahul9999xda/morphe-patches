package app.template.patches.telegram.content

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.mutableClassDefBy
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.TELEGRAM_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_PLUS_COMPATIBILITY
import app.template.patches.shared.Constants.TELEGRAM_WEB_COMPATIBILITY
import app.template.patches.telegram.MessagesControllerAutoDeleteTaskFingerprints
import app.template.patches.telegram.MessagesControllerServerTtlChannelDeleteFingerprints
import app.template.patches.telegram.MessagesControllerServerTtlRegularDeleteFingerprints
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.builder.MutableMethodImplementation
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter

/**
 * Canonical unified Telegram Auto-Delete patch for 12.10.6.
 *
 * It unifies ONLY the two Auto-Delete mechanisms:
 * 1. local TTL worker deletion; and
 * 2. server-synchronized deletion of locally stored TTL-expired messages.
 *
 * Anti-Delete and disappearing-media patches remain independent.
 * Do not select this patch together with the old local-only or server-only
 * Auto-Delete variants, because that would duplicate hooks.
 *
 * 12.10.6 DEX variants verified:
 * - Telegram 71122
 * - Telegram Web 71129
 * - Plus Messenger 22588
 */
@Suppress("unused")
val telegramDisableAutoDeleteMessagesUnifiedPatch = bytecodePatch(
    name = "Disable Telegram auto-delete (unified)",
    description = "Stops local TTL worker deletion and suppresses server deletion updates only for locally stored messages whose Telegram TTL has expired.",
) {
    compatibleWith(
        TELEGRAM_COMPATIBILITY,
        TELEGRAM_PLUS_COMPATIBILITY,
        TELEGRAM_WEB_COMPATIBILITY,
    )

    execute {
        // ---------------------------------------------------------------
        // Hook A: local client-side TTL worker.
        // ---------------------------------------------------------------
        val localMatches = MessagesControllerAutoDeleteTaskFingerprints
            .flatMap { it.matchAllOrNull() ?: emptyList() }

        require(localMatches.size == 1) {
            "Expected exactly one 12.10.6 local Auto-Delete worker, found ${localMatches.size}."
        }

        val local = localMatches.single()
        require(local.instructionMatches.size == 1) {
            "Expected exactly one regular deleteMessages() invoke in the local TTL worker, " +
                "found ${local.instructionMatches.size}."
        }

        // Verified width in all three 12.10.6 APKs: invoke-virtual/range,
        // three code units. Preserve method layout with three NOPs.
        local.method.replaceInstruction(
            local.instructionMatches.single().index,
            "nop
nop
nop",
        )

        // ---------------------------------------------------------------
        // Hook B/C: server deletion-update sinks.
        // ---------------------------------------------------------------
        val regularMatches = MessagesControllerServerTtlRegularDeleteFingerprints
            .flatMap { it.matchAllOrNull() ?: emptyList() }
        val channelMatches = MessagesControllerServerTtlChannelDeleteFingerprints
            .flatMap { it.matchAllOrNull() ?: emptyList() }

        require(regularMatches.size == 1) {
            "Expected exactly one regular server TTL hook, found ${regularMatches.size}."
        }
        require(channelMatches.size == 1) {
            "Expected exactly one channel server TTL hook, found ${channelMatches.size}."
        }

        val allMatches = regularMatches + channelMatches
        val controllerClass = mutableClassDefBy(allMatches.first().method.definingClass)

        require(allMatches.all { it.method.definingClass == controllerClass.type }) {
            "Server TTL hooks resolved to different defining classes."
        }

        val helperName = "patch_filterExpiredServerTtlDeletes"
        val helperParameters = listOf(
            ImmutableMethodParameter(
                "Lorg/telegram/messenger/MessagesStorage;",
                null,
                "storage",
            ),
            ImmutableMethodParameter("J", null, "dialogId"),
            ImmutableMethodParameter(
                "Ljava/util/ArrayList;",
                null,
                "messageIds",
            ),
        )

        require(controllerClass.methods.none { method ->
            method.name == helperName && method.parameterTypes == helperParameters.map { it.type }
        }) {
            "${controllerClass.type}->$helperName already exists."
        }

        // 4 parameter words + 8 local words = 12 total registers.
        val helperMethod = ImmutableMethod(
            controllerClass.type,
            helperName,
            helperParameters,
            "Ljava/util/ArrayList;",
            AccessFlags.PRIVATE.value or AccessFlags.STATIC.value,
            null,
            null,
            MutableMethodImplementation(12),
        ).toMutable().apply {
            addInstructions(
                0,
                """
                # p0 = MessagesStorage
                # p1:p2 = dialogId
                # p3 = server deletion IDs
                # v0 = Iterator
                # v1 = ConnectionsManager
                # v2 = current server time
                # v3 = hasNext / temporary int
                # v4 = Integer / Message
                # v5:v6 = message id / ttl arithmetic
                # v7 = message.date / temporary

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
                """,
            )
        }

        controllerClass.methods.add(helperMethod)

        allMatches.forEach { match ->
            require(match.instructionMatches.size == 1) {
                "Expected exactly one markMessagesAsDeleted() anchor in ${match.method.name}, " +
                    "found ${match.instructionMatches.size}."
            }

            val sinkIndex = match.instructionMatches.single().index

            // Verified at the sink in Telegram, Web and Plus:
            // v0 = MessagesStorage, v1:v2 = dialogId, v3 = deletion-ID list.
            match.method.addInstructions(
                sinkIndex,
                """
                invoke-static { v0, v1, v2, v3 }, ${controllerClass.type}->$helperName(Lorg/telegram/messenger/MessagesStorage;JLjava/util/ArrayList;)Ljava/util/ArrayList;
                move-result-object v3
                """,
            )
        }
    }
}
