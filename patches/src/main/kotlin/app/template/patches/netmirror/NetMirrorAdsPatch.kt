package app.template.patches.netmirror

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.NETMIRROR_COMPATIBILITY

// v3.1 gate-flow strategy derived from the observed remote support page:
// do not remove DOM nodes, do not fake pressfromAPP(), and do not block the
// support ad popup/navigation. Instead, click the support CTA once from the
// page itself and leave the original ad/return flow intact.
//
// This is intentionally conservative: the remote v3.1 HTML is not embedded
// in the APK, so historical CSS selectors are not guessed.
private const val SUPPORT_AD_CLICK_JS = """(function(){function norm(s){return String(s||'').toLowerCase().replace(/\s+/g,' ').trim()}function clickSupport(){try{var a=document.querySelectorAll('a,button,[role=button],input[type=button],input[type=submit]');for(var i=0;i<a.length;i++){var e=a[i],t=norm(e.innerText||e.textContent||e.value||'');if(t==='click here'||t.indexOf('open 1 ads per day')>=0||t.indexOf('open 1 ad per day')>=0){e.click();return true}}}catch(e){}return false}function removeTelegram(){try{var a=document.querySelectorAll('body *');for(var i=0;i<a.length;i++){var e=a[i],t=norm(e.innerText||e.textContent||'');if(t.indexOf('join our telegram channel now')>=0&&t.indexOf('request movies and series')>=0){var c=e;for(var n=0;n<6&&c.parentElement;n++){var r=c.getBoundingClientRect?c.getBoundingClientRect():null;if(c!==document.body&&r&&r.width>250&&r.height>100){c.style.setProperty('display','none','important');return true}c=c.parentElement}}}catch(e){}return false}function enable1080(){try{var a=document.querySelectorAll('body *');for(var i=0;i<a.length;i++){var e=a[i],t=norm(e.innerText||e.textContent||'');if(t.indexOf('1080')<0)continue;var c=e;for(var n=0;n<5&&c;n++,c=c.parentElement){var sw=c.querySelector?c.querySelector('input[type=checkbox],[role=switch],[role=checkbox],button'):null;if(!sw)continue;var aria=sw.getAttribute('aria-checked'),checked=sw.checked;if(aria==='false'||(typeof checked==='boolean'&&!checked)){sw.click();return true}var st=norm(sw.innerText||sw.textContent||'');if(st==='off'||st.indexOf(' off')>=0){sw.click();return true}}} }catch(e){}return false}function run(){removeTelegram();enable1080()}setTimeout(function(){clickSupport();run()},1000);setTimeout(run,3000);setTimeout(run,6000);setTimeout(run,12000);setTimeout(run,20000);if(window.MutationObserver){try{new MutationObserver(function(){run()}).observe(document.documentElement||document,{childList:true,subtree:true})}catch(e){}}})();"""

@Suppress("unused")
val netMirrorAllowOriginalSupportAdFlowPatch = bytecodePatch(
    name = "NetMirror: Preserve original support-ad flow",
    description = "Leaves WebView popup creation and Mobidetect navigation untouched so the original v3.1 support-ad return flow can complete; no DOM removal or synthetic unlock callback is injected.",
    default = true,
) {
    compatibleWith(NETMIRROR_COMPATIBILITY)
    execute {
        // Intentionally no onCreateWindow or shouldOverrideUrlLoading patch.
    }
}

@Suppress("unused")
val netMirrorAutomateSupportAdClickPatch = bytecodePatch(
    name = "NetMirror: Click support ad CTA once",
    description = "On the remote support page, clicks only an exact visible support CTA (Click Here / Open 1 Ads per Day). It does not remove or hide page elements and does not call an invented unlock function.",
    default = true,
) {
    compatibleWith(NETMIRROR_COMPATIBILITY)
    execute {
        val method = webViewPageFinishedFingerprint.method
        val insertIndex = method.implementation!!.instructions.lastIndex
        method.addInstructions(
            insertIndex,
            """
                move-object v0, p1
                const-string v1, "$SUPPORT_AD_CLICK_JS"
                const/4 v2, 0x0
                invoke-virtual { v0, v1, v2 }, Landroid/webkit/WebView;->evaluateJavascript(Ljava/lang/String;Landroid/webkit/ValueCallback;)V
            """.trimIndent(),
        )
    }
}
