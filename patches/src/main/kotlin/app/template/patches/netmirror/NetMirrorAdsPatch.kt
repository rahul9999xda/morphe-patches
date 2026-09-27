package app.template.patches.netmirror

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.NETMIRROR_COMPATIBILITY

// Deliberately contains no backslash escapes or double quotes inside the JS.
// This keeps Morphe's inline-smali compiler happy while still allowing the
// script to remove both normal overlays and full-screen iframe gates.
private const val SUPPORT_GATE_JS = """(function(){function norm(s){return String(s||'').toLowerCase().split(' ').join('').trim()}function full(r,w,h){return r&&r.width>=w*.75&&r.height>=h*.75}function positioned(e){var c=getComputedStyle(e),p=c.position,z=parseInt(c.zIndex||'0',10)||0;return p==='fixed'||p==='absolute'||p==='sticky'||z>20}function hideGate(){try{var w=window.innerWidth||document.documentElement.clientWidth||0,h=window.innerHeight||document.documentElement.clientHeight||0,els=document.querySelectorAll('body *'),hit=null;for(var i=0;i<els.length;i++){var e=els[i],t=norm(e.innerText||e.textContent||'');if(t==='weneedsupport'||t==='open1adsperday'){hit=e;break}}if(!hit)return false;var x=hit;for(var j=0;j<12&&x&&x!==document.body&&x!==document.documentElement;j++,x=x.parentElement){if(positioned(x)){x.style.setProperty('display','none','important');break}}var top=document.elementFromPoint(w/2,h/2);for(var k=0;k<8&&top&&top!==document.body&&top!==document.documentElement;k++,top=top.parentElement){var r=top.getBoundingClientRect(),c=getComputedStyle(top),p=c.position,z=parseInt(c.zIndex||'0',10)||0,f=String(c.filter||'').toLowerCase(),bf=String(c.backdropFilter||'').toLowerCase(),bg=String(c.backgroundColor||'').toLowerCase(),o=parseFloat(c.opacity||'1');if(full(r,w,h)&&(p==='fixed'||p==='absolute'||p==='sticky'||z>20)&&(f.indexOf('blur')>=0||bf.indexOf('blur')>=0||bg.indexOf('rgba')>=0||o<1)){top.style.setProperty('display','none','important');break}}var frames=document.querySelectorAll('iframe');for(var n=0;n<frames.length;n++){var fr=String(frames[n].src||'').toLowerCase();if(fr.indexOf('mobidetect')>=0||fr.indexOf('mobiledetect')>=0)frames[n].style.setProperty('display','none','important')}return true}catch(e){return false}}hideGate();setTimeout(hideGate,150);setTimeout(hideGate,400);setTimeout(hideGate,1000);setTimeout(hideGate,2500);setTimeout(hideGate,5000);try{if(document.documentElement&&typeof MutationObserver!=='undefined'){var ob=new MutationObserver(hideGate);ob.observe(document.documentElement,{childList:true,subtree:true});setTimeout(function(){ob.disconnect()},15000)}}catch(e){}})();"""

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
                move-result v1
                if-nez v1, :nm_block
                const-string v1, "mobiledetect"
                invoke-virtual { v0, v1 }, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                move-result v1
                if-eqz v1, :nm_continue
                :nm_block
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
                move-result v1
                if-nez v1, :nm_block_intent
                const-string v1, "mobiledetect"
                invoke-virtual { v0, v1 }, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                move-result v1
                if-eqz v1, :nm_continue_intent
                :nm_block_intent
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
    description = "Removes the support and one-ad-per-day gate using exact gate text plus elementFromPoint backdrop detection; hides Mobidetect iframes. The WebResourceRequest overload is intentionally not patched because the original DEX already delegates it to the String overload.",
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
