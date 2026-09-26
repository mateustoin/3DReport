# Arquitetura

## Visão geral

Projeto **Kotlin Multiplatform** com três módulos Gradle:

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
│     │  ├─ App.kt            # raiz: navegação pela barra lateral (Orçamento, Pedidos, Catálogo, Dashboard, Filamentos, Impressoras, Serviços, Configurações, Sobre)
│     │  ├─ data/              # contratos dos repositórios (expect class — ver "Persistência" abaixo)
│     │  ├─ platform/          # capacidades específicas de plataforma (expect fun — ver "Capacidades de plataforma")
│     │  └─ ui/                # uma pasta por tela: <tela>/<Tela>Screen.kt + <Tela>ViewModel.kt + <Tela>UiState.kt/FormState.kt
│     ├─ jvmMain/kotlin/com/threedreport/app/
│     │  ├─ data/              # persistência real (actual class): arquivos JSON em ~/.3dreport/
│     │  ├─ platform/          # actual fun: java.awt.FileDialog, Skia, java.time
│     │  └─ Main.kt            # entrada do desktop (janela)
│     └─ jvmTest/              # testes da persistência (kotlin.test)
├─ web/                       # calculadora do site: fachada JS sobre o core (decisão 98)
│  └─ src/jsMain/kotlin/com/threedreport/web/WebCalculator.kt
├─ site/                      # site estático (GitHub Pages); site/assets/calc/ é gerado pelo :web
├─ docs/                      # documentação
├─ gradle/libs.versions.toml  # catálogo de versões
├─ LICENSE
└─ README.md
```

Dependências: `composeApp → core` e `web → core`. O `core` nunca depende da UI.

## Módulos

### `core`
- Kotlin puro, todo o código em `commonMain`. Alvos declarados: **JVM** (app
  desktop) e **JS** (calculadora do site); os testes de `commonTest` rodam nos
  dois (`./gradlew :core:allTests`). Por isso nada de API só da JVM no
  `commonMain`: nem `java.*`, nem `putIfAbsent`, nem flags de regex embutidas
  como `(?i)` (o JavaScript não aceita; use `RegexOption`). Única
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
    marketplace (não somada ao total do cliente). `Quote.channelFeeRate` guarda
    a taxa do canal efetivamente aplicada, e `Quote.channelId`/`channelName`
    qual canal foi. `PricingSettings.taxRate` (imposto) é tratado do mesmo
    jeito.
  - Um orçamento é uma lista de impressões (decisão 105): `PrintJob` é uma
    mesa que a impressora roda (vários `FilamentUsage` numa peça multicolor,
    tempo, `runs`), e `QuotedPrint` é o retrato dela dentro do `Quote`
    (impressora usada e `PrintCost`). O que é do pedido (trabalho, quantidade,
    canal, negociação) fica no `Quote`, uma vez só.
  - `SavedQuote` é o retrato congelado de um orçamento salvo: além do `Quote`,
    guarda serviços, frete, cliente (uso interno), status do pedido, prazo de
    entrega, configurações de impressão e referências aos arquivos de foto e
    STL. `SavedQuote.kind` (`QuoteKind`) separa **pedido** de **produto do
    catálogo** (decisão 101); `SavedQuote.isOrder` é a única regra de "o que é
    pedido", e os relatórios de `report/` deixam os produtos de fora por ela.
    `BrandingSettings` guarda a identidade do vendedor (nome, logo,
    contato) e a aparência dos documentos pro cliente.
- `pricing/ProductRepricer`: recalcula um produto do catálogo com os cadastros de
  hoje (decisão 102), mantendo o preço anunciado; devolve o motivo quando falta
  filamento, impressora ou canal.
- `pricing/PricingCalculator`: função pura
  `calculate(prints: List<Pair<PrintJob, PrinterProfile>>, settings, channel = null, quantity = 1, laborMinutes = 0.0, negotiatedSalePrice = null): Quote`,
  com o atalho `calculate(job, printer, …)` pra uma impressão só. Cada
  impressão é calculada com a própria impressora, e o que é do pedido entra
  depois, uma vez só.
  Fórmulas em [pricing-formulas.md](pricing-formulas.md). Não conhece
  serviços nem frete. Com canal e/ou imposto, o valor de venda é inflado
  (`venda = base / (1 − deduções)`) pra que o lucro real, depois das
  deduções, fique igual ao de uma venda direta (decisões 26 e 78). Com
  `negotiatedSalePrice`, o preço fechado com o cliente vira o `salePrice` e o
  de tabela fica em `Quote.tableSalePrice` (decisões 79 e 84).
- `report/`: agregações puras sobre o histórico (`CatalogReport` pro resumo do
  catálogo no Dashboard de quem ainda não vende, decisão 103; `QuoteReport` pro
  Dashboard, `PrintQueueReport` pra fila de cada impressora,
  `MaintenanceReport` pras horas de uso e a situação de cada componente de
  manutenção), mesmo estilo do `PricingCalculator`.
- `slicer/` e `stl/`: leitura dos metadados de G-code (consumo total e por
  extrusor, tempo, miniatura, configurações de impressão, impressora e
  filamentos, casados com os cadastros por `CatalogMatcher`) e da malha STL
  (parser e análise de complexidade), ambos sem dependência de plataforma.
- Alvo atual: `jvm()`.

### `web`
- Calculadora do site (`site/calculadora.html`, decisão 98). Kotlin/JS
  (`js { browser() }`), gera um bundle UMD de ~75 KB que expõe o objeto global
  `web` com `calculateQuote(...)`.
- `WebCalculator.kt` é só uma **fachada**: recebe números primitivos (peso em
  gramas, percentuais de 0 a 100), monta `Filament`/`PrintJob`/`PrinterProfile`/
  `PricingSettings`/`SalesChannel` e chama `PricingCalculator.calculate`. Não
  faz conta própria; a página (`site/assets/calculadora.js`) também não. Assim
  existe uma fórmula só pro app e pro site.
- `./gradlew :web:syncSiteCalculator` copia o bundle pra `site/assets/calc/`
  (fora do git). O workflow `pages.yml` roda isso antes de publicar.

### `composeApp`
- Compose Multiplatform + Material 3.
- Alvo atual: `jvm()` (desktop). Empacotamento nativo via `compose.desktop`
  (`.deb`, `.msi`, `.dmg`). O `.msi` do Windows tem `upgradeUuid` fixo em
  [`composeApp/build.gradle.kts`](../composeApp/build.gradle.kts) desde a
  decisão 116 (`8681a46c-4db6-3cbf-b285-250929ce61c5`, o mesmo valor que o
  jpackage já derivava de vendor+nome nas versões 1.x), pra instalar por
  cima em vez de duplicar ao atualizar.
- Ao sair (fechar a janela ou depois de restaurar um backup), com tudo já
  gravado, o `ExitWatchdog` força a saída se a JVM ainda estiver viva 10 s
  depois, e grava as pilhas das threads em `logs/encerramento-travado.txt`
  (decisão 117).
- Padrão de apresentação: **MVVM**. Cada tela tem um estado (`data class`
  imutável), um `ViewModel` (Kotlin puro, sem `Composable`, expõe
  `StateFlow`) e um `*Screen` (`@Composable` que só observa o `ViewModel` e
  envia eventos — sem lógica de cálculo).
- **Barra lateral agrupada** (decisão 111, `ui/navigation`): `AppDestination`
  lista as nove telas em três grupos (`DestinationGroup`): **Vendas**
  (Orçamento, Pedidos, Catálogo, Dashboard), **Cadastros** (Filamentos,
  Impressoras, Serviços) e, no pé da barra, Configurações e Sobre, com a
  versão embaixo. `AppSidebar` desenha isso; abaixo de
  `SIDEBAR_LABELS_MIN_WINDOW_WIDTH` (1280dp) ela vira um trilho só de ícones
  com tooltip (o Orçamento perde as duas colunas antes disso). Atalho
  `Ctrl/Cmd+1` a `8` na ordem da barra (`AppDestination.shortcutNumber`;
  Sobre não tem atalho). `Main.kt` guarda posição, tamanho e se a janela
  estava maximizada em `AppPreferences.window` (`SavedWindowBounds`, em
  `preferences.json`) e só reaplica se o retângulo ainda cabe nalgum monitor
  conectado (`isOnSomeScreen`). Cancelar Vender/Duplicar/Guardar no catálogo
  no Orçamento devolve a pessoa pra tela de onde a operação começou
  (`QuoteOperation.originKind`).
  - **Orçamento** (`ui/quote`): em janela larga, duas colunas (decisão 81) —
    à esquerda as entradas (filamento/cor, impressora, comprimento e tempo,
    que podem vir do G-code, trabalho, quantidade, serviços, canal de venda,
    frete) e o formulário de salvar (dividido entre "O que o cliente vê" —
    nome, foto, prazo de entrega — e "Só pra você" — STL com visualizador 3D,
    configurações de impressão, link, cliente); à direita a "nota" com o
    valor cobrado, a barra de composição do preço, a negociação e a
    comparação entre impressoras. Recalcula a cada mudança
    (`QuoteViewModel.calculate`, função pura).
    - **Várias impressões por pedido** (decisão 114): com uma impressão só a
      tela é como antes, mais um botão "+ Adicionar outra impressão". Com
      duas ou mais, cada impressão vira um cartão recolhível
      (`ui/quote/PrintCard.kt`) com nome opcional, impressora, filamentos,
      tempo, "× N vezes", o G-code, miniatura e configurações da própria
      impressão, duplicar e remover (com "Desfazer"); recolhido mostra um
      resumo numa linha ("Corpo · K1 · 9h40 · 180 g · custo R$ 32,10"). O que
      é do pedido (trabalho, serviços, frete, canal, negociação, prazo) fica
      fora dos cartões. `GCodeImporter.apply` devolve um `PrintInput` por
      arquivo; soltar vários G-codes de uma vez cria uma impressão por
      arquivo, e soltar um sobre um pedido já preenchido oferece "Substituir
      a impressão N" ou "Adicionar como nova impressão" conforme a posição
      do soltar. O resultado mostra "N impressões · X de máquina" e um "Por
      impressão" recolhido com o custo de cada uma mais uma linha "Do pedido
      (trabalho, falhas, administrativo)"; "Comparar impressoras" (decisão
      79) virou "Tudo na `<impressora>`". Cada impressão grava as próprias
      configurações e miniatura (`PrintJob.settings`/`thumbnailFileName`, via
      `QuoteHistoryRepository.save`/`update(printThumbnails = …)`), que
      voltam ao reabrir, duplicar, vender ou copiar pro catálogo; mudar só o
      nome ou as configurações de uma impressão reaberta não reprecifica.
      Toda operação (duplicar, remover, importar G-code, desfazer) endereça
      impressões pelo id estável da linha, nunca por posição.
    - **Editar na própria aba** (decisão 113): reabrir um pedido ou produto
      não abre mais diálogo (`EditQuoteDialog` foi removido): enquanto
      `editQuoteViewModel` (uma segunda instância de `QuoteViewModel`, criada no
      `App.kt`) está em `QuoteOperation.Editing`, a tela de Orçamento mostra ele
      no lugar do `quoteViewModel` da aba, que continua com o rascunho intacto
      e volta sozinho ao salvar ou cancelar. `originalInput`/`originalForm`
      são como o pedido editado abriu, pra saber se há alteração a perder. A barra de operação no rodapé mostra
      `Editando #0042 "nome"` com "Cancelar edição" (pede confirmação se algo
      mudou); a barra lateral mostra "Editando" ao lado de Orçamento.
      `Ctrl+S` e um G-code solto na tela vão pra edição em andamento;
      Duplicar, Vender, Guardar no catálogo e `Ctrl+N` perguntam antes de
      encerrar uma edição com alterações. Enquanto nada que muda o preço é
      mexido, salvar mantém o cálculo congelado do orçamento original.
  - **Pedidos** (`ui/history`, chamada de "Histórico" até a decisão 111):
    pedidos salvos em lista ou Kanban por status (decisão 70), com busca,
    filtro e prazo de entrega em destaque; o cartão mostra "N impressões"
    quando há mais de uma. Ações por pedido: exportar PDF, copiar texto,
    abrir no WhatsApp, imagem quadrada, editar (abre na aba Orçamento),
    editar só os detalhes (nome, cliente, foto, prazo, link), duplicar, mover
    de etapa, configurações de impressão, baixar foto/STL, excluir (com
    "Desfazer"). Lista e Kanban usam o mesmo menu "Ações"
    (`QuoteActionsMenu`), e as ações terminam num aviso (`UserNotice`) com
    "Abrir pasta" ou "Desfazer" quando cabe. Tudo o que vai pro cliente
    mostra só o que é do cliente (decisão 19): produção, lucro, link do
    modelo e cliente nunca aparecem. Envio com prazo vencido pede confirmação
    antes (decisão 85). Seleção múltipla exporta vários orçamentos num PDF ou
    um catálogo em grade.
  - **Catálogo** (`ui/history`, decisão 111): tela própria (deixou de ser um
    seletor `Pedidos | Produtos` dentro do Histórico), a mesma
    `QuoteHistoryScreen`/`QuoteHistoryViewModel` de Pedidos, mas com
    `kind = QuoteKind.PRODUCT` fixo, então cada tela tem o próprio
    ViewModel.
  - **Dashboard** (`ui/dashboard`): só vendas (`OrderStatus.isSold`, decisão
    95): total vendido, lucro, descontos dados, filamento mais usado, lucro
    por hora de máquina e de trabalho, ranking de peças e de clientes com
    desconto; orçamentos em aberto numa linha à parte (`QuoteReport`).
  - **Filamentos** (`ui/filaments`), **Impressoras** (`ui/printers`) e
    **Serviços** (`ui/services`): catálogos usados no Orçamento, cada um com
    lista + formulário. Filamentos têm marca, tipo, várias cores e estoque
    manual por cor; Impressoras têm presets de fabricante, mostram a fila de
    impressão de cada máquina e abrem a manutenção dela (`MaintenanceDialog`:
    componentes com intervalo, diário, horas avulsas; `PrinterMaintenance`
    em `maintenance.json`, decisão 96). Em Serviços, o valor é só uma sugestão
    opcional e cada um tem um padrão "por peça / uma vez no pedido": o que
    vale é o digitado no orçamento (`ServiceInput`), congelado em
    `QuoteService` (decisão 92).
    - **Arquivar** (decisão 115): `Filament`, `PrinterProfile`, `Service` e
      `SalesChannel` ganharam `archived: Boolean = false` (campo aditivo, sem
      migração). Cada tela de cadastro tem "Arquivar" e uma seção recolhida
      "Arquivados (n)" com "Restaurar". Um item arquivado some das escolhas
      de um orçamento novo (padrões, casamento automático do G-code,
      "Comparar impressoras"), mas pedidos/produtos que já usavam ele
      continuam calculando, marcados "(arquivado)". Excluir um item em uso
      mostra em quantos pedidos/produtos ele aparece (`CatalogUsage`) e
      sugere Arquivar como botão principal ("Excluir mesmo assim" continua
      possível). Arquivar uma impressora mantém os dados de manutenção dela.
  - **Configurações** (`ui/settings`, decisão 112): uma lista de seções à
    esquerda (`SettingsSection`: Negócio e custos, que inclui Moeda,
    Canais, Documentos pro cliente, Aparência, Dados) mostra uma seção por
    vez, com ~720dp de largura de leitura. Custos (`PricingSettings`) e
    "Documentos pro cliente" (`BrandingViewModel`: nome da marca, logo,
    contato, marca d'água, rodapé, borda, tempo de impressão, prévia "Ver
    como fica" e templates) são rascunho; no lugar dos dois botões "Salvar"
    separados, uma barra fixa embaixo ("Alterações não salvas" com
    "Descartar"/"Salvar") salva ou descarta os dois rascunhos juntos, e um
    erro pula pra seção dele. O rascunho sobrevive a trocar de seção ou de
    tela; a barra lateral mostra um pontinho em Configurações enquanto há
    rascunho. Tema, moeda, canais e backup continuam aplicando na hora.
  - **Sobre** (`ui/about/AboutScreen.kt`, decisão 111): substitui o rodapé
    fixo que ficava embaixo de toda aba (versão, autor, links pro GitHub e
    pro Buy Me a Coffee) e o diálogo de Ajuda: agora é uma tela com a
    versão, um resumo de cada tela e os atalhos de teclado.
    - **Verificação opcional de atualizações** (decisão 116,
      `ui/about/UpdateChecker.kt`): checkbox "Avisar quando sair uma versão
      nova" (`AppPreferences.checkForUpdates`, desligado por padrão) e
      "Verificar agora". Ligado, ao abrir o app um `ReleaseSource` pergunta
      pra API pública de releases do GitHub
      (`api.github.com/repos/mateustoin/3DReport/releases/latest`, sem
      autenticação) qual é a versão publicada mais recente
      (`isNewerVersion` compara MAJOR.MINOR.PATCH) e mostra um snackbar
      "Versão X do 3DReport disponível" com "Ver novidades". É a única
      requisição que sai da máquina, e só quando a opção está ligada; no
      desktop quem faz a chamada é `platform/GitHubReleaseSource.kt`
      (`java.net.http`, sem dependência nova).

### Persistência (decisões 104, 106 e 108)

Camadas, de baixo pra cima:

- **`DataFile<T>`** (`data/store/DataFile.kt`, `commonMain`): o único ponto que
  toca o disco. Lê uma vez, na abertura, e regrava inteiro a cada mudança. No
  desktop é o `JsonDataFile` (`jvmMain/data/JsonDataFile.kt`): grava em
  `<nome>.tmp` forçado no disco e troca de nome, guardando a versão anterior em
  `<nome>.bak`; um arquivo que não abre vira `<nome>.ilegivel-<millis>.json`, o
  `.bak` é usado se abrir, e o `StorageHealth` avisa a tela. Uma falha de
  leitura do disco é tentada de novo e, se continuar, vira `DataReadException`
  (o app avisa e fecha em vez de abrir vazio e gravar por cima).
- **`WriteBehindFile`** embrulha cada arquivo no app: o estado muda na memória na
  hora e a gravação vai pra segundo plano, sempre do estado mais recente (várias
  mudanças seguidas viram uma gravação). Falha de gravação fica na tela até dar
  certo (`StorageHealth.writeFailures`), e ao fechar o app espera tudo chegar ao
  disco (`PendingWrites`). Nos testes, `DataFileWrapper.Direct` grava na hora.
- **`RecordCollection<T>` e `DocumentValue<T>`** (`data/store/Records.kt`): uma
  lista de registros carimbados (`StoredRecord`: id, criação, alteração,
  exclusão lógica) ou um documento com a data da última mudança. Excluir manda
  pra lixeira por 30 dias, com "Desfazer"; depois fica só a marca de que o id
  foi excluído, que é o que uma sincronização futura precisa. Mudança que não
  muda nada não grava.
- **Repositórios** (`data/*Repository.kt`, `commonMain`): **interfaces**
  (`CatalogRepository<T>` pros cadastros, `DocumentRepository<T>` pros
  documentos, e as específicas: histórico, clientes, marca, manutenção, backup,
  preferências) com a implementação sobre as coleções acima. As telas só
  conhecem as interfaces: trocar o armazenamento (SQLite, nuvem) ou usar uma
  versão em memória é trocar a implementação.
- **`AttachmentStore`**: fotos, STLs, logo e miniaturas do G-code, endereçados
  pelo conteúdo (SHA-256 mais a extensão) em `~/.3dreport/attachments/`. O
  mesmo arquivo é gravado uma vez só, um arquivo gravado nunca muda (editar a
  foto de um pedido nunca mexe na de outro), e a chave já serve de chave de
  blob pra nuvem. Miniaturas ficam em `attachments/thumbs/<px>/`. Na abertura,
  o que nenhum registro (nem os da lixeira) referencia é apagado.
- **`LocalStorage`** (`jvmMain`) monta tudo isso numa pasta; o
  `createDesktopContainer` monta o **`AppContainer`** (todos os repositórios,
  saúde do armazenamento e gravações pendentes), criado no `main()` e passado
  pro `App()`. Nenhum repositório é criado dentro de um composable.

Formato e migrações:

- `~/.3dreport/format.json` guarda a versão do formato (`DATA_FORMAT_VERSION`,
  hoje 2). `prepareDataDir()` (`data/DataFormat.kt`) roda no `Main.kt` antes de
  qualquer repositório. Versão anterior **com migração conhecida**
  (`DATA_MIGRATIONS`, uma `DataMigration` por versão, sobre o JSON): converte
  numa cópia e só então troca de lugar, guardando o original em
  `~/.3dreport-v<versão>`. Sem migração, ou de uma versão mais nova: a pasta é
  guardada inteira à parte e o app começa numa nova. O backup restaurado passa
  pelas mesmas migrações.
- **Depois da tag 2.0.0, toda mudança incompatível no formato vem com
  migração e teste** (decisão 106).
- O arquivo grava todos os campos (`encodeDefaults`), e os enums têm nome fixo
  em inglês no arquivo (`@SerialName`), separado do nome da constante.

Outros:

- **Instância única:** o `main()` trava `~/.3dreport.lock` antes de abrir a
  pasta; uma segunda janela avisa e sai.
- **Backup** (`LocalBackupRepository`): o `.zip` é escrito direto no arquivo
  escolhido, sem montar tudo na memória, deixando de fora `logs/`, miniaturas e
  arquivos de passagem. A restauração é transacional (decisão 75) e protegida
  contra zip slip. Há backup automático diário (7 últimos, pasta configurável
  em Configurações); apontar a pasta pro Drive, OneDrive ou Dropbox leva a cópia
  pra nuvem sem servidor.
- **Log** em `~/.3dreport/logs/3dreport.log` (rotação em 1 MB, `DesktopLog`), e
  um tratador global de exceção mostra o erro com "Copiar detalhes" em vez de
  fechar o app sem aviso.
- Testado em `composeApp/src/jvmTest`: round-trip dos repositórios (grava,
  recria, confere que voltou do disco, via `TestRepositories.kt`), gravação
  atômica e recuperação, lixeira, gravação em segundo plano, anexos, migração e
  backup.

### Capacidades de plataforma
- **`PlatformServices`** (`platform/PlatformServices.kt`) é a interface do que
  depende do sistema: escolher arquivo ou pasta, "Salvar como", gravar, área de
  transferência, abrir link ou pasta, pasta de Documentos, sair. Os ViewModels
  recebem a interface (os testes usam `FakePlatform`, sem diálogo), e **nada
  lança exceção**: escolher devolve `PickResult` (escolhido, cancelado ou falhou
  com o motivo) e salvar devolve `SaveResult` (salvo com o caminho, cancelado
  ou falhou). No desktop é o `DesktopPlatform` (`java.awt.FileDialog`, decisão
  59, e `JFileChooser` só pra escolher pasta). G-code grande é lido só pelo
  começo e pelo fim (8 MB), onde ficam os metadados.
- Continuam como `expect`/`actual` as primitivas pequenas: `decodeImageBitmap`
  (Skia), datas (`java.time`), períodos, a barra de rolagem, arrastar arquivo e
  os exportadores.
- `renderSavedQuotesPdf`/`renderCatalogPdf` ([Apache PDFBox](https://pdfbox.apache.org/),
  Apache 2.0): uma página por orçamento, ou a grade do catálogo, na moeda de
  cada orçamento. A foto entra reduzida (1200 px no orçamento, 800 px no
  catálogo) e em JPEG; a logo, sem perda. Número, emissão e validade, a conta
  quantidade × unitário, nome em até duas linhas e "Para" opcional (decisão
  108). Marca d'água e rodapé são desenhados **por cima de todo o conteúdo**
  (decisão 21). Gerar roda fora do thread da tela, com aviso de "Gerando PDF…".

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

### Versão do app e a tela Sobre
O app segue **SemVer** (decisão 53, que revisa a decisão 27): PATCH pra leva
só de correção/documentação, MINOR pra leva com funcionalidade nova (ver
[development.md](development.md#versionamento) pro critério completo). A
versão tem uma fonte só, `appVersion` em `gradle.properties` (decisão 108):
- usada como `packageVersion` do instalador nativo em
  [`composeApp/build.gradle.kts`](../composeApp/build.gradle.kts);
- e gerada como a constante `APP_VERSION` (tarefa `generateAppVersion`, em
  `build/generated/appVersion`), exibida na barra lateral, em Sobre e no
  diálogo de erro.

Até a decisão 111, `App.kt` desenhava um rodapé fixo (`AppFooter`, abaixo do
conteúdo de toda aba) com a versão, o autor e os links, mais um botão "Ajuda"
que abria um `HelpDialog`. Os dois viraram a tela **Sobre**
(`ui/about/AboutScreen.kt`, ver "Barra lateral agrupada" acima).

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
   ViewModels, `App.kt`, repositórios) deve ser necessária. O que muda é o que
   fica atrás das interfaces (decisão 108): um `DataFile` e um `AttachmentStore`
   pro Android (arquivo em `Context.filesDir`, ou um banco), um
   `PlatformServices` com as intents do Android no lugar do `FileDialog`, um
   `AppContainer` montado na `Activity`, e os `actual` das primitivas pequenas
   (`decodeImageBitmap` via `BitmapFactory` em vez de Skia, datas, exportadores).

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
