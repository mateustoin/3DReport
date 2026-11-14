// Plugins declarados aqui com `apply false` para que todos os módulos usem a mesma versão.
plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kover)
}

// ktlint nos três módulos: `ktlintCheck` entra no `check` (e portanto no `build`),
// `ktlintFormat` corrige. As regras estão no .editorconfig da raiz.
subprojects {
    apply(plugin = rootProject.libs.plugins.ktlint.get().pluginId)

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(rootProject.libs.versions.ktlint.get())
        // Arquivos gerados (ex.: AppVersion.kt, de generateAppVersion) ficam em build/.
        filter {
            exclude { it.file.path.contains("/build/") }
        }
    }
}

// Cobertura de testes (Kover) em core e composeApp; o web é só a fachada JS e fica de fora.
configure(listOf(project(":core"), project(":composeApp"))) {
    apply(plugin = rootProject.libs.plugins.kover.get().pluginId)
}

// Cobertura de testes (só medição, sem limite mínimo). Relatório agregado de core e composeApp:
// ./gradlew koverHtmlReport  ->  build/reports/kover/html/index.html
dependencies {
    kover(project(":core"))
    kover(project(":composeApp"))
}
