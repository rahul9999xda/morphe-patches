package app.template.patches.netmirror

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.NETMIRROR_COMPATIBILITY

// Deliberately contains no backslash escapes or double quotes inside the JS.
// This keeps Morphe's inline-smali compiler happy while still allowing the
// script to remove both normal overlays and full-screen iframe gates.
private const val SUPPORT_GATE_JS = """(function(){function clean(){try{var a=document.querySelectorAll('body *');for(var i=0;i<a.length;i++){var e=a[i],t=String(e.innerText||e.textContent||'').toLowerCase().trim().replaceAll(' ','');if(t!=='weneedsupport'&&t!=='open1adsperday')continue;var x=e;for(var j=0;j<8&&x&&x!==document.body&&x!==document.documentElement;j++,x=x.parentElement){var c=getComputedStyle(x),p=c.position,z=parseInt(c.zIndex||'0',10)||0;if(p==='fixed'||p==='absolute'||p==='sticky'||z>10){x.style.setProperty('display','none','important');break;}}}var f=document.querySelectorAll('iframe');for(var k=0;k<f.length;k++){var q=String(f[k].src||'').toLowerCase();if(q.indexOf('mobidetect')>=0||q.indexOf('mobiledetect')>=0)f[k].remove();}}catch(e){}}clean();setTimeout(clean,200);setTimeout(clean,1000);setTimeout(clean,2500);})();"""

@Suppress("unused")
val netMirrorDisableWebViewPopupPatch = bytecodePatch(
    name = "NetMirror: Disable WebView popups",
    description = "Prevents WebView-created secondary windows used by popup and redirect flows.",
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
val netMirrorBlockRedirectPatch = bytecodePatch(
    name = "NetMirror: Block tracking redirects",
    description = "Blocks Mobidetect navigation in WebView and external URL intents without altering normal navigation.",
    default = true,
) {
    compatibleWith(NETMIRROR_COMPATIBILITY)
    execute {
        webViewStringNavigationFingerprint.method.addInstructions(
            0,
            """
                invoke-virtual { p2 }, Ljava/lang/String;->toLowerCase()Ljava/lang/String;
                move-result-object v0
                const-string v1, "mobidetect"
                invoke-virtual { v0, v1 }, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                move-result v0
                if-eqz v0, :nm_continue
                const/4 v0, 0x1
                return v0
                :nm_continue
            """.trimIndent(),
        )

        intentOpenUrlFingerprint.method.addInstructions(
            0,
            """
                invoke-virtual { p1 }, Ljava/lang/String;->toLowerCase()Ljava/lang/String;
                move-result-object v0
                const-string v1, "mobidetect"
                invoke-virtual { v0, v1 }, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                move-result v0
                if-eqz v0, :nm_continue_intent
                const-string v0, ""
                invoke-interface { p2, v0 }, Lcom/facebook/react/bridge/Promise;->resolve(Ljava/lang/Object;)V
                return-void
                :nm_continue_intent
            """.trimIndent(),
        )
    }
}

@Suppress("unused")
val netMirrorBypassSupportGatePatch = bytecodePatch(
    name = "NetMirror: Bypass support gate",
    description = "Removes the support and one-ad-per-day gate, without removing the main page container; also removes Mobidetect iframes after the WebView page finishes loading.",
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
