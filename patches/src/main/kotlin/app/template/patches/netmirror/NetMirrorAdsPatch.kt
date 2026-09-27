package app.template.patches.netmirror

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.NETMIRROR_COMPATIBILITY

private const val SUPPORT_GATE_JS = """javascript:(function(){try{function x(){var a=document.body;if(!a)return;var e=a.querySelectorAll('*');for(var i=0;i<e.length;i++){var t=(e[i].innerText||'').trim();if(t.indexOf('We Need Support')!==-1||t.indexOf('Open 1 ADS per Day')!==-1){var n=e[i];for(var j=0;j<6&&n;j++,n=n.parentElement){var s=getComputedStyle(n);if(s.position==='fixed'||s.position==='absolute'||parseInt(s.zIndex||'0')>100){n.remove();break}}}}a.style.overflow='auto';document.documentElement.style.overflow='auto'}catch(_){} }x();new MutationObserver(x).observe(document.documentElement,{subtree:true,childList:true})})();"""

@Suppress("unused")
val netMirrorDisableWebViewPopupPatch = bytecodePatch(
    name = "NetMirror: Disable WebView popups",
    description = "Prevents WebView-created secondary windows used by popup/redirect flows.",
    default = true,
) {
    compatibleWith(NETMIRROR_COMPATIBILITY)
    execute {
        onCreateWindowFingerprint.method.addInstructions(
            0,
            """
                const/4 v0, 0x0
                return v0
            """.trimIndent(),
        )
    }
}

@Suppress("unused")
val netMirrorBypassSupportGatePatch = bytecodePatch(
    name = "NetMirror: Bypass support gate",
    description = "Removes the support/one-ad-per-day overlay after the WebView page finishes loading without navigating the WebView.",
    default = true,
) {
    compatibleWith(NETMIRROR_COMPATIBILITY)
    execute {
        val method = webViewPageFinishedFingerprint.method
        val insertIndex = method.implementation!!.instructions.lastIndex
        method.addInstructions(
            insertIndex,
            """
                const-string v0, "$SUPPORT_GATE_JS"
                const/4 p0, 0x0
                invoke-virtual { p1, v0, p0 }, Landroid/webkit/WebView;->evaluateJavascript(Ljava/lang/String;Landroid/webkit/ValueCallback;)V
            """.trimIndent(),
        )
    }
}
