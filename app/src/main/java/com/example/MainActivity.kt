package com.example

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ThemeMode
import com.example.data.local.UserSettings
import com.example.data.remote.configureAppCheck
import com.example.di.LexisContainer
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.LexisTheme

class MainActivity : ComponentActivity() {

    private lateinit var container: LexisContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashPrefs = getSharedPreferences("lexis_crash", Context.MODE_PRIVATE)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            crashPrefs.edit().putString("last_trace", Log.getStackTraceString(throwable)).commit()
            defaultHandler?.uncaughtException(thread, throwable)
        }

        val lastTrace = crashPrefs.getString("last_trace", null)
        if (!lastTrace.isNullOrEmpty()) {
            showCrashView(lastTrace, crashPrefs)
            return
        }

        try {
            // MUST run before setContent, ViewModels, or any Firebase AI/Firestore calls
            configureAppCheck(this, intent)
            container = LexisContainer.get(this)

            enableEdgeToEdge()
            setContent {
                val settings by container.prefsRepository.settingsFlow
                    .collectAsStateWithLifecycle(initialValue = UserSettings())

                val darkTheme = when (settings.themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
                val layoutDir = if (settings.arabicUi) LayoutDirection.Rtl else LayoutDirection.Ltr

                LexisTheme(darkTheme = darkTheme) {
                    CompositionLocalProvider(LocalLayoutDirection provides layoutDir) {
                        AppNavigation(
                            container = container,
                            arabicUi = settings.arabicUi
                        )
                    }
                }
            }
        } catch (e: Throwable) {
            showCrashView(Log.getStackTraceString(e), crashPrefs)
        }
    }

    private fun showCrashView(trace: String, crashPrefs: SharedPreferences) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(32, 64, 32, 32)
        }
        val retryButton = Button(this).apply {
            text = "Clear & Retry"
            setOnClickListener {
                crashPrefs.edit().remove("last_trace").commit()
                val restartIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                if (restartIntent != null) {
                    startActivity(restartIntent)
                } else {
                    recreate()
                }
                finish()
            }
        }
        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        val textView = TextView(this).apply {
            text = trace
            setTextColor(Color.BLACK)
            textSize = 12f
            setTextIsSelectable(true)
        }
        scrollView.addView(textView)
        layout.addView(retryButton)
        layout.addView(scrollView)
        setContentView(layout)
    }

    override fun onDestroy() {
        if (isFinishing && ::container.isInitialized) {
            container.audioEngine.shutdown()
        }
        super.onDestroy()
    }
}
