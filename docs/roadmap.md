# Roadmap

Lista viva de evoluções e próximas implementações. Diferente de
[decisions.md](decisions.md) (o que já foi decidido), este documento é um
**backlog**: itens aqui ainda não têm data definida. Um item só sai daqui pra
`decisions.md` quando alguém propuser como implementar, o responsável do
projeto aprovar, e a mudança entrar no código (ver
[development.md](development.md#fluxo-de-mudanças)).

As seções abaixo estão em **ordem de prioridade** (definida em 2026-09-15):
funcionalidades de produto e UI/UX primeiro; só depois disso, instaladores
desktop; só depois disso, infraestrutura/qualidade (não urgente enquanto o
repositório continua privado); Android é a menor prioridade de todas — fica
pra quando o projeto estiver consolidado e houver demanda, ainda sem previsão.

## 1. Funcionalidades do produto e UI/UX

- [x] **Salvar um orçamento**, com:
  - **Nome (opcional).** Se deixado em branco ao salvar, gerar um nome
    genérico automático (ex.: "Orçamento #N" ou com a data) pra aparecer no
    histórico.
  - **Foto do produto (opcional).** Upload de imagem anexada ao orçamento —
    entra no **PDF** e fica disponível no **histórico** (com opção de
    baixar a foto de volta a partir do histórico, caso o criador perca o
    arquivo original com o tempo). **Não** entra no copiar/colar — quem gera
    o orçamento já tem a foto em mãos pra mandar junto por fora.
  - **Link do modelo (opcional).** De onde o modelo 3D foi obtido (ex.:
    Thingiverse, Cults3D). É **só uso interno** — nunca aparece no PDF nem no
    texto de copiar/colar; serve só pro criador reencontrar a origem do
    modelo ao revisitar um orçamento antigo no histórico.

  Feito (2026-09-15): formulário na própria tela de Orçamento, salva em
  `~/.3dreport/quotes.json` + foto em `~/.3dreport/photos/`.
- [x] **Exportar o orçamento.** Dois formatos, disponíveis na aba Histórico
  (opera sobre um orçamento já salvo):
  - **PDF** — formato principal, pra mandar pro cliente; nome, **valor de
    venda** e a foto, quando houver.
  - **Copiar e colar simplificado** — versão em texto (vai pra área de
    transferência), mais rápida pra colar numa conversa de WhatsApp/
    marketplace; nome e **valor de venda**, sem foto.
  - Produção e lucro **não aparecem** em nenhum dos dois formatos (decisão
    19 — não expor custo/margem pro cliente). O link do modelo também nunca
    aparece.
  - Feito (2026-09-15): `platform/QuotePdfExporter` (Apache PDFBox) +
    `platform/Clipboard`.
- [x] **Marca d'água personalizada no PDF.** Texto opcional (não imagem —
  ficou pra uma iteração futura, se fizer falta), configurado em
  Configurações (`BrandingSettings`, separado de `PricingSettings` por não
  ser parâmetro de custo). Desenhado diagonal, cinza claro, translúcido,
  centralizado, **por cima** do resto do conteúdo (inclusive da foto — ver
  correção abaixo) — sem controle de posição/opacidade pelo usuário por ora.
  Feito (2026-09-15): `platform/QuotePdfExporter` recebe o texto e desenha;
  `data/BrandingRepository` persiste em `~/.3dreport/branding.json`.
  - **Correção (2026-09-15):** a marca d'água era desenhada *antes* da foto
    e ficava totalmente encoberta por ela quando o orçamento tinha foto.
    Reordenado pra desenhar por cima de tudo (técnica padrão de marca
    d'água), com opacidade ajustada (0,18) pra continuar discreta.
  - **Rodapé profissional (2026-09-15):** quando há marca d'água
    configurada, o PDF ganha um rodapé — linha fina + nome da marca
    centralizado — no fim da página, como um documento de orçamento formal.
  - **Checkboxes independentes (2026-09-15):** diagonal e rodapé viraram
    opções separadas (`showWatermark`/`showFooter` em `BrandingSettings`) —
    o criador escolhe ter os dois, só um, ou nenhum. Com um nome preenchido,
    ao menos um dos dois precisa estar marcado ao salvar (senão o nome fica
    configurado sem aparecer em lugar nenhum); erro de validação impede
    salvar nesse caso.
  - **Pasta padrão ao exportar (2026-09-15):** "Exportar PDF" agora abre o
    diálogo de salvar já na pasta "Documents"/"Documentos" do usuário (o que
    existir; cai para a pasta pessoal se nenhuma existir) — antes usava o
    padrão do sistema (geralmente a última pasta usada). `platform/FileSaver`
    ganhou `defaultDocumentsDirectory()`; "Baixar foto" no Histórico não foi
    alterado (continua sem pasta inicial definida).
- [x] **Exportar vários orçamentos num PDF só.** Na aba Histórico, marcar a
  caixinha de 1+ orçamentos mostra um botão "Exportar selecionados (PDF)" —
  gera um único PDF com um orçamento por página, na ordem da lista, cada um
  com sua própria foto (se tiver); marca d'água/rodapé (quando configurados)
  aparecem em todas as páginas. Útil pra quem vende mais de um produto de
  uma vez pro mesmo cliente. Feito (2026-09-15): `renderSavedQuotesPdf`
  passou a receber uma lista (`QuoteExportItem`) em vez de um único
  orçamento — o export individual de uma linha só virou uma lista de 1 item,
  sem duplicar a lógica de desenho da página.
- [x] **Histórico de orçamentos.** Lista dos orçamentos salvos (nome, foto —
  com opção de baixar —, link interno, valores), pra consultar depois sem
  refazer as contas. Feito (2026-09-15): aba "Histórico".
- [x] **Serviços opcionais no orçamento** (pintura, lixamento, acabamento,
  etc.). Aba **"Serviços"** (nome + preço, salvo em catálogo — mesmo padrão
  de Filamentos/Impressoras, mas começa vazia: não há serviço "padrão" que
  sirva pra qualquer criador). Na tela de Orçamento, os serviços cadastrados
  aparecem como **checkboxes** ("Serviços opcionais", só visível se houver
  algum cadastrado): o criador marca quais se aplicam àquele orçamento
  específico. O resultado passa a mostrar cada serviço escolhido + um
  "Total (venda + serviços)"; **Lucro continua igual** (decisão 25 — soma só
  no total, não entra no cálculo interno, já que não modelamos custo de
  serviço). PDF/copiar-colar detalham a mesma coisa.
  Feito (2026-09-15): `core/model/Service`; `SavedQuote.services` (retrato
  congelado do preço no momento de salvar, mesmo princípio do resto do
  orçamento) + `totalWithServices`; `ServiceRepository`.
- [x] **Taxa de marketplace (ex.: Shopee).** Configurada em Configurações
  (`PricingSettings.marketplaceFeeRate`, %); marcada por orçamento (checkbox
  na aba Orçamento, só aparece se a taxa estiver configurada). Diferente de
  um serviço: o marketplace desconta a taxa da venda, não é somado ao total
  do cliente — por isso o valor de venda sobe o suficiente pra manter a
  margem de lucro real (decisão 26; `PricingCalculator` ganhou
  `appliesMarketplaceFee`, `Quote.marketplaceFeeRate`). Custo de embalagem/
  spray, por outro lado, **entra na aba Serviços** (é um valor fixo cobrado
  do cliente, cabe direto no que já existe — não precisou de aba nova).
  Feito (2026-09-15).
- [x] **Exclusão com confirmação.** "Excluir" agia na hora, sem diálogo de
  confirmação — risco de exclusão acidental de um item configurado com
  calma. Feito (2026-09-15): `ui/components/ConfirmDialog` (diálogo genérico,
  nomeando o item antes de excluir), usado nas 4 telas com essa ação —
  Filamentos, Impressoras, Serviços e Histórico (decisão 30). Edição não
  precisou de confirmação própria — só é efetivada ao clicar em "Salvar".
- [x] **Versão do app + ajuda + rodapé com crédito/doação.** `APP_VERSION`
  (hoje `0.2.0`, SemVer) em `gradle.properties`/`AppVersion.kt`, bump MINOR a
  cada leva de funcionalidades (decisão 27). Rodapé fixo em todas as telas:
  versão, nome do autor, link do GitHub, link do Buy Me a Coffee e um botão
  "Ajuda" que abre um diálogo com a versão, uma descrição curta e um resumo
  de cada aba. Feito (2026-09-15): `App.kt` (`AppFooter`/`HelpDialog`).
- [x] **Link do modelo clicável.** Na aba Orçamento (formulário de salvar) e
  no Histórico, o link do modelo vira um hyperlink de verdade — clicar abre
  no navegador padrão (`platform/openUrl`, `ui/components/LinkText`). Feito
  (2026-09-15).
- [x] **Peso da peça no Histórico.** `Quote.filamentWeightGrams` já era
  calculado internamente (densidade do filamento × comprimento usado) mas
  nunca era mostrado. Passa a aparecer na linha de Produção/Venda/Lucro do
  Histórico — uso só interno do criador, não entra no PDF nem no
  copiar/colar (decisão 32). Feito (2026-09-15).
Os itens abaixo (ainda sem checkbox marcado) vieram de uma sessão de
brainstorming com o responsável do projeto (2026-09-16), pensando no app do
ponto de vista de quem vende impressão 3D no dia a dia. Estão agrupados por
prioridade sugerida — o responsável do projeto decide a ordem real de
implementação.

### Próxima leva sugerida (maior valor / dependências mais simples)

- [x] **Dark mode / Light mode.** Tema claro e escuro, com opção pra seguir o
  tema do sistema operacional por padrão (`ThemeMode.SYSTEM`) e um seletor
  manual em Configurações → Aparência (decisão 35). Feito (2026-09-16):
  `core/model/ThemeMode`, `data/ThemeRepository` (`~/.3dreport/theme.json`),
  `ui/theme/{Color,Theme,ThemeViewModel}.kt`.
- [x] **Modernização da UI.** Paleta de cores customizada (azul petróleo +
  laranja âmbar, no lugar do roxo padrão do Material3) e componente
  `EmptyState` aplicado às listas de Filamentos/Impressoras/Serviços, que
  antes não mostravam nada quando vazias (decisão 36). Escopo definido:
  unificar as 3 telas de catálogo num componente genérico e migrar pra
  `Scaffold`/`SnackbarHost` ficaram de fora, registrados em "Observações
  técnicas" abaixo. Feito (2026-09-16): `ui/theme/Color.kt`,
  `ui/components/EmptyState.kt`.
- [x] **Cliente vinculado ao orçamento.** Campo opcional (`Client(name,
  contact?)`) embutido em `SavedQuote`, mesmo tratamento do link do modelo —
  uso só interno, nunca exportado (decisão 37). Feito (2026-09-16):
  `core/model/Client`, campos "Cliente"/"Contato" na tela de Orçamento,
  exibido no Histórico.
- [x] **Status do pedido.** Novo enum `OrderStatus` (Orçado → Aprovado → Em
  impressão → Pronto → Entregue, padrão `ORCADO`), editável por um dropdown
  em cada linha do Histórico (decisão 37). Feito (2026-09-16):
  `core/model/OrderStatus`, `QuoteHistoryRepository.updateStatus`.
- [x] **Busca/filtro no histórico.** Busca por nome/cliente + filtro por
  status + atalhos de período (Tudo/7 dias/30 dias/Este mês), usando o mesmo
  `PeriodPreset` do Dashboard (decisão 38). Feito (2026-09-16):
  `ui/history/HistoryFilter`, `QuoteHistoryViewModel.visibleQuotes`.
- [x] **Dashboard/relatório simples.** Nova aba com total vendido, lucro
  acumulado e filamento mais usado, recortados pelo mesmo atalho de período
  do filtro do Histórico (decisão 38). Feito (2026-09-16):
  `core/report/QuoteReport` (agregação pura, mesmo estilo do
  `PricingCalculator`), `ui/dashboard/{DashboardViewModel,DashboardScreen}`.

### Visualização e análise de STL (funcionalidade grande, dividida em fases)

- [ ] **Fase 1 — Upload de STL + visualizador 3D.** Anexar o arquivo STL do
  modelo ao orçamento (além da foto que já existe hoje). Envolve:
  - **Parser de STL** (formato binário e ASCII) pra ler a malha de
    triângulos — vira a base de tudo que vem depois (fases 2 e 3).
  - **Visualizador 3D** dentro do app: carregar a malha, rotacionar/zoom/pan,
    escolher um ângulo e enquadramento de câmera. **Decisão técnica a
    avaliar antes de começar**, porque não há nenhuma dependência de 3D no
    projeto hoje: lib de renderização nativa compatível com Compose
    Desktop/JVM (ex.: JOGL/LWJGL, um canvas OpenGL embutido via
    `SwingPanel`/AWT) vs. embutir um visualizador web local com three.js
    numa `WebView`/CEF. A primeira opção é mais leve e nativa; a segunda é
    mais rápida de implementar (three.js já resolve parsing/render/câmera)
    mas adiciona uma dependência pesada (engine web embarcada) só pra isso.
  - **Exportar a visualização como imagem**: capturar o frame renderizado no
    ângulo escolhido e salvar/anexar como a foto do orçamento — reusa o
    campo de foto que já existe, sem precisar de campo novo no modelo de
    dados do orçamento.
  - O arquivo STL em si fica guardado só pra reuso interno (fases seguintes)
    — não entra no PDF/copiar-colar (mesmo tratamento do link do modelo).
- [ ] **Fase 2 — Estimativa automática de peso/tempo a partir do STL.** Hoje
  o criador digita comprimento de filamento e tempo de impressão na mão. Com
  a malha já carregada (fase 1), dá pra calcular o **volume** da peça
  geometricamente e, com um "perfil de impressão" configurável (altura de
  camada, % de preenchimento, velocidade média — por impressora ou global),
  **sugerir** peso e tempo estimados; o criador continua podendo ajustar na
  mão (é um ponto de partida, não substitui o fatiador real, que considera
  suporte/purga/etc.). Depende só da fase 1.
- [ ] **Fase 3 — Análise de complexidade / nível de dificuldade.**
  **Motivação:** peças com geometria complexa (ex.: uma action figure) dão
  mais trabalho de configurar o fatiador (suporte, orientação) e têm mais
  risco de falha durante a impressão do que uma peça simples de peso/tempo
  equivalente (ex.: um cubo) — hoje isso não é capturado em lugar nenhum do
  orçamento, então duas peças de peso/tempo parecido acabam custando o
  mesmo, mesmo que uma dê muito mais trabalho de verdade. Primeira versão:
  um **nível de dificuldade** (ex. Fácil/Médio/Difícil) calculado a partir
  de heurísticas da própria malha (todas dependem só da fase 1, sem exigir
  um fatiador real embutido):
  - **Razão área de superfície ÷ volume** — proxy de quantidade de detalhe
    (formas lisas tendem a um valor baixo; formas com muitos
    relevos/reentrâncias, um valor alto, pro mesmo volume).
  - **% de superfície em overhang** (faces cuja normal aponta abaixo de um
    ângulo limite configurável, ex. 45°) — proxy de necessidade de suporte.
  - **Contagem de triângulos** (proxy grosseiro de nível de detalhe) e
    **número de componentes desconexos** (a peça é uma malha só ou várias
    partes soltas no mesmo arquivo? mais partes tende a mais trabalho de
    organização na mesa de impressão).
  - **Verificação de malha não-manifold** (STL corrompido, com furos ou
    normais invertidas) como aviso separado — tecnicamente não é "nível de
    dificuldade de impressão", é "esse arquivo tem um problema", mas nasce
    do mesmo parser e vale mostrar no mesmo lugar: evita o criador descobrir
    isso só quando já está fatiando de verdade.
  - O nível de dificuldade fica **só de uso interno** (mesmo padrão do peso
    e do link do modelo) — não entra no PDF nem no copiar-colar; serve pro
    criador decidir se cobra uma margem extra por complexidade (a decisão de
    precificação em si continua manual, o app só informa).
  - **Evolução futura** (fora do escopo da primeira versão, só registrando a
    ideia): detecção de suporte mais precisa que a heurística de ângulo
    (simulação real de fatiamento), sugestão de melhor orientação de
    impressão pra minimizar suporte, estimativa de quantidade de material
    de suporte gerado, e — combinando com a fase 2 — um "custo extra
    sugerido" automático em cima do nível de dificuldade.

### Produção e precificação

- [x] **Controle de estoque de filamento — manual, não dedução automática,
  com várias cores por filamento** (decisões 39 e 40). Cadastro de
  filamento ganha **marca** e uma lista de **cores** (cor visual — paleta
  de swatches + campo de hex personalizado com prévia — e o nome da cor
  por escrito, ex.: "Vermelho Fosco"), cada uma com seu próprio estado
  **"Em estoque" / "Acabou"**, alternado direto na lista (clicar no chip
  da cor), sem tentar calcular automaticamente pelo consumo. Várias cores
  na mesma marca/preço evitam cadastro duplicado. Cor "Acabou" continua
  na lista, acinzentada, mas some da seleção na tela de Orçamento (que
  ganha um segundo dropdown "Cor" quando o filamento tem mais de uma
  disponível — não afeta o cálculo, só fica registrado no orçamento/
  histórico, uso interno). Feito (2026-09-16): `core/model/Filament`
  (`brand`, `colors: List<FilamentColor>`, `hasStockAvailable`),
  `core/model/FilamentColor`, `core/model/PrintJob.filamentColor`,
  `ui/filaments/FilamentColor.kt` (paleta + parse de hex + `displayLabel`).
- [ ] **Fila de impressão / agenda da impressora.** Visão de quanto tempo
  cada impressora cadastrada vai ficar ocupada (soma dos orçamentos com
  status "Em impressão", ver item de status acima) — ajuda a prometer prazo
  com mais segurança pro cliente. Depende do item "Status do pedido".
- [ ] **Custo de falha real acumulado.** Hoje a taxa de falha é um % fixo
  estimado nas Configurações. Permitir marcar um orçamento/impressão como
  "falhou" (com motivo opcional) e, com histórico suficiente, sugerir um %
  de falha calibrado com dado real do próprio criador em vez de um chute
  inicial.

### Vendas e divulgação

- [x] **Catálogo/portfólio exportável** (decisão 44). No Histórico, marcar
  1+ orçamentos mostra um botão "Exportar catálogo (PDF)" ao lado do
  "Exportar selecionados (PDF)" que já existia — gera um PDF em grade
  (2 colunas, foto+nome+preço por célula, várias peças por página, ao
  contrário do "um orçamento por página" já existente), pra mandar pro
  cliente ou postar em grupo de venda. Reaproveita a mesma seleção múltipla
  do Histórico — nenhuma UI de seleção nova, nenhum campo novo pra marcar
  "peça pronta". Feito (2026-09-17): `platform/QuotePdfExporter.renderCatalogPdf`,
  `QuoteHistoryViewModel.exportCatalogPdf`.
- [x] **Templates de orçamento** (decisões 45 e 47). Biblioteca de **fotos
  nomeadas** do `BrandingSettings` ativo — sem tela/aba própria: acessada
  por um diálogo modal a partir de Configurações → "Marca d'água do PDF",
  que continua sendo o único editor ao vivo. Feito (2026-09-17):
  `core/model/QuoteTemplate`, `data/TemplateRepository`,
  `ui/templates/{TemplateListViewModel,TemplateListDialog}`.
  - **Indicador de template ativo (decisão 47, 2026-09-17):** badge "Ativo"
    ao lado do nome, no template cujos campos batem com a config atual de
    `BrandingSettings` (`TemplateListViewModel.activeTemplateId`).
  - **Relação Templates ↔ Configurações repensada (decisão 47,
    2026-09-17):** Templates deixou de ter formulário próprio de
    criar/editar (removido `TemplateFormState`) — vira uma lista simples
    de "Carregar"/"Excluir". Configurações → "Marca d'água do PDF" ganhou
    o botão "Salvar como template" (só pede um nome; tira uma foto do que
    está no formulário na hora do clique, mesmo sem ter clicado em
    "Salvar" antes). Feito:
    `ui/settings/BrandingViewModel.{startSaveAsTemplate,confirmSaveAsTemplate}`.
  - **Aba dedicada removida (decisão 47, 2026-09-17):** revisão de
    usabilidade pedida pelo responsável do projeto — uma aba fixa no menu
    principal era desproporcional pra uma funcionalidade tão estreita
    (só usada dentro de uma sub-seção de Configurações, ao contrário de
    Filamentos/Impressoras/Serviços, catálogos usados no fluxo principal
    de Orçamento). A lista de templates vira um **diálogo modal**
    (`TemplateListDialog`), aberto pelo botão "Ver templates salvos" ao
    lado de "Salvar como template" — volta a 7 abas no menu,
    `Ctrl/Cmd+7`→Configurações.
- [x] **Múltiplas moedas/localização** (decisão 46). Nova seção "Moeda" em
  Configurações — BRL, USD, EUR ou GBP, cada uma com seu próprio símbolo e
  convenção de separador decimal/milhar (não é só trocar "R$" por "$").
  Aplica em toda a interface, no PDF exportado e no copiar/colar. De
  passagem, a formatação ganhou separador de milhar, que não existia antes.
  Feito (2026-09-17): `core/model/Currency`, `data/CurrencyRepository`,
  `ui/format/CurrencyFormat.kt` (`LocalCurrency`, `toMoney()`,
  `toCurrencyText()`).

### Integrações

- [ ] **Exportar histórico pra CSV/Excel.** Pra quem já usa planilha
  (Excel/Google Sheets) como contabilidade paralela do negócio.
- [ ] **Import/export de catálogo de filamentos entre criadores.** Arquivo
  (JSON/CSV) com perfis de filamentos populares (ex. marcas/linhas comuns no
  Brasil) que a comunidade possa compartilhar/importar, evitando cadastro
  manual do zero a cada filamento novo.
- [ ] **Integração com WhatsApp.** Mandar o PDF/texto do orçamento direto
  pro cliente sem sair do app. Prioridade baixa e incerta por ora — exige
  conta WhatsApp Business API (custo/burocracia de aprovação), avaliar se
  compensa frente ao fluxo atual (copiar-colar manual já cobre o uso comum).

### UX extras

- [x] **Atalhos de teclado** (decisão 43). `Ctrl`/`Cmd+1` a `7` pula direto
  pra cada aba; `Ctrl`/`Cmd+S` salva o orçamento atual e `Ctrl`/`Cmd+N`
  limpa a tela de Orçamento pra começar um novo (os dois só na aba
  Orçamento); `Esc` cancela o formulário de adicionar/editar aberto em
  Filamentos/Impressoras/Serviços. Funcionam com `Ctrl` (Windows/Linux) ou
  `Cmd` (macOS) indistintamente. Listados no diálogo de Ajuda. Feito
  (2026-09-17): `App.kt` (`onPreviewKeyEvent` no `Surface` raiz),
  `QuoteViewModel.saveCurrentQuote`/`resetForm`.
- [ ] **Onboarding na primeira execução.** Assistente curto guiando o
  cadastro da primeira impressora/filamento/margem, em vez de abrir numa
  tela vazia sem nenhum dado cadastrado. Baixa prioridade — fica pra
  depois.

## 2. Site (GitHub Pages) — divulgação e instruções de uso

- [ ] **Site institucional em GitHub Pages.** Landing page com o que o app
  faz, screenshots, link de download (GitHub Releases) e instruções de uso
  (equivalente a um manual rápido). Bom SEO (meta tags, `sitemap.xml`,
  conteúdo em português voltado a quem busca "orçamento impressão 3D" e
  termos correlatos) pra ajudar a divulgar organicamente. Faz mais sentido
  **depois** do repositório ficar público (decisão 15 — hoje ainda é
  privado), já que o site vai linkar pro repo/releases.

## 3. Instaladores desktop

- [x] **Fluxo de release.** `./gradlew :composeApp:packageDistributionForCurrentOS`
  gera `.deb`/`.msi`/`.dmg`, mas só do SO em que roda (sem cross-compilation).
  `.github/workflows/release.yml` builda os 3 num runner por SO (GitHub
  Actions), disparado só por push de tag `vX.Y.Z` (sempre confirmar antes de
  criar a tag), e junta tudo num GitHub Release em **rascunho** (nunca
  publicado sozinho), com a descrição vinda do `CHANGELOG.md` (decisão 33).
  Feito (2026-09-15) — primeiro release publicado como **`v1.0.0`**, não
  `v0.3.0` (o bundler do macOS exige versão ≥ 1, decisão 34).
- [x] **Confirmar que atualizar preserva os dados locais** (decisão 41).
  Confirmado inspecionando os `.msi` reais publicados (v1.0.0 e v1.3.0,
  via a tabela do Windows Installer, sem precisar instalar): (1) o
  instalador nunca referencia `~/.3dreport/` — tudo fica sob `INSTALLDIR`
  (`Program Files\3DReport`), a pasta do usuário nunca aparece na tabela
  `Directory` do pacote, já que os dados são gravados em tempo de
  execução pelo próprio app (`user.home`), fora do controle do MSI; (2)
  as duas versões compartilham o mesmo `UpgradeCode`, com a tabela
  `Upgrade` + `FindRelatedProducts`/`RemoveExistingProducts`
  configurados como um **major upgrade MSI padrão** — instalar uma
  versão mais nova detecta e substitui a anterior automaticamente, sem
  instalação lado a lado e sem exigir desinstalar manualmente antes.
  Feito (2026-09-17).
- [x] **Ícone do app + atalho de área de trabalho** (decisão 42). O app não
  tinha ícone próprio (saía com o ícone padrão do Java/jpackage). Criado um
  ícone (cubo isométrico "em camadas" + bico de impressão, nas cores da
  paleta do app — azul petróleo e laranja âmbar), gerado programaticamente
  (sem depender de nenhuma ferramenta de imagem externa) nos 3 formatos
  nativos exigidos por instalador (`.ico`/`.icns`/`.png`). Instalador
  Windows passa a criar atalho na área de trabalho e no menu Iniciar ao
  instalar — a ferramenta de empacotamento usada aqui (jpackage/WiX, via
  Compose Multiplatform) só oferece "criar sempre" ou "nunca criar", não
  uma caixinha de escolha durante a instalação; optou-se por criar sempre.
  Verificado localmente: build gerou o `.msi` sem erros, o `.exe` extraído
  já carrega o ícone customizado (não mais o padrão do Java), e a tabela
  `Shortcut` do MSI lista o atalho em `DesktopFolder` e no menu Iniciar.
  Feito (2026-09-17): `composeApp/packaging/icons/{icon.ico,icon.icns,icon.png}`,
  `composeApp/build.gradle.kts` (`windows`/`macOS`/`linux { iconFile, shortcut }`).

## 4. Infraestrutura e qualidade (open source)

Adiado porque o repositório ainda é privado — não há urgência.

- [ ] **CI no GitHub Actions.** Rodar `./gradlew build` (compila + testa
  `core` e `composeApp`) a cada push/PR.
- [ ] **CONTRIBUTING.md.** Como rodar, testar e propor mudanças — hoje só
  existe [docs/development.md](development.md), voltado a você mesmo.
- [ ] **Badges no README.** Build (CI), licença (Apache 2.0) e o botão de
  apoio (Buy Me a Coffee) já linkado — comuns em repositórios públicos.

## 5. Android (menor prioridade — bem mais pra frente)

- [ ] Só quando o projeto estiver consolidado e houver demanda de verdade.
  Passos técnicos já mapeados em
  [architecture.md](architecture.md#como-adicionar-android-no-futuro).
  Principal trabalho: um `actual` de `data/` para Android (`DataStore` ou
  arquivo em `Context.filesDir`, já que a persistência atual usa
  `java.io.File` com `user.home`, específico de desktop) — e, se o upload de
  foto do orçamento já existir nessa altura, também precisará de um caminho
  de armazenamento de imagem por plataforma.

## Observações técnicas (não são pedidos de mudança, só pontos a reavaliar se algo doer na prática)

- Persistência em arquivo JSON (decisão 14) foi escolhida por simplicidade;
  se o volume de dados crescer muito (ex.: histórico de orçamentos com fotos)
  ou vier a precisar de consultas mais complexas, migrar para SQLDelight é a
  alternativa que já foi cogitada.
- Valores monetários em `Double` (decisão 16): reavaliar só se aparecer um
  bug real de arredondamento — não é esperado no uso atual.
- **Bug de terceiros contornado (2026-09-15):** arrastar a janela entre
  monitores com DPI/escala diferentes deixava o conteúdo (ex.: a barra de
  abas) com o layout antigo até a janela ser redimensionada na mão — bug
  conhecido do Compose Desktop/Skiko, não do nosso código (ver
  [architecture.md](architecture.md) e decisão 22). Contornado forçando um
  redimensionamento programático quando a janela muda de monitor. Se algum
  dia isso for corrigido oficialmente no Compose Multiplatform, o workaround
  em `Main.kt` pode ser removido.
- **Duplicação entre Filamentos/Impressoras/Serviços (observado em
  2026-09-16):** as 3 telas de catálogo (`FilamentListScreen`,
  `PrinterListScreen`, `ServiceListScreen`) são quase idênticas em estrutura
  (lista + formulário de adicionar/editar + `ConfirmDialog` de exclusão).
  Fora do escopo da modernização de UI da leva 2026-09-16 (decisão 36) por
  ser um refactor grande à parte — unificar num componente genérico de
  lista/formulário é candidato a uma leva futura, se a duplicação continuar
  incomodando.
- **`App.kt` sem `Scaffold`/`SnackbarHost` (observado em 2026-09-16):** todo
  feedback de sucesso/erro hoje é `Text` inline (vermelho/erro, colorido/
  sucesso) que aparece/desaparece com a recomposição, em vez de um toast
  transitório. Migrar pra `Scaffold` + `SnackbarHost` foi cogitado na
  modernização de UI da leva 2026-09-16 mas ficou de fora por ser um
  refactor que toca as 5 telas com esse padrão (decisão 36) — candidato a
  uma leva futura de UI.
