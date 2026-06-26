package com.example.security

import android.app.Activity
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

/**
 * Reusable Compose side-effect that applies WindowManager.LayoutParams.FLAG_SECURE
 * to the current activity window, preventing screenshots and screen recordings.
 * It automatically removes the flag when the composable leaves the composition.
 */
@Composable
fun KeepScreenSecure() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
