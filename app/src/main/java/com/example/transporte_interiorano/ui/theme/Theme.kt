package com.example.transporte_interiorano.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 🎨 1. A REGRA DO 60-30-10 APLICADA
// 60% Fundo (Cinza/Branco), 30% Principal (Azul), 10% Destaque (Verde)
private val NossoEsquemaCores = lightColorScheme(
    primary = AzulPrincipal,
    secondary = VerdeBotao,
    background = FundoCinza, // Fundo neutro para descanso visual
    error = VermelhoErro
)

// 📝 2. A MICROTIPOGRAFIA E O LINE-HEIGHT (Baseado no PDF)
val TipografiaUX = Typography(
    // Textos comuns (Body): Fonte Sans Serif padrão, 16sp
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        // Regra de Ouro do PDF: Line-height de 1.5 (150%) -> 16 * 1.5 = 24sp!
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    // Títulos (H1/H2): Mais grossos (Bold) e com leve aperto no espaçamento
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp // Leve aperto em títulos grandes
    ),
    // Botões e Rótulos: Destacados e legíveis
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    )
)

// A ETIQUETA CORRETA QUE O MAINACTIVITY ESTÁ PROCURANDO! 👇
@Composable
fun transporte_interioranoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = NossoEsquemaCores,
        typography = TipografiaUX, // <-- INJETAMOS A NOSSA TIPOGRAFIA AQUI!
        content = content
    )
}