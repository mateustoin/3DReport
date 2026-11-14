# Roadmap

Lista viva de evoluções e próximas implementações. Diferente de
[decisions.md](decisions.md) (o que já foi decidido), este documento é um
**backlog**: itens aqui ainda não têm data definida. Um item só sai daqui pra
`decisions.md` quando alguém propuser como implementar, o responsável do
projeto aprovar, e a mudança entrar no código (ver
[development.md](development.md#fluxo-de-mudanças)).

As seções do [catálogo](roadmap/catalogo.md) estão em **ordem de prioridade** (definida em 2026-09-15):
funcionalidades de produto e UI/UX primeiro; só depois disso, instaladores
desktop; só depois disso, infraestrutura/qualidade (adiantada em 2026-09-17,
decisão 49, antes mesmo do repositório abrir); Android é a menor prioridade
de todas, fica pra quando o projeto estiver consolidado e houver demanda,
ainda sem previsão. O repositório ficou **público** em 2026-09-17 (decisão
50), destravando o que dependia disso: instalação real pelos links do site,
downloads da página de releases e a assinatura "Gerado com 3DReport" nos
PDFs (seção 1, "Vendas e divulgação").

**Ordem de implementação dos itens pendentes:** ver o "Plano de evolução 2"
logo abaixo (revisão de 2026-09-28, depois da
[pesquisa de mercado](pesquisa-de-mercado-2026-09.md)). Ele organiza em levas
tudo o que ainda está aberto, inclusive itens que já estavam registrados nas
seções do catálogo.

Este arquivo guarda só o **backlog vivo**. O resto do roadmap fica em dois
arquivos, pra este aqui continuar curto:

- [roadmap/plano-1-historico.md](roadmap/plano-1-historico.md): o "Plano de
  evolução 1" (levas 0 a 9, quase todo feito), como histórico.
- [roadmap/catalogo.md](roadmap/catalogo.md): as seções numeradas, que continuam
  sendo o catálogo completo por assunto. É lá que está o texto completo dos itens
  marcados *(já no roadmap)*.

**Como uma ideia de fora entra aqui** (decisão 83): sugestões de quem usa ou
testa o app chegam como **issue no GitHub**, uma ideia por issue (template
"Sugerir uma funcionalidade", ver [CONTRIBUTING.md](../CONTRIBUTING.md#sugerindo-ideias)).
O responsável do projeto faz a triagem e a ideia vira item novo, complementa
um item existente, é fechada como "já existe" ou é descartada com o motivo
registrado aqui. Este documento continua sendo a única fonte de verdade: não
existe arquivo paralelo de ideias. Itens vindos de fora levam o crédito no
texto, no padrão "(sugerido por Fulano na issue #N, data)".

## Plano de evolução 2 (revisão de 2026-09-28, pesquisa de mercado)

Revisão feita a pedido do responsável do projeto depois da
[pesquisa de mercado de 2026-09-27](pesquisa-de-mercado-2026-09.md). **Todas as
ideias da pesquisa entram aqui**, intercaladas numa fila só com os itens que ainda
estavam abertos no roadmap. O Plano 1 (levas 0 a 9) está quase todo concluído e
fica em [roadmap/plano-1-historico.md](roadmap/plano-1-historico.md). A numeração
continua a partir dele, na Leva 10.

**Critérios de ordem**, nesta sequência:

1. O que faz o vendedor perder dinheiro sem perceber.
2. O que ele usa todo dia pra vender e receber.
3. Organizar estoque, dinheiro e produção.
4. Facilitar a chegada de quem vem da planilha.
5. O que é conectado mas **não custa nada ao projeto**: primeiro a integração
   direta com a conta do próprio vendedor (D1), depois páginas publicadas numa
   hospedagem grátis (D2).
6. As apostas grandes que continuam locais.
7. Por último, o que exige servidor do projeto (D3). Só começa depois de decidir
   quem paga (decisão 15) e como fica o posicionamento (decisão 90), porque o app
   é gratuito e vive de doação.

Os degraus D1, D2 e D3 estão definidos na
[Parte 2 da pesquisa](pesquisa-de-mercado-2026-09.md#parte-2-funcionalidades-conectadas-nuvem).
Quando dois itens mexem no mesmo lugar, entram na mesma leva, pelo motivo da Leva
1: não obrigar o vendedor a recalibrar os preços várias vezes seguidas.

**Convenções:**

- Item novo traz a descrição completa aqui, com a origem ("Visto em", ou "ideia
  derivada" quando não veio de um produto) e a estimativa da pesquisa (esforço P, M
  ou G; impacto A, M ou B).
- Item que já estava no roadmap aparece marcado *(já no roadmap)*, com o lugar
  onde está a descrição completa, pra não existirem duas fontes de verdade (mesma
  regra do Plano 1).
- Item que contraria uma decisão aprovada diz "revisita a decisão N" e precisa de
  decisão própria antes de implementar.
- Campo novo nasce neutro (zero ou desligado): atualizar o app não muda o preço de
  ninguém em silêncio, e pedido salvo continua congelado.

**Resumo da ordem:** preço certo → textos prontos pra tradução → receber e
conversar → inglês → estoque → dinheiro → cliente → produção → oferta e pronta-entrega → chegar e ficar → conectado sem custo
(D1, depois D2) → apostas locais → Android e sincronização sem servidor → servidor →
depois do servidor.

### Leva 10: Preço certo, parte 2 (feita, decisões 121 a 126, para a 2.2)

Tudo aqui mexe no custo ou no preço calculado (`PricingCalculator`), por isso sai
junto.

- [x] **Taxa fixa por item no canal de venda, com faixas de preço.** **Feito (decisão 121, 2026-09-29).** O canal
  (`SalesChannel`) só tem taxa percentual, mas Shopee e Mercado Livre cobram
  também um valor fixo por item vendido. Na Shopee, em 2026, ele vai de R$ 4 a
  R$ 26 conforme a faixa de preço (com R$ 3 a mais por item pra vendedor CPF de
  alto volume); no Mercado Livre, fica perto de R$ 6,25 a R$ 6,75 nos itens
  baratos. Num chaveiro de R$ 15 na Shopee, R$ 4 são 27% do preço, e hoje o app
  não conta isso. Escopo sugerido: valor fixo por item no canal e, opcional, uma
  tabela de faixas ("até R$ X, fixo Y"). Cuidados na conta: o percentual já infla
  o preço (`venda = base / (1 − deduções)`, decisões 26 e 78), e o fixo entra
  somado à base antes da divisão; com faixas, o preço final pode mudar de faixa,
  então o cálculo precisa convergir (testar a borda de cada faixa); o fixo é por
  item, então multiplica pela quantidade. Visto em: regra dos próprios
  marketplaces; 3D Control e Printora calculam a taxa por marketplace. Esforço
  P/M, impacto A.
- [x] **Custo de embalagem e insumos por produto.** **Feito (decisão 122, 2026-09-29),** com tela própria em Cadastros; margem sim, reserva de falha não. Argola de chaveiro, ímã,
  parafuso, tinta, caixa e saquinho são custo real que hoje só cabe no custo
  administrativo (um valor único pra todo pedido) ou num serviço (que soma no preço
  cobrado sem custo, decisão 25, e por isso não protege a margem). Proposta: um
  cadastro simples de insumos (nome e custo por unidade) e, no orçamento e no
  produto, os insumos usados com a quantidade, entrando no custo de produção.
  Decidir na implementação se a margem incide sobre o insumo (como no material) ou
  não (como na hora de trabalho, desde a 2.1). O estoque dos insumos vem na Leva
  12. Visto em: 3DTAG, SISTEMA3D, Gestor 3D ("extras"), 3DPCC, Craftybase,
  CakeBoss. Esforço M, impacto A.
- [x] **Minutos de trabalho embutidos em cada serviço** **Feito (decisão 123, 2026-09-29),** sugerindo o valor e contando o serviço no lucro. *(já no roadmap: texto
  completo na Leva 1 do Plano 1)*. Entra aqui porque mexe no mesmo custo de
  produção.
- [x] **Frete grátis descontando do lucro** **Feito (decisão 124, 2026-09-29).** *(já no roadmap: Leva 3 do Plano 1)*.
  Ganhou urgência: desde março de 2026 o programa de frete grátis é obrigatório pra
  todo vendedor da Shopee. Continua pendente a decisão de onde o lucro líquido
  passa a ser calculado.
- [x] **Preço mínimo por pedido.** **Feito (decisão 125, 2026-09-29).** Uma peça muito pequena (um chaveiro de 3 g) sai
  por centavos mesmo com a conta certa, e o vendedor não quer ligar a impressora
  por menos de R$ X. Configuração opcional: se o preço calculado ficar abaixo do
  mínimo, sobe pra ele, e a nota mostra que o mínimo foi aplicado. Ideia derivada.
  Esforço P, impacto M.
- [x] **Acréscimo por urgência.** **Feito (decisão 125, 2026-09-29).** Prazo curto fura a fila e às vezes ocupa outra
  impressora. Um percentual configurável aplicado quando o pedido é marcado como
  urgente, visível na nota e, se o vendedor quiser, no PDF como "entrega expressa".
  Visto em: DigiFabster e Xometry (preço conforme o prazo). Esforço P, impacto M.
- [x] **Bandeira tarifária e tarifa branca no kWh.** **Feito (decisão 126, 2026-09-29),** como um diálogo "Calcular o kWh". A conta de luz muda de preço
  pela bandeira (verde, amarela, vermelha) e, na tarifa branca, pelo horário.
  Mínimo viável: um ajudante pra calcular o kWh efetivo, ou um acréscimo por
  bandeira escolhido nas Configurações. Visto em: 3D Prime (tarifa da ANEEL por
  estado), ha-bambu-costs (tarifa por horário). Esforço P, impacto B.

### Leva 10B: Textos prontos pra tradução (idioma, fase A)

Primeira metade do item *(já no roadmap)* "Idioma da interface configurável"
(seção UX extras), avaliado em 2026-09-28 a pedido do responsável do projeto: a
pesquisa de mercado não tinha tratado disso. **Nada muda pro usuário**; o objetivo
é que tudo o que vier depois já nasça traduzível. Vem logo depois da Leva 10 de
propósito: as Levas 11 a 17 criam muitas telas novas, e preparar os textos antes
evita refazer o trabalho em cima de umas 1.500 frases em vez das cerca de 1.050 de
hoje. Pode sair junto da 2.2 ou numa versão só dela. Trabalho quase todo
mecânico, bom pra subagentes com revisão.

- [ ] **Textos em recursos.** As cerca de 1.050 frases escritas direto no código,
  em uns 140 arquivos (as que mais têm: Orçamento, Configurações e Pedidos), vão
  pra arquivos de texto do Compose (`composeResources`, com `stringResource`), só
  em português por enquanto. Plurais ("3 impressões") como plurais de verdade.
- [ ] **Mensagens dos ViewModels como chave e valores.** Avisos e mensagens montados
  fora das telas (`QuoteHistoryViewModel`, `QuoteViewModel` e outros) passam a
  levar a chave do texto e os valores, e a tela é que traduz, porque o
  `stringResource` só existe dentro da tela.
- [ ] **Rótulos fora do `core`.** `OrderStatus`, `QuoteKind` e `ThemeMode` carregam
  o texto em português (`OrderStatus("Orçado")`); o rótulo passa pra interface, e
  o `core` fica sem texto de tela.
- [ ] **Nome automático do pedido sem depender do idioma.** O pedido sem nome ganha
  o prefixo `"Orçamento - "` (`SavedQuote.AUTO_NAME_PREFIX`) e é reconhecido por uma
  expressão regular sobre esse texto, que quebraria em outro idioma. Vira uma marca
  no registro, com migração e teste (regra da decisão 106).
- [ ] **PDF, mensagem e imagem com textos em recursos**, pelo mesmo mecanismo (fora
  da tela, lendo o recurso diretamente).
- [ ] **Datas e números pelo idioma.** `DateFormat.jvm.kt` usa `pt-BR` fixo, e a
  entrada numérica (`DecimalInput`) supõe que "2.700" é dois mil e setecentos.
  Passam a seguir o idioma escolhido (na fase A, continua pt-BR).

Esforço G (somando as duas fases), impacto A no alcance (ver Leva 11B).

### Leva 11: Receber e conversar com o cliente

O que o vendedor faz o dia inteiro depois de mandar o orçamento: cobrar, receber e
avisar. Tudo barato e local; nenhum item precisa de internet (o Pix copia e cola é
gerado no próprio computador).

- [ ] **Pix copia e cola e QR Code no orçamento.** Chave Pix do vendedor em
  Configurações (Documentos pro cliente); o PDF, a mensagem do WhatsApp e a imagem
  quadrada ganham o código copia e cola e o QR com o valor. O formato do Pix (BR
  Code, padrão EMV com CRC16) é gerado sem internet, então é um Pix "estático": o
  app não sabe se foi pago (a confirmação automática é da Leva 18). O valor pode
  ser o total ou o sinal (item abaixo). Conferir a leitura nos apps dos bancos
  antes de publicar. Visto em: Calc3D Pro, Kyte, Gestor 3D. Esforço P, impacto A.
- [ ] **Situação do pagamento, com sinal e pagamento parcial.** Junta o item
  *(já no roadmap)* "Sinal/pagamento parcial no orçamento" (seção Produção e
  precificação, texto completo lá) com o que a pesquisa mostrou: cada pedido com
  a situação (não pago, sinal pago, pago), a forma (Pix, cartão, dinheiro) e a
  data. **Revisita a frase "não é objetivo do app virar um sistema de contas a
  receber"**: continua não sendo, porque aqui não há vencimento, cobrança nem
  conciliação, só o registro do que entrou. Visto em: Manuflo, Atlas3D, Calc3D
  Pro, 3DPCC; sinal com vencimento próprio em Invoice Ninja e Jobber. Esforço P/M,
  impacto A.
- [ ] **Recibo em PDF.** Ao registrar um pagamento (ou o sinal), gerar um recibo
  simples com a marca do vendedor, pelo mesmo exportador do orçamento: número do
  pedido, valor, forma e data. Depende do item acima. Visto em: Kyte, GestãoClick,
  apps de orçamento. Esforço P, impacto M.
- [ ] **Mensagens prontas por andamento.** Hoje só o orçamento vira mensagem de
  WhatsApp. Cada mudança de andamento pode oferecer uma mensagem pronta e
  editável: "seu pedido foi aprovado, fica pronto até X", "ficou pronto", "saiu
  pra entrega, rastreio Y", "lembrete: falta o pagamento de R$ Z". Abre pelo mesmo
  `wa.me` da decisão 80, sem API. É a versão local do portal de acompanhamento.
  Modelos editáveis em Configurações. Visto em: Custos3D (proposta formatada),
  Jobber, Nuvemshop. Esforço P, impacto A.
- [ ] **Lembrete de orçamento parado.** Orçamento "Orçado" há N dias
  (configurável) aparece destacado em Pedidos, com "Retomar conversa" (mensagem
  pronta, item acima) ou "Marcar como cancelado". Aumenta a conversão sem custo.
  Visto em: Jobber (lembrete de orçamento sem resposta). Esforço P, impacto A.
- [ ] **Código de rastreio no pedido.** Campo opcional com o código, um link que
  abre o rastreio no navegador, e o código na mensagem "saiu pra entrega". Visto
  em: Tiny, 3D Control. Esforço P, impacto M.
- [ ] **Observações internas no pedido.** Hoje o pedido não tem campo de texto
  livre ("cliente quer a cor mais escura", "entregar na portaria"). Uso só interno
  (decisão 19). Ideia derivada, comum nos sistemas de gestão. Esforço P, impacto M.

### Leva 11B: Inglês no app, no README e no site (idioma, fase B)

Segunda metade do item de idioma. Depois da Leva 11, o app já tem o núcleo que
serve a qualquer vendedor, de qualquer país (G-code, preço certo, PDF, pedidos),
e esperar a Leva 17 atrasaria em meses o público maior sem motivo técnico.

**Por que vale** (avaliação de 2026-09-28): o nicho "grátis, sem internet e código
aberto" está vazio fora do Brasil (o PrintQuote3D tem 0 estrelas; o projeto aberto
mais popular achado na pesquisa, o de Reitberger, tem 53); quem usa o GitHub, o
Reddit e o Printables lê em inglês, e é disso que dependem estrelas, colaboradores e
divulgação espontânea; e a assinatura "Gerado com 3DReport" (decisão 91) passa a
circular num mercado muito maior. **O que pesa contra:** a busca por "3D print cost
calculator" é dominada pela Prusa, pela Omni e por dezenas de calculadoras, então o
site em inglês deve atrair pouco pelo Google (a divulgação lá fora vem de
comunidades); o diferencial brasileiro (Pix, MEI, Shopee, WhatsApp) não vai junto;
e toda funcionalidade nova passa a ter texto em dois idiomas. Espanhol, pra América
Latina (Mercado Libre, WhatsApp, venda informal), fica registrado como o próximo
candidato natural.

- [ ] **Inglês no app.** Tradução dos recursos da Leva 10B, escolha do idioma em
  Configurações (seguindo o do sistema por padrão, como o tema, decisão 35), datas e
  números no formato do idioma, PDF, mensagem e imagem no idioma do app.
- [ ] **Padrões de produto pra quem não vende no Brasil.** Pix e MEI (Levas 11 e
  13) aparecem só pra quem usa real; canais sugeridos passam a incluir Etsy, eBay e
  Amazon; o imposto e a moeda já são genéricos (decisão 46).
- [ ] **README em inglês** (um `README.en.md`, ligado no topo do README) e notas das
  versões com um resumo em inglês, já que hoje saem do CHANGELOG em português. Os
  documentos internos (`docs/`, decisões, roadmap) continuam em português.
- [ ] **Site em inglês.** As 5 páginas (cerca de 9.400 palavras) e os textos da
  calculadora (`calculadora.js`) numa pasta `/en/`, com seletor de idioma,
  marcações `hreflang`, `sitemap` e dados estruturados em inglês. Esforço M; o maior
  custo é manter as duas versões iguais depois.

### Leva 12: Estoque e compras

Todo concorrente brasileiro direto controla o filamento em gramas. Vem depois das
Levas 10 e 11 por ser maior, e antes da 13 porque o lucro real do mês usa as
compras.

- [ ] **Estoque em gramas com baixa automática. Revisita a decisão 39.** A decisão
  39 escolheu estoque manual e binário (tem ou acabou) porque a baixa automática é
  imprecisa: falha, teste e sobra gastam filamento sem virar pedido. O argumento
  novo tem duas partes. Primeiro, todo concorrente direto faz a baixa (3D Control,
  SISTEMA3D, Manager 3D, Atlas3D, Custos3D, Printora, Gestor 3D, e ainda
  Printforge, FoxTrack e Spoolman). Segundo, a imprecisão tem remédio conhecido:
  **consumo avulso**, igual às horas avulsas da manutenção (decisão 96), pra
  falha, teste e uso próprio; e **"pesei o carretel"**, que corrige o saldo pela
  balança. Proposta: saldo em gramas por cor (ou por carretel), baixa quando o
  pedido chega a Pronto (mesma régua do `MaintenanceReport`) e ajuste manual
  sempre possível; o estoque binário continua pra quem não quiser controlar
  gramas. Esforço M, impacto A.
- [ ] **Alertas de estoque.** Estoque baixo (limite por filamento) e, no
  Orçamento, "não tem filamento suficiente pra este pedido", contando o que os
  pedidos aprovados ainda vão gastar. Depende do item acima. Visto em: 3D Control,
  Printora, Gestor 3D, Manager 3D, plugin FilamentManager do OctoPrint. Esforço P,
  impacto A.
- [ ] **Registro de compras de filamento.** Data, quantidade e preço pago com
  frete. O preço por kg do cadastro passa a vir das compras (custo médio ponderado
  ou última compra, a decidir), e o catálogo vivo (decisão 102) avisa quando ele
  mudou. A compra soma no estoque do primeiro item. Visto em: Custos3D, SISTEMA3D,
  Craftybase, Pitada. Esforço M, impacto M.
- [ ] **Lista de compras dos pedidos.** "O que falta comprar pra entregar os
  pedidos aprovados": o consumo previsto menos o saldo, por filamento e por
  insumo. Visto em: Pitada (lista de compras somando os pedidos). Esforço P/M,
  impacto M.
- [ ] **Estoque dos insumos** da Leva 10 (argola, ímã, embalagem), com a mesma
  baixa e o mesmo alerta. Visto em: 3DTAG, SISTEMA3D, 3DPCC. Esforço P, impacto M.
- [ ] **Fornecedores.** Cadastro simples (nome, contato, link) ligado às compras.
  Visto em: 3DTAG, 3DPCC. Esforço P, impacto B.
- [ ] **Local do carretel, registro de secagem e etiqueta QR.** Onde o carretel
  está (prateleira, caixa seca), quando foi seco pela última vez (importa em
  materiais que absorvem umidade, como PETG, TPU e nylon) e uma etiqueta com QR
  pra achar o carretel no app. Visto em: Spoolman, FoxTrack, Spoolio,
  SimplyPrint. Esforço P/M, impacto B.
- [ ] **Base aberta de filamentos.** Enriquecer os presets de marca, tipo e
  densidade com a Open Filament Database, usada pelo PrintQuote3D (cerca de 2 mil
  produtos e 14 mil cores). Conferir a licença antes de embutir. Esforço M,
  impacto B.
- [ ] **Importar e exportar o catálogo de filamentos entre criadores** *(já no
  roadmap: seção Integrações)*.
- [ ] **Compatibilidade com Spoolman** *(já no roadmap: seção Integrações, no
  mesmo item)*. Entra aqui porque o estoque em gramas é o que permite conversar
  com ele. O Spoolman roda na rede do próprio vendedor, sem custo pro projeto.

### Leva 13: O dinheiro do negócio

Com pagamentos (Leva 11) e compras (Leva 12) registrados, o Dashboard pode mostrar
o dinheiro de verdade, e não só o lucro estimado de cada pedido. Continua sem virar
sistema financeiro: não há conciliação bancária nem boleto.

- [ ] **Despesas e lucro real do mês.** Lançar despesas (as compras da Leva 12
  entram sozinhas; ferramentas, peças, taxas e assinatura de STL à mão) e ver
  entradas, saídas e o que sobrou no mês. O custo fixo mensal de `PricingSettings`
  é a previsão; aqui é o realizado. Visto em: Manager 3D, SISTEMA3D, Atlas3D, 3D
  Control, 3DTAG, FoxTrack, ZupConfeitaria, Pitada. Esforço M, impacto A.
- [ ] **Ponto de equilíbrio e meta do mês.** "Faltam 40 h de máquina vendidas (ou
  R$ X) pra pagar os custos fixos deste mês", a partir dos custos fixos e do lucro
  por hora que o app já calcula, com uma meta opcional e barra de progresso. Visto
  em: 3DTAG. Esforço P, impacto M.
- [ ] **Resultado por canal de venda** no Dashboard: quanto vendeu e lucrou em
  cada canal (o `QuoteReport` hoje não separa). Visto em: 3D Control, Gestor 3D,
  Atlas3D. Esforço P, impacto M.
- [ ] **Retorno da impressora.** "A K1 já se pagou 64%", a partir do investimento
  já cadastrado e do que ela rendeu nos pedidos vendidos. Na tela Impressoras, ao
  lado da manutenção. Visto em: 3D Control. Esforço P, impacto M.
- [ ] **MEI: lembrete do DAS e limite de faturamento.** Lembrete mensal do DAS
  (dia configurável) e a barra "faturamento do ano x limite do MEI", com o limite
  configurável porque a lei muda. Opcional, pra quem é MEI. Visto em: SmartMEI,
  Omie Fit. Esforço P, impacto M.
- [ ] **Custo de falha real acumulado** *(já no roadmap: seção Produção e
  precificação)*. O consumo avulso da Leva 12 dá o registro das falhas que faltava
  pra calibrar o percentual de falha com dado real.
- [ ] **Exportar histórico pra CSV/Excel** *(já no roadmap: seção Integrações)*.
  Entra aqui porque pagamentos e despesas são o que o contador e a planilha
  paralela pedem.

### Leva 14: Cliente

- [ ] **Ficha do cliente.** O cadastro de clientes (decisão 106) guarda nome e
  contato. A ficha mostra os pedidos, o total gasto, o ticket médio, a frequência,
  a última compra e o ranking dos melhores clientes (o de quem mais pede desconto
  já existe, decisão 95). Visto em: 3D Control, FoxTrack, Manager 3D, Printforge,
  CakeBoss. Esforço P/M, impacto M.
- [ ] **Endereço e etiqueta de envio.** Endereço no cadastro do cliente (uso
  interno) e etiqueta 10x15 com remetente e destinatário, pra quem posta pelos
  Correios. Visto em: 3D Control, 3DTAG, Tiny. Esforço P/M, impacto M.
- [ ] **Tipo e origem do cliente.** Tipo (final, revendedor, empresa) e origem
  ("Instagram", "indicação", "feira"), com a origem somada no Dashboard: de onde
  vêm os clientes que compram. Visto em: 3D Control, Printforge. Esforço P,
  impacto B.
- [ ] **Datas comemorativas e aniversário.** Aniversário opcional do cliente e um
  calendário das datas que vendem (Dia das Mães, Dia dos Pais, Natal), com
  lembrete e mensagem pronta. Visto em: CakeBoss, ZupConfeitaria. Esforço P,
  impacto B.

### Leva 15: Produção e prazo

- [ ] **Calendário de produção por impressora.** A fila de hoje (decisão 68) soma
  as horas "Em impressão", sem datas. O calendário distribui os pedidos aprovados
  pelas impressoras e pelas horas produtivas do dia, mostra quando cada um deve
  ficar pronto, ordena a fila pelo prazo e, no Orçamento, sugere um prazo honesto
  ("cabe até sexta") além dos atalhos +3, +7 e +15 dias (decisão 85). Visto em:
  Printforge, AutoFarm3D, 3DTAG, Pitada, Manuflo, Printora, GrabCAD Print.
  Esforço M/G, impacto A.
- [ ] **Checklist das impressões de um pedido.** Num pedido com várias impressões
  (decisão 114), marcar cada mesa que já saiu; o cartão mostra "2 de 3
  impressas". Visto em: FoxTrack, SISTEMA3D. Esforço P, impacto M.
- [ ] **Ficha de produção e lista de separação.** PDF interno, que não vai pro
  cliente: impressões, impressoras, filamentos e cores, configurações de
  impressão, acabamento e observações; e uma lista de separação do dia. Visto em:
  Custos3D, PrintFarmDesk. Esforço P, impacto M.

### Leva 16: Oferta e pronta-entrega

Muda o modelo de "orçamento pra um cliente" pra "produto com estoque e mais de um
preço". Vários itens dependem da pronta-entrega, por isso ela abre a leva.

- [ ] **Estoque de pronta-entrega** *(já no roadmap: ideias registradas da Leva 7B
  do Plano 1)*. Base da venda rápida e do consignado.
- [ ] **Venda rápida pra feira.** Tela simples: toca no produto do catálogo,
  escolhe a forma de pagamento, vendeu; baixa a pronta-entrega e entra no
  Dashboard. Visto em: Atlas3D, Calc3D Pro, Kyte, Gestor 3D. Esforço M, impacto M.
- [ ] **Consignado.** Peças deixadas numa loja: quantas foram, quantas venderam,
  quantas voltaram, a comissão da loja e o acerto. Visto em: Atlas3D, Calc3D Pro,
  3dcalculate. Esforço M, impacto M.
- [ ] **Variações do mesmo produto** *(já no roadmap: ideias registradas da Leva
  7B do Plano 1)*.
- [ ] **Preço de atacado ou revenda.** Um segundo preço no produto (ou desconto
  por tipo de cliente, Leva 14) pra quem revende. Visto em: Atlas3D, 3D Control,
  Katana, inFlow. Esforço M, impacto M.
- [ ] **Tabela de preço por quantidade no PDF.** "1 un. R$ 15; 10 un. R$ 12 cada;
  50 un. R$ 9 cada", calculada pelo motor (o preparo já se dilui no lote, decisão
  77), pro cliente escolher. Não é o desconto por volume descartado na decisão 79:
  não há percentual, é a conta real de cada quantidade. Visto em: DigiFabster;
  matriz de preço das gráficas de bordado (Printavo, DecoNetwork). Esforço P/M,
  impacto M.
- [ ] **Orçamento com opções: rápido x acabamento fino** *(já no roadmap: Leva 5
  do Plano 1)*.
- [ ] **Preço de referência da concorrência.** Campo opcional no produto com o
  preço visto no mercado, e aviso quando o seu está muito abaixo ou acima. A busca
  automática é da Leva 18. Visto em: Gestor 3D. Esforço P, impacto B.

### Leva 17: Chegar e ficar

Facilidade de uso e migração de quem vem da planilha. A tela "Hoje" fica aqui, depois
das levas que criam o que ela mostra.

- [ ] **Importar de planilha.** Clientes, filamentos e produtos a partir de CSV ou
  XLSX, com um passo de "qual coluna é o quê". Quase todo mundo chega da planilha.
  Visto em: 3D Control, FoxTrack, 3DPCC. Esforço M, impacto A.
- [ ] **Importar a planilha de pedidos da Shopee e do Mercado Livre.** Os dois
  exportam os pedidos em planilha; importar vira pedidos com canal, valor e data.
  É a versão sem servidor da integração com marketplace (Leva 22). Visto em:
  Custos3D. Esforço M, impacto M.
- [ ] **Tela "Hoje".** Opcionalmente a primeira tela ao abrir: prazos de hoje e
  atrasados, orçamentos parados (Leva 11), quem falta pagar, estoque baixo (Leva
  12) e manutenção vencida. Mexe na barra lateral e nos atalhos (decisão 111),
  então precisa de decisão sobre onde fica. Visto em: Manager 3D (painel
  operacional), Printforge. Esforço M, impacto A.
- [ ] **Modo demonstração.** Abrir o app com dados de exemplo pra explorar sem
  cadastrar nada, reaproveitando os dados do gerador de prints (`renderScreenshots`,
  decisão 120), numa pasta separada que não se mistura com os dados reais. Visto
  em: Custos3D (demonstração), 3D Control (plano grátis). Esforço P, impacto M.
- [ ] **Busca geral.** Um campo só (atalho Ctrl/Cmd+K) que acha pedido, cliente,
  produto ou filamento. Ideia derivada. Esforço P/M, impacto B.
- [ ] **Importar vários G-codes de uma vez como produtos** *(já no roadmap: ideias
  registradas da Leva 7B do Plano 1)*.
- [ ] **Ler o `.gcode.3mf` do Bambu Studio** *(já no roadmap: Leva 8 do Plano 1)*.
  Continua esperando um arquivo real pra conferir a estrutura.
- [ ] **Portfólio em grade pro Instagram** *(já no roadmap: ideias registradas da
  Leva 7B do Plano 1)*.
- [ ] **Tabela comparativa no site.** "3DReport, planilha e sistema por
  assinatura", sem citar concorrentes, destacando "sem limite de pedidos" e "seus
  dados não ficam presos se você parar de pagar". É divulgação, não funcionalidade
  do app. Origem: pesquisa de mercado. Esforço P.

### Leva 18: Conectado sem custo, parte 1 (integração direta, D1)

Primeiro degrau de conexão: o app chama serviços usando a conta do próprio
vendedor. Não há servidor nem custo mensal pro projeto; tudo é opcional e desligado
por padrão, no espírito da verificação de versão (decisão 116); e só funciona com o
app aberto. Nasce aqui a base que a Leva 19 reaproveita: a **camada de rede**
(cliente HTTP multiplataforma atrás de interfaces, com repetição e o estado da
conexão na tela) e o **cofre de credenciais** (tokens no Keychain, no Gerenciador
de Credenciais do Windows ou no libsecret, nunca num JSON). Cada integração diz, na
tela e na documentação, o que sai do computador.

- [ ] **Atualização automática.** Baixar e instalar a versão nova a partir do
  GitHub Releases, evoluindo o aviso da decisão 116. Os instaladores não são
  assinados, o que pesa nos avisos do Windows e do macOS; avaliar junto. Prática
  comum em apps desktop. Esforço M, impacto M.
- [ ] **Assistente de IA** *(já no roadmap: seção Integrações)*, agora em dois
  formatos sem custo pro projeto: com a chave de API do próprio usuário, ou pelo
  **MCP**, com o app expondo os dados locais pro assistente que a pessoa já usa
  (Claude, ChatGPT), sem IA dentro do app. Visto em: 3D Control (IA conversando com
  os dados), Printforge.
- [ ] **Rascunho de orçamento e descrição de anúncio a partir da foto**, pelo mesmo
  acesso à IA do item acima. Visto em: Printforge. Esforço M, impacto M.
- [ ] **Pix com confirmação.** Com a conta do vendedor num provedor de pagamento,
  gerar um Pix dinâmico e consultar se foi pago (sem webhook, só com o app
  aberto), marcando o pedido como pago (Leva 11). Visto em: Kyte, Omie. Esforço M,
  impacto A.
- [ ] **Link de pagamento com cartão**, pelo mesmo provedor, com parcelamento, na
  mensagem pro cliente. Visto em: Jobber, Invoice Ninja, Kyte. Esforço M, impacto
  M.
- [ ] **Frete e etiqueta pela conta do vendedor.** Cotar, comprar a etiqueta e
  rastrear (ex.: Melhor Envio), preenchendo o frete (decisão 78) e o rastreio
  (Leva 11). Visto em: 3D Control, Tiny. Esforço M, impacto M.
- [ ] **Backup pela API do Drive.** Hoje o backup automático só vai pra nuvem se a
  pasta dele estiver sincronizada (decisão 108); aqui, sem precisar configurar
  pasta. Esforço M, impacto M.
- [ ] **Pesquisa de preço da concorrência.** Buscar o preço de peças parecidas na
  Shopee e no Mercado Livre pro campo de referência da Leva 16, respeitando os
  termos de uso dos sites. Visto em: Gestor 3D (extensão do navegador). Esforço M,
  impacto B.

### Leva 19: Conectado sem custo, parte 2 (publicação estática, D2)

O app gera páginas e publica numa hospedagem grátis da conta do próprio vendedor
(como GitHub Pages, Netlify ou Cloudflare Pages). O cliente só lê; não há banco nem
servidor do projeto. A credencial de publicação fica no cofre da Leva 18.

- [ ] **"Retrato pro cliente" explícito no código.** Pré-requisito dos itens
  abaixo. A regra do que o cliente vê (decisão 19) hoje está implícita em cada
  exportador (PDF, texto, imagem). Com página pública, ela vira um tipo no domínio
  (nome, foto, preço, prazo, andamento), usado por todos os exportadores, pra
  custo, margem, link do modelo e dados do cliente nunca vazarem. Origem: Parte 2
  da pesquisa. Esforço P/M, impacto A (segurança).
- [ ] **Catálogo online.** Página com os produtos (foto, nome, preço e botão
  "pedir no WhatsApp") pra bio do Instagram, atualizada quando o vendedor publica.
  Visto em: Pitada, Kyte, Printora. Esforço M, impacto A.
- [ ] **Link de acompanhamento do pedido** *(já no roadmap como "Portal de
  acompanhamento pro cliente", seção Integrações)*. Sai aqui sem servidor: uma
  página por pedido, com endereço difícil de adivinhar, mostrando o andamento da
  última vez que o vendedor publicou. A versão ao vivo é da Leva 22. Visto em:
  Custos3D, Printora, GestãoClick, 3DTAG.
- [ ] **Presets publicados.** Filamentos e impressoras num arquivo publicado que o
  app baixa, pra lista crescer sem esperar versão nova, com contribuição da
  comunidade (como fazem o Spoolman e a Open Filament Database). Esforço M,
  impacto M.

### Leva 20: Apostas locais grandes

- [ ] **Resina (SLA/MSLA).** O app é só FDM. Resina muda o modelo: material em ml,
  álcool de limpeza, desgaste da tela e do FEP, cura, e outros fatiadores
  (Chitubox, Lychee). Abre miniaturas, joias e odontologia. Visto em: Infinity
  Maker, 3DTAG, Neobrix, 3dprintpricecalculator e várias calculadoras de resina.
  Esforço G, impacto M.
- [ ] **Ligação com a impressora pela rede local.** Ler Bambu (modo LAN), OctoPrint
  e Moonraker na rede de casa: marcar "Em impressão" e "Pronto" sozinho, tempo e
  filamento reais (alimentando o estoque da Leva 12) e a falha registrada no
  pedido. Não usa nuvem, mas a Bambu já mudou o acesso local antes: risco de
  manutenção. Visto em: 3D Control Bridge, Manuflo, SimplyPrint, Repetier-Server,
  Bambu Farm Manager. Esforço G, impacto M.
- [ ] **Energia medida de verdade.** Consumo real da impressora (tomada
  inteligente, Home Assistant) no lugar da potência do manual, que superestima
  (decisão 54). Visto em: Atlas3D, ha-bambu-costs. Esforço M, impacto B.
- [ ] **Fase 2 do STL: estimar peso e tempo** *(já no roadmap: seção Visualização e
  análise de STL)*. Volta pra fila por ser pré-requisito do item seguinte.
- [ ] **Orçamento instantâneo no site do vendedor.** O cliente sobe o STL e vê o
  preço, calculado pelo `core` no navegador (o módulo `web` já faz isso na
  calculadora do site, decisão 98), com a tabela de preço do vendedor publicada
  pela Leva 19. Virar pedido a partir dali exige servidor (Leva 22). Visto em:
  Layers, DigiFabster, PrintQuote (Shopify), Craftcloud. Esforço G, impacto M.
- [ ] **Guardar o `.3mf` do projeto do fatiador** *(já no roadmap: seção
  Visualização e análise de STL, "Evolução futura")*.
- [ ] **Imagens das impressoras** e **marcas brasileiras nos presets** *(já no
  roadmap: seção Presets de cadastro)*.

### Leva 21: Android e sincronização sem servidor

Acesso pelo celular é o que todo concorrente tem por ser web. O caminho sem custo
pro projeto é sincronizar pela nuvem do próprio vendedor (Drive ou Dropbox), sem
servidor do projeto: resolve celular e mais de um computador, mas nada voltado ao
cliente (isso é da Leva 22). Antes, as mudanças levantadas na Parte 2 da pesquisa:

- [ ] **SQLite** como nova implementação das mesmas interfaces de repositório, no
  lugar do JSON reescrito inteiro a cada mudança (já previsto na decisão 108).
- [ ] **Carimbo pra sincronizar:** id do aparelho e a marca de "mudou desde a
  última sincronização" no `StoredRecord`, que hoje só tem o relógio do
  computador.
- [ ] **Número do pedido entre aparelhos.** Hoje é `lastNumber + 1` local
  (`QuoteHistoryRepository`): dois aparelhos sem internet gerariam o mesmo
  "#0042", que aparece no PDF. Faixa por aparelho ou prefixo.
- [ ] **Preservar campos desconhecidos.** A leitura usa `ignoreUnknownKeys = true`
  (`JsonDataFile.kt`): um app antigo descartaria o campo novo e, ao sincronizar,
  apagaria esse dado no outro aparelho.
- [ ] **Configurações do aparelho e do negócio separadas.** Janela, tema e
  verificação de versão ficam no aparelho; custos, marca, canais, cadastros e
  moeda sincronizam.
- [ ] **Regra de conflito** entre aparelhos: o último que grava vence, por registro
  ou por campo, e o histórico de status junta os dois lados.
- [ ] **Android** *(já no roadmap: seção 5 e "Nuvem e Android" da revisão
  pré-lançamento)*. O PDF (Apache PDFBox) só roda na JVM e precisa de um
  exportador pro Android.
- [ ] **Sincronização pela nuvem do próprio vendedor.** Esforço G, impacto A.

### Leva 22: Servidor (D3)

Tudo que precisa de um servidor do projeto: contas, páginas em que o cliente
interage, webhooks e tarefas com o app fechado. **Só começa depois destas decisões**
(detalhadas na Parte 2 da pesquisa):

- Quem paga o servidor. Revisita a decisão 15: só auto-hospedado, serviço oficial
  grátis com limites bancado por doação, ou hospedagem paga com o código aberto
  (como fazem Obico e Invoice Ninja).
- Como fica o posicionamento "100% no seu computador". Revisita a decisão 90,
  provavelmente pra "funciona sem internet, a nuvem é opcional".
- Sincronização cifrada de ponta a ponta ou servidor que lê os dados.
- Backend pronto e aberto (ex.: Supabase com PowerSync) ou próprio (Ktor
  reaproveitando o `core`).
- Quem responde pela LGPD, já que o projeto passaria a guardar dados pessoais dos
  clientes dos vendedores.

Dentro da leva, a ordem segue a utilidade pro vendedor:

- [ ] **Sincronização pelo servidor**, evolução da Leva 21, que habilita o resto.
- [ ] **Pedidos do Mercado Livre e da Shopee entrando sozinhos**, com o lucro real
  de cada venda. Exige servidor: o Mercado Livre pede o segredo do app mesmo com
  PKCE, e a Shopee assina cada chamada com a chave do parceiro; segredo não pode
  ir num app de código aberto. Visto em: 3D Control, Tiny, Bling. Impacto A.
- [ ] **Versão pelo navegador.** O Compose pra web está em Beta desde a 1.9, e o
  PDF precisa de outro exportador. Visto em: todos os concorrentes brasileiros.
  Esforço G, impacto A.
- [ ] **Pix com confirmação por webhook**, sem depender do app aberto (evolução da
  Leva 18).
- [ ] **Link de acompanhamento ao vivo e aviso por e-mail** a cada mudança de
  andamento (evolução da Leva 19). Visto em: Custos3D, Printora, Jobber, Wave.
  Impacto M.
- [ ] **Aceite do orçamento online**, com data e hora registradas e sinal pago no
  aceite. Visto em: Invoice Ninja, Jobber. Impacto M.
- [ ] **Equipe.** Vários usuários com permissão por módulo, aprovação do dono pro
  pedido criado por um membro, registro de quem fez o quê e verificação em duas
  etapas. Visto em: 3D Control, SISTEMA3D, SimplyPrint, PrintFarmDesk, Prinate.
  Impacto M.
- [ ] **Formulário de encomenda sob medida** (o cliente descreve e manda foto ou
  STL) e **pedido a partir do orçamento instantâneo** da Leva 20. Visto em:
  Castiron, Layers. Impacto M.
- [ ] **Impressora de fora de casa.** Ver e controlar, câmera e aviso no celular
  quando termina ou falha, com uma ponte entre a rede de casa e o servidor. Visto
  em: 3D Control Bridge, SimplyPrint, Obico, Prusa Connect. Impacto M.
- [ ] **Referência anônima de preço** ("quanto se cobra por hora de máquina na sua
  região"), com adesão explícita. Ideia derivada. Impacto M.
- [ ] **Relatório de erro e de uso**, opcionais, pra decidir o roadmap com dado.
  Ideia derivada. Impacto B.

### Leva 23: Depois do servidor

- [ ] **Loja com carrinho e pagamento.** Visto em: Printora, Nuvemshop.
- [ ] **Estoque sincronizado com os anúncios** (não vender o que acabou). Visto
  em: Craftybase, Tiny.
- [ ] **Etsy, Shopify e Nuvemshop.** Visto em: Printago, Craftybase, 3D
  PrintForce, Manuflo.
- [ ] **WhatsApp pela API oficial:** mensagem automática a cada andamento e
  atendente com IA que responde preço e prazo. Exige verificação da empresa na
  Meta e tem custo por mensagem (desde 2026). Visto em: Bakerly, Nuvem Chat.
- [ ] **Nota fiscal (NFS-e ou NF-e) a partir do pedido.** Exige certificado
  digital ou uma API paga de emissão; traz risco legal e demanda de suporte. Visto
  em: Bling, SmartMEI, Omie, GestãoClick.
- [ ] **Detecção de falha por IA na câmera.** Visto em: Obico, AutoFarm3D,
  SimplyPrint.

### Sem posição (registradas, fora do foco hoje)

Vistas na pesquisa, registradas pra não se perderem, mas sem lugar na fila:

- **Contabilidade estrangeira** (Xero, QuickBooks). O público é brasileiro; se
  fizer falta, o CSV da Leva 13 serve de ponte.
- **Terceirizar a impressão** (Craftcloud, Treatstock). Mandar o pedido pra outro
  fornecedor imprimir é outro produto, um marketplace.
- **Ferramentas de criação** (imagem pra SVG, separar as cores de um 3MF; Gestor
  3D). Fora do foco de vender.
- **Roteamento automático e encaixe de peças numa fazenda grande** (Printago,
  GrabCAD Print). Serve a quem tem dezenas de máquinas.
- **Recursos industriais** (rastreabilidade de material, análise de viabilidade de
  peça, certificação; Authentise, Castor, Materialise).
