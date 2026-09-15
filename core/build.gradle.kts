// Módulo `core`: Kotlin Multiplatform puro, sem UI.
// Contém os modelos de domínio e o motor de cálculo de orçamento.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    jvmToolchain(21)

    // Alvo atual: JVM (usado pelo app desktop).
    // Para Android/iOS/Web, basta declarar novos alvos aqui — o código em commonMain não muda.
    jvm()

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
