package app.template.patches.netmirror

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.NETMIRROR_COMPATIBILITY

// Deliberately contains no backslash escapes or double quotes inside the JS.
// This keeps Morphe's inline-smali compiler happy while still allowing the
// script to remove both normal overlays and full-screen iframe gates.
private const val SUPPORT_GATE_JS = """(function(){function clean(){try{var d=document.documentElement;if(!d)return;var w=window.innerWidth||d.clientWidth||0,h=window.innerHeight||d.clientHeight||0,a=d.querySelectorAll('iframe,body *');for(var i=0;i<a.length;i++){var e=a[i],t=String(e.innerText||e.textContent||'').toLowerCase(),s=String(e.src||'').toLowerCase(),r=e.getBoundingClientRect?e.getBoundingClientRect():null,c=getComputedStyle(e),big=r&&w&&h&&r.width>w*.5&&r.height>h*.5,fixed=c.position==='fixed'||c.position==='absolute'||c.position==='sticky',z=(parseInt(c.zIndex||'0',10)||0)>10;if(s.indexOf('mobidetect')>=0||s.indexOf('mobiledetect')>=0||t.indexOf('we need support')>=0||t.indexOf('open 1 ads per day')>=0||(big&&(fixed||z))){if(e!==document.body&&e!==document.documentElement)e.remove();}}if(document.body){document.body.style.overflow='auto';document.body.style.removeProperty('position');}d.style.overflow='auto';}catch(x){}}clean();setTimeout(clean,100);setTimeout(clean,500);setTimeout(clean,1500);setInterval(clean,3000);})();"""

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
    description = "Removes the support and one-ad-per-day gate, including gates hosted inside a full-screen iframe, after the WebView page finishes loading.",
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
