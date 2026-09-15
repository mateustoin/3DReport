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
- [ ] **Exportar vários orçamentos num PDF só.** Ideia levantada em
  2026-09-15: na aba Histórico, poder selecionar 2+ orçamentos e gerar um
  único PDF compilado — útil pra quem vende mais de um produto de uma vez
  pro mesmo cliente. Precisa de seleção múltipla na lista (hoje cada linha
  só tem ações individuais).
- [x] **Histórico de orçamentos.** Lista dos orçamentos salvos (nome, foto —
  com opção de baixar —, link interno, valores), pra consultar depois sem
  refazer as contas. Feito (2026-09-15): aba "Histórico".
- [ ] **Serviços opcionais no orçamento** (pintura, lixamento, acabamento,
  etc.). Ideia levantada em 2026-09-15: como os serviços e preços são
  particulares de cada criador, precisaria de uma **aba "Serviços"** —
  mesmo padrão de cadastro de Filamentos/Impressoras (nome + preço, salvo
  em catálogo). Na tela de Orçamento, os serviços cadastrados apareceriam
  como **checkboxes**: o criador marca quais se aplicam àquele orçamento
  específico (dinâmico, por peça). O **PDF/copiar-colar** passaria a
  detalhar, além do que já mostra hoje, cada serviço selecionado (nome +
  valor) e o **valor total** (venda + serviços).
  Em aberto pra quando for implementar: se o valor do serviço soma só na
  venda ou também entra no cálculo interno de produção/lucro; se o
  `SavedQuote` precisa congelar os serviços escolhidos e seus preços no
  momento do salvamento (mesmo princípio do retrato congelado já usado pro
  resto do orçamento — decisão 12/18), pra não mudar retroativamente se o
  preço de um serviço for editado depois no cadastro.
- [ ] **Taxas de marketplace (ex.: Shopee) e custo de embalagem/spray.**
  Estava fora de escopo por ser "recurso pago" (decisão 7, hoje substituída)
  — não há mais essa barreira, só falta decidir como esses custos entram na
  fórmula (ver planilha de referência).
- [ ] **Edição/exclusão com confirmação.** Nas telas de Filamentos e
  Impressoras, "Excluir" age na hora, sem diálogo de confirmação — risco de
  exclusão acidental de um perfil configurado com calma.

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
