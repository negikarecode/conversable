package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.example.R

// Google Font Provider
val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

val PlusJakartaSansFont = GoogleFont("Plus Jakarta Sans")

val PlusJakartaSans = FontFamily(
    Font(googleFont = PlusJakartaSansFont, fontProvider = provider, weight = FontWeight.Normal),
    Font(googleFont = PlusJakartaSansFont, fontProvider = provider, weight = FontWeight.Medium),
    Font(googleFont = PlusJakartaSansFont, fontProvider = provider, weight = FontWeight.SemiBold),
    Font(googleFont = PlusJakartaSansFont, fontProvider = provider, weight = FontWeight.Bold)
)

// Typography Scale using Plus Jakarta Sans
val TextXs = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Medium, // Labels/badges: 11px weight 500
    fontSize = 11.sp,
    lineHeight = 11.sp,
    letterSpacing = 0.06.sp
)

val TextSm = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Normal, // Card body: 14px weight 400
    fontSize = 14.sp,
    lineHeight = 21.sp, // 1.5
    letterSpacing = 0.sp
)

val TextBase = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Normal, // Body text: 15px weight 400 line-height 1.6
    fontSize = 15.sp,
    lineHeight = 24.sp, // 1.6
    letterSpacing = 0.sp
)

val TextLg = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.SemiBold, // Card titles: 17px weight 600
    fontSize = 17.sp,
    lineHeight = 22.1.sp, // 1.3
    letterSpacing = (-0.34).sp // -0.02em
)

val TextXl = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 26.4.sp,
    letterSpacing = (-0.44).sp
)

val Text2Xl = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Bold, // Section headlines: 28px weight 700
    fontSize = 28.sp,
    lineHeight = 33.6.sp,
    letterSpacing = (-0.56).sp
)

val DisplayLg = TextStyle(
    fontFamily = PlusJakartaSans,
    fontWeight = FontWeight.Bold, // Hero headline: 36px weight 700
    fontSize = 36.sp,
    lineHeight = 43.2.sp,
    letterSpacing = (-0.72).sp
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

