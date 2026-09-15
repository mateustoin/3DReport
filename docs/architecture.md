# Arquitetura

## Visão geral

Projeto **Kotlin Multiplatform** com dois módulos Gradle:

```
3DReport/
├─ core/                      # KMP puro: domínio e cálculo (sem UI)
│  └─ src/
│     ├─ commonMain/kotlin/com/threedreport/core/
│     │  ├─ model/            # Filament, PrinterProfile, PrintJob, PricingSettings, Quote
│     │  └─ pricing/          # PricingCalculator
│     └─ commonTest/          # testes unitários (kotlin.test)
├─ composeApp/                # UI Compose Multiplatform
│  └─ src/
│     ├─ commonMain/kotlin/com/threedreport/app/
│     │  ├─ App.kt            # raiz: navegação por abas (Orçamento / Filamentos / Impressoras / Configurações)
│     │  ├─ data/              # contratos dos repositórios (expect class — ver "Persistência" abaixo)
│     │  └─ ui/                # uma pasta por tela: <tela>/<Tela>Screen.kt + <Tela>ViewModel.kt + <Tela>UiState.kt/FormState.kt
│     ├─ jvmMain/kotlin/com/threedreport/app/
│     │  ├─ data/              # persistência real (actual class): arquivos JSON em ~/.3dreport/
│     │  └─ Main.kt            # entrada do desktop (janela)
│     └─ jvmTest/              # testes da persistência (kotlin.test)
├─ docs/                      # documentação
├─ gradle/libs.versions.toml  # catálogo de versões
├─ LICENSE
└─ README.md
```

Dependência: `composeApp → core`. O `core` nunca depende da UI.

## Módulos

### `core`
- Kotlin puro, todo o código em `commonMain` — roda em qualquer alvo. Única
  dependência externa: `kotlinx.serialization` (anotação `@Serializable` nos
  modelos — usada pela persistência do `composeApp`, não pelo `core` em si).
- `model/`: classes imutáveis (`data class`) que validam suas entradas no `init`
  (`IllegalArgumentException` para valores negativos/zero inválidos).
  - `Filament` e `PrinterProfile` têm `id: String` — são entidades salvas em
    catálogo (a UI gera o id ao criar, com `kotlin.uuid.Uuid`); os demais
    modelos continuam sendo apenas objetos de valor.
- `pricing/PricingCalculator`: função pura `calculate(job, printer, settings): Quote`.
  Fórmulas em [pricing-formulas.md](pricing-formulas.md).
- Alvo atual: `jvm()`.

### `composeApp`
- Compose Multiplatform + Material 3.
- Alvo atual: `jvm()` (desktop). Empacotamento nativo via `compose.desktop`
  (`.deb`, `.msi`, `.dmg`).
- Padrão de apresentação: **MVVM**. Cada tela tem um estado (`data class`
  imutável), um `ViewModel` (Kotlin puro, sem `Composable`, expõe
  `StateFlow`) e um `*Screen` (`@Composable` que só observa o `ViewModel` e
  envia eventos — sem lógica de cálculo).
- Quatro telas, navegadas por abas em [`App.kt`](../composeApp/src/commonMain/kotlin/com/threedreport/app/App.kt):
  - **Orçamento** (`ui/quote`): escolhe filamento + impressora (dropdowns,
    alimentados pelos catálogos salvos) e entra comprimento + tempo de
    impressão; mostra produção/venda/lucro calculados a cada mudança.
  - **Filamentos** (`ui/filaments`) e **Impressoras** (`ui/printers`): cadastro
    (listar, adicionar, editar, excluir) dos catálogos usados no Orçamento.
    Mesmo padrão de tela nos dois: lista + formulário (`FormState`) que abre
    para adicionar/editar um item por vez.
  - **Configurações** (`ui/settings`): edita os parâmetros gerais do negócio
    (`PricingSettings` — iguais para qualquer impressora) em rascunho; só
    grava no repositório compartilhado ao clicar em "Salvar".

### Persistência
- `data/FilamentRepository`, `data/PrinterRepository` e `data/SettingsRepository`
  guardam o estado compartilhado entre as telas (`StateFlow`) e persistem em
  disco: arquivos JSON em `~/.3dreport/` (`filaments.json`, `printers.json`,
  `settings.json`), lidos uma vez na criação e regravados a cada mudança.
  Pré-carregados com um catálogo/perfil padrão no primeiro uso.
- Cada repositório é um `expect class` em `commonMain` (contrato) com um
  `actual class` em `jvmMain` (implementação com `java.io.File`) — assim o
  restante da UI (`ui/`, `App.kt`) continua compartilhado, só a leitura/escrita
  de arquivo é específica da plataforma. Formato/local do arquivo em
  `data/JsonFileStore.kt` (só em `jvmMain`).
- Testado em `composeApp/src/jvmTest` (round-trip: grava, recria o
  repositório, confere que o valor voltou do disco).

## Plataformas

| Plataforma | Estado |
|---|---|
| Desktop (JVM) | Ativo |
| Android | Planejado. Estrutura compatível; nada configurado ainda |
| iOS / Web | Não planejado por enquanto |

### Como adicionar Android no futuro
1. Adicionar o plugin Android (AGP) ao catálogo e ao `build.gradle.kts` raiz.
2. No `core`, declarar o alvo Android (plugin `com.android.kotlin.multiplatform.library`).
3. Criar um módulo de aplicação Android (ex.: `androidApp`) que dependa de
   `composeApp`/`core` e contenha a `Activity` chamando `App()`.
4. Nenhuma mudança em `core/commonMain` nem em `composeApp/commonMain` (telas,
   ViewModels, `App.kt`) deve ser necessária — exceto os repositórios em
   `data/`: cada `expect class` precisa de um `actual` para Android (ex.:
   `DataStore` ou arquivo em `Context.filesDir`, já que `java.io.File` com
   `user.home` do `jvmMain` não existe nesse alvo).

Esse passo não foi feito agora para manter o build simples e independente do
Android SDK enquanto o foco é desktop.

## Edição gratuita x paga

Ainda não há separação de código. A ideia é que o cálculo básico de orçamento
(atual `core`) componha a edição gratuita, e recursos pagos entrem em módulos
próprios. O mecanismo (flavors, módulos, licença em runtime) será proposto
quando houver o primeiro recurso pago.

## Convenções

- Pacote base: `com.threedreport`.
- Identificadores em inglês; KDoc e documentação em português.
- Percentuais como fração decimal (`0.10` = 10%).
- Valores monetários em `Double`, arredondados só na exibição.
- Versões centralizadas em `gradle/libs.versions.toml`.
