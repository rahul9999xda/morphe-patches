package app.template.patches.netmirror

import app.morphe.patcher.PatchException
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_NETMIRROR

private const val PROMISE = "Lcom/facebook/react/bridge/Promise;"
private const val BLOCKED_HOST = "mobidetect.click"

@Suppress("unused")
val netMirrorDisableWebViewPopupPatch = bytecodePatch(
    name = "NetMirror: Disable WebView popups",
    description = "Prevents WebView-created secondary windows used by popup/redirect flows.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_NETMIRROR)
    execute {
        try {
            onCreateWindowFingerprint.method.addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """.trimIndent(),
            )
        } catch (e: PatchException) {
            println("[NetMirror: Disable WebView popups] fingerprint not applied: ${e.message}")
        }
    }
}

@Suppress("unused")
val netMirrorBlockRedirectPatch = bytecodePatch(
    name = "NetMirror: Block ad redirect",
    description = "Blocks the embedded mobidetect.click redirect in WebView navigation and external URL launches.",
    default = true,
) {
    compatibleWith(COMPATIBILITY_NETMIRROR)
    execute {
        try {
            webViewStringNavigationFingerprint.method.addInstructionsWithLabels(
                0,
                """
                    const-string v0, "$BLOCKED_HOST"
                    invoke-virtual {p2, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                    move-result v0
                    if-eqz v0, :netmirror_allow_webview_url
                    const/4 v0, 0x1
                    return v0
                    :netmirror_allow_webview_url
                    nop
                """.trimIndent(),
            )

            intentOpenUrlFingerprint.method.addInstructionsWithLabels(
                0,
                """
                    const-string v0, "$BLOCKED_HOST"
                    invoke-virtual {p1, v0}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                    move-result v0
                    if-eqz v0, :netmirror_allow_external_url
                    const-string v0, ""
                    invoke-interface {p2, v0}, $PROMISE->resolve(Ljava/lang/Object;)V
                    return-void
                    :netmirror_allow_external_url
                    nop
                """.trimIndent(),
            )
        } catch (e: PatchException) {
            println("[NetMirror: Block ad redirect] fingerprint not applied: ${e.message}")
        }
    }
}
