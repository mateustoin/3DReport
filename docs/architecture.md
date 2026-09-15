# Arquitetura

## Visão geral

Projeto **Kotlin Multiplatform** com dois módulos Gradle:

```
3DReport/
├─ core/                      # KMP puro: domínio e cálculo (sem UI)
│  └─ src/
│     ├─ commonMain/kotlin/com/threedreport/core/
│     │  ├─ model/            # Filament, PrinterProfile, PrintJob, PricingSettings, BrandingSettings, Quote, SavedQuote
│     │  └─ pricing/          # PricingCalculator
│     └─ commonTest/          # testes unitários (kotlin.test)
├─ composeApp/                # UI Compose Multiplatform
│  └─ src/
│     ├─ commonMain/kotlin/com/threedreport/app/
│     │  ├─ App.kt            # raiz: navegação por abas (Orçamento / Histórico / Filamentos / Impressoras / Configurações)
│     │  ├─ data/              # contratos dos repositórios (expect class — ver "Persistência" abaixo)
│     │  ├─ platform/          # capacidades específicas de plataforma (expect fun — ver "Capacidades de plataforma")
│     │  └─ ui/                # uma pasta por tela: <tela>/<Tela>Screen.kt + <Tela>ViewModel.kt + <Tela>UiState.kt/FormState.kt
│     ├─ jvmMain/kotlin/com/threedreport/app/
│     │  ├─ data/              # persistência real (actual class): arquivos JSON em ~/.3dreport/
│     │  ├─ platform/          # actual fun: java.awt.FileDialog, Skia, java.time
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
- Cinco telas, navegadas por abas em [`App.kt`](../composeApp/src/commonMain/kotlin/com/threedreport/app/App.kt):
  - **Orçamento** (`ui/quote`): escolhe filamento + impressora (dropdowns,
    alimentados pelos catálogos salvos) e entra comprimento + tempo de
    impressão; mostra produção/venda/lucro calculados a cada mudança. Quando
    o resultado é válido, mostra também o formulário pra salvar (nome, foto,
    link do modelo — todos opcionais; ver `SaveQuoteFormState`).
  - **Histórico** (`ui/history`): lista os orçamentos salvos (`SavedQuote` —
    um retrato congelado do `Quote` no momento em que foi salvo, não afetado
    por edições posteriores em filamento/impressora/configurações), com
    ações de baixar a foto, **exportar PDF**, **copiar** (texto simplificado
    pra área de transferência) e excluir. Exportação (decisão 19) mostra só
    nome + valor de venda + foto — produção, lucro e o link do modelo nunca
    aparecem, porque é documento pro cliente.
  - **Filamentos** (`ui/filaments`) e **Impressoras** (`ui/printers`): cadastro
    (listar, adicionar, editar, excluir) dos catálogos usados no Orçamento.
    Mesmo padrão de tela nos dois: lista + formulário (`FormState`) que abre
    para adicionar/editar um item por vez.
  - **Configurações** (`ui/settings`): edita os parâmetros gerais do negócio
    (`PricingSettings` — iguais para qualquer impressora) em rascunho; só
    grava no repositório compartilhado ao clicar em "Salvar". Na mesma tela,
    uma seção separada (`BrandingViewModel`, próprio botão "Salvar") edita a
    marca d'água opcional do PDF exportado (decisão 20) — fica fora de
    `PricingSettings` por não ser parâmetro de custo.

### Persistência
- `data/FilamentRepository`, `data/PrinterRepository`, `data/SettingsRepository`,
  `data/QuoteHistoryRepository` e `data/BrandingRepository` guardam o estado
  compartilhado entre as telas (`StateFlow`) e persistem em disco: arquivos
  JSON em `~/.3dreport/` (`filaments.json`, `printers.json`, `settings.json`,
  `quotes.json`, `branding.json`), lidos uma vez na criação e regravados a
  cada mudança. Pré-carregados com um catálogo/perfil padrão no primeiro uso
  (exceto histórico e marca d'água, que começam vazios).
- A foto de um `SavedQuote`, quando existe, **não** vai dentro do JSON — fica
  como arquivo à parte em `~/.3dreport/photos/<id>.<extensão>`, referenciado
  pelo campo `photoFileName`. Motivo: manter o JSON pequeno e legível; o
  `Quote` embutido no `SavedQuote` já tem os números todos (produção, venda,
  detalhamento de custos), então o histórico não precisa recalcular nada.
- Cada repositório é um `expect class` em `commonMain` (contrato) com um
  `actual class` em `jvmMain` (implementação com `java.io.File`) — assim o
  restante da UI (`ui/`, `App.kt`) continua compartilhado, só a leitura/escrita
  de arquivo é específica da plataforma. Formato/local do arquivo em
  `data/JsonFileStore.kt` (só em `jvmMain`).
- Testado em `composeApp/src/jvmTest` (round-trip: grava, recria o
  repositório, confere que o valor voltou do disco).

### Capacidades de plataforma
- `platform/` guarda funções (não classes) que dependem do SO/plataforma,
  seguindo o mesmo padrão `expect`/`actual` da persistência: `pickImageFile`
  e `saveBytesToFile` (diálogo nativo de escolher/salvar arquivo, via
  `java.awt.FileDialog` no `jvmMain`), `decodeImageBitmap` (bytes → `ImageBitmap`
  pra exibir no Compose, via Skia no `jvmMain`), `formatDateTime`
  (formatação de data/hora, via `java.time` no `jvmMain`), `copyToClipboard`
  (via `java.awt.Toolkit` no `jvmMain`) e `renderSavedQuotePdf` (monta o PDF
  do orçamento — nome, valor de venda, foto, marca d'água opcional — via
  [Apache PDFBox](https://pdfbox.apache.org/) no `jvmMain`; Apache 2.0, mesma
  licença do projeto). Quando há marca d'água configurada, ela é desenhada
  **por cima de todo o conteúdo** (inclusive a foto — decisão 21;
  `PDExtendedGraphicsState` pra opacidade, `Matrix.getRotateInstance` pra
  rotação) e o PDF ganha um rodapé (linha fina + nome da marca centralizado).
- Usado pela tela de Orçamento (escolher foto ao salvar) e pela de Histórico
  (baixar foto, mostrar miniatura, formatar a data salva, exportar PDF,
  copiar texto).

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
   ViewModels, `App.kt`) deve ser necessária — exceto tudo que é `expect` em
   `data/` e `platform/`, que precisa de um `actual` para Android: os
   repositórios (ex.: `DataStore` ou arquivo em `Context.filesDir`, já que
   `java.io.File` com `user.home` do `jvmMain` não existe nesse alvo) e as
   capacidades de plataforma (`pickImageFile`/`saveBytesToFile` via intents
   do Android em vez de `FileDialog`, `decodeImageBitmap` via `BitmapFactory`
   em vez de Skia).

Esse passo não foi feito agora para manter o build simples e independente do
Android SDK enquanto o foco é desktop.

## Modelo: gratuito e de código aberto

Não há edição paga nem separação de código por licença (decisão 15 em
[decisions.md](decisions.md)): todas as funcionalidades, atuais e futuras,
fazem parte do mesmo software gratuito, sob [Apache 2.0](../LICENSE). Não há
verificação de licença, flag de build "pro" ou módulo pago em lugar nenhum do
projeto — se algo assim aparecer no futuro, é sinal de que essa decisão foi
revista (e deve estar documentada aqui). Sustentação financeira do projeto é
via doação voluntária (Buy Me a Coffee, ver README), não via venda de
funcionalidades.

## Convenções

Revisadas e ratificadas na decisão 16 de [decisions.md](decisions.md).

- Pacote base: `com.threedreport`.
- Identificadores em inglês; KDoc e documentação em português.
- Percentuais como fração decimal (`0.10` = 10%).
- Valores monetários em `Double`, arredondados só na exibição.
- Versões centralizadas em `gradle/libs.versions.toml`.
