package app.template.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

/**
 * Telegram-only compatibility definitions for this personal/testing repository.
 */
object Constants {
    val TELEGRAM_COMPATIBILITY = Compatibility(
        name = "Telegram",
        packageName = "org.telegram.messenger",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x2CA5E0,
        targets = listOf(
            AppTarget(version = "13.2.2", versionCode = 73031),
            // Previously audited legacy target; older named fingerprints remain available.
            AppTarget(version = "13.1.1", versionCode = 72371),
        )
    )

    val TELEGRAM_PLUS_COMPATIBILITY = Compatibility(
        name = "Telegram Plus",
        packageName = "org.telegram.plus",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x2CA5E0,
        targets = listOf(
            AppTarget(version = "13.2.1.0", versionCode = 22678),
            // Preserve the earlier Plus Messenger target and its independent legacy hooks.
            AppTarget(version = "13.1.1.0", versionCode = 22621),
        )
    )

    val TELEGRAM_WEB_COMPATIBILITY = Compatibility(
        name = "Telegram Web",
        packageName = "org.telegram.messenger.web",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x2CA5E0,
        targets = listOf(
            AppTarget(version = "13.2.2", versionCode = 73039),
            // Previously audited Web target; legacy fingerprints are retained separately.
            AppTarget(version = "13.1.1", versionCode = 72379),
        )
    )
}