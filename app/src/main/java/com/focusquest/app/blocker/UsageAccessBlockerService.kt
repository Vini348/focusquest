package com.focusquest.app.blocker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.focusquest.app.MainActivity

/**
 * Monitora apenas os eventos de primeiro plano fornecidos pelo UsageStatsManager.
 * Não lê textos, campos, notificações ou conteúdo das janelas.
 */
class UsageAccessBlockerService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var overlay: FrameLayout? = null
    private var lastForegroundPackage: String? = null
    private var lastHandledEventTime = 0L
    private val usageManager by lazy {
        getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }
    private val windowManager by lazy {
        getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val poll = object : Runnable {
        override fun run() {
            if (!FocusBlockerPreferences.isFocusActive(this@UsageAccessBlockerService)) {
                stopSelf()
                return
            }
            if (!FocusBlockerPreferences.hasRequiredPermissions(this@UsageAccessBlockerService)) {
                removeOverlay()
                stopSelf()
                return
            }

            val foreground = findForegroundPackage()
            if (foreground != null) {
                lastForegroundPackage = foreground
                val blocked = FocusBlockerPreferences.getBlockedPackages(this@UsageAccessBlockerService)
                if (foreground in blocked && foreground != packageName) {
                    showOverlay()
                } else {
                    removeOverlay()
                }
            }
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!FocusBlockerPreferences.hasRequiredPermissions(this)) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, buildNotification())
        handler.removeCallbacks(poll)
        handler.post(poll)
        return START_NOT_STICKY
    }

    private fun findForegroundPackage(): String? {
        val now = System.currentTimeMillis()
        val events = usageManager.queryEvents(now - EVENT_LOOKBACK_MS, now)
        val event = UsageEvents.Event()
        var newestPackage: String? = null
        var newestTime = lastHandledEventTime
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val isForeground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED
            } else {
                @Suppress("DEPRECATION")
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            }
            if (isForeground && event.timeStamp >= newestTime) {
                newestTime = event.timeStamp
                newestPackage = event.packageName
            }
        }
        if (newestPackage != null) lastHandledEventTime = newestTime
        return newestPackage ?: lastForegroundPackage
    }

    private fun showOverlay() {
        if (overlay != null || !Settings.canDrawOverlays(this)) return
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(12, 16, 27))
            isClickable = true
            isFocusable = true
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(28), dp(30), dp(28), dp(30))
            setBackgroundColor(Color.rgb(27, 34, 49))
            elevation = dp(12).toFloat()
        }
        panel.addView(TextView(this).apply {
            text = "⏳"
            textSize = 46f
            gravity = Gravity.CENTER
        }, LinearLayout.LayoutParams(-1, -2))
        panel.addView(TextView(this).apply {
            text = "Hora de manter o foco"
            textSize = 23f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(14), 0, dp(10))
        }, LinearLayout.LayoutParams(-1, -2))
        panel.addView(TextView(this).apply {
            text = "Este aplicativo está na sua lista de distrações. Pause ou encerre sua sessão no Focus Quest para continuar."
            textSize = 16f
            setTextColor(Color.rgb(210, 218, 232))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(22))
        }, LinearLayout.LayoutParams(-1, -2))
        panel.addView(Button(this).apply {
            text = "Abrir Focus Quest"
            setOnClickListener {
                removeOverlay()
                val launch = packageManager.getLaunchIntentForPackage(packageName)
                    ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                if (launch != null) startActivity(launch)
            }
        }, LinearLayout.LayoutParams(-1, -2))

        root.addView(panel, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        ).apply {
            leftMargin = dp(24)
            rightMargin = dp(24)
        })

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        try {
            windowManager.addView(root, params)
            overlay = root
        } catch (_: Exception) {
            overlay = null
        }
    }

    private fun removeOverlay() {
        overlay?.let {
            try { windowManager.removeView(it) } catch (_: Exception) {}
        }
        overlay = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Sessão de foco",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Indica que o Focus Quest está monitorando a sessão de foco."
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        val pending = launch?.let {
            PendingIntent.getActivity(
                this, 0, it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Focus Quest: sessão de foco ativa")
            .setContentText("Proteção de foco está monitorando apenas o app em primeiro plano.")
            .setOngoing(true)
            .setContentIntent(pending)
            .build()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        removeOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "focus_session_monitor"
        private const val NOTIFICATION_ID = 4102
        private const val POLL_INTERVAL_MS = 850L
        private const val EVENT_LOOKBACK_MS = 15_000L

        fun start(context: Context) {
            if (!FocusBlockerPreferences.hasRequiredPermissions(context)) return
            val intent = Intent(context, UsageAccessBlockerService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, UsageAccessBlockerService::class.java))
        }
    }
}
