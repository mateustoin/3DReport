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

## 2. Instaladores desktop

- [ ] `./gradlew :composeApp:packageDistributionForCurrentOS` já gera
  `.deb`/`.msi`/`.dmg`, mas nunca foi publicado um release — decidir um fluxo
  (ex.: GitHub Releases) quando o repositório for público.

## 3. Infraestrutura e qualidade (open source)

Adiado porque o repositório ainda é privado — não há urgência.

- [ ] **CI no GitHub Actions.** Rodar `./gradlew build` (compila + testa
  `core` e `composeApp`) a cada push/PR.
- [ ] **CONTRIBUTING.md.** Como rodar, testar e propor mudanças — hoje só
  existe [docs/development.md](development.md), voltado a você mesmo.
- [ ] **Badges no README.** Build (CI), licença (Apache 2.0) e o botão de
  apoio (Buy Me a Coffee) já linkado — comuns em repositórios públicos.

## 4. Android (menor prioridade — bem mais pra frente)

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
