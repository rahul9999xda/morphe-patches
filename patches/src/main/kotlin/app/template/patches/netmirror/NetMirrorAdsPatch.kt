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
private const val SUPPORT_AD_CLICK_JS = """(function(){function norm(s){return String(s||'').toLowerCase().replaceAll(' ','').trim()}function clickSupport(){try{var a=document.querySelectorAll('a,button,[role=button],input[type=button],input[type=submit]');for(var i=0;i<a.length;i++){var e=a[i],t=norm(e.innerText||e.textContent||e.value||'');if(t==='clickhere'||t.indexOf('open1adsperday')>=0||t.indexOf('open1adperday')>=0){e.click();return true}}}catch(e){}return false}setTimeout(clickSupport,1000);setTimeout(clickSupport,3000);})();"""

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
                const-string v0, "$SUPPORT_AD_CLICK_JS"
                const/4 p0, 0x0
                invoke-virtual { p1, v0, p0 }, Landroid/webkit/WebView;->evaluateJavascript(Ljava/lang/String;Landroid/webkit/ValueCallback;)V
            """.trimIndent(),
        )
    }
}
