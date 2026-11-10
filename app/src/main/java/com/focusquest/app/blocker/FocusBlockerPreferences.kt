package com.focusquest.app.blocker

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import android.provider.Settings

object FocusBlockerPreferences {
    private const val PREFS = "focus_blocker_preferences"
    private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
    private const val KEY_FOCUS_ACTIVE = "focus_active"
    private const val KEY_FOCUS_END_AT = "focus_end_at"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getBlockedPackages(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_BLOCKED_PACKAGES, emptySet())?.toSet() ?: emptySet()

    fun setBlockedPackages(context: Context, packages: Set<String>) {
        prefs(context).edit().putStringSet(KEY_BLOCKED_PACKAGES, packages.toSet()).apply()
    }

    fun isFocusActive(context: Context): Boolean {
        val store = prefs(context)
        val active = store.getBoolean(KEY_FOCUS_ACTIVE, false)
        val endAt = store.getLong(KEY_FOCUS_END_AT, 0L)
        return active && (endAt == 0L || System.currentTimeMillis() < endAt)
    }

    fun setFocusActive(context: Context, active: Boolean, endAtMillis: Long = 0L) {
        prefs(context).edit()
            .putBoolean(KEY_FOCUS_ACTIVE, active)
            .putLong(KEY_FOCUS_END_AT, if (active) endAtMillis else 0L)
            .apply()
    }

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlayAccess(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun hasRequiredPermissions(context: Context): Boolean =
        hasUsageAccess(context) && hasOverlayAccess(context)
}
