package app.template.patches.netmirror

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.NETMIRROR_COMPATIBILITY

private const val PROMISE = "Lcom/facebook/react/bridge/Promise;"
private const val BLOCKED_HOST = "mobidetect.click"

@Suppress("unused")
val netMirrorDisableWebViewPopupPatch = bytecodePatch(
    name = "NetMirror: Disable WebView popups",
    description = "Prevents WebView-created secondary windows used by popup/redirect flows.",
    default = true,
) {
    compatibleWith(NETMIRROR_COMPATIBILITY)
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
    compatibleWith(NETMIRROR_COMPATIBILITY)
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
@Suppress("unused")
val netMirrorBlockWebResourceRedirectPatch = bytecodePatch(
    name = "NetMirror: Block ad resource redirects",
    description = "Disabled diagnostically: this overload has no spare local register in the analyzed APK.",
    default = false,
) {
    compatibleWith(NETMIRROR_COMPATIBILITY)
    execute {
        try {
            webViewRequestNavigationFingerprint.method.addInstructionsWithLabels(
                0,
                """
                    invoke-interface {p2}, Landroid/webkit/WebResourceRequest;->getUrl()Landroid/net/Uri;
                    move-result-object v0
                    invoke-virtual {v0}, Landroid/net/Uri;->toString()Ljava/lang/String;
                    move-result-object v0
                    const-string v1, "$BLOCKED_HOST"
                    invoke-virtual {v0, v1}, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                    move-result v0
                    if-eqz v0, :netmirror_allow_webresource_url
                    const/4 v0, 0x1
                    return v0
                    :netmirror_allow_webresource_url
                    nop
                """.trimIndent(),
            )
        } catch (e: PatchException) {
            println("[NetMirror: Block ad resource redirects] fingerprint not applied: ${e.message}")
        }
    }
}

@Suppress("unused")
val netMirrorBypassSupportGatePatch = bytecodePatch(
    name = "NetMirror: Remove support/ad gate",
    description = "Disabled in diagnostic build because modifying onPageFinished can destabilize WebView startup.",
    default = false,
) {
    compatibleWith(NETMIRROR_COMPATIBILITY)
    execute { }
}
