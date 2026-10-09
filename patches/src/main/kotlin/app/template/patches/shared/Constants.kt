package app.template.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.SupportedAbi

object Constants {
    val TELEGRAM_COMPATIBILITY = Compatibility(
        name = "Telegram",
        packageName = "org.telegram.messenger",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x2CA5E0,
        targets = listOf(AppTarget(version = "12.10.6", versionCode = 71122))
    )

    val TELEGRAM_PLUS_COMPATIBILITY = Compatibility(
        name = "Telegram Plus",
        packageName = "org.telegram.plus",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0x2CA5E0,
        targets = listOf(AppTarget(version = "12.10.6.0", versionCode = 22588))
    )

    val TELEGRAM_WEB_COMPATIBILITY = Compatibility(
        name = "Telegram Web",
        packageName = "org.telegram.messenger.web",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x2CA5E0,
        targets = listOf(AppTarget(version = "12.10.6", versionCode = 71129))
    )

    val TRUECALLER_COMPATIBILITY = Compatibility(
        name = "Truecaller",
        packageName = "com.truecaller",
        apkFileType = ApkFileType.APKM,
        targets = listOf(AppTarget(version = "26.39.6", versionCode = 2639006))
    )
}
