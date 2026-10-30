package com.rankfodinha.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rankfodinha.app.R

/**
 * Figtree é uma fonte variável: um único arquivo, com um ajuste de peso para cada peso usado.
 * Assim o app funciona offline, sem depender de downloads de fontes.
 */
val FigtreeFamily = FontFamily(
    Font(R.font.figtree_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.figtree_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.figtree_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.figtree_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))),
    Font(R.font.figtree_variable, FontWeight.ExtraBold, variationSettings = FontVariation.Settings(FontVariation.weight(800))),
)

private val Base = Typography()

/** Textos maiores que o padrão do Material 3, em sp, então também acompanham a escala de fonte do aparelho. */
private fun TextStyle.figtree(size: Int, line: Int, weight: FontWeight): TextStyle =
    copy(
        fontFamily = FigtreeFamily,
        fontSize = size.sp,
        lineHeight = line.sp,
        fontWeight = weight,
    )

val AppTypography = Typography(
    displayLarge = Base.displayLarge.figtree(72, 80, FontWeight.ExtraBold),
    displayMedium = Base.displayMedium.figtree(60, 68, FontWeight.ExtraBold),
    displaySmall = Base.displaySmall.figtree(52, 60, FontWeight.ExtraBold),
    headlineLarge = Base.headlineLarge.figtree(44, 52, FontWeight.ExtraBold),
    headlineMedium = Base.headlineMedium.figtree(38, 46, FontWeight.ExtraBold),
    headlineSmall = Base.headlineSmall.figtree(32, 40, FontWeight.ExtraBold),
    titleLarge = Base.titleLarge.figtree(28, 36, FontWeight.Bold),
    titleMedium = Base.titleMedium.figtree(24, 32, FontWeight.Bold),
    titleSmall = Base.titleSmall.figtree(20, 28, FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.figtree(22, 30, FontWeight.Normal),
    bodyMedium = Base.bodyMedium.figtree(20, 28, FontWeight.Normal),
    bodySmall = Base.bodySmall.figtree(18, 26, FontWeight.Normal),
    labelLarge = Base.labelLarge.figtree(20, 26, FontWeight.Bold),
    labelMedium = Base.labelMedium.figtree(18, 24, FontWeight.SemiBold),
    labelSmall = Base.labelSmall.figtree(16, 22, FontWeight.SemiBold),
)
