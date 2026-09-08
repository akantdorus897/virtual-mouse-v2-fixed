package com.example.virtualmouse

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ErrorLogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val error = intent?.getStringExtra("error") ?: return
        logError(error)
    }
    private fun logError(error: String) {
        try {
            val logDir = File(context?.getExternalFilesDir(null), "virtualmouse_logs")
            if (!logDir.exists()) logDir.mkdirs()
            val logFile = File(logDir, "error_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.log")
            FileWriter(logFile).use { writer ->
                writer.write("=== ERROR LOG ===\n")
                writer.write("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
                writer.write("Error: $error\n")
                writer.write("\n=== END ===\n")
            }
        } catch (e: Exception) {
            Log.e("ErrorLogReceiver", "Failed to write error log", e)
        }
    }
}
