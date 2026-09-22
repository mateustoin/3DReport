# Roadmap

Lista viva de evoluções e próximas implementações. Diferente de
[decisions.md](decisions.md) (o que já foi decidido), este documento é um
**backlog**: itens aqui ainda não têm data definida. Um item só sai daqui pra
`decisions.md` quando alguém propuser como implementar, o responsável do
projeto aprovar, e a mudança entrar no código (ver
[development.md](development.md#fluxo-de-mudanças)).

As seções abaixo estão em **ordem de prioridade** (definida em 2026-09-15):
funcionalidades de produto e UI/UX primeiro; só depois disso, instaladores
desktop; só depois disso, infraestrutura/qualidade (adiantada em 2026-09-17,
decisão 49, antes mesmo do repositório abrir); Android é a menor prioridade
de todas, fica pra quando o projeto estiver consolidado e houver demanda,
ainda sem previsão. O repositório ficou **público** em 2026-09-17 (decisão
50), destravando o que dependia disso: instalação real pelos links do site,
downloads da página de releases e a assinatura "Gerado com 3DReport" nos
PDFs (seção 1, "Vendas e divulgação").

**Ordem de implementação dos itens pendentes:** ver "Plano de evolução"
logo abaixo — ele organiza em levas o que ainda está aberto (inclusive
itens que já estavam registrados nas seções seguintes). As seções numeradas
continuam sendo o catálogo completo, por assunto.

## Plano de evolução (revisão de 2026-09-22)

Revisão do produto inteiro feita em 2026-09-22, a pedido do responsável do
projeto: olhar o app com a cabeça de quem vende impressão 3D
profissionalmente, revisitar as decisões já tomadas e comparar com o que o
mercado de software do setor oferece. O diagnóstico foi que o app já é bem
mais do que uma calculadora (histórico, clientes, kanban, fila por
impressora, catálogos com preset, viewer 3D, análise de STL), e que o que
falta se concentra em três frentes: **o motor de precificação tem erros
conceituais que fazem o vendedor cobrar barato sem perceber**, **falta o que
o vendedor faz o dia inteiro (vender quantidade, negociar, mandar no
WhatsApp)** e **o posicionamento ainda é "calculadora", quando o que existe
já é "o sistema do vendedor"**.

As levas abaixo estão **em ordem de implementação sugerida**. A ordem foi
escolhida pra evitar retrabalho, não por tamanho: proteger os dados antes de
mudar o formato deles, acertar a conta antes de construir em cima dela,
fechar o conjunto de campos antes de redesenhar as telas, e deixar o app bom
antes de empurrar divulgação. Itens que já estavam registrados em outras
seções deste documento aparecem aqui só como agendamento, com link — a
descrição completa continua no lugar original, pra não haver duas fontes de
verdade.

**Resumo da ordem:** rede de proteção → custo real do trabalho → quantidade
→ preço final correto → negociação → saída pro cliente → leitura do negócio
→ UI/UX → crescimento → apostas maiores.

### Leva 0 — Rede de proteção (antes de mexer no modelo de dados)

- [x] **Backup e restauração dos dados locais** (decisão 75, 2026-09-22).
  Hoje o negócio inteiro do vendedor (histórico, clientes, catálogos, fotos,
  STLs) vive só em `~/.3dreport/`. Formatar o computador, trocar de máquina
  ou corromper um arquivo significa perder tudo, sem nenhum caminho de
  recuperação dentro do app — é o maior risco silencioso do produto hoje.
  **Vem primeiro de propósito:** as levas 1 a 3 mudam o formato dos dados
  salvos, e não se mexe no arquivo do negócio de alguém sem oferecer um
  botão de backup antes.
  - Escopo sugerido: Configurações ganha "Fazer backup" (gera um `.zip` com
    todos os JSONs + `photos/` + `models/`, nome com data) e "Restaurar
    backup" (substitui tudo, com `ConfirmDialog` explícito, mesmo padrão já
    usado nas exclusões). Resolve de uma vez os três casos: perda de dados,
    troca de computador e "quero levar meu histórico pro notebook".
  - **Feito:** `data/BackupRepository` (expect/actual),
    `platform/pickBackupFile`, `platform/exitApp` e a seção "Backup" em
    Configurações. A restauração é transacional (extrai numa pasta
    temporária e só troca no fim), guarda os
    dados substituídos numa pasta `.3dreport-anterior-<data>` em vez de
    apagá-los, recusa `.zip` que não seja backup do app ou que tente gravar
    fora da pasta de dados, e fecha o app ao final (ver decisão 75 pro
    porquê).
- [x] **Menu "⋮" na linha do Histórico** (decisão 75, 2026-09-22). A lista
  já chegou a 8 botões de texto lado a lado (Exportar PDF, Copiar, Editar,
  Duplicar, Configurações de impressão, Baixar foto, Baixar STL, Excluir) e
  as levas seguintes ainda somam mais (WhatsApp, imagem, comparar
  impressoras). O card do Kanban já resolveu isso com um menu "⋮" — a lista
  deve seguir o mesmo padrão: 2 ou 3 ações principais visíveis, o resto no
  menu. Item pequeno, entra junto do
  backup por ser arrumação de casa antes da obra.

### Leva 1 — O custo real do trabalho (maior impacto no bolso)

As quatro mudanças abaixo devem entrar **na mesma leva**: todas alteram o
custo de produção, e soltá-las separadas obrigaria o vendedor a recalibrar a
tabela de preços quatro vezes seguidas. Nenhuma delas mexe em orçamento já
salvo (`SavedQuote` é um retrato congelado, ver KDoc) — o histórico continua
coerente, só os orçamentos novos usam a fórmula nova. Campos novos em
`PricingSettings` devem nascer com valor neutro (zero), pra que atualizar o
app não mude o preço de ninguém em silêncio: a conta só muda quando a pessoa
preencher.

- [x] **Mão de obra do vendedor entra no cálculo** (decisão 76, 2026-09-22). É o furo mais grave do
  motor hoje: o custo de produção soma material, energia, manutenção, falha,
  acabamento, retorno da máquina e administrativo, e **nenhuma dessas
  parcelas é o tempo da pessoa**. Preparar o arquivo, fatiar, tirar da mesa,
  remover suporte, lixar, pintar, embalar, responder o cliente e ir ao
  correio são o trabalho de verdade de quem vende impressão 3D. Ficando de
  fora, toda peça pequena e trabalhosa sai subprecificada, e o vendedor
  descobre isso só quando percebe que trabalhou o mês inteiro sem sobrar
  nada. Proposta: taxa horária do operador em `PricingSettings` + minutos de
  trabalho por orçamento (campo na tela de Orçamento), e — opcionalmente —
  minutos embutidos em cada `Service` cadastrado, pra "Pintura" já trazer
  seus 30 min por padrão quando marcada.
- [x] **Acabamento deixa de ser percentual do material** (decisão 76, 2026-09-22). Hoje
  `acabamento = material × taxa`, o que está matematicamente errado pro caso
  real: uma action figure de 30 g dá 40 min de pós-processamento; um suporte
  de parede liso de 200 g dá 2 min — mas o suporte "paga" quase 7x mais
  acabamento que a action figure. Acabamento escala com tempo de trabalho,
  não com gramas, então vira uma aplicação do item de mão de obra acima.
  Duas opções de migração, a decidir na implementação: **(A)** manter
  `finishingRate` funcionando como está quando nenhuma mão de obra estiver
  configurada (fallback, ninguém perde nada) e tratá-lo como legado; **(B)**
  remover o campo e avisar na atualização que acabamento agora se informa em
  minutos. A opção A é menos traumática pra quem já usa o app hoje.
- [x] **Reserva de falha passa a incidir sobre o custo refeito inteiro** (decisão 76, 2026-09-22).
  Hoje `falhas = material × taxa`. Quando uma impressão de 8 h falha no fim,
  o prejuízo foi material **e** energia **e** hora de máquina **e** o tempo
  de recomeçar — aplicar a taxa só sobre o material subestima a reserva em
  algo entre 2x e 4x, dependendo da peça. Mudança de uma linha no
  `PricingCalculator`, com efeito real no bolso. Atenção à ordem de cálculo
  pra não gerar referência circular (a reserva incide sobre as parcelas de
  custo, não sobre si mesma).
- [x] **Custos fixos mensais do negócio diluídos por hora produtiva** (decisão 76, 2026-09-22).
  Aluguel do espaço, internet, prateleira/embalagem, assinatura de modelos
  (Patreon de STL é gasto real de quem revende), energia fora da impressão.
  Quem precifica só o custo variável quebra devagar. O padrão pra resolver
  isso já existe pronto no código: `MachineInvestment.costPerHour` dilui o
  valor da máquina por hora de impressão — basta aplicar a mesma fórmula a
  um "custo fixo mensal do negócio ÷ horas produtivas por mês". Fica em
  `PricingSettings` (é do negócio, não de uma impressora específica).
- [ ] **Minutos de trabalho embutidos em cada serviço cadastrado** (separado
  da leva 1 em 2026-09-22, decisão 76). A ideia original era "Pintura" já
  trazer seus 30 min por padrão ao ser marcada num orçamento. Ficou de fora
  porque esbarra na decisão 25: hoje um `Service` só entra no total cobrado
  do cliente, sem custo modelado e sem afetar o lucro. Fazer os minutos do
  serviço entrarem no custo de produção muda essa regra, e isso merece uma
  decisão própria em vez de entrar de carona. Alternativa mais barata a
  avaliar junto: os minutos do serviço só **sugerirem** o preenchimento do
  campo de tempo de trabalho do orçamento, sem entrar sozinhos na conta.

### Leva 2 — Quantidade e lote

- [x] **Campo de quantidade no orçamento** (decisão 77, 2026-09-22). Conferido em 2026-09-22:
  `PrintJob`, `Quote` e `SavedQuote` não têm nenhum campo de quantidade.
  "Quero 10 chaveiros" é provavelmente o pedido mais comum do mercado, e
  hoje o vendedor resolve na calculadora do celular, fora do app. Toca o
  modelo, o resultado na tela, o PDF, o copiar/colar, o Histórico e a
  agregação do Dashboard — por isso vem depois da leva 1 (a conta por
  unidade precisa estar certa antes de multiplicar) e antes das telas serem
  redesenhadas.
- [x] **Regra de lote, não multiplicação simples** (decisão 77, 2026-09-22). 10 peças na mesma mesa
  não custam 10 impressões separadas: o tempo de setup/preparo se dilui, e
  normalmente o vendedor dá desconto por volume. Mínimo viável: mostrar
  "preço unitário" e "total", com um desconto por quantidade opcional
  (percentual ou valor), deixando claro no PDF o que é unitário e o que é
  total. O tempo de mão de obra fixo (leva 1) deve ser cobrado uma vez por
  lote, não por peça — é justamente isso que torna o lote mais barato por
  unidade.
  - **Feito:** `Quote.quantity` + `Quote.setupMinutes`. O desconto percentual
    por volume citado acima **não** foi implementado: o preço por unidade já
    cai sozinho porque o preparo é cobrado uma vez só, e ter dois mecanismos
    de desconto (um de custo, um de negociação) confundiria. O desconto
    explícito fica pra leva 4, junto do preço alvo. A semântica escolhida
    (entradas são de uma peça, o app multiplica) está na decisão 77.
- [ ] **Serviço cobrado por pedido em vez de por peça** (levantado em
  2026-09-22, decisão 77). Hoje todo serviço marcado multiplica pela
  quantidade, porque pintura/lixamento/embalagem são trabalho peça a peça.
  Um serviço como "entrega" ou "projeto/modelagem" deveria ser cobrado uma
  vez pelo pedido. Se aparecer caso real, `Service` ganha um marcador de
  "por peça / por pedido" e a tela de Serviços um seletor. Não implementado
  por ora pra não somar campo e tela antes de existir a necessidade.

### Leva 3 — O preço final correto (deduções e acréscimos da venda)

- [ ] **Canais de venda com taxa própria.** Item já registrado (ver
  "Taxa de marketplace" na seção 1) — agendado aqui, porque é a base pros
  dois itens seguintes: em vez de uma taxa única em Configurações, um
  catálogo de canais (nome + taxa), no mesmo padrão de Serviços.
- [ ] **Imposto e taxa de pagamento.** Faltam duas deduções que todo
  vendedor brasileiro sente: imposto (MEI/Simples) e taxa de recebimento
  (maquininha ou link de pagamento, tipicamente ~4%; Pix, zero). Mesma
  mecânica já implementada pra marketplace (decisão 26: o preço de venda
  sobe o suficiente pra margem real não mudar), aplicada a mais duas
  parcelas. Junto com os canais de venda, é o que faz o "Você cobra" virar
  de fato o que entra na conta do vendedor.
- [ ] **Frete.** Pra venda online, frete é metade da conversa com o cliente
  e hoje não existe em lugar nenhum do app. Escopo mínimo: valor informado
  na mão por orçamento, somado ao total do cliente como linha própria no PDF
  e no copiar/colar (nunca embutido no preço da peça, pra não parecer que a
  peça ficou mais cara), com a opção "frete grátis" descontando do lucro
  explicitamente — assim o vendedor vê quanto o "frete grátis" custou de
  verdade.

### Leva 4 — Negociação (depende de a conta estar completa)

Só faz sentido depois das levas 1 a 3: uma calculadora reversa em cima de
uma conta incompleta mente com mais confiança.

- [ ] **Preço alvo (negociação reversa).** "O cliente quer pagar R$ 30,
  quanto sobra?" e "quanto preciso cobrar pra ter 40% de lucro real depois
  da taxa da Shopee?". Um campo de preço alvo (ou um slider) que recalcula
  margem e lucro ao vivo, em cima do preço já calculado. Vendedor negocia
  todo dia e nenhuma ferramenta do setor faz isso bem.
- [ ] **Preço mínimo com aviso de prejuízo.** O custo de produção já é
  calculado; falta usá-lo como piso: quando o preço negociado cair abaixo
  dele, avisar visualmente ("abaixo disso você paga pra imprimir"). Depende
  do item acima e reaproveita `Quote.productionCost`.
- [ ] **Comparar impressoras no mesmo orçamento.** "Essa peça sai R$ 16,19
  na K1 e R$ 14,80 na Ender." Todos os dados já estão cadastrados — é rodar
  o `PricingCalculator` (função pura, sem estado) uma vez por impressora
  cadastrada e mostrar o resultado lado a lado. Barato de implementar,
  ajuda a decidir em qual máquina imprimir e rende ótima screenshot.

### Leva 5 — Saída pro cliente

- [ ] **WhatsApp de verdade.** O copiar/colar já existe, mas o passo
  seguinte é abrir a conversa pronta: botão que monta um link `wa.me` com o
  texto do orçamento já preenchido, usando o contato que já está salvo em
  `Client` (100% local, é só abrir uma URL — reaproveita o
  `platform/openUrl` que já existe).
- [ ] **Imagem quadrada pro WhatsApp/Instagram.** Foto da peça + preço +
  prazo numa imagem pronta pra mandar no zap ou postar no status, em vez de
  um PDF anexado. A maior parte das vendas no Brasil acontece em conversa,
  não em documento formal. Reaproveita o mesmo caminho de renderização já
  usado na captura do visualizador 3D (`encodeImageBitmapToPng`, decisão
  63) e a assinatura discreta do item de divulgação (leva 8) cabe no
  rodapé dessa imagem também.

### Leva 6 — Leitura do negócio (Dashboard vira consultor, não relatório)

- [ ] **Lucro por hora de impressão e por hora de trabalho.** Total vendido
  e lucro acumulado são contabilidade; "sua máquina te paga R$ X por hora" e
  "seu trabalho te paga R$ Y por hora" são gestão — é a métrica que diz se o
  negócio funciona e qual tipo de peça vale a pena repetir. Depende da mão
  de obra (leva 1) pra segunda métrica existir; a primeira já é possível
  hoje com `printTimeMinutes` + lucro.
- [ ] **Ranking de produto mais lucrativo.** Item já registrado (ver
  "Dashboard/relatório simples" na seção 1) — agendado aqui, por ser a mesma
  leva de leitura do negócio e reaproveitar a mesma agregação.

### Leva 7 — UI/UX (depois que o conjunto de campos parar de mudar)

Deliberadamente **depois** das levas 1 a 5: elas somam campos na tela de
Orçamento (mão de obra, quantidade, desconto, imposto, frete). Redesenhar o
layout antes disso significa redesenhar duas vezes.

- [ ] **Layout de duas colunas na tela de Orçamento.** Hoje é um app desktop
  com layout de celular: uma coluna só, rolando por três assuntos diferentes
  (calcular, anexar arquivos, salvar), e na janela padrão não dá pra ver o
  preço e os campos ao mesmo tempo. Proposta: entradas à esquerda, a "nota"
  fixa à direita recalculando ao vivo enquanto se digita. É a maior melhoria
  visual disponível e finalmente usa o formato de tela que o app escolheu.
- [ ] **Barra de composição do preço.** O "Você cobra R$ 16,19" não mostra
  onde o dinheiro está. Uma barra empilhada pequena (material, energia,
  máquina, mão de obra, lucro) ensina o vendedor a precificar, dá sentido a
  todas as parcelas somadas nas levas 1 a 3 e rende boa screenshot pro site.
- [ ] **Onboarding de 3 perguntas na primeira execução.** Substitui o item
  de onboarding já registrado (ver "UX extras"), agora com escopo definido:
  impressora (pelo preset que já existe), preço do kWh e margem. Detalhe
  local que vale a pena: **sugerir o kWh por estado brasileiro** a partir de
  uma tabela estática embutida (sem rede, no mesmo espírito dos presets de
  impressora) — é o tipo de carinho que faz a pessoa sentir que o app foi
  feito pra ela. Vem antes da leva de crescimento de propósito: não adianta
  atrair gente nova pra uma tela vazia.
- [ ] **Unificar as telas de catálogo e migrar pra Scaffold/Snackbar.** Itens
  já registrados em "Observações técnicas" (fim deste documento) — agendados
  aqui porque são a mesma obra de UI: as 3 telas quase idênticas
  (Filamentos/Impressoras/Serviços) podem virar uma aba "Catálogos" só,
  liberando espaço na barra de 7 abas, e o feedback inline vira snackbar.
  Atenção: mexer na quantidade de abas mexe na numeração dos atalhos de
  teclado (decisões 43 e 45).

### Leva 8 — Crescimento (com o produto já bom)

- [ ] **Assinatura discreta "Gerado com 3DReport" nos PDFs.** Item já
  registrado e detalhado (ver "Vendas e divulgação") — **é a maior alavanca
  de distribuição do projeto inteiro e continua sem implementação.** Cada
  orçamento que um vendedor manda pro cliente é uma chance de outro vendedor
  conhecer o app, e é a única divulgação orgânica disponível pra um projeto
  sem verba de marketing (decisão 15).
- [ ] **Reposicionar a comunicação: de "calculadora" pra "o sistema de quem
  vende impressão 3D".** Site e README hoje lideram com orçamento/
  calculadora, categoria em que existem centenas de páginas web gratuitas
  concorrendo. O que está construído já é outra coisa: histórico, clientes,
  kanban, fila de impressora, catálogos, reimpressão com a mesma
  configuração. A diferenciação defensável tem duas pernas: **ser o sistema
  completo** e **ser 100% local, grátis e offline** ("seus preços e seus
  clientes não saem do seu computador"), contra concorrentes SaaS de
  mensalidade. O fosso competitivo é justamente ser brasileiro: MEI, Pix,
  maquininha, Shopee/ML, kWh por estado, WhatsApp — coisas que as levas 3, 5
  e 7 constroem e que nenhuma ferramenta internacional vai fazer bem.
- [ ] **Arrastar o G-code na janela e sair um orçamento completo.** Hoje a
  importação preenche dois campos, mas o fluxo ainda exige escolher
  impressora e filamento na mão. O bloco de configuração do G-code das
  famílias PrusaSlicer/Bambu/Orca também traz o **modelo da impressora** e o
  **tipo/marca do filamento** — dá pra casar automaticamente com o que está
  cadastrado nos catálogos e deixar o orçamento pronto num arrasto só.
  Primeiro passo obrigatório: conferir em arquivos reais de cada fatiador
  quais chaves existem de fato (mesma disciplina das decisões 55 e 56, que
  não inventaram formato). É o "wow" demonstrável em 10 segundos que nenhuma
  planilha responde — por isso está na leva de crescimento, e não na de
  importação.

### Leva 9 — Apostas maiores (por último)

- [ ] **Peça com mais de um filamento (multicolor/AMS).** `PrintJob` tem um
  único `filament`, então uma peça com 3 cores não é representável — pior: o
  parser de G-code **soma** os extrusores num total só, então uma peça com
  PLA + PETG é cobrada como se tudo fosse do mesmo preço/kg, em silêncio.
  Com AMS virando comum, isso vira uma limitação de verdade. Fica por último
  por ser a mudança mais profunda do modelo (`PrintJob`/`Quote` + todos os
  exports). **Nota de sequenciamento:** se houver intenção real de fazer
  isso, fazer junto da leva 2 (quantidade, que também mexe em
  `PrintJob`/`Quote`) economiza metade do trabalho de migração.
- [ ] **Calculadora web no próprio site, reaproveitando o módulo `core`.**
  O `core` é Kotlin Multiplatform puro, sem dependência de desktop — dá pra
  publicar uma calculadora simples no GitHub Pages que já existe (alvo
  Wasm/JS) usando exatamente a mesma fórmula, sem segunda fonte de verdade.
  Serve de funil: quem pesquisa "calcular preço impressão 3D" no Google cai
  na calculadora e termina com "quer salvar histórico, clientes e PDF? baixe
  o app". Por último por ser a maior incógnita técnica da lista (alvo de
  build novo, pipeline de publicação, e uma versão web que precisa não
  canibalizar o app).

### Fora das levas

Continuam no backlog, sem posição definida nesta revisão (nenhum foi
descartado): Fase 2 do STL (estimativa geométrica de peso/tempo, pulada pela
decisão 66), custo de falha real acumulado, sinal/pagamento parcial,
lembrete e histórico de manutenção por impressora, guardar o `.3mf` do
projeto do fatiador, exportar histórico pra CSV/Excel, compatibilidade com
Spoolman, portal de acompanhamento pro cliente, assistente de IA, idioma da
interface configurável, banner dedicado pro compartilhamento do site,
`CODE_OF_CONDUCT.md` e Android. Os três primeiros da lista encaixariam
naturalmente nas levas 1 e 6, se em algum momento virarem prioridade.

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
  - **Múltiplos canais de venda com taxa própria (levantado em 2026-09-19,
    pesquisa de concorrentes, ainda não implementado).** Hoje só existe uma
    taxa de marketplace única. Quem vende em mais de um canal (Shopee,
    Mercado Livre, Etsy etc., cada um com sua própria taxa) precisa trocar a
    configuração manualmente antes de cada orçamento. Ideia: um pequeno
    catálogo de canais (nome + taxa), mesmo padrão de Serviços, com o
    checkbox da aba Orçamento virando um dropdown de canal quando houver mais
    de um cadastrado.
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
  - **Regra de bump revisada (decisão 53, 2026-09-18):** de `0.1.0` até
    `1.8.0`, todo bump foi MINOR — nunca PATCH, mesmo em levas que eram só
    correção ou só documentação. Passa a valer: **PATCH** pra leva sem
    funcionalidade nova (só correção e/ou só documentação); **MINOR** pra
    leva com funcionalidade nova ou melhoria visível (regra completa em
    [development.md](development.md#versionamento)).
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
  Segunda leva (decisão 73, 2026-09-21, pedida pelo responsável do
  projeto via `/frontend-design`): números (preço, peso, tempo) em fonte
  monoespaçada em todo o app — Orçamento, Dashboard, Kanban, Histórico —
  pra ter uma leitura consistente de "instrumento de medição", já que
  produzir esses números é o motivo do app existir. Resultado do cálculo
  vira uma "nota" (valor cobrado em destaque no topo, custo/lucro/
  serviços como itens abaixo) em vez de uma pilha de texto do mesmo peso.
  Cartões de estatística do Dashboard trocam 4 blocos brancos com sombra
  por uma leitura em régua. Status do pedido ganha uma cor de progresso
  fria→quente (reaproveita `primary`/`secondary` já existentes, sem cores
  novas), visível como um ponto no Kanban e no seletor do Histórico.
  Feito: `ui/format/NumericText.kt`, `ui/theme/OrderStatusColor.kt`.
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
  - **Ranking de produto mais lucrativo (levantado em 2026-09-19, pesquisa de
    concorrentes, ainda não implementado).** Hoje o Dashboard só destaca o
    filamento mais *usado* (volume); adicionar um ranking por *lucro* (que
    produto/orçamento deu mais margem, não só mais volume) — os dois podem
    apontar pra peças diferentes. Reaproveita a mesma agregação de
    `QuoteReport` sobre o período já filtrado.

### Importar dados do slicer (G-code)

- [x] **Preencher peso de filamento e tempo de impressão a partir do G-code
  exportado pelo slicer, em vez de digitar os dois campos na mão** (decisão
  55). Levantado em 2026-09-19 (pesquisa de concorrentes — PrintQuote3D,
  projeto open source parecido, também prioriza isso antes de um parser de
  STL próprio). A maioria dos slicers (PrusaSlicer, Cura, Bambu Studio)
  grava o consumo de filamento e o tempo estimado como comentário no
  cabeçalho/rodapé do arquivo `.gcode` — ler esse texto (sem precisar
  interpretar a malha 3D nem desenhar nada) já cobre o essencial do que a
  Fase 2 abaixo tentaria estimar geometricamente, e com mais precisão (o
  slicer já considera suporte/purga, que uma estimativa por volume não
  considera). Os campos continuam editáveis manualmente depois de
  importados — é um atalho pra preencher, não uma trava. Não depende da
  Fase 1 (upload de STL) — funciona mesmo sem visualizador 3D. Feito
  (2026-09-20): `core/slicer/GCodeMetadataParser` (função pura, testada,
  cobre os formatos PrusaSlicer/Bambu Studio/OrcaSlicer e Cura, com soma de
  múltiplos extrusores), `platform/pickGCodeFile` (mesmo padrão de
  `pickImageFile`), botão "Preencher a partir do G-code" na tela de
  Orçamento (`QuoteViewModel.pickAndImportGCode`).
  - **Miniatura do modelo + desfazer importação (decisão 56, 2026-09-20):**
    fatiadores da família PrusaSlicer (PrusaSlicer, SuperSlicer, OrcaSlicer,
    Bambu Studio) embutem uma prévia renderizada do modelo no próprio
    G-code — o parser passou a extrair essa miniatura também
    (`GCodeMetadata.thumbnail`, pega a maior entre as várias resoluções
    presentes) e usá-la como a foto do orçamento, **só quando nenhuma foto
    já tiver sido escolhida** (não sobrescreve uma foto própria do
    usuário). Cura não embute miniatura em G-code puro, por isso fica sem
    esse extra nesse fatiador. Botão "Desfazer importação do G-code"
    aparece junto da mensagem de resultado — limpa comprimento/tempo e, se
    a foto atual também veio do G-code, remove ela também, caso o usuário
    desista do arquivo importado.
  - **Correção, tentativa 1 (decisão 57, 2026-09-20, insuficiente):** o
    diálogo de escolher o G-code mostrava todos os arquivos no Windows,
    ignorando o filtro de extensão. Tentativa de corrigir definindo o
    padrão wildcard (`*.gcode;*.gco;*.g`) em `FileDialog.file` não
    resolveu — o responsável do projeto reportou que o texto do wildcard
    ia parar na caixa de nome do arquivo, não num filtro de tipo de
    verdade, então a lista de arquivos continuava sem filtrar.
  - **Correção, tentativa 2 (decisão 58, 2026-09-20, revertida):** trocado
    `java.awt.FileDialog` por `javax.swing.JFileChooser` (com
    `FileNameExtensionFilter`) nos dois diálogos de escolher arquivo do
    app — G-code e foto —, que tem um combo real de "Files of type"
    respeitado em qualquer SO. Funcionava, mas o responsável do projeto
    não gostou do visual Swing (destoa do resto do app/SO) e preferiu
    manter o diálogo nativo mesmo sem filtro garantido no Windows.
  - **Decisão final (decisão 59, 2026-09-20):** revertido pro
    `java.awt.FileDialog` nativo original (decisão 55), sem o wildcard da
    tentativa 1. Filtro de tipo (`filenameFilter`) continua funcionando
    nativamente no Linux/macOS; no Windows, a limitação de não filtrar
    fica **aceita como está** — é só uma conveniência de busca, não
    impede escolher o arquivo certo manualmente.

### Visualização e análise de STL (funcionalidade grande, dividida em fases)

- [x] **Fase 1 — Upload de STL + visualizador 3D.** Anexar o arquivo STL do
  modelo ao orçamento (além da foto que já existe hoje). Envolve:
  - [x] **Anexar e guardar o arquivo STL no histórico** (decisão 61,
    2026-09-21), independente do parser/visualizador abaixo — pedido do
    responsável do projeto: organização de arquivo de modelo é uma dor real
    de quem vende impressão 3D (encontrar o STL de uma peça já vendida pra
    imprimir de novo pra outro cliente). Botão "Anexar arquivo STL
    (opcional)" na tela de Orçamento (`platform/pickStlFile`, mesmo padrão
    de `pickImageFile`/`pickGCodeFile`), persistido em
    `~/.3dreport/models/` (mesmo tratamento de `~/.3dreport/photos/`), com
    "Baixar STL" no Histórico. **Uso só interno** — nunca entra no PDF nem
    no copiar-colar, mesmo tratamento do link do modelo. Ainda **não** é
    usado pra visualização/cálculo — isso é o restante desta Fase 1,
    abaixo. Feito: `core/model/SavedQuote.stlFileName`,
    `data/QuoteHistoryRepository.stlBytes`.
    - **Evolução futura (fora de escopo por ora, só registrando a ideia,
      2026-09-21):** permitir guardar também o arquivo `.3mf` do projeto do
      fatiador junto com o orçamento (opcional) — o `.3mf` carrega as
      configurações de fatiamento usadas (perfil de impressora/filamento,
      suportes, orientação etc.), então recuperar um projeto salvo pra uma
      venda repetida da mesma peça pouparia reconfigurar tudo de novo no
      fatiador, não só reimprimir com configuração genérica a partir do
      STL puro. Mesmo tratamento de uso interno do STL/foto/link.
  - [x] **Parser de STL** (formato binário e ASCII) pra ler a malha de
    triângulos — vira a base de tudo que vem depois (fases 2 e 3). Feito
    (2026-09-21): `core/stl/StlParser.parseStl` (função pura, testada —
    binário e ASCII, inclusive o caso de um binário cujo cabeçalho começa
    com o texto "solid" por engano do exportador), `core/stl/{Vec3,StlMesh}`.
  - [x] **Visualizador 3D** dentro do app: carregar a malha, rotacionar/zoom,
    escolher um ângulo de câmera. **Decisão técnica resolvida (2026-09-20,
    decisão 61):** rasterizador simples desenhado no próprio `Canvas` do
    Compose (sem OpenGL/LWJGL nem WebView/three.js) — sombreamento plano por
    triângulo (normal geométrica · direção da câmera), ordenação pintor pra
    profundidade, câmera orbital (arrastar gira, scroll dá zoom). Escolhido
    por não somar nenhuma dependência nova nem inflar o instalador
    (WebView+three.js somaria 100+ MB por SO via JCEF; OpenGL nativo exigiria
    integrar um componente pesado do AWT dentro da janela do Compose,
    historicamente delicado). Feito (2026-09-21):
    `ui/viewer/Stl3DViewer`, aparece na tela de Orçamento assim que um STL é
    anexado. **Pan não implementado nesta primeira versão** (só orbitar e
    zoom) — detectar arrasto com botão direito/modificador de teclado
    dentro do Compose exige API de mais baixo nível que a usada aqui; fica
    pra uma iteração seguinte se fizer falta na prática. Culling de face
    traseira com fallback automático pra STL com normais invertidas (evita
    tela em branco nesse caso).
    - **Correções pós-teste com STL reais (decisão 62, 2026-09-21):**
      sentido do arrasto invertido corrigido (arrastar pra direita agora
      gira a peça pra direita); zoom com a roda do mouse não "vaza" mais
      pra rolagem da página por trás; visualizador envolvido num
      `OutlinedCard` pra deixar clara a área interativa; desempenho em
      malhas densas melhorado (normal/centroide de cada triângulo
      calculados uma vez na criação da malha, não a cada frame; `Path` de
      cada triângulo reaproveitado entre frames em vez de recriado a cada
      redesenho). Ainda pode ficar perceptível em malhas muito densas
      (centenas de milhares de triângulos, mais comuns em scans 3D do que
      em produtos de venda) — otimização adicional (ex.: nível de detalhe)
      fica pra se um caso real precisar.
    - **STL muito pesado trava o app ao tentar pré-visualizar (decisão 63,
      2026-09-21):** um STL com centenas de milhares/milhões de triângulos
      travava a interface (parse + render de tudo isso na thread principal,
      sem indicação de progresso). Corrigido com um limite heurístico de
      500 mil triângulos (`peekStlTriangleCount`, conta os triângulos sem
      montar a malha inteira — lê 4 bytes no caso binário): acima disso, o
      app **não tenta renderizar** e mostra um aviso, mas o STL continua
      sendo salvo/recuperável no Histórico normalmente. Limite ajustável
      se um caso real mostrar que está conservador ou generoso demais.
  - [x] **Exportar a visualização como imagem**: capturar o frame
    renderizado no ângulo/zoom escolhido e usar como foto do orçamento —
    reusa o campo de foto que já existe, sem campo novo no modelo de
    dados. Feito (2026-09-21): `Stl3DViewerState.captureSnapshot`
    (renderiza off-screen com `CanvasDrawScope`, mesmo ângulo da tela),
    `platform/encodeImageBitmapToPng`, botão "Capturar como foto do
    orçamento".
- [ ] **Fase 2 — Estimativa automática de peso/tempo a partir do STL.** Hoje
  o criador digita comprimento de filamento e tempo de impressão na mão. Com
  a malha já carregada (fase 1), dá pra calcular o **volume** da peça
  geometricamente e, com um "perfil de impressão" configurável (altura de
  camada, % de preenchimento, velocidade média — por impressora ou global),
  **sugerir** peso e tempo estimados; o criador continua podendo ajustar na
  mão (é um ponto de partida, não substitui o fatiador real, que considera
  suporte/purga/etc.). Depende só da fase 1. **Prioridade reavaliada
  (2026-09-19):** com o item "Importar dados do slicer" acima, boa parte do
  valor desta fase (peso/tempo sem digitar na mão) já fica coberta com bem
  menos esforço e mais precisão — avaliar se ainda compensa implementar a
  estimativa geométrica própria antes de investir nela. **Pulada por ora
  (decisão 66, 2026-09-21):** o responsável do projeto decidiu ir direto
  pra Fase 3 (abaixo), que não depende da Fase 2 e cobre um problema que a
  importação de G-code não resolve (nível de dificuldade, não peso/tempo)
  — volta pro topo da fila só se um pedido concreto justificar o esforço
  de estimar peso/tempo pela geometria mesmo já tendo o G-code.
- [x] **Fase 3 — Análise de complexidade / nível de dificuldade** (decisão
  66, 2026-09-21; Fase 2 pulada de propósito — dependência resolvida pelo
  item "Importar dados do slicer" acima, ver nota lá).
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
    relevos/reentrâncias, um valor alto, pro mesmo volume). **Ajuste na
    implementação:** razão bruta cai com o tamanho do objeto pra qualquer
    forma (um cubo pequeno tem razão maior que um cubo grande, sem ser
    mais "complexo") — usada em vez disso a razão contra a área de uma
    **esfera do mesmo volume** (quociente isoperimétrico, sempre ≥ 1,
    independente de escala), que mede complexidade de forma sem confundir
    com tamanho.
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
    de suporte gerado, e — combinando com a fase 2, se algum dia for
    implementada — um "custo extra sugerido" automático em cima do nível
    de dificuldade.
  - **Não persistido no Histórico por ora** — o nível de dificuldade e as
    medidas só aparecem ao vivo na tela de Orçamento, enquanto o STL está
    carregado (decisão de precificação já fica registrada no valor de
    venda salvo; a análise em si é descartada depois). Guardar isso junto
    do `SavedQuote` fica de fora por ora, sem pedido concreto pra isso
    ainda — fácil de adicionar depois se fizer falta.
  - Feito (2026-09-21): `core/stl/StlAnalyzer` (função pura, testada com
    um tetraedro verificado à mão — área/volume/manifold/componentes — e
    casos isolados de overhang), exibido na tela de Orçamento logo abaixo
    do visualizador 3D (mesma condição de "não muito pesado pra
    renderizar").

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
- [x] **Fila de impressão / agenda da impressora** (decisão 68, 2026-09-21).
  Visão de quanto tempo cada impressora cadastrada vai ficar ocupada (soma
  dos orçamentos com status "Em impressão", ver item de status acima) —
  ajuda a prometer prazo com mais segurança pro cliente. Depende do item
  "Status do pedido". Feito: `core/report/PrintQueueReport` (função pura,
  mesmo estilo do `QuoteReport`/`PricingCalculator` — soma o tempo de
  impressão dos orçamentos "Em impressão" que usaram cada impressora,
  `Quote.printerId`), exibido direto na aba Impressoras, embaixo de cada
  impressora ("Fila: X h em N pedido(s) 'Em impressão'" ou "Sem pedidos em
  impressão no momento"). **Não considera o filtro de período do
  Dashboard** — é sobre o estado atual, não uma janela de tempo passada.
  Orçamentos salvos antes de `Quote.printerId` existir (decisão 64) não
  têm como saber qual impressora usaram, então não entram em nenhuma fila.
- [ ] **Custo de falha real acumulado.** Hoje a taxa de falha é um % fixo
  estimado nas Configurações. Permitir marcar um orçamento/impressão como
  "falhou" (com motivo opcional) e, com histórico suficiente, sugerir um %
  de falha calibrado com dado real do próprio criador em vez de um chute
  inicial.
- [ ] **Sinal/pagamento parcial no orçamento** (levantado em 2026-09-19,
  pesquisa de comunidade — prática comum em encomenda sob medida em fóruns
  de venda como Etsy: cobrar 25–50% adiantado, saldo na entrega). Complementa
  o `OrderStatus` que já existe: registrar se o orçamento tem sinal
  configurado, o valor/percentual do sinal e se já foi pago, sem virar um
  controle financeiro completo (não é objetivo do app virar um sistema de
  contas a receber).
- [ ] **Lembrete de manutenção de impressora por horas acumuladas**
  (levantado em 2026-09-19, pesquisa de concorrentes — FoxTrack tem
  agendamento de manutenção). `PrinterProfile` já registra o custo de
  manutenção da máquina; falta um aviso quando o total de horas impressas
  (somadas pelos orçamentos daquela impressora) passar de um limiar
  configurável, lembrando de fazer a manutenção preventiva.
- [ ] **Histórico de manutenção/alterações por impressora** (levantado pelo
  responsável do projeto, 2026-09-21). Um campo discreto no cadastro de
  cada impressora (ex.: um botão/link "Histórico de manutenção" na linha
  da impressora, ao lado de "Editar"/"Excluir") que abre uma janela com uma
  lista dos itens já cadastrados: o que foi trocado/ajustado/consertado e
  quando (ex.: "Trocado bico 0,4mm — 12/03/2026", "Nivelada a mesa —
  20/01/2026", "Trocada correia do eixo X — 05/12/2025"). Serve só como
  registro/lembrete pro próprio criador — não afeta nenhum cálculo (não é
  o mesmo item que o "Lembrete de manutenção por horas" acima, que é um
  aviso automático baseado em uso; este aqui é um diário manual de
  manutenção, tipo um "log" da impressora). Complementar ao item acima —
  os dois podem conviver: um lembra "já está na hora de mexer", o outro
  registra "o que já foi mexido". Detalhes de implementação (ex.: se cada
  entrada tem só texto+data ou também um tipo/categoria) ficam pra quando
  for de fato implementar.

### Presets de cadastro (impressoras e filamentos)

Ideia trazida pelo responsável do projeto (2026-09-18): reduzir o trabalho de
digitação inicial em Impressoras e Filamentos oferecendo listas pré-prontas
pra escolher, no mesmo espírito do fatiador OrcaSlicer (que já vem com um
catálogo enorme de impressoras/filamentos pra selecionar em vez de cadastrar
do zero). Em ambos os casos, **escolher da lista nunca é obrigatório** — quem
não achar o que precisa continua cadastrando manualmente, exatamente como
funciona hoje.

- [x] **Preset de impressoras.** Lista embutida no app (sem rede/download,
  só bundle local) com impressoras populares — a pessoa que vende impressão
  3D só marca quais possui, e essas aparecem na tela de Orçamento; ela não
  precisa ter cadastrado a impressora do zero. Um preset preenche só
  `PrinterProfile.name` e `printerPowerWatts` — `maintenanceCostPerHour` e
  `machineInvestment` (preço pago, prazo de payback, dias/horas de uso)
  continuam manuais, porque dependem de quanto *aquele* vendedor pagou e de
  como usa a própria máquina, não têm como vir de um preset genérico. Ao
  escolher um preset, o watts vem preenchido mas continua editável (a
  unidade específica do vendedor pode variar do catálogo).
  - **Levantamento de consumo elétrico (pesquisado em 2026-09-18, fonte
    manual/spec oficial quando encontrado):** confiança alta/média-alta em
    Bambu Lab P1S (1000 W @220V / 350 W @110V, spec oficial — valor de PSU,
    não consumo médio), Creality Ender 3 V3 SE (PSU 350 W, ~125 W típico
    imprimindo), Creality K1 (PSU 350 W, 24V), Elegoo Neptune 4 (PSU 400 W;
    fabricante recomenda não passar de ~320 W reais) e Prusa MK4 (13 W
    ocioso, 80–130 W imprimindo, picos >325 W ao aquecer — sem um "watt
    nominal" único de fábrica). Confiança média, só fonte secundária/fórum
    (sem datasheet único): Bambu Lab A1 mini (~57 W médio), Bambu Lab A1
    (~95 W médio, PSU até 350 W), Bambu Lab X1-Carbon (~100–200 W
    imprimindo, pico ~400 W), Creality Ender 3 V2 (PSU 270–360 W, fontes
    conflitantes), Creality CR-10 original (PSU 270 W), Anycubic Kobra 2
    (~350 W citado, sem datasheet confirmado).
  - **Marcas brasileiras (Voolt, GTMax3D, 3D Lab, Cliever) confirmadas como
    reais e vendidas no Brasil, mas nenhuma publica o consumo em watts** nas
    páginas de produto/spec (a página de specs da GTMax3D nem tem essa
    coluna). Pra incluir essas marcas — importantes pro público-alvo
    brasileiro — alguém precisa abrir o manual/etiqueta elétrica de cada
    modelo, ou contatar o fabricante direto; não dá pra inventar o número.
    Escopo de estreia sugerido: lançar só com os modelos de watts
    confirmados acima, completar as marcas nacionais assim que o dado
    elétrico for levantado manualmente (item fica pra uma leva seguinte, não
    trava o lançamento da funcionalidade).
  - **Estrutura e como adicionar impressoras novas.** Documentar em
    [development.md](development.md) o formato do arquivo de presets e o
    passo a passo pra propor uma impressora nova (útil pra manter a lista
    crescendo em versões futuras, inclusive via contribuição externa de
    quem tiver a impressora e o manual em mãos). Como referência de
    organização — não pra copiar a complexidade toda, já que aqui bastam 2
    campos por impressora, bem menos que um perfil de fatiamento completo —
    o OrcaSlicer guarda um preset por fabricante em
    `resources/profiles/<fabricante>/`, um arquivo por modelo.
  - **Imagens das impressoras — decisão em aberto, não assumir que está
    liberado.** Usar fotos reais de produto (site oficial/kit de imprensa
    do fabricante) esbarra em licenciamento: kit de imprensa costuma cobrir
    uso editorial, não redistribuição embutida dentro de outro produto —
    ser Apache 2.0 não muda a licença da imagem em si, que é do fabricante.
    Alternativas mais seguras a avaliar antes de implementar: (a) ícone
    genérico por formato de impressora (cartesiana, CoreXY etc.), ou (b)
    ilustração própria, desenhada pro projeto (mesmo caminho já usado pro
    ícone do app, decisão 42 — sem depender de imagem de terceiros). Preciso
    decidir isso antes de qualquer imagem entrar no repositório.
  - **Feito (2026-09-19, decisão 54):** `ui/printers/PrinterPresetDialog`
    (botão "Escolher da lista" em Impressoras), `ui/printers/PrinterPresets.kt`.
    Fotos de produto ficaram de fora, como já registrado acima (sem decisão
    de licenciamento tomada) — sem ícone/ilustração também, ver observação
    de ícones no item de filamentos abaixo. Depois de testar a primeira
    versão (5 impressoras, consumo médio estimado), o responsável do projeto
    pediu pra trocar de critério: catálogo ampliado pra **39 modelos** das 7
    principais marcas (Bambu Lab, Creality, Prusa, Elegoo, Anycubic,
    Flashforge, Snapmaker), usando a **potência máxima/nominal do manual ou
    ficha técnica oficial** em vez de uma estimativa de consumo médio — mais
    fácil de sourciar de forma confiável (a maioria das marcas não publica
    consumo médio, só o nominal), à custa de poder superestimar o custo de
    energia real se a pessoa não ajustar (o diálogo deixa isso explícito no
    texto). Ficaram de fora modelos sem número oficial confiável (Anycubic
    Vyper, Flashforge Creator Pro/Guider II, Anycubic Kobra original, CR-10
    original) — não inventado. Marcas brasileiras continuam sem entrar por
    falta de dado elétrico publicado (mesma limitação já registrada acima).
    Com o catálogo maior, o diálogo ganhou um **campo de busca** (marca ou
    modelo, mesmo filtro `contains`/case-insensitive do Histórico).
- [x] **Preset de marcas de filamento + tipo de material com densidade
  automática.** Hoje `Filament` (`core/model/Filament.kt`) só tem `brand`
  (texto livre, opcional) e `densityGPerCm3` digitada manualmente sem
  nenhuma sugestão — não existe campo de "tipo" (PLA/PETG/etc.) hoje. Duas
  melhorias, que podem entrar juntas ou em etapas:
  - **Lista de marcas pré-cadastradas** (autocomplete/dropdown, com opção de
    digitar uma marca nova de qualquer jeito, igual ao preset de
    impressoras). Levantamento 2026-09-18 — internacionais confirmadas:
    eSUN, Polymaker, Prusament, Overture, SUNLU, Hatchbox, ColorFabb, Bambu
    Lab, Fillamentum, Elegoo, Inland. Brasileiras confirmadas: Voolt3D,
    GTMax3D, 3D Lab, Cliever, 3D Fila (achada via listagem de revenda, sem
    checar site próprio direto); Masterprint/Stllix/Conjure apareceram só
    numa resenha de blog (2026) — confiança mais baixa, revisar antes de
    entrar como marca "oficial" da lista de estreia.
  - **Tipo de material com densidade padrão sugerida, sempre editável.**
    Novo campo (não obrigatório de imediato, pra não quebrar filamentos já
    salvos sem tipo definido) com densidade padrão pesquisada em
    2026-09-18:
    - Tipos "puros" (densidade confiável, faixa estreita): PLA 1,24 · PETG
      1,27 · ABS 1,04 · ASA 1,07 · TPU 1,21 · Nylon/PA 1,14 · PC 1,20 · HIPS
      1,04 · PVA 1,23.
    - Tipos compostos/carregados (densidade varia bastante por fabricante e
      % de carga — sugerir um valor representativo, deixando claro na UI
      que costuma precisar ajuste): PLA-CF ≈1,25 (varia 1,22–1,30) ·
      PETG-CF/PET-CF ≈1,28 (varia 1,26–1,32) · madeira (wood-fill)
      ≈1,15–1,25.
    - PA-CF/PAHT-CF e PC-CF: nenhum valor confiável encontrado — melhor não
      sugerir densidade nenhuma nesses dois (campo em branco, preenchido à
      mão, igual já funciona hoje pra qualquer tipo fora da lista).
    - Metal-fill: variação enorme (1,8 a 3,5, dependendo do metal — cobre,
      latão, bronze, aço) — não faz sentido um padrão único; se algum dia
      entrar na lista, precisa ser por metal específico, nunca um
      "metal-fill" genérico.
    - Tipo escolhido preenche a densidade automaticamente, mas o campo
      continua editável (pedido explícito: "a pessoa pode querer modificar
      manualmente depois"). Tipo fora da lista: nome e densidade digitados
      à mão, do jeito que já funciona hoje.
  - **Diferença pro item já existente "Import/export de catálogo de
    filamentos entre criadores"** (seção Integrações, abaixo): são coisas
    complementares, não a mesma coisa. Aquele item é sobre a **comunidade**
    compartilhar catálogos completos entre si (arquivo exportado por um
    criador, importado por outro); este aqui é o app já vir com uma lista
    base de marcas/tipos comuns, sem depender de ninguém ter compartilhado
    nada antes. Os dois podem conviver.
  - **Estrutura e como adicionar marcas/tipos novos:** mesma lógica do
    preset de impressoras — o OrcaSlicer organiza presets de filamento em
    `resources/profiles/<fabricante>/filament/`, um arquivo por perfil, com
    nome de arquivo seguindo um padrão (`filament_<marca>_<nome>@<vendor>
    <variante>.json`). 3DReport precisa de bem menos campo por preset (só
    marca/tipo/densidade padrão), mas a ideia de pasta por marca + arquivo
    por perfil serve de referência pro formato a documentar em
    [development.md](development.md).
  - **Feito (2026-09-19, decisão 54):** `core/model/Filament.materialType`
    (texto livre, mesmo tratamento de `brand`); chips de "Tipo de material"
    e de marca em `ui/filaments/FilamentListScreen`, dados em
    `ui/filaments/FilamentPresets.kt`. Reusa o chip de texto já usado nas
    cores de filamento em vez de um dropdown novo, pra manter uma única
    linguagem de interação no formulário — nenhum ícone adicionado (ver
    observação abaixo).
  - **Ícones — avaliado e descartado por ora (decisão 54, 2026-09-19):** o
    responsável do projeto perguntou se ícones deixariam as telas de
    Filamentos/Impressoras mais bonitas. Avaliação: as 3 telas de catálogo
    (Filamentos/Impressoras/Serviços) não usam nenhum ícone gráfico hoje —
    introduzir um só nas duas telas novas criaria inconsistência com
    Serviços (que já está no backlog técnico pra uma futura unificação das
    3 telas, ver "Observações técnicas" no fim deste documento) e exigiria
    uma dependência nova (nenhuma lib de ícones no projeto) só por um ganho
    visual marginal. Revisitar junto da unificação das telas de catálogo,
    se/quando ela acontecer.

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
- [ ] **Assinatura discreta "Gerado com 3DReport" nos PDFs exportados.**
  Ideia trazida pelo responsável do projeto (2026-09-17): já que o projeto
  vive só de doação voluntária, sem verba de marketing (decisão 15), cada
  PDF gerado (orçamento individual, múltiplos orçamentos e catálogo —
  `platform/QuotePdfExporter`) ganharia uma linha pequena e discreta, num
  canto do documento, com algo como "Gerado com 3DReport" — hyperlink pro
  repositório (ou pro site institucional, quando existir) e uma referência
  curta ao link de apoio (Buy Me a Coffee). Cada orçamento/catálogo enviado
  pro cliente de um vendedor vira uma chance de outro criador conhecer o
  app — o mesmo tipo de divulgação orgânica de "Feito com X" que ferramentas
  gratuitas usam, só que aqui sem versão paga pra "tirar a marca".
  - **Não é a marca d'água/rodapé do vendedor** (`BrandingSettings`, decisões
    20-22) — aquele espaço já é do vendedor, pra construir a marca *dele*
    pro cliente final. Essa assinatura precisa ser um elemento visualmente
    distinto e menor (ex.: canto inferior, fonte pequena e discreta), sem
    competir com a identidade que o vendedor está tentando passar no
    documento que ele manda pro cliente dele — o app continua sendo uma
    ferramenta a serviço do vendedor, não uma vitrine pro 3DReport.
  - **Ligada por padrão, mas configurável.** Sem edição paga pra remover a
    marca (decisão 15), a única alavanca de alcance aqui é vir ligada por
    padrão — mas precisa dar pra desligar em Configurações (checkbox, mesmo
    espírito de `showWatermark`/`showFooter`), pra quem prefira entregar um
    documento sem nenhuma referência a terceiros. Perde algo de alcance,
    ganha em confiança de quem usa a ferramenta a longo prazo.
  - **Escopo: só PDF**, não no copiar/colar (decisão 19) — aquele formato é
    pra colar direto numa conversa informal de WhatsApp/marketplace, onde um
    link a mais soa mais invasivo do que discreto. Reavaliar só se fizer
    sentido depois.
  - **Dependências resolvidas (decisão 50):** o repositório e o site
    institucional em GitHub Pages (seção 2) já existem e estão públicos, então
    o link pode apontar direto pro site (`https://mateustoin.github.io/3DReport/`),
    com o repositório como alternativa. Item segue no backlog só pela
    implementação em si (ainda não entrou no código).

### Integrações

- [ ] **Exportar histórico pra CSV/Excel.** Pra quem já usa planilha
  (Excel/Google Sheets) como contabilidade paralela do negócio.
- [ ] **Import/export de catálogo de filamentos entre criadores.** Arquivo
  (JSON/CSV) com perfis de filamentos populares (ex. marcas/linhas comuns no
  Brasil) que a comunidade possa compartilhar/importar, evitando cadastro
  manual do zero a cada filamento novo. Complementar ao **preset de marcas de
  filamento** (seção "Produção e precificação" acima) — aquele é uma lista
  base já embutida no app; este é a comunidade compartilhando catálogos
  próprios entre si por fora dela. **Compatibilidade com Spoolman (levantado
  em 2026-09-19, pesquisa de concorrentes):** o formato/API do
  [Spoolman](https://github.com/Donkie/Spoolman) virou o padrão de facto
  open source pra banco de dados de carretel de filamento — vale avaliar ler
  esse formato em vez de inventar um próprio. **Baixa prioridade — depende
  de um serviço externo rodando** (o Spoolman roda como servidor à parte,
  fora do escopo 100% local/arquivo do app hoje); fica pra depois do que é
  local.
- [ ] **Portal de acompanhamento pro cliente** (levantado em 2026-09-19,
  pesquisa de concorrentes — visto na Printforge). Um link que o cliente
  acessa pra ver o status do próprio pedido sem precisar perguntar. **Baixa
  prioridade — depende de infraestrutura online** (hospedagem/backend), que
  o app não tem hoje (é 100% local/desktop); só cabe se o projeto um dia
  ganhar um componente online.
- [ ] **Assistente de IA pra sugerir preço/descrição do produto** (levantado
  em 2026-09-19, pesquisa de concorrentes — visto na Printforge). **Baixa
  prioridade — depende de uma API de IA externa**, incompatível com o app
  ser 100% local hoje; fica pra depois do que é local.

### UX extras

- [x] **Fotos WebP não aparecem no PDF exportado (bug)** (decisão 65,
  2026-09-21). Reportado pelo responsável do projeto (2026-09-17): a
  miniatura da foto aparece normalmente no app, mas ela some do PDF
  exportado (orçamento individual e catálogo) quando o upload original
  foi um arquivo `.webp` — causa raiz: `platform/QuotePdfExporter.jvm.kt`
  decodificava a foto via `javax.imageio.ImageIO.read(...)`, que não lê
  WebP sem plugin e retorna `null` em silêncio (sem exceção), então o
  bloco que desenha a foto era pulado sem erro nenhum aparecer. Corrigido
  com a opção (A) sem dependência nova: a exportação passa a decodificar
  via `decodeImageBitmap` (Skia, mesmo decoder já usado na miniatura do
  app) + `ImageBitmap.toAwtImage()` (extensão do Compose Desktop) pra
  virar o `BufferedImage` que o PDFBox espera — miniatura e PDF nunca mais
  divergem em formato suportado. Escolhida em vez da opção (B) porque
  corrige retroativamente **fotos já salvas** antes dessa correção (o
  ajuste é só na leitura, não na gravação — nenhuma migração necessária),
  e reaproveita infraestrutura Skia que o app já tinha (usada agora
  também na captura do visualizador 3D). Feito:
  `platform/QuotePdfExporter.decodePhotoAsBufferedImage`, teste de
  regressão codificando um WebP de verdade via Skia e conferindo que a
  imagem foi embutida no PDF (`QuotePdfExporterTest`).
- [x] **Atalhos de teclado** (decisão 43). `Ctrl`/`Cmd+1` a `7` pula direto
  pra cada aba; `Ctrl`/`Cmd+S` salva o orçamento atual e `Ctrl`/`Cmd+N`
  limpa a tela de Orçamento pra começar um novo (os dois só na aba
  Orçamento); `Esc` cancela o formulário de adicionar/editar aberto em
  Filamentos/Impressoras/Serviços. Funcionam com `Ctrl` (Windows/Linux) ou
  `Cmd` (macOS) indistintamente. Listados no diálogo de Ajuda. Feito
  (2026-09-17): `App.kt` (`onPreviewKeyEvent` no `Surface` raiz),
  `QuoteViewModel.saveCurrentQuote`/`resetForm`.
- [x] **Formulário de salvar (nome, foto, STL, link, cliente) sempre
  visível na aba Orçamento** (decisão 63, 2026-09-21) — antes só aparecia
  depois de um cálculo válido, o que impedia anexar STL/foto antes de
  preencher filamento/impressora/comprimento/tempo. Só o botão "Salvar
  orçamento" continua exigindo cálculo válido pra habilitar.
- [x] **Editar um orçamento já salvo** (decisão 64, 2026-09-21) — pedido do
  responsável do projeto: refazer o orçamento inteiro do zero por causa de
  um erro de digitação era uma UX ruim. Botão "Editar" no Histórico reabre
  o orçamento na aba Orçamento (`QuoteViewModel.loadForEditing`), com tudo
  repopulado (filamento/cor/impressora/comprimento/tempo/serviços/
  marketplace/nome/foto/STL/link/cliente); salvar de novo atualiza o mesmo
  registro (`QuoteHistoryRepository.update`) em vez de criar um novo —
  **mantém a data de criação original**, e marca discretamente "· Editado
  em DD/MM/AAAA HH:mm" ao lado dela no Histórico. `Quote` ganhou
  `printerId`/`printerName` (não existia antes — sem isso não dava pra
  restaurar a impressora usada ao editar).
  Correção pós-teste (decisão 72, 2026-09-21): editar levava direto pra
  aba Orçamento, e a única forma de desistir era rolar até o fim do
  formulário achar "Cancelar edição" (ou acabar salvando sem querer) —
  ruim pra quem só queria começar um orçamento novo. `EditQuoteDialog`
  passa a abrir o mesmo formulário (reaproveita `QuoteScreen` inteiro)
  num diálogo sobre o Histórico, com "Cancelar edição" sempre visível no
  topo; Duplicar continua indo pra aba Orçamento (nada é sobrescrito ali,
  então o risco que motivou essa mudança não se aplica).
- [ ] **Onboarding na primeira execução.** Assistente curto guiando o
  cadastro da primeira impressora/filamento/margem, em vez de abrir numa
  tela vazia sem nenhum dado cadastrado. Baixa prioridade — fica pra
  depois.
- [x] **Duplicar orçamento** (decisão 69, 2026-09-21). Pedido do
  responsável do projeto (2026-09-17): vender a mesma peça, com os mesmos
  parâmetros, pra outra pessoa antes exigia refazer o orçamento do zero.
  Botão "Duplicar" no Histórico (ao lado de "Editar") copia tudo de um
  `SavedQuote` (filamento/cor/impressora/comprimento/tempo/serviços/
  marketplace/nome/foto/STL/link/cliente) pra um rascunho na aba Orçamento,
  pra revisar (nome/cliente/contato, tipicamente) e salvar pro cliente
  novo. As duas regras já definidas foram implementadas como pedido:
  - **Data/hora do duplicado é a do momento da duplicação**, status volta
    a `ORCADO` — implementado reaproveitando `QuoteViewModel.loadForEditing`
    (decisão 64) sem marcar `editingQuoteId`: "Salvar" cai no caminho de
    criar um orçamento novo (`QuoteHistoryRepository.save`, não `update`),
    com `savedAtEpochMillis`/status frescos.
  - **Se a foto/STL não mudar, reaproveita o mesmo arquivo** — não duplica
    o arquivo em disco. `save`/`update` ganharam `photoReferenceFileName`/
    `stlReferenceFileName` (opcionais): quando presentes, o repositório usa
    esse nome de arquivo existente direto, sem regravar; `delete` (e a
    limpeza durante `update`) passam a checar se **outro** `SavedQuote`
    ainda referencia aquele nome antes de apagar o arquivo, pra não quebrar
    um duplicado que ainda depende dele. Esse mesmo mecanismo passou a
    beneficiar edição também (decisão 64): editar sem trocar a foto/STL não
    regrava mais o arquivo à toa.
- [x] **Quadro Kanban de pedidos** (decisão 70, 2026-09-21; levantado em
  2026-09-19, pesquisa de concorrentes — FoxTrack e Printforge usam isso
  como visão principal). Visão alternativa ao Histórico em lista: colunas
  por `OrderStatus` (Orçado/Aprovado/Em impressão/Pronto/Entregue),
  arrastando o card do orçamento entre colunas pra mudar o status — mais
  fácil de enxergar volume quando há vários pedidos simultâneos do que o
  dropdown por linha que já existe na lista. Não substitui a lista/filtro
  do Histórico: é um alternador "Lista"/"Kanban" **dentro** da própria aba
  Histórico (não uma 8ª aba nova) — os dois modos leem o mesmo
  `savedQuotes`, busca/período continuam valendo nos dois; o filtro de
  status especificamente some no modo Kanban (as colunas já são a
  organização por status, filtrar deixaria colunas vazias sem explicação).
  Feito: `ui/history/KanbanBoard.kt` — arrasto de verdade via
  `pointerInput`/`detectDragGestures`, usando `boundsInWindow()` de cada
  coluna como referência comum pra decidir sobre qual coluna o card foi
  solto. Cada card também tem um menu "⋮" (Editar/Duplicar/Excluir,
  reaproveitando os mesmos callbacks da lista) como caminho alternativo
  caso o arrasto não seja preciso o bastante num mouse/tela específico —
  os dois caminhos levam à mesma mudança de status. Sem scroll vertical
  próprio por coluna (reaproveita o scroll da página, mais simples).
  Correções pós-teste (decisão 71, 2026-09-21): `boundsInWindow()` é
  recortado pela área visível dos ancestrais, então soltar o card fora da
  parte da coluna visível na tela (rolada pra fora pelo scroll por trás)
  não contava — trocado por `positionInWindow()` + tamanho medido, sem
  recorte. Adicionado destaque visual na coluna sob o ponteiro durante o
  arrasto (indicando onde o card cairia) e miniatura da foto no topo do
  card, quando o orçamento tiver uma.
  Mais correções pós-teste (decisão 72, 2026-09-21): soltar num espaço
  vazio de uma coluna (abaixo do último card, ou em qualquer parte de uma
  coluna sem itens) ainda não funcionava — cada coluna só media a altura
  do próprio conteúdo, então o espaço "vazio" visual não fazia parte do
  retângulo conhecido pra hit-test. Corrigido esticando todas as colunas
  até a altura da mais alta (`Row(Modifier.height(IntrinsicSize.Max))` +
  `fillMaxHeight()` por coluna). A miniatura de foto também deixou os
  cards grandes demais — trocada de um banner no topo (largura total,
  100dp de altura) pra uma miniatura pequena (48dp) ao lado do texto,
  card bem mais compacto.
- [ ] **Idioma da interface configurável** (levantado em 2026-09-19, pesquisa
  de concorrentes — apps internacionais atendem público global). Hoje a UI é
  fixa em português. Baixa prioridade dado o foco atual no mercado
  brasileiro — mas é uma funcionalidade 100% local (só strings/tradução, sem
  depender de nenhum serviço externo), diferente dos outros itens adiados
  por dependerem de nuvem.

## 2. Site (GitHub Pages) — divulgação e instruções de uso

- [x] **Site institucional em GitHub Pages** (decisão 49). Landing page
  (`site/index.html`) com funcionalidades, fórmula de cálculo resumida,
  instalação por SO, FAQ (com dados estruturados `FAQPage`) e apoio; manual
  rápido em página própria (`site/manual.html`, dados estruturados `HowTo`).
  Meta tags de SEO (title/description por página, Open Graph, Twitter Card,
  `SoftwareApplication` JSON-LD), `sitemap.xml`, `robots.txt` e `404.html`
  personalizado. Conteúdo em português voltado a quem busca "orçamento
  impressão 3D", "calculadora de preço impressão 3D" e termos correlatos.
  Feito (2026-09-17): `.github/workflows/pages.yml` publica `site/` a cada
  push em `main` que muda a pasta. **No ar** desde que o repositório ficou
  público (decisão 50): https://mateustoin.github.io/3DReport/.
  - **Reforço de SEO pra "venda de impressão 3D" (decisão 51, 2026-09-17):**
    além de "orçamento", title/meta description/Open Graph/JSON-LD passaram a
    cobrir "venda"/"vender impressão 3D" também. Nova seção "Do orçamento à
    venda concluída" expõe status do pedido, cliente vinculado e dashboard de
    vendas (funcionalidades que já existiam no app, agora visíveis no site);
    novo item de FAQ e um 7º passo no manual ("Acompanhe a venda até a
    entrega"). Tópicos `venda-impressao-3d`/`vendas` adicionados ao
    repositório GitHub pelo mesmo motivo.
  - **Screenshots reais adicionadas (decisão 52, 2026-09-17):** os 3
    placeholders (`orcamento.png`, `historico.png`, `pdf.png`, 1280×800) foram
    substituídos por capturas reais do app em `site/assets/screenshots/`
    (também usadas no README) — `PLACEHOLDER.md` removido. `og:image`/
    `twitter:image` do site (antes o ícone do app) passaram a usar
    `orcamento.png`, com `twitter:card` mudado pra `summary_large_image`
    (prévia grande ao compartilhar o link, em vez de miniatura). Ainda vale um
    banner dedicado (1200×630, nome do app + composição de telas) no lugar da
    screenshot crua, se quiser refinar mais a prévia de compartilhamento —
    ideia registrada, não crítica.

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

Adiantado pra preparar o repositório antes de ficar público (decisão 49),
já que funciona independente da visibilidade do repo (GitHub Actions roda em
repositório privado também). O repositório ficou público em 2026-09-17
(decisão 50).

- [x] **CI no GitHub Actions.** `.github/workflows/ci.yml` roda
  `./gradlew build` (compila + testa `core` e `composeApp`) a cada push/PR em
  `main`. Feito (2026-09-17).
- [x] **CONTRIBUTING.md.** Feito (2026-09-17): setup, fluxo de PR, mensagens
  de commit (Conventional Commits) e como propor mudanças, complementando
  [docs/development.md](development.md) (que continua focado em você mesmo).
- [x] **Templates de issue/PR.** Feito (2026-09-17):
  `.github/ISSUE_TEMPLATE/{bug_report,feature_request}.yml` +
  `.github/PULL_REQUEST_TEMPLATE.md`.
- [x] **Badges no README.** Build (CI), release, licença (Apache 2.0) e o
  botão de apoio (Buy Me a Coffee) já linkado. Feito (2026-09-17).
- [ ] **CODE_OF_CONDUCT.md.** Avaliado e deixado de fora por ora (2026-09-17)
  — mais útil se/quando surgir uma comunidade de contribuidores ativa.

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
