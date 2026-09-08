package com.example.virtualmouse

import android.app.Application
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            logCrash(throwable)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                try {
                    android.widget.Toast.makeText(
                        this@CrashApplication,
                        "برنامه با خطا مواجه شد. لاگ ذخیره شد.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {}
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(10)
        }
    }
    private fun logCrash(throwable: Throwable) {
        try {
            val logDir = File(getExternalFilesDir(null), "virtualmouse_logs")
            if (!logDir.exists()) logDir.mkdirs()
            val logFile = File(logDir, "crash_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.log")
            FileWriter(logFile).use { writer ->
                writer.write("=== CRASH REPORT ===\n")
                writer.write("Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
                writer.write("App Version: 1.1.0-crashproof\n")
                writer.write("Android: ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})\n")
                writer.write("Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n\n")
                throwable.printStackTrace(writer)
                writer.write("\n=== END ===\n")
            }
            Log.e("CrashApplication", "Crash logged to: \${logFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("CrashApplication", "Failed to write crash log", e)
        }
    }
}
