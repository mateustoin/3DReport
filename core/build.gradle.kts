// Módulo `core`: Kotlin Multiplatform puro, sem UI.
// Contém os modelos de domínio e o motor de cálculo de orçamento.
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvmToolchain(21)

    // Alvos: JVM (usado pelo app desktop) e JS (usado pela calculadora do site, módulo `:web`).
    // Os testes de commonTest rodam nos dois, o que garante que a conta no navegador é a mesma do app.
    // Para Android/iOS, basta declarar novos alvos aqui — o código em commonMain não muda.
    jvm()
    js {
        nodejs()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
