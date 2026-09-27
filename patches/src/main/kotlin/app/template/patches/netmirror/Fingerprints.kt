package app.template.patches.netmirror

import app.morphe.patcher.Fingerprint

private const val RN_DEVICE = "Lcom/learnium/RNDeviceInfo/RNDeviceModule;"
private const val WEBVIEW_CHROME = "Lcom/reactnativecommunity/webview/c;"
private const val WEBVIEW_CLIENT = "Lcom/reactnativecommunity/webview/i;"
private const val INTENT_MODULE = "Lcom/facebook/react/modules/intent/IntentModule;"
private const val WEBVIEW = "Landroid/webkit/WebView;"
private const val WEB_RESOURCE_REQUEST = "Landroid/webkit/WebResourceRequest;"
private const val STRING = "Ljava/lang/String;"
private const val PROMISE = "Lcom/facebook/react/bridge/Promise;"
private const val MESSAGE = "Landroid/os/Message;"

// Morphe's current Fingerprint API uses constructor parameters directly.
// These are exact signatures from NetMirror 3.1 / versionCode 1.

val getDeviceIdFingerprint = Fingerprint(
    definingClass = RN_DEVICE, name = "getDeviceId", returnType = STRING, parameters = emptyList(),
)
val getAndroidIdSyncFingerprint = Fingerprint(
    definingClass = RN_DEVICE, name = "getAndroidIdSync", returnType = STRING, parameters = emptyList(),
)
val getUniqueIdSyncFingerprint = Fingerprint(
    definingClass = RN_DEVICE, name = "getUniqueIdSync", returnType = STRING, parameters = emptyList(),
)
val getInstanceIdSyncFingerprint = Fingerprint(
    definingClass = RN_DEVICE, name = "getInstanceIdSync", returnType = STRING, parameters = emptyList(),
)
val getInstallReferrerSyncFingerprint = Fingerprint(
    definingClass = RN_DEVICE, name = "getInstallReferrerSync", returnType = STRING, parameters = emptyList(),
)
val getInstallerPackageNameSyncFingerprint = Fingerprint(
    definingClass = RN_DEVICE, name = "getInstallerPackageNameSync", returnType = STRING, parameters = emptyList(),
)
val getIpAddressSyncFingerprint = Fingerprint(
    definingClass = RN_DEVICE, name = "getIpAddressSync", returnType = STRING, parameters = emptyList(),
)
val getMacAddressSyncFingerprint = Fingerprint(
    definingClass = RN_DEVICE, name = "getMacAddressSync", returnType = STRING, parameters = emptyList(),
)
val getCarrierSyncFingerprint = Fingerprint(
    definingClass = RN_DEVICE, name = "getCarrierSync", returnType = STRING, parameters = emptyList(),
)
val getFingerprintSyncFingerprint = Fingerprint(
    definingClass = RN_DEVICE, name = "getFingerprintSync", returnType = STRING, parameters = emptyList(),
)
val getSerialNumberSyncFingerprint = Fingerprint(
    definingClass = RN_DEVICE, name = "getSerialNumberSync", returnType = STRING, parameters = emptyList(),
)
val getFirstInstallTimeSyncFingerprint = Fingerprint(
    definingClass = RN_DEVICE, name = "getFirstInstallTimeSync", returnType = "D", parameters = emptyList(),
)
val getLastUpdateTimeSyncFingerprint = Fingerprint(
    definingClass = RN_DEVICE, name = "getLastUpdateTimeSync", returnType = "D", parameters = emptyList(),
)

val onCreateWindowFingerprint = Fingerprint(
    definingClass = WEBVIEW_CHROME,
    name = "onCreateWindow",
    returnType = "Z",
    parameters = listOf(WEBVIEW, "Z", "Z", MESSAGE),
)

val webViewStringNavigationFingerprint = Fingerprint(
    definingClass = WEBVIEW_CLIENT,
    name = "shouldOverrideUrlLoading",
    returnType = "Z",
    parameters = listOf(WEBVIEW, STRING),
)

val webViewRequestNavigationFingerprint = Fingerprint(
    definingClass = WEBVIEW_CLIENT,
    name = "shouldOverrideUrlLoading",
    returnType = "Z",
    parameters = listOf(WEBVIEW, WEB_RESOURCE_REQUEST),
)


val webViewPageFinishedFingerprint = Fingerprint(
    definingClass = WEBVIEW_CLIENT,
    name = "onPageFinished",
    returnType = "V",
    parameters = listOf(WEBVIEW, STRING),
)

val intentOpenUrlFingerprint = Fingerprint(
    definingClass = INTENT_MODULE,
    name = "openURL",
    returnType = "V",
    parameters = listOf(STRING, PROMISE),
)
