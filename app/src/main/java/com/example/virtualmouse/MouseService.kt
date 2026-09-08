package com.example.virtualmouse

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class MouseService : Service() {

    private var windowManager: WindowManager? = null
    private var cursorView: ImageView? = null
    private var menuView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var menuParams: WindowManager.LayoutParams? = null
    private var sensitivity = 10
    private var cursorSize = 48
    private var cursorShape = 0
    private var cursorColor = 0xFF6200EE
    private var screenWidth = 0
    private var screenHeight = 0
    private var lastTouchTime = 0L
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private val DOUBLE_TAP_TIMEOUT = 300L
    private val handler = Handler(Looper.getMainLooper())
    private var isMenuShowing = false
    private val TAG = "VirtualMouseService"

    override fun onCreate() {
        super.onCreate()
        try {
            val dm = android.util.DisplayMetrics()
            windowManager?.defaultDisplay?.getMetrics(dm)
            screenWidth = dm.widthPixels
            screenHeight = dm.heightPixels
            createNotificationChannel()
            startForeground(1, createNotification())
            createCursorView()
            createMenuView()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            sensitivity = intent?.getIntExtra("sensitivity", 10) ?: 10
            cursorSize = intent?.getIntExtra("cursorSize", 48) ?: 48
            cursorShape = intent?.getIntExtra("cursorShape", 0) ?: 0
            cursorColor = intent?.getIntExtra("cursorColor", 0xFF6200EE) ?: 0xFF6200EE
            
            // Recreate cursor with new params
            cursorView?.let { windowManager?.removeView(it) }
            menuView?.let { windowManager?.removeView(it) }
            isMenuShowing = false
            
            createCursorView()
            createMenuView()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand", e)
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channel = NotificationChannel(
                    "mouse_service_channel",
                    "موس مجازی",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "سرویس موس شناور در حال اجراست"
                    setShowBadge(false)
                }
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channel", e)
            }
        }
    }

    private fun createNotification(): Notification {
        try {
            val stopIntent = Intent(this, MouseService::class.java).apply {
                action = "STOP_MOUSE"
            }
            val stopPendingIntent = android.app.PendingIntent.getService(
                this, 0, stopIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val openIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val openPendingIntent = android.app.PendingIntent.getActivity(
                this, 0, openIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            return NotificationCompat.Builder(this, "mouse_service_channel")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("موس مجازی فعال است")
                .setContentText("لمس برای توقف • باز کردن برنامه برای تنظیمات")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .addAction(android.R.drawable.ic_media_pause, "توقف", stopPendingIntent)
                .addAction(android.R.drawable.ic_menu_manage, "تنظیمات", openPendingIntent)
                .setContentIntent(openPendingIntent)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification", e)
            return NotificationCompat.Builder(this, "mouse_service_channel").build()
        }
    }

    private fun createCursorView() {
        try {
            cursorView = ImageView(this).apply {
                setImageResource(getCursorDrawable())
                setColorFilter(cursorColor)
                layoutParams = ViewGroup.LayoutParams(cursorSize, cursorSize)
                setOnTouchListener(cursorTouchListener)
                setOnLongClickListener {
                    showMenu()
                    true
                }
            }

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }

            layoutParams = WindowManager.LayoutParams(
                cursorSize, cursorSize,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (screenWidth / 2 - cursorSize / 2).coerceIn(0, screenWidth - cursorSize)
                y = (screenHeight / 2 - cursorSize / 2).coerceIn(0, screenHeight - cursorSize)
            }

            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager?.addView(cursorView!!, layoutParams!!)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating cursor view", e)
        }
    }

    private fun createMenuView() {
        try {
            menuView = LayoutInflater.from(this).inflate(R.layout.menu_popup, null)

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }

            menuParams = WindowManager.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
            }

            // Setup menu item clicks
            val menuItems = mapOf(
                R.id.menu_click to { performClick() },
                R.id.menu_right_click to { performRightClick() },
                R.id.menu_copy to { performCopy() },
                R.id.menu_paste to { performPaste() },
                R.id.menu_select_all to { performSelectAll() },
                R.id.menu_scroll_up to { performScroll(-1) },
                R.id.menu_scroll_down to { performScroll(1) },
                R.id.menu_close to { stopSelf() }
            )

            menuItems.forEach { (id, action) ->
                menuView?.findViewById<View>(id)?.setOnClickListener {
                    action()
                    hideMenu()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating menu view", e)
        }
    }

    private fun getCursorDrawable(): Int {
        return when (cursorShape) {
            1 -> R.drawable.cursor_hand
            2 -> R.drawable.cursor_crosshair
            3 -> R.drawable.cursor_circle
            else -> R.drawable.cursor_arrow
        }
    }

    private val cursorTouchListener = View.OnTouchListener { v, event ->
        try {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val currentTime = System.currentTimeMillis()
                    val isDoubleTap = (currentTime - lastTouchTime < DOUBLE_TAP_TIMEOUT) &&
                        Math.abs(event.rawX - lastTouchX) < 50 &&
                        Math.abs(event.rawY - lastTouchY) < 50

                    lastTouchTime = currentTime
                    lastTouchX = event.rawX
                    lastTouchY = event.rawY

                    if (isDoubleTap) {
                        performClick()
                    }
                    v.performClick()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams?.x = (event.rawX - v.width / 2).toInt()
                    layoutParams?.y = (event.rawY - v.height / 2).toInt()
                    layoutParams?.x = layoutParams!!.x.coerceIn(0, screenWidth - cursorSize)
                    layoutParams?.y = layoutParams!!.y.coerceIn(0, screenHeight - cursorSize)
                    windowManager?.updateViewLayout(v, layoutParams!!)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMenuShowing) {
                        performClick()
                    }
                    true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in cursor touch listener", e)
        }
        true
    }

    private fun showMenu() {
        if (isMenuShowing || menuView == null || windowManager == null) return

        try {
            val cursorX = layoutParams?.x ?: 0
            val cursorY = layoutParams?.y ?: 0

            menuParams?.x = (cursorX + cursorSize).coerceAtMost(screenWidth - 200)
            menuParams?.y = cursorY.coerceAtMost(screenHeight - 300)

            windowManager?.addView(menuView!!, menuParams!!)
            isMenuShowing = true

            handler.postDelayed({ hideMenu() }, 3000)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing menu", e)
        }
    }

    private fun hideMenu() {
        if (isMenuShowing && menuView != null && windowManager != null) {
            try {
                windowManager?.removeView(menuView!!)
                isMenuShowing = false
                handler.removeCallbacksAndMessages(null)
            } catch (e: Exception) {
                Log.e(TAG, "Error hiding menu", e)
            }
        }
    }

    private fun performClick() {
        sendAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
        Toast.makeText(this, "کلیک", Toast.LENGTH_SHORT).show()
    }

    private fun performRightClick() {
        sendAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_LONG_CLICK)
        Toast.makeText(this, "راست کلیک", Toast.LENGTH_SHORT).show()
    }

    private fun performCopy() {
        sendAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_COPY)
        Toast.makeText(this, "کپی شد", Toast.LENGTH_SHORT).show()
    }

    private fun performPaste() {
        sendAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_PASTE)
        Toast.makeText(this, "چسباندن", Toast.LENGTH_SHORT).show()
    }

    private fun performSelectAll() {
        sendAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_SELECT_ALL)
        Toast.makeText(this, "انتخاب همه", Toast.LENGTH_SHORT).show()
    }

    private fun performScroll(direction: Int) {
        val action = if (direction < 0) {
            android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        } else {
            android.view.accessibility.AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        }
        sendAccessibilityAction(action)
        Toast.makeText(this, if (direction < 0) "اسکرول بالا" else "اسکرول پایین", Toast.LENGTH_SHORT).show()
    }

    private fun sendAccessibilityAction(action: Int) {
        try {
            val intent = Intent("com.example.virtualmouse.ACCESSIBILITY_ACTION").apply {
                putExtra("action", action)
                putExtra("x", layoutParams?.x ?: 0)
                putExtra("y", layoutParams?.y ?: 0)
            }
            sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending accessibility action", e)
        }
    }

    override fun onDestroy() {
        try {
            cursorView?.let { windowManager?.removeView(it) }
            if (isMenuShowing) {
                menuView?.let { windowManager?.removeView(it) }
            }
            stopForeground(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
