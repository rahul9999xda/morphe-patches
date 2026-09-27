package app.template.patches.netmirror

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.NETMIRROR_COMPATIBILITY

private const val SUPPORT_GATE_JS = """(function(){try{function hide(){var d=document.documentElement;if(!d)return;var w=window.innerWidth||document.documentElement.clientWidth||0,h=window.innerHeight||document.documentElement.clientHeight||0,a=d.querySelectorAll('*');for(var i=0;i<a.length;i++){var e=a[i],t=(e.innerText||e.textContent||'').replace(/\s+/g,' ').trim();if(t.indexOf('We Need Support')!==-1||t.indexOf('Open 1 ADS per Day')!==-1){var n=e,b=null;for(var j=0;j<10&&n&&n!==document.body;j++,n=n.parentElement){var c=getComputedStyle(n),r=n.getBoundingClientRect(),z=parseInt(c.zIndex||'0',10)||0;if(c.position==='fixed'||c.position==='absolute'||c.position==='sticky'||(w&&h&&r.width>w*.5&&r.height>h*.25)||z>10)b=n;}if(b)b.remove();else{e.style.setProperty('display','none','important');e.style.setProperty('visibility','hidden','important');e.style.setProperty('pointer-events','none','important');}}}if(document.body){document.body.style.overflow='auto';document.body.style.removeProperty('position');}d.style.overflow='auto';}hide();new MutationObserver(function(){hide();}).observe(d=document.documentElement,{subtree:true,childList:true});}catch(e){}})();"""

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
