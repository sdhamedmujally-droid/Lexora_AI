package com.example

import android.os.Bundle
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
    }

 
    override fun onDestroy() {
        if (isFinishing && ::container.isInitialized) {
            container.audioEngine.shutdown()
        }
        super.onDestroy()
    }
}
