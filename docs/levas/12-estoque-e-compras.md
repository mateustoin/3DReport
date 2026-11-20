# Leva 12: estoque e compras

> Especificação de trabalho da leva. É o que cada agente recebe pra fazer a sua
> parte. Depois do merge, o que vale como registro são as decisões em
> `docs/decisions/`; este arquivo fica como histórico do que foi pedido.
> O responsável do projeto aceitou de antemão as recomendações desta leva
> (2026-11-18) e pediu um resumo das escolhas e alternativas no fim.

## Objetivo

Quem quiser passa a saber quantos gramas sobram de cada cor e quantas unidades de
cada insumo, sem fazer conta: o pedido que fica Pronto dá baixa sozinho, falha e
teste entram como consumo avulso, e a balança corrige o saldo. O app avisa quando
algo está acabando, quando um orçamento novo não cabe no que sobrou, e monta a lista
do que comprar. As compras ficam registradas com o frete e atualizam o preço do
cadastro quando a pessoa confirma.

Quem não quiser controlar gramas continua com o "tem / acabou" de hoje, sem mudança.

## Itens

### A. Saldo em gramas por cor, com baixa automática (decisão 158, revisita a 39)

1. **Opcional por item.** Cada cor de filamento (`FilamentColor`) e cada insumo
   (`Consumable`) pode passar a ter controle de saldo. Nada é controlado até a
   pessoa ativar ("Controlar saldo"), informando o **saldo atual** (gramas pra cor,
   unidades pro insumo). Atualizar o app não ativa nada.
2. **Unidade de controle: a cor**, não o carretel. Dois carretéis da mesma cor somam
   no mesmo saldo. É o que o vendedor pergunta ("tenho vermelho pra este pedido?") e
   é como o orçamento já escolhe (filamento + cor).
3. **O saldo é a soma dos movimentos gravados** do item. Tipos de movimento:
   - **Início**: o saldo informado ao ativar (entra como ajuste até o valor dado).
   - **Compra**: soma (item C).
   - **Pedido**: subtrai o consumo do pedido (item 4).
   - **Consumo avulso**: subtrai, com motivo `Falha`, `Teste`, `Uso próprio` ou
     `Outro` e observação opcional. Mesma ideia das horas avulsas da manutenção
     (decisão 96).
   - **Ajuste** ("Pesei o carretel" ou "Corrigir saldo"): grava a diferença entre o
     saldo novo informado e o saldo de agora.
   Cada movimento tem data, quantidade com sinal e, conforme o tipo, o pedido, a
   compra, o motivo ou a observação. Excluir um movimento avulso ou de ajuste desfaz
   o efeito dele; movimento de pedido e de compra não se exclui à mão (some com a
   regra do item 4 ou ao excluir a compra).
4. **Baixa do pedido, por transição de andamento.** Mesma régua do `MaintenanceReport`:
   o material é gasto quando o pedido chega a **Pronto** ou **Entregue**.
   - Pedido que **entra** em Pronto/Entregue (vindo de qualquer outro andamento, ou
     criado já assim) ganha um movimento "Pedido #0042" por item controlado que ele
     usa.
   - Pedido que **sai** de Pronto/Entregue pra outro andamento (voltou por engano, ou
     Cancelado) perde esse movimento: o material volta.
   - Pedido Pronto/Entregue **editado** (reabrir o cálculo e salvar) tem o movimento
     refeito com o consumo novo.
   - Pedido **excluído** mantém o movimento: o material foi gasto. Restaurar da
     lixeira não desconta de novo.
   - Pronto para Entregue não muda nada.
   - Pedido que já estava Pronto/Entregue quando o controle começou não desconta (o
     saldo informado no início já está "depois" dele).
   - Produto do catálogo nunca desconta.
   Por ser fato gravado, e não conta refeita, a baixa não depende de o pedido
   continuar existindo. É a diferença pra decisão 96, que recalcula horas a partir
   dos pedidos e por isso devolve horas quando um pedido é excluído.
5. **Consumo de um pedido, por item.**
   - Filamento: para cada `FilamentUsage` de cada impressão, `weightGrams × runs ×
     quantity` do pedido, somado por (id do filamento, id da cor). Usa o retrato
     gravado no pedido (densidade e diâmetro de quando foi orçado), então mudar a
     densidade do cadastro não mexe em baixa já feita. Cor `null` num filamento que
     hoje tem uma cor só vai pra essa cor; com várias cores, não é atribuída a
     nenhuma (e não desconta).
   - A reserva de falha **não** entra: ela é preço, não material gasto. Falha de
     verdade entra como consumo avulso.
   - Insumo: `quantity × (1 se chargedPerOrder, senão quantity do pedido)`, pelo id
     do insumo (`QuotedConsumable.id`).
6. **"Pesei o carretel".** Diálogo com "Peso na balança (g)" e "Carretel vazio (g)"
   (opcional, lembrado por cor, o último usado). Mostra "Saldo novo: X g (era Y g)"
   e grava o ajuste. Sem carretel vazio, o peso é o próprio saldo. Texto de ajuda:
   o carretel vazio vem na embalagem ou dá pra pesar um vazio.
7. **Saldo não vira "Acabou" sozinho.** O "tem / acabou" (`inStock`) continua sendo
   só da pessoa: uma conta errada nunca esconde uma cor do Orçamento. Saldo zero ou
   negativo aparece em vermelho e entra nos alertas (item B). Saldo negativo é
   mostrado como está ("−40 g"), porque indica que falta registrar uma compra ou
   pesar.
8. **Parar de controlar**: apaga o controle e os movimentos daquele item, com
   confirmação. As compras continuam registradas (são dinheiro, e a Leva 13 usa).
9. **Excluir** uma cor, um filamento ou um insumo apaga o controle deles (o aviso de
   exclusão diz). Arquivar mantém.

### B. Alertas de estoque (decisão 159)

10. **Limite por item** ("Avisar abaixo de", em g ou un), opcional, nasce vazio.
    Item com limite e saldo abaixo dele está **baixo**; item controlado com saldo
    zero ou menor está **acabando** (vale com ou sem limite).
11. **Reservado**: o que os pedidos **Aprovados** e **Em impressão** ainda vão gastar
    de cada item (mesma conta do item 5). **Disponível** = saldo − reservado.
12. **Na barra lateral**, o item Estoque mostra um selo com o número de itens baixos
    ou acabando (some quando é zero).
13. **No Orçamento**, pra cada cor e cada insumo controlados que o orçamento usa:
    se o consumo deste orçamento for maior que o disponível, um aviso em âmbar:
    "Falta PLA Voolt Vermelho: este pedido usa 320 g e sobram 250 g livres (180 g já
    reservados pra pedidos aprovados)." Ao editar um pedido Aprovado ou Em impressão,
    o próprio pedido sai do reservado (não conta duas vezes). Só avisa, nunca
    impede. Cor sem controle não avisa nada (ou continua a regra de hoje: esgotada
    nem aparece pra escolher).

### C. Registro de compras (decisão 160)

14. **Compra** = data, fornecedor (opcional), link (opcional), frete (R$, opcional,
    nasce zero), observação (opcional) e **uma ou mais linhas**. Cada linha: o item
    (cor de filamento ou insumo), a quantidade (g ou un) e o preço pago pela linha.
    Na linha de filamento, atalhos "1 kg" e "500 g" preenchem a quantidade.
15. **Frete rateado pelo valor**: cada linha recebe a parte do frete proporcional ao
    preço dela. **Custo da linha** = preço + parte do frete. Custo por kg (ou por
    unidade) = custo da linha / quantidade. O formulário mostra esse custo por linha.
    Arredondar só na exibição.
16. **Soma no saldo**: cada linha de item controlado vira um movimento de compra.
    Linha de item **sem** controle marca a cor (ou nada, no insumo) como "tem" e não
    cria saldo; o formulário diz "Esta cor não tem controle de saldo: a compra fica
    registrada, mas não soma gramas".
    Editar a compra refaz os movimentos dela; excluir a compra apaga os movimentos
    dela, com confirmação.
17. **Preço do cadastro pela última compra, com confirmação.** Ao salvar, pra cada
    filamento (ou insumo) da compra cujo custo novo difere do cadastro em mais de
    meio centavo, o formulário mostra uma caixa marcada "Atualizar o preço de PLA
    Voolt de R$ 89,90/kg pra R$ 97,40/kg". Duas cores do mesmo filamento na mesma
    compra: o custo por kg do filamento é o custo somado das linhas dele dividido
    pelos kg somados. Desmarcar grava a compra sem mexer no cadastro. O catálogo vivo
    (decisão 102) já avisa os produtos que mudaram de custo, sem trabalho novo.
    Pedido salvo nunca muda (retrato congelado).
18. **Custo médio só como informação**: o painel do item no Estoque mostra "Média das
    compras: R$ X/kg" (média ponderada pela quantidade de todas as compras do item).
    Não muda preço.
19. **Fornecedor como texto com sugestão**, sem cadastro próprio: o campo sugere os
    já usados (mesma regra da categoria da decisão 102: tira espaços e reaproveita a
    grafia de um já usado ignorando maiúsculas). O link é por compra (abre no
    navegador) e o painel do item oferece "Comprar de novo" com o link da última
    compra que tinha link.

### D. Lista de compras (decisão 161)

20. Pra cada item controlado: **comprar** = max(0, reservado + limite − saldo)
    (limite vazio conta zero). Entra na lista quem tem comprar > 0. A linha mostra o
    total e o porquê: "pedidos aprovados: 180 g · saldo: 120 g · limite: 200 g".
21. Também entram, numa seção à parte, as cores **sem controle** marcadas "Acabou"
    que algum pedido Aprovado ou Em impressão usa: "Marcada como acabou, usada em 2
    pedidos aprovados".
22. Ordem: filamentos (por nome, depois cor) e insumos. "Copiar lista" copia em texto
    simples, uma linha por item ("PLA Voolt Vermelho: 260 g"), pra colar numa
    mensagem pro fornecedor. "Registrar compra" a partir da lista abre o formulário
    já com as linhas e quantidades da lista (preço vazio).
23. Lista vazia: "Nada a comprar pros pedidos aprovados." e, se não houver nenhum
    item controlado, como ativar o controle.

### E. Estoque dos insumos (decisão 162)

24. Mesmas regras de A, B e D, em **unidades** (número decimal, pra quem controla em
    metro ou ml). Insumo não tem "Pesei o carretel": só "Corrigir saldo". Insumo não
    tem "tem / acabou" hoje e não ganha.

### F. Tela Estoque (decisão 163)

25. **Item novo na barra lateral**, "Estoque", no grupo Cadastros, depois de Insumos.
    Ícone novo copiado do Material Symbols (`shelves` ou `warehouse`, a escolher pelo
    `construtor-ui` contra o `inventory_2` dos Insumos, que precisa continuar
    distinto). Atalho Ctrl/Cmd+9; **Configurações passa pra Ctrl/Cmd+vírgula** (o
    atalho comum de preferências), e a dica do atalho acompanha. Sobre continua sem
    atalho.
26. **Três vistas** num segmentado, no padrão do Histórico (decisão 102):
    `Saldo | Compras | Lista de compras (N)`.
27. **Saldo**: aviso no topo quando há itens baixos ou acabando. Duas seções,
    Filamentos (agrupados por filamento, uma linha por cor, com a amostra de cor) e
    Insumos. Cada linha controlada: nome, saldo, reservado (quando > 0), limite, e o
    estado (baixo/acabando). Ações por linha: "Pesei o carretel" (só filamento),
    "Consumo avulso", e abrir o painel. Itens sem controle ficam numa seção recolhida
    "Sem controle de saldo (N)", cada um com "Controlar saldo". Filamentos e insumos
    arquivados não aparecem.
28. **Painel do item** (lateral, `ListScreenLayout` + `FormPanel`, como os
    cadastros): saldo, reservado, disponível, média das compras, "Comprar de novo";
    campos "Avisar abaixo de" e "Carretel vazio"; o **histórico de movimentos**
    (data, tipo, quantidade com sinal, pedido/compra/motivo), com excluir nos avulsos
    e ajustes; "Corrigir saldo"; "Parar de controlar".
29. **Compras**: lista por data, a mais nova em cima: data, fornecedor, resumo das
    linhas ("PLA Voolt Vermelho 1 kg, Argola 50 un"), total pago. Botão "Registrar
    compra" no cabeçalho da tela (vale pras três vistas). Clicar abre a compra no
    painel pra editar ou excluir. Vazio: "Nenhuma compra registrada."
30. **Lista de compras**: item D.
31. **Fora da tela Estoque**:
    - Filamentos: o chip da cor controlada mostra o saldo ("Vermelho · 640 g"), em
      vermelho quando baixo ou acabando. O toque no chip continua alternando
      tem/acabou.
    - Insumos: a linha do insumo controlado mostra "8 un em estoque" nos detalhes.
    - Orçamento: aviso do item 13.

### G. Importar e exportar filamentos (decisão 164)

32. Na tela Filamentos, "Exportar filamentos" grava um `.json` com os filamentos não
    arquivados: nome, marca, tipo, densidade, diâmetro, preço por kg e cores (nome e
    hex). **Sem** saldo, compras, ids nem "tem / acabou". Formato:
    `{"format": "3dreport-filaments", "version": 1, "filaments": [...]}`. Nome
    sugerido: `filamentos-3dreport-AAAA-MM-DD.json`.
33. "Importar filamentos" lê esse arquivo e acrescenta os filamentos com ids novos,
    pulando os que já existem com o mesmo nome, marca e tipo (ignorando maiúsculas e
    espaços). Cores importadas nascem "tem". Aviso no fim: "5 filamentos importados,
    2 já estavam no cadastro." Arquivo que não é desse formato, ou de versão maior
    que a conhecida: aviso de erro sem importar nada. Valores inválidos (densidade
    zero, nome vazio): o filamento é pulado e contado à parte ("1 com dados
    inválidos").
34. Seletor de arquivo nativo (`java.awt.FileDialog`), como o resto do app.

## Fora de escopo

- **Local do carretel, registro de secagem e etiqueta QR.** Impacto baixo, e a
  etiqueta só faz sentido com câmera pra ler o QR, que o desktop não tem. Volta pro
  roadmap junto da Leva 21 (Android).
- **Base aberta de filamentos (Open Filament Database).** Precisa conferir a
  licença e o tamanho embutido; impacto baixo. Fica no catálogo.
- **Compatibilidade com Spoolman.** O Spoolman é um servidor na rede do vendedor:
  ler e escrever nele é integração conectada. Passa pra Leva 18 (integração direta),
  que já trata conexão. O formato de exportação do item G não tenta imitar o do
  Spoolman.
- **Cadastro de fornecedores** com contato e endereço: aqui o fornecedor é só texto
  sugerido com link na compra.
- **Lucro real do mês com as compras**: Leva 13.
- **Carretel como unidade** (vários carretéis da mesma cor com saldo separado).
- Baixa automática em "Em impressão", baixa proporcional e baixa pela reserva de
  falha.

## Decisões

| Nº | Decisão | Situação |
|---|---|---|
| 158 | Saldo opcional por cor e por insumo, soma de movimentos gravados; baixa por transição pra Pronto/Entregue, que volta se o pedido volta e fica se ele é excluído; consumo avulso e "Pesei o carretel"; saldo não marca "Acabou" sozinho. Revisita a 39 | aprovada em 2026-11-18 |
| 159 | Limite opcional por item; reservado = Aprovado + Em impressão; selo na barra lateral; aviso (não bloqueio) no Orçamento | aprovada em 2026-11-18 |
| 160 | Compra com várias linhas e frete rateado pelo valor; soma no saldo; preço do cadastro pela última compra, com caixa marcada que dá pra desmarcar; média só informativa; fornecedor texto sugerido + link | aprovada em 2026-11-18 |
| 161 | Lista de compras = reservado + limite − saldo, mais as cores sem controle marcadas "Acabou" usadas em pedidos; copiar e registrar compra a partir dela | aprovada em 2026-11-18 |
| 162 | Insumos com o mesmo controle, em unidades decimais | aprovada em 2026-11-18 |
| 163 | Tela Estoque na barra (Cadastros), três vistas, Ctrl+9; Configurações vai pra Ctrl+vírgula | aprovada em 2026-11-18 |
| 164 | Importar/exportar filamentos em JSON próprio, versão 1, pula repetidos por nome+marca+tipo | aprovada em 2026-11-18 |

## Modelo e cálculo (core e dados)

Nomes indicativos; o `engenheiro-core` pode ajustar mantendo as regras.

### core (`model/` e `report/`, alvos JVM e JS)

- `StockItemRef`: identifica o item. Duas formas: cor de filamento
  (`filamentId`, `colorId`) e insumo (`consumableId`). Serializável, com igualdade
  por valor.
- `StockItem` (o controle ligado): `ref`, `lowThreshold: Double? = null`,
  `emptySpoolGrams: Double? = null` (só filamento), `id`. Existir = controlado.
- `StockMovement`: `id`, `ref`, `epochMillis` (ou data), `amount: Double` (com
  sinal), `kind` (`START`, `PURCHASE`, `ORDER`, `MANUAL_USE`, `ADJUSTMENT`),
  `quoteId: String? = null`, `orderNumber: Int? = null`, `purchaseId: String? = null`,
  `reason` (`FAILURE`, `TEST`, `OWN_USE`, `OTHER`, só em `MANUAL_USE`),
  `note: String? = null`.
- `Purchase`: `id`, `date`, `supplier: String? = null`, `link: String? = null`,
  `shipping: Double = 0.0`, `note: String? = null`, `lines: List<PurchaseLine>` (não
  vazia). `PurchaseLine`: `ref`, `amount > 0`, `price >= 0`. Funções puras:
  `shippingShare(line)`, `lineCost(line)`, `unitCost(line)` (por g ou un) e
  `costPerKgByFilament()` / `unitCostByConsumable()` pra o item 17. Frete rateado
  com soma de preços zero: divide igual entre as linhas.
- `StockReport` (objeto puro, estilo `MaintenanceReport`):
  - `consumptionOf(quote: SavedQuote, filaments: List<Filament>): Map<StockItemRef, Double>`
    (item 5; recebe os filamentos pra resolver cor `null` de filamento de uma cor).
  - `balance(ref, movements)`, `reserved(ref, savedQuotes, filaments, excludingQuoteId?)`,
    `available`, `state` (OK, LOW, OUT) e `shoppingList(...)` (item D, inclusive as
    cores sem controle marcadas "Acabou").
  - `averagePurchaseCost(ref, purchases)` (item 18).
  - `PRINTED_STATUSES` reaproveita `MaintenanceReport.PRINTED_STATUSES`;
    `RESERVED_STATUSES = {APROVADO, EM_IMPRESSAO}`.
- `StockLedger.reconcile(previous: List<SavedQuote>, current: List<SavedQuote>,
  movements, trackedRefs, filaments, nowMillis): List<StockMovement>`: a regra do
  item 4 como função pura. Compara cada pedido pelo id: entrou em impresso → cria;
  saiu de impresso (e ainda existe) → remove; continua impresso com consumo
  diferente → refaz; sumiu da lista → mantém; continua impresso sem mudança → nada.
  Só cria movimento pra item controlado. Testes cobrindo cada transição, o restaurar
  da lixeira e o pedido já impresso antes do controle começar.
- Funções de ajuste: `startTracking(amount)` grava `START` com o valor (é o primeiro
  movimento); `adjustTo(newBalance)` grava `ADJUSTMENT` com `newBalance − balance`;
  "Pesei o carretel" = `adjustTo(peso − carretelVazio)`.
- Exportar/importar filamentos: serializador e leitor puros no `core` (formato do
  item 32), com o casamento de repetidos (item 33) e testes de arquivo inválido e de
  versão desconhecida.

### dados (`composeApp/data/`)

- Arquivos novos, todos `RecordCollection` (decisão 106): `stock-items.json`,
  `stock-movements.json`, `purchases.json`. **Nenhum campo novo em `Filament`,
  `FilamentColor` nem `Consumable`**, e `filaments.json` não muda de formato (mesma
  escolha da manutenção na decisão 96). O backup já leva a pasta inteira; conferir
  com um teste que restaurar um backup antigo, sem esses arquivos, abre com estoque
  vazio.
- `StockRepository`: itens, movimentos e compras como `StateFlow`; ativar/parar,
  limite e carretel vazio, avulso, ajuste, excluir movimento, salvar/editar/excluir
  compra (refazendo os movimentos dela), e `deleteAllFor(filamentId | colorId |
  consumableId)`.
- **Reconciliação**: quem liga o `QuoteHistoryRepository` ao estoque guarda a última
  lista de pedidos vista e chama `StockLedger.reconcile` a cada mudança. A primeira
  lista ao abrir o app é só a base (nada é criado na abertura). Teste: mover um
  pedido pra Pronto, voltar, excluir, restaurar; e abrir o app com pedidos já
  Prontos não desconta.
- Atualizar o preço do cadastro (item 17) usa `FilamentRepository` e
  `ConsumableRepository`, só nos itens marcados.
- Excluir cor/filamento/insumo chama `deleteAllFor`.
- Importação usa `FilamentRepository.add` com ids novos.

### O que garante que nada muda sozinho

- Sem ativar o controle, nenhum arquivo novo tem conteúdo e nenhuma tela muda além
  do item Estoque na barra (com a lista "Sem controle de saldo") e das ações de
  importar/exportar.
- Nenhuma fórmula de preço muda. O preço do cadastro só muda por compra registrada
  com a caixa marcada.
- `inStock` continua só manual. Orçamento continua escondendo cor esgotada como
  hoje.
- Abrir o app com pedidos já Prontos não desconta nada.

## Telas

Componentes do kit: `ScreenHeader`, `ListScreenLayout`, `FormPanel`,
`PanelSection`, `ListRow`, `RowAction`, `ArchivedSection` (pra seção recolhida),
`AppDialog`, `AppTextField`, `AppSuggestField`, `AppDropdownField`, `ColorSwatch`,
`EmptyState`, `HelpText`, o segmentado do Histórico. Selo da barra lateral é
componente novo pequeno no kit, seguindo `docs/design-system.md`. Todo texto novo em
`strings.xml` **e** na tradução em inglês (Leva 11B), sem travessão.

Esboço do Saldo:

```
Estoque                                          [Registrar compra]
[ Saldo | Compras | Lista de compras (2) ]

 ! 2 itens acabando ou abaixo do limite

 Filamentos
   PLA Voolt
     (●) Vermelho     640 g   180 g reservados      [Pesei] [Avulso] [>]
     (●) Preto        120 g   abaixo de 200 g       [Pesei] [Avulso] [>]
   PETG 3D Fila
     (●) Branco       -40 g   acabando              [Pesei] [Avulso] [>]
 Insumos
     Argola de chaveiro   8 un   abaixo de 20 un            [Avulso] [>]

 > Sem controle de saldo (6)
     PLA Voolt Azul                                  [Controlar saldo]
     Caixa P                                         [Controlar saldo]
```

Esboço do formulário de compra (painel):

```
Registrar compra
 Data [18/11/2026]   Fornecedor [Voolt loja oficial  v]
 Link [https://...]
 Itens
   [PLA Voolt · Vermelho  v] [1000] g (1 kg)(500 g)  R$ [89,90]   R$ 97,40/kg
   [Argola de chaveiro    v] [50] un                 R$ [12,00]   R$ 0,26/un
   + Adicionar item
 Frete R$ [18,00]   Total R$ 119,90
 [x] Atualizar o preço de PLA Voolt de R$ 89,90/kg pra R$ 97,40/kg
 [x] Atualizar o custo de Argola de chaveiro de R$ 0,20 pra R$ 0,26
                                                  [Cancelar] [Salvar]
```

Aviso no Orçamento (no cartão da impressão, embaixo do filamento; insumos na seção
de insumos):

```
 ! Falta PLA Voolt Vermelho: este pedido usa 320 g e sobram 250 g livres
   (180 g já reservados pra pedidos aprovados).
```

Estados: vazio de cada vista (itens 23 e 29), arquivo inválido na importação (item
33), confirmação ao parar de controlar e ao excluir compra. Números de gramas sem
casa decimal na tela; unidades de insumo com até duas casas, sem zeros sobrando.

## Local ou conectado

Tudo local: três arquivos novos na pasta de dados, entram no backup existente. Nada
exige conexão. No conectado futuro (Leva 21/22): movimentos são registros com data
de criação e exclusão lógica (decisão 106), então dá pra sincronizar somando; a
reconciliação por transição precisa rodar num lugar só (o dispositivo que muda o
andamento), o que já é verdade hoje. Spoolman (Leva 18) é conexão na rede local do
vendedor, sem servidor nem custo pro projeto. LGPD: fornecedor é empresa, sem dado
pessoal novo.

## Critérios de aceite

- `scripts/check.sh build` verde, com testes no `core` pra consumo por pedido,
  reconciliação (cada transição), saldo, reservado, lista de compras, rateio de
  frete, custo por kg da compra e importação/exportação.
- Teste de compatibilidade: pasta de dados da 2.2 sem os arquivos novos abre igual,
  e `filaments.json` gravado antes continua abrindo.
- Nenhum teste toca `~/.3dreport` (comparar `ls -la ~/.3dreport` antes e depois).
- Capturas novas do Estoque (três vistas, com e sem dados) e do aviso no Orçamento,
  em português e inglês, revisadas pelo `revisor-design`.
- Teste manual do responsável: ativar o controle de uma cor com 1000 g, fazer um
  pedido de 100 g e levar a Pronto (saldo 900), voltar pra Aprovado (1000, 100
  reservados), excluir um pedido Pronto (saldo não volta), registrar consumo avulso,
  pesar o carretel, registrar uma compra com frete e ver o preço do filamento
  atualizar, ver o aviso no Orçamento e a lista de compras, exportar e importar
  filamentos num app com dados temporários.
