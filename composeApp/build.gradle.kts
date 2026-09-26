// Módulo `composeApp`: interface Compose Multiplatform.
// Hoje apenas desktop (JVM); a UI definitiva ainda será proposta e aprovada.
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    // Os repositórios (data/) montam o formato dos arquivos em código comum (registro com metadados,
    // documento), então o módulo gera serializadores próprios.
    alias(libs.plugins.kotlinSerialization)
}

// A versão do app sai de `appVersion` em gradle.properties, a mesma do instalador: antes eram duas
// fontes mantidas à mão (decisão 108).
val generateAppVersion by tasks.registering {
    val version = providers.gradleProperty("appVersion")
    val outputDir = layout.buildDirectory.dir("generated/appVersion/commonMain/kotlin")
    inputs.property("appVersion", version)
    outputs.dir(outputDir)
    doLast {
        val file = outputDir.get().file("com/threedreport/app/AppVersion.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.threedreport.app
            |
            |/** Versão do app, exibida no rodapé e na Ajuda. Gerada a partir de `appVersion` em gradle.properties. */
            |const val APP_VERSION = "${version.get()}"
            |
            """.trimMargin(),
        )
    }
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
        commonMain {
            kotlin.srcDir(generateAppVersion)
        }
        commonMain.dependencies {
            implementation(project(":core"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.pdfbox)
        }
        jvmTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.threedreport.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Deb, TargetFormat.Msi, TargetFormat.Dmg)
            packageName = "3DReport"
            vendor = "Mateus Antonio da Silva"
            // Lido de gradle.properties, a mesma fonte do APP_VERSION do app (generateAppVersion).
            packageVersion = providers.gradleProperty("appVersion").get()

            // O instalador leva um Java reduzido pelo jlink, só com os módulos padrão do Compose mais os
            // listados aqui (decisão 119). Na IDE o app roda no JDK completo, então um `java.*` novo no
            // código que fique de fora só quebra no app instalado: a 2.1.0 não abria por faltar o
            // `java.net.http` (decisão 116). O CI confere esta lista com o jdeps.
            modules("java.net.http")

            // Ícone do app (cubo isométrico "em camadas" + bico de impressão,
            // nas cores da paleta do app) — decisão 42. Cada plataforma exige
            // o formato nativo do seu instalador (.ico/.icns/.png).
            windows {
                iconFile.set(project.file("packaging/icons/icon.ico"))
                // Cria atalho na área de trabalho ao instalar (o jpackage/WiX
                // usado aqui não oferece uma caixinha de escolha no instalador
                // — só a opção de criar sempre ou nunca; ver decisão 42).
                shortcut = true
                menu = true
                // Fixo (decisão 116): é o que o jpackage derivava do fabricante e do nome, lido da tabela
                // Property dos .msi publicados da 1.8.0 e da 1.37.0 (iguais nos dois). Com ele fixo, trocar o
                // nome do fabricante ou do pacote não faz o instalador novo deixar de atualizar o antigo.
                upgradeUuid = "8681a46c-4db6-3cbf-b285-250929ce61c5"
            }
            macOS {
                iconFile.set(project.file("packaging/icons/icon.icns"))
            }

            // O bundler .deb do jpackage exige nome de pacote começando com
            // letra minúscula — "3DReport" (packageName acima) é rejeitado.
            linux {
                packageName = "threedreport"
                iconFile.set(project.file("packaging/icons/icon.png"))
                shortcut = true
            }
        }
    }
}
