# Roadmap: Plano de evolução 1 (histórico)

> Parte do [roadmap](../roadmap.md). Fica aqui o plano das levas 0 a 9, quase todo
> feito. O backlog vivo está no roadmap principal.

## Plano de evolução 1 (revisão de 2026-09-22, histórico)

> **Histórico.** Quase tudo deste plano foi feito. Os itens que ficaram abertos
> foram reagendados no Plano 2, no roadmap principal, e cada um diz em qual leva está; o texto
> original continua aqui.


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
→ UI/UX → produtos sem venda → crescimento → apostas maiores.

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
  **Revisado na decisão 93 (2026-09-24):** a hora de trabalho deixou de
  desligar a taxa de acabamento, porque configurar a hora com os minutos
  ainda vazios fazia o preço cair. Hoje as duas somam, e quem conta o
  acabamento nos minutos deixa a taxa em 0.
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
- [x] **Minutos de trabalho embutidos em cada serviço cadastrado** *Feito na Leva 10 do Plano 2 (decisão 123).*
  (separado
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
- [x] **Serviço cobrado por pedido em vez de por peça** (decisão 92,
  2026-09-24). Feito junto com o valor do serviço digitado em cada
  orçamento: o preço do cadastro virou só uma sugestão, e cada serviço tem
  um padrão "por peça / uma vez no pedido", ajustável no orçamento quando a
  quantidade passa de 1. Texto original abaixo. Hoje todo serviço marcado multiplica pela
  quantidade, porque pintura/lixamento/embalagem são trabalho peça a peça.
  Um serviço como "entrega" ou "projeto/modelagem" deveria ser cobrado uma
  vez pelo pedido. Se aparecer caso real, `Service` ganha um marcador de
  "por peça / por pedido" e a tela de Serviços um seletor. Não implementado
  por ora pra não somar campo e tela antes de existir a necessidade.

### Leva 3 — O preço final correto (deduções e acréscimos da venda)

- [x] **Canais de venda com taxa própria** (decisão 78, 2026-09-22). Item já registrado (ver
  "Taxa de marketplace" na seção 1) — agendado aqui, porque é a base pros
  dois itens seguintes: em vez de uma taxa única em Configurações, um
  catálogo de canais (nome + taxa), no mesmo padrão de Serviços.
- [x] **Imposto** (decisão 78, 2026-09-22). A taxa de pagamento **não** virou campo separado: foi absorvida pelo canal de venda, porque somar a taxa do marketplace com a do cartão cobraria em dobro (o marketplace já embute o processamento). Texto original abaixo. Faltam duas deduções que todo
  vendedor brasileiro sente: imposto (MEI/Simples) e taxa de recebimento
  (maquininha ou link de pagamento, tipicamente ~4%; Pix, zero). Mesma
  mecânica já implementada pra marketplace (decisão 26: o preço de venda
  sobe o suficiente pra margem real não mudar), aplicada a mais duas
  parcelas. Junto com os canais de venda, é o que faz o "Você cobra" virar
  de fato o que entra na conta do vendedor.
- [x] **Frete** (decisão 78, 2026-09-22), sem a opção de frete grátis, que virou item próprio abaixo. Pra venda online, frete é metade da conversa com o cliente
  e hoje não existe em lugar nenhum do app. Escopo mínimo: valor informado
  na mão por orçamento, somado ao total do cliente como linha própria no PDF
  e no copiar/colar (nunca embutido no preço da peça, pra não parecer que a
  peça ficou mais cara), com a opção "frete grátis" descontando do lucro
  explicitamente — assim o vendedor vê quanto o "frete grátis" custou de
  verdade.

- [x] **Frete grátis descontando do lucro** *Feito na Leva 10 do Plano 2 (decisão 124).*
  (separado da leva 3 em
  2026-09-22, decisão 78). A ideia é registrar que o vendedor absorveu o
  frete e mostrar quanto isso custou de lucro. Ficou de fora porque exigiria
  recalcular o lucro fora do `PricingCalculator`, e ter dois números de
  lucro no app (um do motor, outro ajustado na tela) é exatamente como
  Dashboard e Histórico passam a divergir entre si. Precisa de decisão
  própria sobre onde o lucro líquido passa a ser calculado.

### Leva 4 — Negociação (depende de a conta estar completa)

Só faz sentido depois das levas 1 a 3: uma calculadora reversa em cima de
uma conta incompleta mente com mais confiança.

- [x] **Preço alvo (negociação reversa)** (decisão 79, 2026-09-22). "O cliente quer pagar R$ 30,
  quanto sobra?" e "quanto preciso cobrar pra ter 40% de lucro real depois
  da taxa da Shopee?". Um campo de preço alvo (ou um slider) que recalcula
  margem e lucro ao vivo, em cima do preço já calculado. Vendedor negocia
  todo dia e nenhuma ferramenta do setor faz isso bem.
- [x] **Preço mínimo com aviso de prejuízo** (decisão 79, 2026-09-22). O custo de produção já é
  calculado; falta usá-lo como piso: quando o preço negociado cair abaixo
  dele, avisar visualmente ("abaixo disso você paga pra imprimir"). Depende
  do item acima e reaproveita `Quote.productionCost`.
- [x] **Comparar impressoras no mesmo orçamento** (decisão 79, 2026-09-22). "Essa peça sai R$ 16,19
  na K1 e R$ 14,80 na Ender." Todos os dados já estão cadastrados — é rodar
  o `PricingCalculator` (função pura, sem estado) uma vez por impressora
  cadastrada e mostrar o resultado lado a lado. Barato de implementar,
  ajuda a decidir em qual máquina imprimir e rende ótima screenshot.

- [x] **Registrar que o preço foi negociado** (decisão 84, 2026-09-23;
  levantado em 2026-09-22, decisão 79). Hoje o preço fechado com o cliente substitui o de tabela e
  fica indistinguível dele no orçamento salvo. Guardar essa marca (e o preço
  que a tabela daria) permitiria responder "quanto de desconto eu dei esse
  mês" e "quais clientes puxam mais o preço pra baixo". Custa um campo novo
  em `Quote`, então só vale com um pedido concreto.
  - **Feito:** o pedido concreto apareceu como bug: reabrir um orçamento
    negociado pra editar não restaurava o preço fechado, e salvar de novo
    voltava pro preço de tabela. `Quote.tableSalePrice` resolve as duas
    coisas. O Histórico mostra a marca e o Dashboard soma "Descontos dados"
    (líquido: preço acima da tabela abate). O ranking de clientes que mais
    puxam o preço pra baixo não entrou aqui; entrou na leva 6 (decisão 95),
    reaproveitando o mesmo campo.
- **Descartado (decisão 79): desconto percentual por volume.** Quem quer
  dar desconto digita o preço fechado e vê o efeito na hora. Fica aqui só
  como registro de que foi avaliado e por quê, pra não voltar à fila sem
  argumento novo.

### Leva 5 — Saída pro cliente

- [x] **WhatsApp de verdade** (decisão 80, 2026-09-22). O copiar/colar já existe, mas o passo
  seguinte é abrir a conversa pronta: botão que monta um link `wa.me` com o
  texto do orçamento já preenchido, usando o contato que já está salvo em
  `Client` (100% local, é só abrir uma URL — reaproveita o
  `platform/openUrl` que já existe).
- [x] **Imagem quadrada pro WhatsApp/Instagram** (decisão 80, 2026-09-22). Foto da peça + preço +
  prazo numa imagem pronta pra mandar no zap ou postar no status, em vez de
  um PDF anexado. A maior parte das vendas no Brasil acontece em conversa,
  não em documento formal. Reaproveita o mesmo caminho de renderização já
  usado na captura do visualizador 3D (`encodeImageBitmapToPng`, decisão
  63) e a assinatura discreta do item de divulgação (leva 8) cabe no
  rodapé dessa imagem também.

- [x] **Prazo de entrega no orçamento** (decisão 85, 2026-09-23; levantado
  em 2026-09-22, decisão 80). A imagem de divulgação pediria "foto + preço + prazo", mas prazo não
  existe como campo em lugar nenhum do modelo, então ficou de fora em vez de
  ser inventado a partir do tempo de impressão (que é tempo de máquina, não
  promessa de entrega). Encaixa bem com a fila por impressora que já existe
  na aba Impressoras: ela sabe quanto tempo cada máquina está ocupada, que é
  metade da conta de um prazo honesto.
  - **Pedido também em teste externo** (sugerido por João Antonio,
    2026-09-23, decisão 83): o prazo deve aparecer **em destaque** no PDF,
    porque é o que o cliente usa para decidir se autoriza a fabricação.
    Segunda fonte pedindo o mesmo item, sem mudança de escopo.
  - **Feito:** data fixa (`SavedQuote.deliveryDateEpochDay`), escolhida com
    atalhos "+3 / +7 / +15 dias" ou calendário, com a fila da impressora
    mostrada como dica. Em destaque no PDF, no fim do texto/WhatsApp e na
    imagem. Como data envelhece: duplicar não copia o prazo, o Histórico
    marca atraso e prazo vencido, o menu "Ações" muda só a data, e todo envio
    pro cliente com prazo vencido pede confirmação antes (decisão 85).
- [x] **Identidade do vendedor no PDF: logo, contato e borda** (decisão 86,
  2026-09-23; sugerido por João Antonio em teste externo, 2026-09-23,
  decisão 83). **Feito:** logo e contato (WhatsApp, e-mail, Instagram) num
  cabeçalho de papel timbrado, borda opcional, prévia "Ver como fica" em
  Configurações; logo e contato fora dos templates; contato também na
  imagem quadrada (a logo não, por causa do fundo escuro); rodapé mantido
  só com o nome, com o canto direito reservado pra assinatura da leva 8.
  Texto original abaixo. Hoje o PDF só
  carrega a marca do vendedor como texto (marca d'água diagonal e rodapé,
  `BrandingSettings`), e a versão com imagem já estava registrada como
  "iteração futura" no item de marca d'água (seção 1). Três acréscimos
  opcionais: **logomarca** (imagem, no cabeçalho), **contato do vendedor**
  (telefone/WhatsApp, e-mail, Instagram) e uma **borda** simples na página.
  Cabe em `BrandingSettings` e, por consequência, nos templates (decisões
  45/47), que já guardam essa configuração. Cuidados: o contato do
  **vendedor** é público e diferente de `Client.contact`, que é do cliente e
  só de uso interno (decisão 37); e o rodapé precisa ser desenhado junto com
  a assinatura "Gerado com 3DReport" (leva 8), pra os dois não disputarem o
  mesmo espaço. O mesmo contato serve pro rodapé da imagem quadrada
  (decisão 80).
- [x] **Tempo de fabricação no orçamento do cliente** (decisão 85,
  2026-09-23; sugerido por João Antonio em teste externo, 2026-09-23,
  decisão 83). Entrou como "Tempo de impressão", opção global em
  Configurações desligada por padrão, no PDF e no texto/WhatsApp (não na
  imagem). O tempo de impressão
  já existe em todo orçamento (`Quote.printTimeMinutes`, importado do G-code
  ou digitado), mas não aparece no PDF nem no copiar/colar. Mostrar como
  opção ("Tempo de fabricação: 6 h 30 min") não fere a decisão 19, porque
  tempo não é custo nem margem. Item pequeno. Não confundir com prazo de
  entrega (item acima): tempo de máquina não é promessa de entrega.
- [ ] **Orçamento com opções: rápido × acabamento fino** *Agendado na Leva 16 do Plano 2.*
  (sugerido por João
  Antonio em teste externo, 2026-09-23, decisão 83). O mesmo pedido em duas
  (ou mais) variantes, cada uma com o próprio G-code/tempo e preço, lado a
  lado no PDF, pro cliente escolher. Tem valor educativo: quem é leigo
  normalmente não sabe que dá pra trocar qualidade de acabamento por prazo
  e preço. Parente do "Comparar impressoras" (decisão 79), que já roda o
  `PricingCalculator` várias vezes e mostra lado a lado, mas aqui as
  variantes vão **pro cliente** e precisam ficar salvas, então mexe no
  formato de `SavedQuote`. Por isso é bem maior que o item anterior e merece
  decisão própria sobre o modelo de dados antes de implementar. Se vier
  depois do pedido com várias impressões (leva 9), cada opção pode ser um
  conjunto de impressões, reaproveitando aquele modelo.

### Leva 6 — Leitura do negócio (Dashboard vira consultor, não relatório)

- [x] **Lucro por hora de impressão e por hora de trabalho** (decisão 95,
  2026-09-24). **Feito:** "Lucro por hora de máquina" (lucro ÷ horas de
  impressão) e "Seu trabalho rendeu por hora" ((lucro + mão de obra) ÷ seu
  tempo, só nos pedidos com tempo informado, ao lado da hora configurada).
  De quebra, o Dashboard inteiro passou a contar só vendas: orçamento ainda
  "Orçado" sai das somas e aparece numa linha à parte, com a conversão.
  Texto original: Total vendido
  e lucro acumulado são contabilidade; "sua máquina te paga R$ X por hora" e
  "seu trabalho te paga R$ Y por hora" são gestão — é a métrica que diz se o
  negócio funciona e qual tipo de peça vale a pena repetir. Depende da mão
  de obra (leva 1) pra segunda métrica existir; a primeira já é possível
  hoje com `printTimeMinutes` + lucro.
- [x] **Ranking de produto mais lucrativo** (decisão 95, 2026-09-24).
  **Feito:** top 5 por lucro, agrupando pedidos de mesmo nome, com o lucro
  por hora de máquina de cada peça; nome automático fica de fora. Junto
  entrou o ranking de clientes que mais puxam o preço pra baixo (sobra da
  decisão 84). Texto original: item já registrado (ver
  "Dashboard/relatório simples" na seção 1) — agendado aqui, por ser a mesma
  leva de leitura do negócio e reaproveitar a mesma agregação.
- [x] **Manutenção por impressora** (decisão 96, 2026-09-24; promovido de
  "Fora das levas" em 2026-09-23, decisão 83, depois de pedido em teste
  externo por João Antonio). **Feito:** botão "Manutenção" em cada
  impressora da aba Impressoras, com componentes (cada um com o seu
  intervalo em horas e barra de progresso), diário (registrar com
  componente zera o contador dele) e horas avulsas; a linha da impressora
  mostra o componente mais urgente, em âmbar quando perto e em vermelho
  quando vencido. As horas vêm dos pedidos Prontos/Entregues daquela
  impressora mais as avulsas. Texto original abaixo. Junta dois itens que já estavam registrados em "Produção e
  precificação" (seção 1): o **lembrete por horas acumuladas** e o
  **histórico/diário de manutenção**. Continuam lá descritos; aqui entra o
  agendamento e os refinamentos que o teste externo trouxe:
  - **Alertas por componente, cada um com seu intervalo** (ex.: trocar bico
    a cada X h, lubrificar eixos a cada Y h), em vez de um limiar único por
    impressora. Registrar a manutenção no diário zera o contador daquele
    componente, e é assim que os dois itens se ligam.
  - **Horas de uso fora de orçamento.** As horas vêm dos orçamentos daquela
    impressora, mas teste de calibração, reimpressão de peça que falhou e uso
    próprio também gastam a máquina. Sem um jeito de registrar essas horas
    avulsas, o alerta chega atrasado.
  - **Não vira aba nova.** Fica na aba Impressoras, onde a fila de impressão
    já mostra o estado de cada máquina (decisão 68). Mais uma aba mexeria na
    barra e nos atalhos, que a decisão 82 acabou de defender. A parte "saber
    que o orçamento foi aprovado e impresso" do pedido já existe no
    `OrderStatus`.
  - Encaixa nesta leva porque reaproveita a mesma soma por impressora do
    `PrintQueueReport` e é leitura do negócio: quanto a máquina rodou e o
    que ela está pedindo.

### Leva 7 — UI/UX (depois que o conjunto de campos parar de mudar)

Deliberadamente **depois** das levas 1 a 5: elas somam campos na tela de
Orçamento (mão de obra, quantidade, desconto, imposto, frete). Redesenhar o
layout antes disso significa redesenhar duas vezes.

- [x] **Layout de duas colunas na tela de Orçamento** (decisão 81, 2026-09-22). Hoje é um app desktop
  com layout de celular: uma coluna só, rolando por três assuntos diferentes
  (calcular, anexar arquivos, salvar), e na janela padrão não dá pra ver o
  preço e os campos ao mesmo tempo. Proposta: entradas à esquerda, a "nota"
  fixa à direita recalculando ao vivo enquanto se digita. É a maior melhoria
  visual disponível e finalmente usa o formato de tela que o app escolheu.
- [x] **Barra de composição do preço** (decisão 81, 2026-09-22). O "Você cobra R$ 16,19" não mostra
  onde o dinheiro está. Uma barra empilhada pequena (material, energia,
  máquina, mão de obra, lucro) ensina o vendedor a precificar, dá sentido a
  todas as parcelas somadas nas levas 1 a 3 e rende boa screenshot pro site.
- [x] **Onboarding de 3 perguntas na primeira execução** (decisão 81, 2026-09-22). Perguntas ajustadas: kWh, valor da hora de trabalho e margem, e sem sugestão de kWh por estado (ver decisão). Substitui o item
  de onboarding já registrado (ver "UX extras"), agora com escopo definido:
  impressora (pelo preset que já existe), preço do kWh e margem. Detalhe
  local que vale a pena: **sugerir o kWh por estado brasileiro** a partir de
  uma tabela estática embutida (sem rede, no mesmo espírito dos presets de
  impressora) — é o tipo de carinho que faz a pessoa sentir que o app foi
  feito pra ela. Vem antes da leva de crescimento de propósito: não adianta
  atrair gente nova pra uma tela vazia.
- [x] **Snackbar no lugar do feedback inline** (decisão 82, 2026-09-23). Só
  confirmações de sucesso; erro de validação continua ao lado do campo.
  Entrou como sobreposição no layout que já existia, sem migrar pra
  `Scaffold`: os slots dele (barra superior/inferior, FAB) não são usados.
- **Descartado (decisão 82):** ~~unificar o código das três telas de catálogo~~ e ~~fundir as abas
  num "Catálogos"~~, depois de conferir a
  premissa. As telas deixaram de ser "quase idênticas" (362/199/142 linhas,
  com conteúdo próprio vindo das decisões 39/40, 54 e 68), e a fusão das
  abas custaria um clique diário e a renumeração dos atalhos pra liberar
  espaço que a janela de 1280dp (decisão 81) não precisa mais. Fica
  registrado pra não voltar à fila sem argumento novo. Itens
  já registrados em "Observações técnicas" (fim deste documento) — agendados
  aqui porque são a mesma obra de UI: as 3 telas quase idênticas
  (Filamentos/Impressoras/Serviços) podem virar uma aba "Catálogos" só,
  liberando espaço na barra de 7 abas, e o feedback inline vira snackbar.
  Atenção: mexer na quantidade de abas mexe na numeração dos atalhos de
  teclado (decisões 43 e 45).
- [x] **Ícones junto dos textos, pra orientar quem usa** (sugerido por João
  Antonio em teste externo, 2026-09-23; decisão 83, que revisa a 54).
  **Feito (decisão 87, 2026-09-23), com o teste de reconhecimento feito e
  aprovado em 2026-09-24**, usando a folha
  [`docs/assets/teste-reconhecimento-icones.png`](../assets/teste-reconhecimento-icones.png).
  Nenhum ícone precisou ser trocado; se algum dia precisar, a troca é só no
  `AppIcons`. Campos de formulário ficaram sem ícone (inclusive
  "Filamento"), confirmado pelo responsável do projeto. Texto original
  abaixo. Hoje o
  app não tem nenhum ícone: 7 abas só texto, 12 seções empilhadas em
  Configurações e dezenas de botões só texto. A decisão 54 descartou ícones
  perguntando "fica mais bonito?"; a pergunta do teste externo é outra,
  "fica mais fácil de usar?", e a resposta das referências é sim, **desde
  que o ícone acompanhe o texto e nunca o substitua**: a Nielsen Norman Group
  mostra que quase nenhum ícone é universal e que o ganho dele é servir de
  âncora pra varrer a tela e reconhecer o que já foi visto; o Material
  Design 3 (que o app segue) prevê ícone + rótulo em abas e navegação, com
  cautela pra ícone sozinho. Os dois motivos da decisão 54 caem: a
  inconsistência some se a aplicação for sistemática em todas as telas, e
  dá pra não somar dependência copiando os Material Symbols (Apache 2.0)
  como vetores no próprio código.
  - **Escopo:** (1) abas principais, ícone + texto; (2) títulos de seção das
    telas longas (Configurações e Orçamento), ícone à esquerda, que é
    exatamente o pedido ("rolinho ao lado de Filamento"); (3) ações do
    Histórico e do Kanban, nos botões e no menu "⋮".
  - **Fora do escopo:** ícone em cada rótulo de campo (vira ruído e perde o
    efeito de âncora) e qualquer botão só com ícone.
  - **Ícones do domínio 3D são o ponto fraco:** o ícone "print" do Material
    é uma impressora **de papel** e confundiria justamente a aba
    Impressoras, e não existe carretel de filamento pronto. Esses poucos
    devem ser desenhados pro projeto, no mesmo caminho do ícone do app
    (decisão 42) e no mesmo traço dos Material Symbols. O ícone de
    impressora 3D desenhado aqui serve também pro item "Imagens das
    impressoras" (Presets, seção 1).
  - **Um conjunto, um estilo:** contornado, preenchido só na aba ativa (padrão
    do M3); ícone ao lado de texto é decorativo pra leitor de tela
    (`contentDescription = null`), contraste mínimo de 3:1.
  - **Validação antes de fechar:** teste de reconhecimento com 3 a 5 pessoas
    (mostrar só os ícones do domínio e perguntar o que significam),
    incluindo quem sugeriu. Não muda aba nem atalho de teclado.

### Leva 7B — Produtos sem venda (quem está começando)

Levantado pelo responsável do projeto em 2026-09-24. Hoje todo orçamento
salvo nasce `OrderStatus.ORCADO`, então qualquer peça registrada só pra
precificar ou pra montar portfólio entra no Kanban, na linha "Orçado" e na
conversão do Dashboard e no filtro de status do Histórico. O app é voltado
pra quem vende, mas dois públicos ficam desamparados: **quem está começando**
e ainda não tem cliente (quer saber quanto cobrar e guardar o que já sabe
fazer) e **o vendedor profissional que monta vitrine/portfólio** (peças que
oferece, não pedidos que recebeu). Os dois acabam obrigados a usar o fluxo de
venda sem ter venda, e isso ainda suja as métricas de quem vende.

A proposta separa **produto** (registro com preço, sem cliente nem
andamento) de **pedido** (venda, com status). Um produto vira pedido pelo
botão "Vender". Não é uma mudança de paradigma: calcular sem salvar, o
Duplicar, o catálogo em PDF (decisão 44) e o Dashboard que já conta só
vendas (decisão 95) existem, e falta um conceito só. Vem antes da Leva 8 de
propósito: não adianta atrair gente nova se quem ainda não vende tem que
fingir que vende. Revisita a decisão 44, que recusou um campo pra marcar o
item, com um argumento novo: um público que ela não considerou.

Decidido com o responsável do projeto (2026-09-24): os produtos ficam
**dentro do Histórico**, num seletor `Pedidos | Produtos`, sem aba nova (a
barra e os atalhos continuam como a decisão 82 deixou); salvar oferece
**dois botões**, "Salvar no catálogo" e "Salvar como pedido".

**Fase 1: separar produto de pedido (o núcleo)** (feita, decisão 101, 2026-09-24, v1.42.0)

- [x] **`SavedQuote.kind` (Produto | Pedido)**, com padrão Pedido: os JSONs
  antigos continuam iguais e nenhum pedido existente muda. Campo no mesmo
  `SavedQuote`, e não modelo novo, pra reaproveitar foto, STL, configurações
  de impressão, PDF, imagem quadrada, WhatsApp, edição e comparar
  impressoras sem duplicar código. Produto guarda cliente, frete e prazo
  vazios e ignora o `status`.
- [x] **Uma regra só pra "o que é pedido"** (`SavedQuote.isOrder`), no mesmo espírito do `isSold`
  (um lugar só, pra tela e relatório não divergirem). Produto fica fora do
  Kanban, do Dashboard (inclusive da linha "Orçado" e da conversão), da fila
  de impressão (`PrintQueueReport`), das horas de manutenção
  (`MaintenanceReport`), da etiqueta de prazo e do filtro de status.
- [x] **Seletor `Pedidos | Produtos` no Histórico**, ao lado de
  Lista/Kanban. No modo Produtos o Kanban não aparece (produto não tem
  andamento); busca e seleção múltipla continuam funcionando.
- [x] **Dois botões ao salvar:** **Feito de outro jeito (decisão 101):** seletor "Pedido de cliente | Produto do catálogo" no topo da seção de salvar e um botão só, cujo texto muda, porque esconder cliente, prazo e frete exige saber o tipo antes de preencher. Texto original: "Salvar no catálogo" e "Salvar como
  pedido". O destacado segue o perfil do onboarding (Fase 3). No modo
  catálogo, cliente, frete e prazo somem do formulário, porque não fazem
  sentido sem venda.
- [x] **"Vender" como ação principal do card do produto.** Abre o Orçamento
  preenchido (mesmo caminho de `QuoteViewModel.duplicateForNewQuote`), com
  aviso "a partir do produto X". O pedido nasce Orçado e segue o fluxo
  normal: cliente, serviços, negociação, prazo. O produto continua no
  catálogo, intacto.
- [x] **Mover entre os dois:** ("Guardar no catálogo" passa pelo Orçamento em modo produto, pra revisar e voltar ao preço de tabela) "Guardar no catálogo" num pedido cria uma
  cópia como produto, sem mexer no pedido (que é histórico de venda);
  "Transformar em pedido" num produto que nunca foi vendido, pra quem salvou
  no lugar errado.
- [x] **Catálogo em PDF parte dos produtos:** no modo Produtos, "Exportar
  catálogo" já sugere todos, sem obrigar a marcar um por um. A seleção
  manual continua valendo pra quem quer um recorte.

**Fase 2: catálogo vivo (preço que não envelhece)** (feita, decisão 102, 2026-09-24, v1.43.0)

- [x] **Atualizar o preço com os cadastros de hoje.** **Feito com um ajuste (decisão 102):** o aviso aparece quando os custos mudaram ("Custos mudaram: hoje sairia R$ X"), e não pela idade do preço, porque preço antigo com custos iguais continua certo. Texto original: O pedido é um retrato
  congelado (KDoc de `SavedQuote`), e isso está certo pra venda. O produto
  de catálogo precisa acompanhar o filamento que subiu e a hora de trabalho
  que mudou: "Atualizar preço" roda o `PricingCalculator` de novo e mostra
  antes e depois antes de gravar. Produto antigo ganha um aviso discreto
  ("preço de N meses atrás").
- [x] **Preço de vitrine:** (com o atalho "Arredondar pra R$ X,90") calculado R$ 18,37, anunciado R$ 18,90. Mesma
  ideia do `tableSalePrice` (decisão 84): o anunciado vai pro catálogo, e o
  aviso de prejuízo (decisão 79) avisa se o arredondamento ficou abaixo do
  custo.
- [x] **Categorias simples** (Chaveiros, Decoração, Utilidades) pra filtrar
  a lista e separar o PDF do catálogo em seções. Texto livre com sugestão
  das já usadas, sem tela de cadastro nova.

**Fase 3: quem está começando** (feita, decisão 103, 2026-09-24, v1.44.0; leva 7B concluída)

- [x] **Onboarding pergunta como a pessoa usa o app:** "Já vendo sob
  encomenda" ou "Estou começando / quero precificar e montar portfólio". A
  resposta só define **padrões** (botão de salvar em destaque, vista inicial
  do Histórico); nada fica escondido e a escolha muda em Configurações.
  Entra no onboarding da decisão 81 sem transformá-lo em formulário.
  *Removido na 2.0.0 (decisão 110): redundante com a escolha no Orçamento.*
- [x] **Dashboard sem vendas não fica vazio:** quem ainda não tem pedido vê
  o resumo do catálogo (quantos produtos, faixa de preço, lucro por hora de
  máquina de cada um) e o convite "quando aparecer um cliente, é só clicar
  em Vender". O lucro por hora responde a pergunta de quem está começando:
  qual peça vale a pena oferecer primeiro.
- [x] **Produto mais vendido do catálogo:** o pedido guarda de qual produto
  veio (id de origem), e o ranking da decisão 95 passa a juntar por produto
  de origem, e não só pelo nome.

**Ideias registradas, fora da leva (sem posição):**

- **Estoque de pronta-entrega:** *Agendado na Leva 16 do Plano 2.*
  quantidade já feita por produto, que baixa
  ao vender. É o caso da feira e do evento, onde se imprime antes e vende
  depois. Mexe nas horas de máquina (peça impressa sem pedido) e merece
  decisão própria.
- **Variações do mesmo produto** *Agendado na Leva 16 do Plano 2.*
  (cor, tamanho), parente do "orçamento com
  opções" da Leva 5.
- **Portfólio como imagem em grade** *Agendado na Leva 17 do Plano 2.*
  pro Instagram, reaproveitando a imagem
  quadrada (decisão 80).
- **Importar vários G-codes de uma vez** *Agendado na Leva 17 do Plano 2.*
  como produtos, pra quem chega com
  portfólio pronto.

**Cuidados de usabilidade na implementação:**

- Na tela, as palavras são "produto" e "catálogo", nunca "rascunho":
  produto não é pedido incompleto, é um item com vida própria.
- Nenhuma função some por perfil; o perfil só muda o que vem em destaque.
- Vender a partir do catálogo é um clique a partir do card.
- Atualizar o app não muda nada no que já está salvo: tudo continua pedido,
  e produto só existe quando alguém cria um.

### Leva 8 — Crescimento (com o produto já bom)

- [x] **Assinatura discreta "Gerado com 3DReport" nos PDFs** (decisões 88 e
  91, 2026-09-23). **Feito:** sempre presente, sem opção de desligar
  (decisão 91: opcional, todo mundo desligaria), discreta no canto inferior
  direito do PDF, com só "3DReport" como link pro site, e também na imagem
  do WhatsApp (sem link). Detalhe no item de "Vendas e divulgação". Texto original:
  item já registrado e detalhado (ver "Vendas e divulgação") — é a maior alavanca
  de distribuição do projeto inteiro. Cada
  orçamento que um vendedor manda pro cliente é uma chance de outro vendedor
  conhecer o app, e é a única divulgação orgânica disponível pra um projeto
  sem verba de marketing (decisão 15).
- [x] **Reposicionar a comunicação: de "calculadora" pra "o sistema de quem
  vende impressão 3D"** (decisão 90, 2026-09-23). **Feito:** hero do site,
  abertura do README e manual reescritos em torno do sistema completo e do
  "100% no seu computador"; seção nova sobre isso no site; funcionalidades
  reorganizadas pelo fluxo de venda; FAQ do G-code e da assinatura; prints
  novos. Título, meta description e dados estruturados continuam com
  "orçamento/preço de impressão 3D", que é o que se pesquisa. Texto
  original: site e README hoje lideram com orçamento/
  calculadora, categoria em que existem centenas de páginas web gratuitas
  concorrendo. O que está construído já é outra coisa: histórico, clientes,
  kanban, fila de impressora, catálogos, reimpressão com a mesma
  configuração. A diferenciação defensável tem duas pernas: **ser o sistema
  completo** e **ser 100% local, grátis e offline** ("seus preços e seus
  clientes não saem do seu computador"), contra concorrentes SaaS de
  mensalidade. O fosso competitivo é justamente ser brasileiro: MEI, Pix,
  maquininha, Shopee/ML, kWh por estado, WhatsApp — coisas que as levas 3, 5
  e 7 constroem e que nenhuma ferramenta internacional vai fazer bem.
- [x] **Arrastar o G-code na janela e sair um orçamento completo** (decisão
  89, 2026-09-23). **Feito:** arrastar pra qualquer lugar da janela (ou o
  botão de sempre) preenche comprimento, tempo, foto e configurações, e
  escolhe impressora e filamento quando o nome bate com o cadastrado;
  parecido vira sugestão, nunca escolha. Chaves conferidas em G-codes reais
  de Bambu Studio, OrcaSlicer, PrusaSlicer, SuperSlicer e Cura (este último
  só informa a impressora). De quebra, os arquivos reais mostraram que o
  tempo do Bambu Studio 2.x não era importado, e isso foi corrigido.
  - [ ] **Ler o `.gcode.3mf` do Bambu Studio** *Agendado na Leva 17 do Plano 2.*
    ("Exportar arquivo fatiado da
    placa", um zip com `Metadata/plate_N.gcode` dentro). Ficou de fora porque
    nenhum exemplo público confirmou a estrutura; pela regra de não inventar
    formato, entra quando houver um arquivo real pra conferir. O arquivo
    pode trazer várias placas; depois da fase 3 do pedido com várias
    impressões (leva 9), cada placa vira uma impressão do mesmo pedido.
  Texto original: hoje a
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

- [x] **Calculadora web no próprio site, reaproveitando o módulo `core`**
  (decisão 98, 2026-09-24). **Feito:** `site/calculadora.html`, página
  própria ligada pela home e no sitemap. Peso, tempo, filamento, potência,
  kWh e margem à vista; falhas, acabamento, trabalho, quantidade, canal e
  imposto em "Mais custos". Mostra venda, produção, lucro, ponto de
  equilíbrio e o detalhamento, com a chamada pro app logo abaixo. A conta é
  o `PricingCalculator` compilado pra JavaScript (módulo `:web`), e os
  testes do `core` rodam também no alvo JS. Texto original:
  O `core` é Kotlin Multiplatform puro, sem dependência de desktop — dá pra
  publicar uma calculadora simples no GitHub Pages que já existe (alvo
  Wasm/JS) usando exatamente a mesma fórmula, sem segunda fonte de verdade.
  Serve de funil: quem pesquisa "calcular preço impressão 3D" no Google cai
  na calculadora e termina com "quer salvar histórico, clientes e PDF? baixe
  o app". Por último por ser a maior incógnita técnica da lista (alvo de
  build novo, pipeline de publicação, e uma versão web que precisa não
  canibalizar o app).

#### Vários filamentos numa impressão e várias impressões num pedido

Junta dois problemas que são o mesmo visto de lados diferentes. Hoje o
modelo supõe "1 pedido = 1 impressão = 1 filamento": `PrintJob` tem um
`filament` só e `Quote` tem um `job` só, com uma impressora.

- **Multicolor/AMS** (item que já estava aqui): uma peça com 3 cores não é
  representável, e pior, o parser de G-code **soma** os extrusores num total
  só, então uma peça com PLA + PETG é cobrada como se tudo fosse do mesmo
  preço/kg. Desde a decisão 89, importar um G-code com materiais diferentes
  em extrusores diferentes pelo menos **avisava** e não escolhia filamento
  sozinho. **Resolvido na 2.0.0 (decisão 105):** cada extrusor vira uma linha
  com o próprio consumo e o próprio preço.
- **Pedido feito de várias impressões** (levantado pelo responsável do
  projeto, 2026-09-24): uma action figure grande ou um diorama sai em várias
  rodadas de impressão, às vezes em impressoras e filamentos diferentes, mas
  é uma entrega só e um preço só pro cliente. Hoje o vendedor não tem como
  orçar isso no app sem fingir que é uma peça só.

A forma real do pedido:

```
Pedido (1 entrega, 1 preço pro cliente)
 └─ Impressão 1 "Cabeça" · K1    · 3h10 · ×1 · PLA pele 42 g + PLA preto 6 g
 └─ Impressão 2 "Corpo"  · K1    · 9h40 · ×1 · PLA azul 180 g
 └─ Impressão 3 "Base"   · Ender · 6h00 · ×1 · PETG cinza 210 g
```

Dois cuidados de conceito:

- **O custo é de cada mesa, não de cada parte.** Uma mesa pode ter várias
  partes; o que custa é uma rodada da máquina (tempo, impressora, filamento).
- **Não é somar N orçamentos.** O que é do pedido entra uma vez só: tempo de
  trabalho (decisão 94), custo administrativo, serviços por pedido, frete,
  prazo, canal, imposto e preço negociado. Somar orçamentos separados
  cobraria o administrativo N vezes.

Decidido com o responsável do projeto (2026-09-24): na tela, cada bloco se
chama **"Impressão"** ("Impressão 2 · Corpo"); o cliente vê **um item só**
(nome, foto, preço, prazo), e a divisão fica de uso interno, como custo e
margem (decisão 19); o modelo é migrado **uma vez**, já suportando as duas
coisas, e as telas vêm em fases, multicolor primeiro porque corrige uma
conta errada.

**Fase 1: modelo e cálculo** (feita, decisões 104 e 105, 2026-09-25, v2.0.0)

- [x] **Modelo em lista, não "principal + adicionais".** **Feito de outro jeito
  (decisão 104):** como o app ainda não está em produção, o formato do JSON foi
  quebrado de propósito e o app recomeça do zero, guardando a pasta antiga em
  `~/.3dreport-v1`. `FilamentUsage` (filamento, metros, cor); `PrintJob` com
  `filaments`, tempo, `runs` e nome; `QuotedPrint` (a impressão, a impressora e
  o custo dela, congelados); `Quote.prints`. Texto original: `PrintJob` mantinha
  os campos atuais como filamento principal e ganhava `additionalFilaments`, e
  `Quote` ganhava `additionalJobs`, pra JSON antigo continuar válido.
- [x] **`PricingCalculator` por impressão.** Material por filamento; energia,
  manutenção, retorno e custo fixo com a impressora **daquela** impressão;
  soma; e só então o que é do pedido. Acabamento em cada impressão, junto com o
  material dela. O tempo de trabalho é só do pedido: `PrintJob.laborMinutes`
  saiu (decisão 104). Os números da planilha de referência não mudaram, nos
  alvos JVM e JS, e a calculadora do site usa o atalho de uma impressão.
- [x] `quantity` continua sendo "quantos pedidos iguais"; cada impressão
  multiplica pelos próprios `runs` e depois pela quantidade.
- [x] **Legado de compatibilidade removido na mesma quebra** (decisão 104):
  nomes antigos mantidos só pelo JSON (`marketplaceFeeRate`, `setupMinutes`,
  `watermarkText`, `Service.price`), dados gravados em duplicidade
  (`productionCost`, `filamentWeightGrams`), migrações de configuração e o
  canal achado pelo nome.

**Fase 2: vários filamentos numa impressão (multicolor/AMS)** (feita, decisão 105, 2026-09-25, v2.0.0)

- [x] **"+ Adicionar filamento"** abaixo do filamento. Com mais de um, cada
  linha tem filamento, cor, metros e remover; com um só, a tela fica como antes.
- [x] **G-code por extrusor:** o parser para de somar e devolve o consumo de
  cada extrusor, e o `CatalogMatcher` casa cada um com a regra da decisão 89.
  **Conferido em G-codes multicolor reais** (Bambu Studio com AMS lite,
  OrcaSlicer IDEX com torre, OrcaSlicer com slot sem uso): o valor por extrusor
  já inclui a purga e a torre.
- [x] **Resultado mostra o consumo por filamento** ("PLA · Preto 320 g · PETG
  80 g"), e o card do Histórico também.

**Fase 3: várias impressões num pedido** (feita, decisão 114, 2026-09-25, v2.1.0)

- [x] **Revelação progressiva:** pedido de uma impressão continua idêntico
  ao de hoje. "Adicionar outra impressão" transforma o bloco atual no cartão
  "Impressão 1" e cria a "Impressão 2", já com a impressora e o filamento da
  anterior.
- [x] **Cartão da impressão:** nome opcional ("Cabeça"), impressora,
  filamentos, metros, tempo, "× N vezes", botão de G-code e miniatura.
  Recolhível, com resumo numa linha ("Corpo · K1 · 9h40 · 180 g · produção
  R$ 32,10"). Duplicar e remover, remover com desfazer. **Feito (decisão
  114):** o resumo mostra o custo da própria impressão ("· custo R$ 32,10"),
  e o cartão fica em `ui/quote/PrintCard.kt`.
- [x] **O que é do pedido fica fora dos cartões:** tempo de trabalho,
  serviços, frete, canal, negociação e prazo, uma vez só.
- [x] **Arrastar vários G-codes de uma vez cria uma impressão por arquivo**
  (o vendedor fatia 6 mesas e arrasta as 6). Com um arquivo só, o aviso de
  arraste se divide em "Substituir a impressão N" e "Adicionar como nova
  impressão"; soltar em cima de um cartão preenche aquele cartão. Desfazer
  continua valendo.
- [x] **Resultado:** total do pedido em destaque, "Por impressão" recolhido
  logo abaixo. **Feito (decisão 114):** o total mostra "N impressões · X de
  máquina", e "Por impressão" recolhido soma o custo de cada impressão com
  uma linha "Do pedido (trabalho, falhas, administrativo)".
- [x] **Comparar impressoras** (decisão 79) vira "tudo nesta impressora".
  **Feito (decisão 114):** o texto ficou "Tudo na <impressora>".

**Fase 4: o resto do app entende o pedido com várias impressões**

O núcleo saiu junto da Fase 1 (v2.0.0), porque com o modelo em lista cada
consumidor precisou tratar a lista.

- [x] **Histórico e Kanban:** um cartão por pedido, com "3 impressões"
  discreto. **Feito (decisão 114):** o Histórico virou a tela "Pedidos"
  (decisão 111), mas o cartão e o Kanban seguem mostrando "N impressões".
- [x] **Fila de impressão e horas de manutenção:** cada impressão soma na
  impressora dela (`PrintQueueReport`, `MaintenanceReport`, via
  `Quote.printMinutesOn`).
- [x] **Dashboard:** horas de máquina somadas; o filamento mais usado conta
  cada filamento de cada pedido (`QuoteReport`), sem repetir por impressão.
- [x] **PDF, texto/WhatsApp e imagem:** um item só; o tempo de impressão
  (opção da decisão 85) é a soma (`Quote.totalPrintTimeMinutes`).
- [x] **Duplicar, reimprimir, editar e o "Vender" do catálogo** (leva 7B)
  copiam todas as impressões. O código já lê a lista inteira; faltam os testes
  com várias impressões, que só existem na tela na Fase 3. **Feito (decisão
  114):** cada impressão guarda as próprias configurações e miniatura
  (`PrintJob.settings`/`thumbnailFileName`) e elas voltam ao reabrir,
  duplicar, vender ou copiar pro catálogo; editar só o nome ou as
  configurações de uma impressão reaberta não reprecifica.
- [x] **Backup:** o manifesto guarda a versão do formato de dados, e backup de
  outro formato é recusado (decisão 104). Texto original: "o formato só ganha
  campos opcionais".

**Cuidados de usabilidade na implementação:**

- Quem nunca clica em "Adicionar" não vê nada novo.
- Na tela, a palavra é "impressão", nunca "job", "placa" ou "parte".
- Nenhum número do pedido fica escondido em cartão recolhido: total e tempo
  total ficam sempre à vista.
- O cliente nunca vê a divisão (decisão 19).

### Revisão pré-lançamento (2026-09-25, decisões 106 a 109)

Antes de divulgar a 2.0.0, uma revisão do app inteiro (código, dados, telas,
distribuição) com a pergunta "o que dá errado quando mil pessoas começarem a
usar, e o que trava a nuvem e o Android depois?". Entrou na própria 2.0.0:

- [x] **Nada se perde, nada fecha o app** (decisão 108): gravação atômica com
  cópia anterior, instância única, log e diálogo de erro, anexos pelo
  conteúdo, backup em fluxo e automático, avisos em vez de exceção.
- [x] **Números e conta certos** (decisão 107): canal e imposto sobre o total
  cobrado, entrada numérica pt-BR com erro por campo, gramas ou metros, "3h20".
- [x] **Desempenho** (decisão 108): G-code pelas pontas, trabalho pesado fora
  do thread da tela, Histórico preguiçoso, PDF leve.
- [x] **Modelo pronto pra migrar e sincronizar** (decisão 106): metadados por
  registro com lixeira, número do pedido, histórico de status, Cancelado,
  moeda no orçamento, cadastro de clientes, retrato por impressão, migrações.
- [x] **Arquitetura aberta a mudança** (decisão 108): repositórios como
  interfaces, `AppContainer`, serviços de plataforma injetados.
- [x] **UX essencial** (decisão 108): Orçamento, Histórico/Kanban, PDF,
  onboarding, cadastros, janela e tema (Histórico virou "Pedidos" na 2.1,
  decisão 111).

Leva seguinte, a 2.1 (decisão 109):

- [x] **Barra lateral agrupada** (Vendas: Orçamento, Pedidos, Catálogo,
  Dashboard; Cadastros: Filamentos, Impressoras, Serviços; Configurações
  embaixo), com atalhos renumerados e o rodapé indo pro "Sobre". **Feito
  (decisão 111, 2026-09-25, v2.1.0):** abaixo de 1280dp de largura a barra
  vira um trilho só de ícones com tooltip; atalhos são Ctrl/Cmd+1 a 8 na
  ordem da barra (Sobre não tem atalho); o rodapé e o diálogo de ajuda
  viraram a tela "Sobre".
- [x] **Configurações em seções**, com barra fixa de "Alterações não salvas" e
  largura máxima de leitura. **Feito (decisão 112, 2026-09-25, v2.1.0).**
- [x] **Editar na própria aba**, com a barra de operação, em duas colunas.
  **Feito (decisão 113, 2026-09-25, v2.1.0):** `EditQuoteDialog` foi removido;
  a edição abre no próprio Orçamento, numa segunda instância do `QuoteViewModel`
  (a da decisão 108), então o rascunho que estava na aba fica intacto e volta
  depois de salvar ou cancelar.
- [x] **Fase 3 da Leva 9** (várias impressões num pedido), sobre os ids
  estáveis e o retrato por impressão. **Feito (decisão 114, 2026-09-25,
  v2.1.0)**, ver seção acima.
- [x] "Arquivar" um cadastro em uso, lembrar tamanho e posição da janela,
  fixar o `upgradeUuid` do instalador do Windows (conferindo antes o valor
  que o jpackage já deriva, pra não quebrar a atualização da 1.x) e a
  verificação opcional de atualizações. **Feito:** arquivar (decisão 115),
  lembrar tamanho e posição da janela (decisão 111), `upgradeUuid` fixo e
  verificação opcional de atualizações (decisão 116), todos 2026-09-25,
  v2.1.0. **Textos em `composeResources` ficaram pra depois da 2.1**
  (idioma da interface configurável, ver "Fora das levas").
- [ ] **Nuvem e Android:** *Agendado nas Levas 21 (sem servidor) e 22 (servidor) do Plano 2.*
  SQLite (ou outro banco) como nova implementação das
  mesmas interfaces de repositório; sincronizar por registro usando
  `updatedAt`/`deletedAt` e anexos pela chave de conteúdo. Ficou de fora da
  2.1, continua pra depois.

### Fora das levas

Todos os itens que estavam aqui foram reagendados no Plano 2 (revisão de
2026-09-28): custo de falha real acumulado e exportar pra CSV/Excel na Leva 13;
sinal/pagamento parcial na Leva 11; compatibilidade com Spoolman na Leva 12;
idioma da interface nas Levas 10B e 11B; assistente de IA na Leva 18; portal de
acompanhamento nas Levas 19 e 22; Fase 2 do STL e guardar o `.3mf` na Leva 20;
Android na Leva 21. O banner de compartilhamento do site foi feito (decisão
120). Só o `CODE_OF_CONDUCT.md` continua sem posição (seção 4).
