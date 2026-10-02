package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val AmiriFontFamily = FontFamily(
    Font(R.font.amiri, FontWeight.Normal),
    Font(R.font.amiri, FontWeight.Bold)
)

val CairoFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo, FontWeight.Medium),
    Font(R.font.cairo, FontWeight.Bold)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 30.sp
    ),
    titleMedium = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 26.sp
    ),
    // Story reading text uses Amiri with generous lineHeight for Arabic diacritics (تشكيل)
    bodyLarge = TextStyle(
        fontFamily = AmiriFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 40.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp
    ),
    labelLarge = TextStyle(
        fontFamily = CairoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 22.sp
    )
)
