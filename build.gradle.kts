// AGP 9 ma wbudowana obsluge Kotlina - plugin org.jetbrains.kotlin.android
// jest zbedny i niekompatybilny z nowym DSL. Zostaje tylko plugin Compose.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
