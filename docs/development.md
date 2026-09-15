# Guia de desenvolvimento

## Pré-requisitos

| Ferramenta | Versão | Observação |
|---|---|---|
| JDK | 21 | Necessário. Verifique com `java -version` |
| Gradle | — | Não instale: use o wrapper `./gradlew` (baixa o Gradle 9.7.1) |
| Android SDK | — | **Não** é necessário hoje (só desktop) |

Versões das bibliotecas: [gradle/libs.versions.toml](../gradle/libs.versions.toml)
(Kotlin 2.4.20, Compose Multiplatform 1.12.0, Material 3 1.9.0).

## IDE recomendada

**Android Studio** ou **IntelliJ IDEA** (com o plugin Kotlin Multiplatform).
Elas trazem autocompletar, navegação, depuração, preview de Compose e execução de
testes pela interface para KMP. Basta abrir a pasta raiz do projeto; o Gradle
sincroniza sozinho.

**VS Code** funciona para editar e rodar tudo pelo terminal (comandos abaixo), mas
o suporte a Kotlin/KMP no VS Code é limitado (análise de código e depuração
inferiores). Use-o se preferir, mas a experiência será melhor no Android Studio.

## Comandos

Todos na raiz do projeto (no Windows use `gradlew.bat`).

| Objetivo | Comando |
|---|---|
| Executar o app desktop | `./gradlew :composeApp:run` |
| Rodar todos os testes | `./gradlew allTests` |
| Rodar só testes do core | `./gradlew :core:jvmTest` |
| Rodar só testes do composeApp (persistência) | `./gradlew :composeApp:jvmTest` |
| Compilar e testar tudo | `./gradlew build` |
| Gerar instalador do SO atual | `./gradlew :composeApp:packageDistributionForCurrentOS` |
| Limpar build | `./gradlew clean` |

- Relatório de testes: `core/build/reports/tests/jvmTest/index.html`.
- Instaladores gerados: `composeApp/build/compose/binaries/`.
- Dados do app (filamentos, impressoras, configurações): `~/.3dreport/*.json`.
  Apague a pasta para resetar para os valores padrão.

### Rodando pela IDE
- Android Studio/IntelliJ: abra `Main.kt` e clique no ▶ ao lado de `fun main()`,
  ou use o painel Gradle → `composeApp > Tasks > compose desktop > run`.
- Testes: ▶ ao lado da classe de teste.

## Testes

- Framework: `kotlin.test` (multiplataforma), em `src/commonTest`.
- O motor de cálculo é validado contra os valores da planilha de referência
  ([pricing-formulas.md](pricing-formulas.md)). Toda mudança de fórmula deve
  atualizar teste **e** documentação.

## Fluxo de mudanças

1. Decisões de UI/UX, arquitetura, organização e funcionalidades são propostas e
   aprovadas antes da implementação e registradas em [decisions.md](decisions.md).
2. Código e documentação mudam no mesmo commit; o `README.md` é mantido atualizado.
3. `./gradlew build` deve passar antes de commitar.

## Versionamento

O app segue **SemVer** (decisão 27 em [decisions.md](decisions.md)), com bump
de **MINOR a cada leva de funcionalidades entregue** (`PATCH` fica pra
correções isoladas fora de uma leva; `MAJOR` fica pra quebras de
compatibilidade, sem caso de uso previsto ainda). Ao fechar uma leva, atualize
a versão **nos dois lugares** (fonte única mantida manualmente em sincronia,
sem geração automática):

- `gradle.properties` → `appVersion`
- [`composeApp/.../app/AppVersion.kt`](../composeApp/src/commonMain/kotlin/com/threedreport/app/AppVersion.kt) → `APP_VERSION`

A versão aparece no rodapé do app e no instalador nativo (`packageVersion`).
