package com.focusquest.app.blocker

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.content.Intent
import com.focusquest.app.MainActivity

class FocusAccessibilityService : AccessibilityService() {
    private var windowManager: WindowManager? = null
    private var blockerView: View? = null
    private var blockedPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        val active = FocusBlockerPreferences.isFocusActive(this)

        if (!active) {
            removeBlocker()
            return
        }

        if (packageName == applicationContext.packageName) return

        val blocked = FocusBlockerPreferences.getBlockedPackages(this)
        if (packageName in blocked) {
            showBlocker(packageName)
        }
    }

    private fun showBlocker(packageName: String) {
        if (blockerView != null && blockedPackage == packageName) return
        removeBlocker()
        blockedPackage = packageName

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(12, 16, 27))
            isClickable = true
            isFocusable = true
            setOnClickListener { /* Consome toques para impedir interação com o app abaixo. */ }
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(32), dp(28), dp(32))
            setBackgroundColor(Color.rgb(27, 34, 49))
            elevation = dp(12).toFloat()
        }

        val icon = TextView(this).apply {
            text = "⏳"
            textSize = 48f
            gravity = Gravity.CENTER
        }
        val title = TextView(this).apply {
            text = "Hora de manter o foco"
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(10))
        }
        val message = TextView(this).apply {
            text = "Este aplicativo está na sua lista de distrações. Conclua ou pause sua sessão no Focus Quest para continuar."
            textSize = 16f
            setTextColor(Color.rgb(210, 218, 232))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(24))
        }
        val backButton = Button(this).apply {
            text = "Abrir Focus Quest"
            setOnClickListener {
                removeBlocker()
                val intent = Intent(this@FocusAccessibilityService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                startActivity(intent)
            }
        }

        panel.addView(icon, LinearLayout.LayoutParams(-1, -2))
        panel.addView(title, LinearLayout.LayoutParams(-1, -2))
        panel.addView(message, LinearLayout.LayoutParams(-1, -2))
        panel.addView(backButton, LinearLayout.LayoutParams(-1, -2))

        val panelParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ).apply {
            leftMargin = dp(24)
            rightMargin = dp(24)
        }
        root.addView(panel, panelParams)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        try {
            windowManager?.addView(root, params)
            blockerView = root
        } catch (_: Exception) {
            blockerView = null
            blockedPackage = null
        }
    }

    private fun removeBlocker() {
        val view = blockerView ?: run {
            blockedPackage = null
            return
        }
        try {
            windowManager?.removeView(view)
        } catch (_: Exception) {
        }
        blockerView = null
        blockedPackage = null
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onInterrupt() {
        removeBlocker()
    }

    override fun onDestroy() {
        removeBlocker()
        super.onDestroy()
    }
}
