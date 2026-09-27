package app.template.patches.netmirror

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.NETMIRROR_COMPATIBILITY

// Deliberately contains no backslash escapes or double quotes inside the JS.
// This keeps Morphe's inline-smali compiler happy while still allowing the
// script to remove both normal overlays and full-screen iframe gates.
private const val SUPPORT_GATE_JS = """(function(){function norm(s){return String(s||'').toLowerCase().split(' ').join('').trim()}function full(r,w,h){return r&&r.width>=w*.75&&r.height>=h*.75}function visualOverlay(e,w,h){try{var c=getComputedStyle(e),r=e.getBoundingClientRect(),p=c.position,z=parseInt(c.zIndex||'0',10)||0,f=String(c.filter||'').toLowerCase(),bf=String(c.backdropFilter||'').toLowerCase(),bg=String(c.backgroundColor||'').toLowerCase(),o=parseFloat(c.opacity||'1');return full(r,w,h)&&(p==='fixed'||p==='absolute'||p==='sticky'||z>20)&&(f.indexOf('blur')>=0||bf.indexOf('blur')>=0||bg.indexOf('rgba')>=0||o<1)}catch(x){return false}}function clean(){try{var w=window.innerWidth||document.documentElement.clientWidth||0,h=window.innerHeight||document.documentElement.clientHeight||0,found=false,els=document.querySelectorAll('body *');for(var i=0;i<els.length;i++){var e=els[i],t=norm(e.innerText||e.textContent||'');if(t!=='weneedsupport'&&t!=='open1adsperday')continue;found=true;var x=e;for(var j=0;j<12&&x&&x!==document.body&&x!==document.documentElement;j++,x=x.parentElement){var c=getComputedStyle(x),r=x.getBoundingClientRect(),p=c.position,z=parseInt(c.zIndex||'0',10)||0;if(p==='fixed'||p==='absolute'||p==='sticky'||z>20){x.style.setProperty('display','none','important');break;}}}if(!found)return;for(var k=0;k<els.length;k++){var q=els[k];if(visualOverlay(q,w,h))q.style.setProperty('display','none','important');}for(var m=0;m<els.length;m++){var u=els[m],uc=getComputedStyle(u),ur=u.getBoundingClientRect(),uf=String(uc.filter||'').toLowerCase();if(full(ur,w,h)&&uf.indexOf('blur')>=0)u.style.setProperty('filter','none','important');}var frames=document.querySelectorAll('iframe');for(var n=0;n<frames.length;n++){var fr=String(frames[n].src||'').toLowerCase();if(fr.indexOf('mobidetect')>=0||fr.indexOf('mobiledetect')>=0)frames[n].style.setProperty('display','none','important');}}catch(e){}}clean();setTimeout(clean,150);setTimeout(clean,400);setTimeout(clean,1000);setTimeout(clean,2500);setTimeout(clean,5000);try{if(document.documentElement&&typeof MutationObserver!=='undefined'){var ob=new MutationObserver(clean);ob.observe(document.documentElement,{childList:true,subtree:true});setTimeout(function(){ob.disconnect()},15000)}}catch(e){}})();"""

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

        webViewRequestNavigationFingerprint.method.addInstructions(
            0,
            """
                invoke-interface { p2 }, Landroid/webkit/WebResourceRequest;->getUrl()Landroid/net/Uri;
                move-result-object v0
                invoke-virtual { v0 }, Landroid/net/Uri;->toString()Ljava/lang/String;
                move-result-object v0
                invoke-virtual { v0 }, Ljava/lang/String;->toLowerCase()Ljava/lang/String;
                move-result-object v0
                const-string v1, "mobidetect"
                invoke-virtual { v0, v1 }, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                move-result v1
                if-nez v1, :nm_block_request
                const-string v1, "mobiledetect"
                invoke-virtual { v0, v1 }, Ljava/lang/String;->contains(Ljava/lang/CharSequence;)Z
                move-result v1
                if-eqz v1, :nm_continue_request
                :nm_block_request
                const/4 v0, 0x1
                return v0
                :nm_continue_request
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
    description = "Removes the support and one-ad-per-day gate, removes its full-screen backdrop, clears a gate-induced full-page blur, and hides Mobidetect iframes after the WebView page finishes loading.",
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
