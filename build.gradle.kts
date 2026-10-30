// Arquivo de build de nível superior: declara os plugins usados pelo módulo app.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.jetbrains.kotlin.plugin.serialization) apply false
}
