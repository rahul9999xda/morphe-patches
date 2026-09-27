package app.template.patches.netmirror

import app.morphe.patcher.fingerprint

private const val RN_DEVICE = "Lcom/learnium/RNDeviceInfo/RNDeviceModule;"
private const val WEBVIEW_CHROME = "Lcom/reactnativecommunity/webview/c;"
private const val WEBVIEW_CLIENT = "Lcom/reactnativecommunity/webview/i;"
private const val INTENT_MODULE = "Lcom/facebook/react/modules/intent/IntentModule;"
private const val WEBVIEW = "Landroid/webkit/WebView;"
private const val WEB_RESOURCE_REQUEST = "Landroid/webkit/WebResourceRequest;"
private const val STRING = "Ljava/lang/String;"
private const val PROMISE = "Lcom/facebook/react/bridge/Promise;"
private const val MESSAGE = "Landroid/os/Message;"

// These are intentionally signature-anchored: all target classes are present in the supplied
// APK and the signatures are exact. This is more robust here than hard-coding DEX offsets.
val getAndroidIdSyncFingerprint = fingerprint {
    definingClass(RN_DEVICE)
    name("getAndroidIdSync")
    returnType(STRING)
    parameterTypes()
}

val getUniqueIdSyncFingerprint = fingerprint {
    definingClass(RN_DEVICE)
    name("getUniqueIdSync")
    returnType(STRING)
    parameterTypes()
}

val getInstanceIdSyncFingerprint = fingerprint {
    definingClass(RN_DEVICE)
    name("getInstanceIdSync")
    returnType(STRING)
    parameterTypes()
}

val getInstallReferrerSyncFingerprint = fingerprint {
    definingClass(RN_DEVICE)
    name("getInstallReferrerSync")
    returnType(STRING)
    parameterTypes()
}

val getInstallerPackageNameSyncFingerprint = fingerprint {
    definingClass(RN_DEVICE)
    name("getInstallerPackageNameSync")
    returnType(STRING)
    parameterTypes()
}

val getIpAddressSyncFingerprint = fingerprint {
    definingClass(RN_DEVICE)
    name("getIpAddressSync")
    returnType(STRING)
    parameterTypes()
}

val getMacAddressSyncFingerprint = fingerprint {
    definingClass(RN_DEVICE)
    name("getMacAddressSync")
    returnType(STRING)
    parameterTypes()
}

val getCarrierSyncFingerprint = fingerprint {
    definingClass(RN_DEVICE)
    name("getCarrierSync")
    returnType(STRING)
    parameterTypes()
}

val getFingerprintSyncFingerprint = fingerprint {
    definingClass(RN_DEVICE)
    name("getFingerprintSync")
    returnType(STRING)
    parameterTypes()
}

val getSerialNumberSyncFingerprint = fingerprint {
    definingClass(RN_DEVICE)
    name("getSerialNumberSync")
    returnType(STRING)
    parameterTypes()
}

val getFirstInstallTimeSyncFingerprint = fingerprint {
    definingClass(RN_DEVICE)
    name("getFirstInstallTimeSync")
    returnType("D")
    parameterTypes()
}

val getLastUpdateTimeSyncFingerprint = fingerprint {
    definingClass(RN_DEVICE)
    name("getLastUpdateTimeSync")
    returnType("D")
    parameterTypes()
}

val onCreateWindowFingerprint = fingerprint {
    definingClass(WEBVIEW_CHROME)
    name("onCreateWindow")
    returnType("Z")
    parameterTypes(WEBVIEW, "Z", "Z", MESSAGE)
}

val webViewStringNavigationFingerprint = fingerprint {
    definingClass(WEBVIEW_CLIENT)
    name("shouldOverrideUrlLoading")
    returnType("Z")
    parameterTypes(WEBVIEW, STRING)
}

val webViewRequestNavigationFingerprint = fingerprint {
    definingClass(WEBVIEW_CLIENT)
    name("shouldOverrideUrlLoading")
    returnType("Z")
    parameterTypes(WEBVIEW, WEB_RESOURCE_REQUEST)
}

val intentOpenUrlFingerprint = fingerprint {
    definingClass(INTENT_MODULE)
    name("openURL")
    returnType("V")
    parameterTypes(STRING, PROMISE)
}
