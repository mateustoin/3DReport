// Módulo `composeApp`: interface Compose Multiplatform.
// Hoje apenas desktop (JVM); a UI definitiva ainda será proposta e aprovada.
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvmToolchain(21)

    // expect/actual class: usado para a camada de persistência (data/), que
    // precisa de I/O de arquivo específico por plataforma (java.io no JVM).
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // Desktop (Windows/Linux/macOS). Código específico de desktop fica em src/jvmMain.
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.kotlinx.coroutines.core)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.pdfbox)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.threedreport.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "3DReport"
            packageVersion = "0.1.0"
        }
    }
}
