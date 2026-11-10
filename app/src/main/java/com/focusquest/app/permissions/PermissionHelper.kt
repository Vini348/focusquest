package com.focusquest.app.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.focusquest.app.blocker.FocusBlockerPreferences

object PermissionHelper {
    fun hasUsageAccess(context: Context): Boolean =
        FocusBlockerPreferences.hasUsageAccess(context)

    fun hasOverlayAccess(context: Context): Boolean =
        FocusBlockerPreferences.hasOverlayAccess(context)

    fun usageAccessIntent(context: Context): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    fun overlayIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
}
