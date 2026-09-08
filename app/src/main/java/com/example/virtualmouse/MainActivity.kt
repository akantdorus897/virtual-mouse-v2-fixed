package com.example.virtualmouse

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnEnableOverlay: Button
    private lateinit var btnEnableAccessibility: Button
    private lateinit var btnEnableNotifications: Button
    private lateinit var seekSensitivity: SeekBar
    private lateinit var seekCursorSize: SeekBar
    private lateinit var tvSensitivity: TextView
    private lateinit var tvCursorSize: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvStatusDetail: TextView
    private lateinit var tvLoading: TextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var ivOverlayStatus: ImageView
    private lateinit var ivAccessibilityStatus: ImageView
    private lateinit var ivNotificationStatus: ImageView
    private lateinit var rgCursorShape: RadioGroup
    private lateinit var rgCursorColor: RadioGroup

    private var sensitivity = 10
    private var cursorSize = 48
    private var cursorShape = 0
    private var cursorColor = 0xFF6200EE

    private val TAG = "VirtualMouseMain"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        checkPermissions()
        updateStatusUI(false)
    }

    private fun initViews() {
        btnStart = findViewById(R.id.btnStart)
        btnStop = findViewById(R.id.btnStop)
        btnEnableOverlay = findViewById(R.id.btnEnableOverlay)
        btnEnableAccessibility = findViewById(R.id.btnEnableAccessibility)
        btnEnableNotifications = findViewById(R.id.btnEnableNotifications)
        seekSensitivity = findViewById(R.id.seekSensitivity)
        seekCursorSize = findViewById(R.id.seekCursorSize)
        tvSensitivity = findViewById(R.id.tvSensitivity)
        tvCursorSize = findViewById(R.id.tvCursorSize)
        tvStatus = findViewById(R.id.tvStatus)
        tvStatusDetail = findViewById(R.id.tvStatusDetail)
        tvLoading = findViewById(R.id.tvLoading)
        progressLoading = findViewById(R.id.progressLoading)
        ivOverlayStatus = findViewById(R.id.ivOverlayStatus)
        ivAccessibilityStatus = findViewById(R.id.ivAccessibilityStatus)
        ivNotificationStatus = findViewById(R.id.ivNotificationStatus)
        rgCursorShape = findViewById(R.id.rgCursorShape)
        rgCursorColor = findViewById(R.id.rgCursorColor)
    }

    private fun setupListeners() {
        seekSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                sensitivity = progress
                tvSensitivity.text = getString(R.string.sensitivity, sensitivity)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        seekCursorSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                cursorSize = progress.coerceAtLeast(24)
                tvCursorSize.text = getString(R.string.cursor_size, cursorSize)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        rgCursorShape.setOnCheckedChangeListener { _, checkedId ->
            cursorShape = when (checkedId) {
                R.id.rbHand -> 1
                R.id.rbCrosshair -> 2
                R.id.rbCircle -> 3
                else -> 0
            }
        }

        rgCursorColor.setOnCheckedChangeListener { _, checkedId ->
            cursorColor = when (checkedId) {
                R.id.rbColorWhite -> 0xFFFFFFFF
                R.id.rbColorRed -> 0xFFFF0000
                R.id.rbColorGreen -> 0xFF00FF00
                R.id.rbColorBlue -> 0xFF0000FF
                else -> 0xFF6200EE
            }
        }

        btnStart.setOnClickListener { startMouseService() }
        btnStop.setOnClickListener { stopMouseService() }
        btnEnableOverlay.setOnClickListener { openOverlaySettings() }
        btnEnableAccessibility.setOnClickListener { openAccessibilitySettings() }
        btnEnableNotifications.setOnClickListener { requestNotificationPermission() }
    }

    private fun checkPermissions() {
        // Overlay permission
        val overlayEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true
        updateOverlayStatus(overlayEnabled)

        // Accessibility service
        val accessibilityEnabled = isAccessibilityServiceEnabled()
        updateAccessibilityStatus(accessibilityEnabled)

        // Notification permission (Android 13+)
        val notificationEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
        updateNotificationStatus(notificationEnabled)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager
            val services = accessibilityManager.enabledAccessibilityServiceList(android.view.accessibility.AccessibilityServiceInfo.FEEDBACK_GENERIC)
            services.any { it.id.contains("VirtualMouseAccessibilityService") }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility service", e)
            false
        }
    }

    private fun updateOverlayStatus(enabled: Boolean) {
        runOnUiThread {
            ivOverlayStatus.setImageResource(if (enabled) android.R.drawable.ic_menu_accept else android.R.drawable.ic_delete)
            ivOverlayStatus.setColorFilter(ContextCompat.getColor(this, if (enabled) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
            btnEnableOverlay.visibility = if (enabled) View.GONE else View.VISIBLE
        }
    }

    private fun updateAccessibilityStatus(enabled: Boolean) {
        runOnUiThread {
            ivAccessibilityStatus.setImageResource(if (enabled) android.R.drawable.ic_menu_accept else android.R.drawable.ic_delete)
            ivAccessibilityStatus.setColorFilter(ContextCompat.getColor(this, if (enabled) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
            btnEnableAccessibility.visibility = if (enabled) View.GONE else View.VISIBLE
        }
    }

    private fun updateNotificationStatus(enabled: Boolean) {
        runOnUiThread {
            ivNotificationStatus.setImageResource(if (enabled) android.R.drawable.ic_menu_accept else android.R.drawable.ic_delete)
            ivNotificationStatus.setColorFilter(ContextCompat.getColor(this, if (enabled) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
            btnEnableNotifications.visibility = if (enabled) View.GONE else View.VISIBLE
        }
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, 1001)
        }
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        Toast.makeText(this, R.string.accessibility_service_message, Toast.LENGTH_LONG).show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1002)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            checkPermissions()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1002) {
            checkPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun startMouseService() {
        // Check all permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, R.string.overlay_permission_message, Toast.LENGTH_LONG).show()
            return
        }

        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(this, R.string.accessibility_service_message, Toast.LENGTH_LONG).show()
            openAccessibilitySettings()
            return
        }

        showLoading(true)
        btnStart.isEnabled = false

        findViewById<View>(android.R.id.content).postDelayed({
            try {
                val intent = Intent(this, MouseService::class.java).apply {
                    putExtra("sensitivity", sensitivity)
                    putExtra("cursorSize", cursorSize)
                    putExtra("cursorShape", cursorShape)
                    putExtra("cursorColor", cursorColor)
                }
                ContextCompat.startForegroundService(this, intent)
                updateStatusUI(true)
                showLoading(false)
                Toast.makeText(this, "موس مجازی فعال شد", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start MouseService", e)
                showLoading(false)
                btnStart.isEnabled = true
                Toast.makeText(this, "خطا در شروع: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, 800)
    }

    private fun stopMouseService() {
        try {
            val intent = Intent(this, MouseService::class.java)
            stopService(intent)
            updateStatusUI(false)
            Toast.makeText(this, "موس مجازی غیرفعال شد", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop MouseService", e)
        }
    }

    private fun updateStatusUI(active: Boolean) {
        runOnUiThread {
            btnStart.isEnabled = !active
            btnStop.isEnabled = active
            tvStatus.text = if (active) "فعال" else "غیرفعال"
            tvStatus.setTextColor(ContextCompat.getColor(this, if (active) android.R.color.holo_green_dark else android.R.color.holo_red_dark))
            tvStatusDetail.text = if (active) R.string.mouse_active else "موس مجازی متوقف است"
        }
    }

    private fun showLoading(show: Boolean) {
        runOnUiThread {
            progressLoading.visibility = if (show) View.VISIBLE else View.GONE
            tvLoading.visibility = if (show) View.VISIBLE else View.GONE
            btnStart.visibility = if (show) View.GONE else View.VISIBLE
        }
    }
}
