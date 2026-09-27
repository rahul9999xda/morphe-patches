package app.template.patches.netmirror

import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.NETMIRROR_COMPATIBILITY

private const val PROMISE = "Lcom/facebook/react/bridge/Promise;"
private const val BLOCKED_HOST = "mobidetect.click"
private const val VALUE_CALLBACK = "Landroid/webkit/ValueCallback;"

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
val netMirrorBypassSupportGatePatch = bytecodePatch(
    name = "NetMirror: Bypass support/ad gate",
    description = "Hides the server-delivered support overlay after a WebView page finishes loading, so the app does not depend on opening the ad redirect.",
    default = true,
) {
    compatibleWith(NETMIRROR_COMPATIBILITY)
    execute {
        try {
            webViewPageFinishedFingerprint.method.addInstructions(
                0,
                """
                    const-string v0, "(function(){try{var n=['We Need Support','Open 1 ADS per Day','Click Here'];var e=document.querySelectorAll('body *');for(var i=0;i<e.length;i++){var t=(e[i].innerText||'').trim();if(n.some(function(x){return t.indexOf(x)>=0;})&&t.length<2500){var p=e[i];for(var j=0;j<6&&p.parentElement;j++){var s=getComputedStyle(p);if(s.position==='fixed'||s.position==='absolute'||parseInt(s.zIndex||'0',10)>10){p.style.setProperty('display','none','important');break}p=p.parentElement}}}}catch(_){}})();"
                    const/4 v1, 0x0
                    invoke-virtual {p1, v0, v1}, Landroid/webkit/WebView;->evaluateJavascript(Ljava/lang/String;Landroid/webkit/ValueCallback;)V
                """.trimIndent(),
            )
        } catch (e: PatchException) {
            println("[NetMirror: Bypass support/ad gate] fingerprint not applied: ${e.message}")
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
