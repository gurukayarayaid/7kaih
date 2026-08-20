package com.sdn.semambung.kaih

import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sdn.semambung.kaih.ui.KaihViewModel
import com.sdn.semambung.kaih.ui.LoginScreen
import com.sdn.semambung.kaih.ui.MainScreen
import com.sdn.semambung.kaih.ui.theme.KaihTheme
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : ComponentActivity() {

    private lateinit var overlay: TextView
    private val handler = Handler(Looper.getMainLooper())
    private val updateOverlay = object : Runnable {
        override fun run() {
            try {
                overlay.text = "🔍 ${DebugLog.current}"
            } catch (_: Exception) {
            }
            handler.postDelayed(this, 500)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Penangkap error global (redundan dengan provider)
        Thread.setDefaultUncaughtExceptionHandler { _, t ->
            val sw = StringWriter()
            t.printStackTrace(PrintWriter(sw))
            val trace = sw.toString()
            DebugLog.log("UNCAUGHT: ${t.javaClass.name}: ${t.message}")
            runCatching {
                val i = Intent(this, CrashActivity::class.java)
                    .putExtra("trace", if (trace.length > 4000) trace.substring(0, 4000) else trace)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                startActivity(i)
            }
            Handler(Looper.getMainLooper()).postDelayed({ Process.killProcess(Process.myPid()) }, 6000)
        }

        super.onCreate(savedInstanceState)
        DebugLog.init(this)
        DebugLog.log("A onCreate")

        enableEdgeToEdge()

        // 1) Splash (View klasik) — bukti rendering dasar berfungsi
        val splash = TextView(this).apply {
            text = "✦ Aplikasi 7 KAIH\nv1.0.2 (build 8)\nmemuat…"
            textSize = 18f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(12, 74, 110))
        }
        val root = FrameLayout(this).apply {
            addView(splash, FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        setContentView(root)

        // 2) Overlay debug (View klasik, selalu di atas)
        overlay = TextView(this).apply {
            text = "🔍 mulai"
            textSize = 10f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(200, 3, 105, 161))
            setPadding(16, 8, 16, 8)
        }
        val lp = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
        lp.gravity = Gravity.BOTTOM or Gravity.START
        root.addView(overlay, lp)
        handler.post(updateOverlay)

        // 3) Setelah splash singkat, mulai Compose
        root.postDelayed({
            DebugLog.log("B setContent mulai")
            try {
                setContent {
                    KaihTheme {
                        App(vm = viewModel())
                    }
                }
                DebugLog.log("C setContent OK")
            } catch (t: Throwable) {
                DebugLog.log("D setContent GAGAL: ${t.javaClass.name}: ${t.message}")
                t.printStackTrace()
                runOnUiThread {
                    val err = TextView(this).apply {
                        text = "❌ GAGAL RENDER:\n${t.javaClass.name}: ${t.message}\n\n" +
                            t.stackTrace.take(12).joinToString("\n") { "  at $it" }
                        textSize = 12f
                        setTextColor(Color.rgb(127, 29, 29))
                        setBackgroundColor(Color.WHITE)
                        setPadding(40, 40, 40, 40)
                    }
                    setContentView(err)
                }
            }
        }, 600)
    }
}

@Composable
fun App(vm: KaihViewModel = viewModel()) {
    SideEffect { DebugLog.log("App composed") }
    LaunchedEffect(Unit) { vm.restoreSession() }
    val user = vm.user
    SideEffect { DebugLog.log("App: user=${user?.nama ?: "(null)"}") }
    if (user == null) {
        LoginScreen(vm)
    } else {
        MainScreen(vm)
    }
}
