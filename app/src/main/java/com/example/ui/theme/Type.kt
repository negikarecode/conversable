package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Typography Scale
val TextXs = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 11.sp,
    letterSpacing = 0.06.sp
)

val TextSm = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 19.5.sp, // 1.5
    letterSpacing = 0.sp
)

val TextBase = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.5.sp, // 1.5
    letterSpacing = 0.sp
)

val TextLg = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold, // 600
    fontSize = 17.sp,
    lineHeight = 20.4.sp, // 1.2
    letterSpacing = (-0.34).sp // -0.02em
)

val TextXl = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold, // 600
    fontSize = 22.sp,
    lineHeight = 26.4.sp, // 1.2
    letterSpacing = (-0.44).sp // -0.02em
)

val Text2Xl = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.SemiBold, // 600
    fontSize = 28.sp,
    lineHeight = 33.6.sp, // 1.2
    letterSpacing = (-0.56).sp // -0.02em
)

val Typography = Typography(
    bodyLarge = TextBase,
    bodyMedium = TextSm,
    bodySmall = TextXs,
    titleLarge = TextXl,
    titleMedium = TextLg,
    titleSmall = TextSm,
    labelLarge = TextBase.copy(fontWeight = FontWeight.Medium),
    labelMedium = TextSm.copy(fontWeight = FontWeight.Medium),
    labelSmall = TextXs.copy(fontWeight = FontWeight.Medium)
)

