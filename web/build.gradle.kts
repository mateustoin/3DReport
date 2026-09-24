// Módulo `web`: a calculadora do site (site/calculadora.html). Só uma fachada fina sobre o
// `PricingCalculator` do `core`, compilada pra JavaScript: a conta é a mesma do app, sem
// segunda fonte de verdade (decisão 98).
plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    js {
        outputModuleName.set("calculadora")
        browser {
            // Os testes rodam no Node (abaixo), sem precisar de um navegador instalado no CI.
            testTask { enabled = false }
        }
        nodejs()
        binaries.executable()
    }

    sourceSets {
        jsMain.dependencies {
            implementation(project(":core"))
        }
        jsTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

// Copia o JS gerado pra pasta do site. A pasta fica fora do git; o workflow do GitHub Pages
// roda esta task antes de publicar.
tasks.register<Sync>("syncSiteCalculator") {
    from(tasks.named("jsBrowserDistribution"))
    into(rootProject.layout.projectDirectory.dir("site/assets/calc"))
}
