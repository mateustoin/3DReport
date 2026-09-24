# Arquitetura

## Visão geral

Projeto **Kotlin Multiplatform** com dois módulos Gradle:

```
3DReport/
├─ core/                      # KMP puro: domínio e cálculo (sem UI)
│  └─ src/
│     ├─ commonMain/kotlin/com/threedreport/core/
│     │  ├─ model/            # Filament, PrinterProfile, Service, PrintJob, PricingSettings, BrandingSettings, Quote, SavedQuote
│     │  └─ pricing/          # PricingCalculator
│     └─ commonTest/          # testes unitários (kotlin.test)
├─ composeApp/                # UI Compose Multiplatform
│  └─ src/
│     ├─ commonMain/kotlin/com/threedreport/app/
│     │  ├─ App.kt            # raiz: navegação por abas (Orçamento / Histórico / Filamentos / Impressoras / Serviços / Configurações)
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
  - `Filament`, `PrinterProfile` e `Service` têm `id: String` — são entidades
    salvas em catálogo (a UI gera o id ao criar, com `kotlin.uuid.Uuid`); os
    demais modelos continuam sendo apenas objetos de valor.
  - `Service` não entra em `PricingCalculator` — seu preço já é o valor
    cobrado do cliente (não um custo com margem em cima), então soma
    diretamente no valor de venda na camada de UI/export, não no cálculo
    interno de produção/lucro (decisão 25).
  - Canais de venda (`SalesChannel`, decisão 78) substituíram a taxa única de
    marketplace: cada canal tem a própria taxa, **descontada** da venda pelo
    marketplace (não somada ao total do cliente). `PricingSettings.marketplaceFeeRate`
    ficou como campo legado, e `Quote.marketplaceFeeRate` guarda a taxa do
    canal efetivamente aplicada (o nome antigo foi mantido pra orçamentos já
    salvos não perderem a taxa). `PricingSettings.taxRate` (imposto) é
    tratado do mesmo jeito.
  - `SavedQuote` é o retrato congelado de um orçamento salvo: além do `Quote`,
    guarda serviços, frete, cliente (uso interno), status do pedido, prazo de
    entrega, configurações de impressão e referências aos arquivos de foto e
    STL. `BrandingSettings` guarda a identidade do vendedor (nome, logo,
    contato) e a aparência dos documentos pro cliente.
- `pricing/PricingCalculator`: função pura
  `calculate(job, printer, settings, channel = null, quantity = 1, setupMinutes = 0.0, negotiatedSalePrice = null): Quote`.
  Fórmulas em [pricing-formulas.md](pricing-formulas.md). Não conhece
  serviços nem frete. Com canal e/ou imposto, o valor de venda é inflado
  (`venda = base / (1 − deduções)`) pra que o lucro real, depois das
  deduções, fique igual ao de uma venda direta (decisões 26 e 78). Com
  `negotiatedSalePrice`, o preço fechado com o cliente vira o `salePrice` e o
  de tabela fica em `Quote.tableSalePrice` (decisões 79 e 84).
- `report/`: agregações puras sobre o histórico (`QuoteReport` pro
  Dashboard, `PrintQueueReport` pra fila de cada impressora), mesmo estilo do
  `PricingCalculator`.
- `slicer/` e `stl/`: leitura dos metadados de G-code (consumo, tempo,
  miniatura, configurações de impressão) e da malha STL (parser e análise de
  complexidade), ambos sem dependência de plataforma.
- Alvo atual: `jvm()`.

### `composeApp`
- Compose Multiplatform + Material 3.
- Alvo atual: `jvm()` (desktop). Empacotamento nativo via `compose.desktop`
  (`.deb`, `.msi`, `.dmg`).
- Padrão de apresentação: **MVVM**. Cada tela tem um estado (`data class`
  imutável), um `ViewModel` (Kotlin puro, sem `Composable`, expõe
  `StateFlow`) e um `*Screen` (`@Composable` que só observa o `ViewModel` e
  envia eventos — sem lógica de cálculo).
- Sete abas em [`App.kt`](../composeApp/src/commonMain/kotlin/com/threedreport/app/App.kt),
  cada uma com ícone ao lado do nome (decisão 87; só texto em janela
  estreita) e atalho `Ctrl/Cmd+1` a `7` (decisão 43):
  - **Orçamento** (`ui/quote`): em janela larga, duas colunas (decisão 81) —
    à esquerda as entradas (filamento/cor, impressora, comprimento e tempo,
    que podem vir do G-code, trabalho, quantidade, serviços, canal de venda,
    frete) e o formulário de salvar (dividido entre "O que o cliente vê" —
    nome, foto, prazo de entrega — e "Só pra você" — STL com visualizador 3D,
    configurações de impressão, link, cliente); à direita a "nota" com o
    valor cobrado, a barra de composição do preço, a negociação e a
    comparação entre impressoras. Recalcula a cada mudança
    (`QuoteViewModel.calculate`, função pura). Editar um orçamento salvo abre
    a mesma tela num diálogo (`EditQuoteDialog`).
  - **Histórico** (`ui/history`): orçamentos salvos em lista ou Kanban por
    status (decisão 70), com busca, filtro e prazo de entrega em destaque.
    Ações por orçamento: exportar PDF, copiar texto, abrir no WhatsApp,
    imagem quadrada, editar, duplicar, prazo, configurações de impressão,
    baixar foto/STL, excluir (com `ConfirmDialog`). Tudo o que vai pro
    cliente mostra só o que é do cliente (decisão 19): produção, lucro,
    link do modelo e cliente nunca aparecem. Envio com prazo vencido pede
    confirmação antes (decisão 85). Seleção múltipla exporta vários
    orçamentos num PDF ou um catálogo em grade.
  - **Dashboard** (`ui/dashboard`): total vendido, lucro, descontos dados e
    filamento mais usado no período (`QuoteReport`).
  - **Filamentos** (`ui/filaments`), **Impressoras** (`ui/printers`) e
    **Serviços** (`ui/services`): catálogos usados no Orçamento, cada um com
    lista + formulário. Filamentos têm marca, tipo, várias cores e estoque
    manual por cor; Impressoras têm presets de fabricante e mostram a fila de
    impressão de cada máquina.
  - **Configurações** (`ui/settings`): parâmetros do negócio
    (`PricingSettings`, gravados só no "Salvar"), canais de venda, moeda,
    tema, backup/restauração, e "Documentos pro cliente" (`BrandingViewModel`:
    nome da marca, logo, contato, marca d'água, rodapé, borda, tempo de
    impressão, prévia "Ver como fica" e templates).

### Persistência
- Os repositórios de `data/` (filamentos, impressoras, serviços,
  configurações, histórico, marca, templates, canais de venda, tema, moeda,
  onboarding) guardam o estado compartilhado entre as telas (`StateFlow`) e
  persistem em disco: um arquivo JSON por assunto em `~/.3dreport/`
  (`filaments.json`, `quotes.json`, `branding.json` etc.), lido uma vez na
  criação e regravado a cada mudança. Filamentos/impressoras vêm com um
  catálogo/perfil padrão no primeiro uso; o resto começa vazio.
- `data/BackupRepository` empacota a pasta de dados inteira num `.zip` e
  restaura de forma transacional (decisão 75). Como todos os arquivos do app
  moram nessa pasta, nada novo precisa ser registrado no backup.
- Arquivos binários **não** vão dentro do JSON: a foto de um `SavedQuote`
  fica em `~/.3dreport/photos/`, o STL em `~/.3dreport/models/` e a logo do
  vendedor em `~/.3dreport/branding/`, referenciados por nome de arquivo
  (`photoFileName`, `stlFileName`, `logoFileName`). Motivo: manter o JSON pequeno e legível; o
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
  (via `java.awt.Toolkit` no `jvmMain`), `renderSavedQuotesPdf` (monta o PDF
  a partir de uma lista de `QuoteExportItem` — orçamento + foto já carregada
  —, uma página por item, na ordem dada; nome, valor de venda, foto, marca
  d'água/rodapé opcionais e independentes em cada página, via
  [Apache PDFBox](https://pdfbox.apache.org/) no `jvmMain`; Apache 2.0,
  mesma licença do projeto), `defaultDocumentsDirectory` (pasta
  "Documents"/"Documentos" do usuário, decisão 22, com fallback pra pasta
  pessoal) e `openUrl` (abre uma URL no navegador padrão do sistema, via
  `java.awt.Desktop` no `jvmMain`, decisão 29 — usado por
  `ui/components/LinkText`, um `Text` clicável e sublinhado reaproveitado
  onde houver link do modelo e no rodapé/diálogo de ajuda). Um único item
  produz o mesmo PDF de uma página do export individual — a tela de
  Histórico usa a mesma função pra exportar 1 ou vários orçamentos, só muda
  o tamanho da lista (decisão 24).
- Quando configurados, marca d'água e rodapé são desenhados **por cima de
  todo o conteúdo** (inclusive a foto — decisão 21; `PDExtendedGraphicsState`
  pra opacidade, `Matrix.getRotateInstance` pra rotação da diagonal).
- Usado pela tela de Orçamento (escolher foto ao salvar) e pela de Histórico
  (baixar foto, mostrar miniatura, formatar a data salva, exportar PDF —
  já abrindo o diálogo na pasta de Documentos —, copiar texto).

### Workaround: janela em monitores com DPI diferentes
`Main.kt` contorna um bug conhecido do Compose Desktop/Skiko (decisão 23):
arrastar a janela pra um monitor com DPI/escala diferente deixa o conteúdo
com o layout antigo até algo forçar um relayout — normalmente só volta ao
redimensionar a janela na mão (ver
[JetBrains/compose-multiplatform#3685](https://github.com/JetBrains/compose-multiplatform/issues/3685)
e relacionadas; sem correção oficial até a versão do Compose Multiplatform
usada aqui). `FixMultiMonitorDpiRedrawBug()` escuta `componentMoved` na
janela; quando a `GraphicsConfiguration` muda (trocou de monitor), simula um
redimensionamento de 1px programaticamente, que é o que já resolvia na mão.
Se isso for corrigido oficialmente numa versão futura do Compose
Multiplatform, esse workaround pode ser removido.

### Versão do app e rodapé
O app segue **SemVer** (decisão 53, que revisa a decisão 27): PATCH pra leva
só de correção/documentação, MINOR pra leva com funcionalidade nova (ver
[development.md](development.md#versionamento) pro critério completo). A
versão tem fonte única mantida manualmente em
sincronia em dois lugares (sem geração automática, pra não adicionar
complexidade de build num projeto de um mantenedor só):
- `gradle.properties` (`appVersion`) — usado como `packageVersion` do
  instalador nativo em [`composeApp/build.gradle.kts`](../composeApp/build.gradle.kts).
- [`AppVersion.kt`](../composeApp/src/commonMain/kotlin/com/threedreport/app/AppVersion.kt)
  (`APP_VERSION`) — usado em runtime, exibido no rodapé e no diálogo de ajuda.

[`App.kt`](../composeApp/src/commonMain/kotlin/com/threedreport/app/App.kt)
também define um rodapé fixo (`AppFooter`, abaixo do conteúdo de todas as
abas) com a versão, o nome do autor, links pro GitHub e pro Buy Me a Coffee
(`ui/components/LinkText`) e um botão "Ajuda" que abre um `HelpDialog` com a
versão, uma descrição curta do app e um resumo de cada aba (decisão 28).

Toda leva de funcionalidades também ganha uma entrada em
[`CHANGELOG.md`](../CHANGELOG.md) (formato Keep a Changelog, decisão 31),
no mesmo commit do bump de versão — ver
[development.md](development.md#changelog).

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
