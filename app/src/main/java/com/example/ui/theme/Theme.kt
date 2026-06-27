package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.padding

private val DarkColorScheme =
  darkColorScheme(
    primary = androidx.compose.ui.graphics.Color.White,
    secondary = SleekPrimaryLight,
    tertiary = SleekSuccess,
    background = androidx.compose.ui.graphics.Color(0xFF1A1A2E), // Premium dark navy matching website's APK banner
    surface = androidx.compose.ui.graphics.Color(0xFF252538),     // Lighter dark surface
    onPrimary = androidx.compose.ui.graphics.Color(0xFF1A1A2E),
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SleekPrimary,
    secondary = SleekPrimaryLight,
    tertiary = SleekSuccess,
    background = SleekBackground,
    surface = SleekSurface,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = SleekTextDark,
    onBackground = SleekTextDark,
    onSurface = SleekTextDark
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled by default to show branded Conversable custom theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

fun Modifier.bounceClick() = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1.0f, label = "bounce_scale")
    
    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val down = awaitFirstDown(requireUnconsumed = false)
                isPressed = true
                val up = waitForUpOrCancellation()
                isPressed = false
            }
        }
    }
}

@Composable
fun DifficultyPill(difficulty: String, modifier: Modifier = Modifier) {
    val upper = difficulty.uppercase(java.util.Locale.US)
    val (bgColor, textColor, borderColor) = when (upper) {
        "EASY" -> Triple(androidx.compose.ui.graphics.Color(0xFFEAF6EE), androidx.compose.ui.graphics.Color(0xFF1F8A4D), androidx.compose.ui.graphics.Color(0xFFD0ECD8))
        "MEDIUM" -> Triple(androidx.compose.ui.graphics.Color(0xFFFFF9E6), androidx.compose.ui.graphics.Color(0xFFD97706), androidx.compose.ui.graphics.Color(0xFFFEE6C2))
        "HARD" -> Triple(androidx.compose.ui.graphics.Color(0xFFFDF2F2), androidx.compose.ui.graphics.Color(0xFFD64545), androidx.compose.ui.graphics.Color(0xFFFCD4D4))
        else -> Triple(androidx.compose.ui.graphics.Color(0xFFF6F6F6), androidx.compose.ui.graphics.Color(0xFF777777), androidx.compose.ui.graphics.Color(0xFFD9D9D9))
    }
    androidx.compose.material3.Surface(
        modifier = modifier,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(100.dp),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        androidx.compose.material3.Text(
            text = upper,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 11.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                color = textColor,
                letterSpacing = 0.55.sp
            )
        )
    }
}
