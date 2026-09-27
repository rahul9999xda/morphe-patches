package app.template.patches.netmirror

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.NETMIRROR_COMPATIBILITY

// Evidence-based support-gate strategy for NetMirror 3.1:
// Do NOT delete or blur DOM nodes. The v3.1 Hermes bundle contains the
// callback text `pressfromAPP()` used by the app's ad-return flow. We leave
// the original support screen intact and invoke that same callback after
// the documented 20-second wait. This avoids guessing historical CSS/DOM
// selectors and avoids destroying the catalogue/backdrop hierarchy.
private const val SUPPORT_WAIT_JS = """(function(){function unlock(){try{if(typeof pressfromAPP==='function'){pressfromAPP();return true}}catch(e){}return false}setTimeout(function(){if(!unlock()){setTimeout(function(){unlock()},1000);setTimeout(function(){unlock()},5000)}},20000)})();"""

@Suppress("unused")
val netMirrorDisableWebViewPopupPatch = bytecodePatch(
    name = "NetMirror: Disable WebView popups",
    description = "Prevents secondary WebView windows used by the support-ad redirect flow from opening.",
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
    description = "Blocks Mobidetect/MobileDetect navigation and external intents without altering normal navigation.",
    default = true,
) {
    compatibleWith(NETMIRROR_COMPATIBILITY)
    execute {
        // The WebResourceRequest overload in the original v3.1 DEX already
        // delegates to this String overload. Do not inject into that 3-register
        // method: there are no spare local registers.
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
val netMirrorTimedSupportUnlockPatch = bytecodePatch(
    name = "NetMirror: Complete support wait after 20 seconds",
    description = "Keeps the original support screen intact and invokes the v3.1 pressfromAPP return callback after the documented 20-second ad wait; no DOM removal or blur manipulation.",
    default = true,
) {
    compatibleWith(NETMIRROR_COMPATIBILITY)
    execute {
        val method = webViewPageFinishedFingerprint.method
        val insertIndex = method.implementation!!.instructions.lastIndex
        method.addInstructions(
            insertIndex,
            """
                const-string v0, "$SUPPORT_WAIT_JS"
                const/4 p0, 0x0
                invoke-virtual { p1, v0, p0 }, Landroid/webkit/WebView;->evaluateJavascript(Ljava/lang/String;Landroid/webkit/ValueCallback;)V
            """.trimIndent(),
        )
    }
}
