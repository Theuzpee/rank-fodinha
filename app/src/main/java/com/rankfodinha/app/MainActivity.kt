package com.rankfodinha.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rankfodinha.app.ui.MainScreen
import com.rankfodinha.app.ui.theme.RankFodinhaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // O tema é sempre claro. As barras do sistema precisam de ícones escuros,
        // mesmo quando o aparelho está no modo escuro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            RankFodinhaTheme {
                MainScreen()
            }
        }
    }
}
