package com.sdn.semambung.kaih.testc

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.Process
import java.io.PrintWriter
import java.io.StringWriter

/** Application test3 v4: pasang crash handler + log sejak paling awal. */
class TestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TestLog.tulis(this, "APP: onCreate (v4)")
        Thread.setDefaultUncaughtExceptionHandler { _, t ->
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            val trace = sw.toString()
            TestLog.tulis(this, "UNCAUGHT: ${t.javaClass.name}: ${t.message}\n" + trace.take(2500))
            try {
                val i = Intent(this, MainActivity::class.java)
                    .putExtra("trace", trace.take(4000))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(i)
            } catch (_: Exception) {
            }
            Handler(Looper.getMainLooper()).postDelayed({ Process.killProcess(Process.myPid()) }, 30000)
        }
    }
}
