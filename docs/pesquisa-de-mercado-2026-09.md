# Pesquisa de mercado (setembro de 2026)

> **Retrato datado, não é backlog.** Este documento registra uma pesquisa feita em
> 2026-09-27 sobre o que os softwares parecidos com o 3DReport oferecem. Nada aqui
> está aprovado nem agendado. O [roadmap](roadmap.md) continua sendo a única fonte de
> verdade das evoluções: uma ideia daqui só vira item de lá pela triagem de sempre
> (decisão 83), com o motivo registrado.

## Sumário

- [Escopo e método](#escopo-e-método)
- [Resumo executivo](#resumo-executivo)
- [Panorama do mercado](#panorama-do-mercado)
- [Onde o 3DReport já está à frente](#onde-o-3dreport-já-está-à-frente)
- [Parte 1: o que roda no computador](#parte-1-o-que-roda-no-computador)
- [Parte 2: funcionalidades conectadas (nuvem)](#parte-2-funcionalidades-conectadas-nuvem)
- [Fora do foco](#fora-do-foco)
- [Fontes](#fontes)

## Escopo e método

**Pergunta:** o que os softwares parecidos (pagos e grátis, web, desktop e celular)
oferecem que o 3DReport ainda não tem e que também não está no roadmap? O que
facilitaria o uso?

**Como foi feito:**

- Três levantamentos em paralelo: (1) software de gestão de negócio e de fazenda de
  impressão 3D, incluindo plataformas de orçamento instantâneo; (2) calculadoras de
  custo, controle de filamento e o mercado brasileiro; (3) ferramentas de negócios
  vizinhos com o mesmo fluxo (confeitaria sob encomenda, apps de MEI, orçamento e
  fatura).
- Conferência direta das páginas dos concorrentes brasileiros mais próximos.
- Conferência no código do 3DReport de cada item listado como "falta", pra não
  repetir o que já existe. Exemplo: a validade do orçamento já existe
  (`BrandingSettings.quoteValidityDays`) e ficou fora da lista.
- Parte 2 (nuvem) acrescentada a pedido do responsável do projeto, que considera
  provável o app ficar conectado pra atender demandas maiores.

**Limitações:**

- Quase tudo vem das páginas de venda e da documentação pública; nenhum produto foi
  testado na prática. Um recurso anunciado pode estar incompleto ou ter mudado.
- Preços de setembro de 2026, sujeitos a mudança.
- Alguns sites montados em JavaScript não puderam ser lidos por completo (Neobrix
  Suite, 3D Print Flow, My3D Farm, GestorMaker, Controlefy): entram no panorama só
  com o que o título e a busca mostraram.
- "Ideia derivada" marca itens que não vieram de um produto específico, e sim da
  análise.

## Resumo executivo

- **O mercado brasileiro está lotado e é todo por assinatura na nuvem.** Foram pelo
  menos 15 sistemas brasileiros de gestão de impressão 3D, de R$ 9,90 a R$ 130 por
  mês, fora as calculadoras grátis e as planilhas vendidas na Hotmart. Todos são web,
  e os planos grátis são limitados: Manager 3D (4 filamentos, 4 projetos, 3
  clientes), Atlas3D (5 pedidos por mês), Gestor 3D (10 pedidos por mês), Printora (1
  impressora, 10 produtos), 3D Control (grátis só pra leitura). A exceção é o 3DTAG,
  grátis pra membros da comunidade dele.
- **O diferencial do 3DReport continua de pé:** grátis sem limite, funciona sem
  internet, os dados ficam no computador e o código é aberto. O fechamento do
  Castiron (plataforma de pedidos de confeitaria, encerrada no fim de 2025) e o modo
  "só leitura" do 3D Control quando a assinatura vence mostram o risco de depender da
  nuvem de outra empresa.
- **O que quase todo concorrente direto tem e o 3DReport não tem:** estoque de
  filamento em gramas com baixa automática e alerta de estoque baixo, despesas e
  fluxo de caixa (o lucro real do mês), situação do pagamento de cada pedido e acesso
  pelo celular.
- **Achado que mexe no preço:** o canal de venda do 3DReport só tem taxa percentual
  (`SalesChannel.feeRate`), mas Shopee e Mercado Livre cobram também uma **taxa fixa
  por item**. Na Shopee, em 2026, ela vai de R$ 4 a R$ 26 conforme a faixa de preço;
  no Mercado Livre, fica perto de R$ 6,25 a R$ 6,75 nos itens baratos. Num chaveiro
  de R$ 15 vendido na Shopee, os R$ 4 fixos são 27% do preço, e hoje o app não conta
  isso.
- **Nuvem (Parte 2):** o app já foi preparado pra sincronizar (decisões 106 e 108) e
  não precisa ser reescrito. A mudança grande é somar um servidor e trocar o
  princípio "100% no seu computador" por "funciona sem internet, a nuvem é
  opcional". Boa parte do valor conectado (catálogo online, link de acompanhamento,
  Pix com confirmação, frete, IA) cabe antes de existir um servidor próprio.
  Integrar pedidos do Mercado Livre e da Shopee não cabe: exige servidor.

## Panorama do mercado

### Concorrentes diretos no Brasil

Sistemas de gestão feitos pra quem vende impressão 3D no Brasil. São a comparação
que o vendedor brasileiro vai fazer.

| Produto | Modelo e preço | Destaques |
|---|---|---|
| [3D Control](https://3dcontrol.com.br/) | Web. Maker grátis (só leitura e calculadora), Pro R$ 49,90/mês, Studio R$ 129,90/mês | Pedidos do Mercado Livre entram sozinhos (Shopee, Amazon, Shein e Elo7 anunciados); estoque com baixa e alerta; "Bridge" com impressoras Bambu (câmera, pausar, baixa de filamento, falha ligada ao pedido); IA conversando com os dados (Claude, ChatGPT, Cursor); equipe com permissão por módulo e aprovação do dono; etiqueta 4x6 e Melhor Envio; retorno do investimento por impressora; clientes por segmento (final, empresa, revenda); importação de planilha |
| [SISTEMA3D](https://sistema3d.com.br/) | Web. R$ 47/mês, R$ 235/semestre, R$ 470/ano, tudo liberado | Orçamento com PDF próprio; estoque de filamento e insumos em gramas ou unidades; compras, contas a pagar, parcelas e previsão de caixa; produção em etapas com checklist por item; controle de perdas; comodato; permissões por módulo e histórico de ações |
| [Printora](https://useprintora.com.br/) | Web. Grátis (1 impressora, 10 produtos), Básico R$ 19,90, Pro R$ 39,90 | Fila que prioriza urgência e lucro; preço sugerido pra Mercado Livre e Shopee; lista de materiais e alerta de estoque; manutenção por horas; **loja online própria** com área do cliente pra acompanhar o pedido (ou só catálogo) |
| [Atlas3D](https://atlas3d.com.br/) | Web. Grátis (5 pedidos/mês, 2 produtos), Pro R$ 9,90/mês ou R$ 99/ano | Orçamento em PDF com logo pelo WhatsApp; **PDV** com Pix, cartão e dinheiro, preço de atacado e varejo; **consignado** (peça na prateleira de uma loja, venda parcial, acerto); energia medida pelo Home Assistant; manutenção preventiva; integração com Mercado Livre e Shopee anunciada |
| [Manager 3D](https://www.manager3d.com.br/) | Web. Grátis (4 filamentos, 4 projetos, 3 clientes), Pro R$ 29,90/mês | Calculadora; filamento com consumo e alerta; Kanban; lucro por projeto; despesas da empresa separadas do projeto; painel operacional (em andamento, estoque crítico, entregas do mês) |
| [3DTAG](https://www.3dtag.com.br/) | Web. Grátis pra membros da comunidade 3DTAG, 28 módulos | DRE, ponto de equilíbrio, fluxo de caixa, custos fixos, ROAS; fila e capacidade do mês; manutenção; resina; embalagens; fornecedores; etiquetas; acompanhamento pelo cliente; comparação entre marketplaces |
| [Gestor 3D](https://gestor3d.app/) | Web. Grátis (10 pedidos/mês), Básico R$ 15, Pro R$ 30, Premium R$ 60 | Calculadora com embalagem e mão de obra; composição do produto (filamentos, extras, tempo); estágios de produção; estoque com alerta; relatórios por produto, canal e impressora; extensão que compara preço na Shopee, Mercado Livre e TikTok Shop; ferramentas de criação (imagem pra SVG, QR de Pix, separador de 3MF) |
| [Custos3D](https://appcustos3d.com.br/) | Web. Basic R$ 29,90/mês, PRO R$ 69,90/mês | Custo médio ponderado do estoque; leitor de G-code (PRO); importação da planilha de pedidos da Shopee e do Mercado Livre (PRO); **link público de acompanhamento** sem login; proposta formatada pro WhatsApp; ficha de produção em PDF com a composição do custo; taxa de risco |
| [Calc3D Pro](https://www.calc3dpro.com.br/) | Web. A partir de R$ 39,90/mês (grátis durante o beta) | **Orçamento em PDF com QR Code de Pix**; PDV; consignado; cobranças pendentes; financeiro diário, mensal e anual |
| [Calc3D](https://calc3d.com.br/) | Calculadora grátis + plano PRO | Segundo o levantamento: histórico, PDF com marca, integração com Mercado Livre e estoque no PRO (não conferido na página) |
| [3D Prime](https://3dprime.com.br/) | Calculadora grátis | Segundo o levantamento: tarifa de energia da ANEEL por estado e presets de impressora (não conferido na página) |
| [Neobrix Suite](https://www.neobrixsuite.com.br/) | Web | Pela busca: custo real, estoque de filamento **e resina**, vendas e lucro |
| [3D Print Flow](https://3dprintflow.com.br/) | Web, grátis pra começar | Pela busca: custo (filamento e energia), estoque, precificação, lucro por pedido |
| Infinity Maker, Pricify3D, [Precifi3D](https://precifi3d.com/) | Web | Segundo o levantamento: Infinity Maker e Precifi3D aceitam resina; Pricify3D (R$ 29,90) deixa montar a fórmula de preço |
| [My3D Farm](https://my3dfarm.com.br/), [GestorMaker](https://gestormaker.com.br/), [Controlefy](https://controle3d.com/) | Web | Só o título pôde ser lido: "gestão para impressão 3D" |
| [3dcalculate](https://github.com/rodrigoinhaia/3dcalculate) | Código aberto | Precificação, catálogo de peças e consignado com comissão |
| Planilhas na Hotmart e Kiwify | Pagas, uso único | Ex.: "Planilha de Precificação de Impressão 3D: Shopee e Mercado Livre" |

Calculadoras web grátis também são muitas (Calc3D, CalculaSTL, Calculo3D, 3D Prime,
Acelera3D, MelhorImpressora3D, [Objeto3D](https://objeto3d.com.br/calculadora-custos),
PrintReady3D) e competem com a calculadora do site do 3DReport.

**Dores do vendedor que aparecem nos textos do setor** (blogs e materiais de
concorrentes, não em fórum com citação direta): não saber o custo real e copiar o
preço de outro sem saber o que está embutido nele; preço baixo que vira fila
bagunçada, manutenção atrasada e cansaço; a planilha que quebra quando chegam mais
clientes, versões de produto e canais (e as taxas dos marketplaces mudam sempre).

### Gestão de negócio de impressão 3D (internacional)

| Produto | Modelo e preço | Destaques |
|---|---|---|
| [Printforge](https://crm.printforge.com.au/) | SaaS. Hobby grátis, Starter ~US$ 12, Scale ~US$ 49 | Custo a partir do STL/G-code; orçamento em PDF com marca; Kanban de 7 etapas e **calendário da fazenda**; estoque com alerta; clientes com etiquetas e histórico de contato; Shopify, Xero, Google Drive; rascunho de orçamento com IA |
| [FoxTrack](https://foxtrack.studio/) | Desktop/web, grátis (beta) | Pedido rápido com prazo; importação e exportação CSV; carretel com cor, uso, alerta e **QR pra pesar e registrar a secagem**; outros insumos; cliente com total gasto; despesas no relatório de lucro; **checklist das peças de um pedido** |
| [PrintFarmHQ](https://printfarmhq.io/) | SaaS (beta) | Custo real com licenças de software; depreciação; manutenção com atraso; equipe com papéis |
| [PrintFarmDesk](https://www.printfarmdesk.com/) | SaaS sobre o Printago. Grátis, €5, €20 | Custo com embalagem e postagem; margem alvo por produto; pedido casado com filamento e impressora por tabela; lista de materiais e de separação em PDF; aviso de troca de carretel no AMS; papéis Dono, Admin, Operador, Leitor |
| [Manuflo](https://manuflo.app/) | SaaS. Grátis (10 pedidos, 2 impressoras), US$ 19, US$ 39 | Pedido de "recebido" a "pago"; fila por prazo entre impressoras; fatura em PDF; OctoPrint, Bambu, Prusa Connect, Klipper, Etsy e Shopify |
| [3DPCC](https://3dpcc.com/) | SaaS. Grátis, Pro US$ 12,50/mês | Orçamento com margem ajustável, lote, **link compartilhável** e PDF; estoque de filamento, resina, peças e embalagem com fornecedor e CSV; reserva de estoque por pedido; catálogo com variações; app de iPhone |
| [Prinate](https://prinate.app/) | SaaS. Grátis, US$ 5,99/mês, US$ 199 vitalício | Lucro por trabalho; estoque; clientes com histórico; orçamento e fatura; espaço de equipe |
| [3DPBOSS](https://3dpboss.com/) | Modelo no Notion, US$ 49 a US$ 139 (pagamento único) | CRM de oportunidades; produção de modelagem a pós-processamento; manutenção; peças de reposição; DRE; base de conhecimento e tarefas |
| [Craftybase](https://craftybase.com/3d-printing-inventory-software) | SaaS (virando "Stocksmith") | Custo por grama recalculado quando o preço muda; lista de materiais em vários níveis; pedidos de Etsy, Shopify, Amazon e outros com estoque devolvido ao canal; relatório de custo pro imposto |
| [3D PrintForce](https://3dprintforce.com/) | SaaS. Grátis, US$ 12,50, US$ 32,50 | Taxas do marketplace detalhadas (transação, processamento, anúncio); pedidos da Etsy; fila no SimplyPrint; perfis de frete |
| [Layers](https://layers.app/) | SaaS, grátis e pagos | **Vitrine em que o cliente sobe o modelo, vê o preço e paga**; fatura automática; marca própria; 11 idiomas |
| [3DPrintOps](https://www.3dprintops.com/) | SaaS, a partir de US$ 14/mês | Orçamento com custo real, funil de "orçado" a "enviado", registro de contato com o cliente |

### Fazendas de impressão e controle das impressoras

| Produto | Modelo e preço | Destaques |
|---|---|---|
| [Printago](https://printago.io/) | SaaS, grátis (1 trabalho por vez) e pagos | Pedidos da Shopify e Etsy viram trabalhos; roteamento automático pra impressora com o filamento certo carregado; API aberta |
| [SimplyPrint](https://simplyprint.io/print-farms) | SaaS, grátis e US$ 39,99/mês (10 impressoras) | Fatiar no navegador; controle remoto; iniciar todos os trabalhos iguais de uma vez; filamento com etiqueta NFC; verificação da mesa por IA; manutenção; apps de celular |
| [3DPrinterOS](https://www.3dprinteros.com/) | SaaS, por orçamento | Fila central de qualquer marca; cota de horas por usuário; login único corporativo |
| [AutoFarm3D](https://www.3dque.com/autofarm3d) | Grátis até 3 impressoras, Pro US$ 40/mês | Fila inteligente; detecção de falha por IA grátis; ejeção automática; timelapse; calendário de pedidos; pedidos da Etsy e Shopify direto na fila |
| [OctoFarm](https://github.com/OctoFarm/OctoFarm) | Código aberto, auto-hospedado | Painel de várias impressoras OctoPrint, câmeras, comandos em massa, filamento, histórico |
| [Obico](https://www.obico.io/) | Código aberto, grátis (1 impressora) e US$ 6,99/mês | Detecção de falha por IA, avisos no celular, acesso remoto; também auto-hospedável |
| [Prusa Connect](https://www.prusa3d.com/p/prusa-connect/) | Grátis pra quem tem Prusa | Fila e histórico por impressora, câmera, fatiador na nuvem, equipe com papéis |
| [Bambu Farm Manager](https://wiki.bambulab.com/en/software/bambu-farm-features) | Grátis, só rede local | Iniciar e parar em lote; fila por disponibilidade; arquivos organizados; ligar as impressoras aos poucos pra não sobrecarregar a rede elétrica |
| [Repetier-Server](https://www.repetier-server.com/) | €59,99, pagamento único | Fila; timelapse; estimativa de custo antes de imprimir; integração com Home Assistant e MQTT |
| [Polar Cloud](https://polar3d.com/), [AstroPrint](https://www.astroprint.com/), [GrabCAD Print](https://grabcad.com/print), [3D Print Log](https://www.3dprintlog.com/) | Variados | Aprovação antes de imprimir (escolas); fila pra próxima impressora livre; encaixe automático de peças na mesa; diário de impressões com status e fotos |

### Calculadoras e controle de filamento

| Produto | Modelo | Destaques |
|---|---|---|
| [PrintQuote3D](https://github.com/g4l4xy/PrintQuote3D) | Código aberto (AGPL), dados locais | Banco de filamentos da **Open Filament Database** (cerca de 2 mil produtos e 14 mil cores) e 1.007 perfis de impressora; carretel com restante, local e notas; bicos múltiplos |
| [3D-Print-Cost-Calculator](https://github.com/AndreasReitberger/3D-Print-Cost-Calculator) | Código aberto (GPL), Windows | STL e G-code; app de celular com clientes, etapas de trabalho, PDF e integração com contabilidade |
| [Calculadora da Prusa](https://blog.prusa3d.com/3d-printing-price-calculator_38905) | Web, grátis | G-code preenche tempo e peso; mão de obra; retorno da máquina; itens avulsos; imposto; compartilhar o resultado |
| [3dprintpricecalculator](https://3dprintpricecalculator.com) | Web, grátis | FDM **e resina**; G-code; painel de orçamentos; PDF |
| [Omni Calculator](https://www.omnicalculator.com/other/3d-printing) | Web, grátis | Conta simples com a fórmula explicada |
| [Plugins do OctoPrint](https://plugins.octoprint.org/plugins/costestimation/) | Grátis | Custo por impressão no histórico; **aviso de que o carretel não tem filamento suficiente** antes de começar |
| Calculadoras de resina ([PrintPal](https://printpal.io), GrandpaCAD, SANIX3D, PEA3D, ResinCalc) | Web, grátis | Resina em ml, álcool de limpeza, desgaste da tela e do FEP, cura |
| [Spoolman](https://github.com/Donkie/Spoolman) | Código aberto, auto-hospedado | Padrão aberto de estoque de carretel; peso atualizado durante a impressão (Klipper, OctoPrint); etiqueta QR; banco de filamentos da comunidade; API |
| [FilaMan](https://filaman.app/), [SpoolEase](https://spoolease.io/), [OpenSpool](https://github.com/spuder/OpenSpool), [OpenTag3D](https://opentag3d.info/) | Código aberto + hardware | Balança e etiqueta NFC no carretel; padrão aberto de etiqueta (a Prusa já grava etiqueta nos carretéis Prusament) |
| [ha-bambu-costs](https://github.com/Oose97/ha-bambu-costs) | Código aberto (Home Assistant) | Custo por slot do AMS pelo filamento carregado; tarifa de energia por horário; conta o consumo parado e o de impressão abortada |
| [Spool](https://apps.apple.com/us/app/spool/id6756892049), [Spoolio](https://spoolio.net) e outros apps | Celular, grátis e pagos | Umidade e secagem do filamento; leitura de etiqueta; análise de consumo |

### Plataformas de orçamento instantâneo (ideias pra emprestar)

Não são concorrentes: são serviços industriais ou marketplaces. Ficam aqui pelas
ideias. [DigiFabster](https://digifabster.com/) (widget no site do vendedor com preço
na hora, **tabela por quantidade e por prazo**); [Xometry](https://www.xometry.com/)
e [Protolabs Network](https://www.hubs.com/) (preço por IA, visualizador 3D com
aviso de fabricação); [Shapeways](https://www.shapeways.com/) (pedido direto do
orçamento); [Craftcloud](https://craftcloud3d.com/) e
[Treatstock](https://www.treatstock.com/) (comparação entre fornecedores);
[AMFG](https://amfg.ai/), [3YOURMIND](https://www.3yourmind.com/),
[Castor](https://get.castor.tech/), [Authentise](https://www.authentise.com/) e
[Materialise](https://www.materialise.com/) (industrial: agenda, rastreabilidade,
análise de viabilidade). [PrintQuote](https://apps.shopify.com/3d-print-quote) é um
app da Shopify em que o cliente sobe o STL e recebe o preço.

### Negócios vizinhos (mesmo fluxo de encomenda)

Confeitaria sob encomenda tem o fluxo mais parecido com o de quem vende impressão 3D:
custo de material e tempo, orçamento pelo WhatsApp, sinal, entrega com data.

| Produto | Modelo e preço | O que vale olhar |
|---|---|---|
| [Pitada](https://www.usepitada.com) | R$ 14,90 e R$ 29,90/mês | **Calendário de produção**; **lista de compras** somando os pedidos pendentes; mudou o preço de um ingrediente, tudo que usa ele recalcula; energia e gás divididos pelo tempo de preparo; fator de perda; link de catálogo pra bio do Instagram |
| [ZupConfeitaria](https://www.zupconfeitaria.com) | R$ 14,90/mês (anual) | Alerta de estoque; **aniversário do cliente**; agenda de compromissos e pedidos |
| [Bakerly](https://bakerlyapp.com) | A partir de R$ 19,90/mês | Robô no WhatsApp que responde preço e prazo e cria o pedido sozinho |
| [CakeBoss](https://cakeboss.com) | US$ 149, pagamento único | Custo de receita com embalagem; aniversários; agenda; fatura |
| [Castiron](https://findhomegrown.com/blog/castiron-alternative-food-vendors) | Encerrado no fim de 2025 | Formulário de encomenda; serve de alerta sobre depender de uma nuvem de terceiros |
| [Doce Lucro](https://www.appdocelucro.com.br), [DoceGestor](https://docegestor.github.io), [Bake Diary](https://www.bakediary.com) | Variados | Ficha técnica e preço; DoceGestor funciona com e sem internet |
| [Kyte](https://www.kyteapp.com) | Grátis e pagos | PDV no celular; **Pix por QR Code sem taxa**; catálogo com link pro WhatsApp; recibo digital |
| [Bling](https://www.bling.com.br/segmentos/erp-mei), [Tiny](https://tiny.com.br), [GestãoClick](https://gestaoclick.com.br), [Omie](https://www.omie.com.br) | ERPs, de grátis (Omie Fit) a pagos | Nota fiscal; proposta comercial; pedidos de Mercado Livre e Shopee com etiqueta e rastreio; área do cliente; inadimplência |
| [SmartMEI](https://apps.apple.com/br/app/smart-mei-brasil/id6746266914) | Grátis, extras por R$ 15/mês | Nota fiscal, **guia do DAS**, declaração anual, livro-caixa |
| [Nuvemshop](https://www.nuvemshop.com.br) | Grátis a R$ 59+/mês | Loja; atendente com IA que fecha pedido no WhatsApp |
| [Invoice Ninja](https://invoiceninja.com), [Jobber](https://www.getjobber.com), [Zoho Invoice](https://www.zoho.com/us/invoice/), [Wave](https://www.waveapps.com) | Grátis a pagos | Aceite do orçamento online; **sinal com vencimento próprio**; **lembrete automático de orçamento sem resposta** (Jobber); aviso de fatura vista e atrasada |
| [Katana](https://katanamrp.com), [inFlow](https://www.inflowinventory.com), [Vela](https://www.getvela.com) | Variados | Lista de materiais; fila do que produzir; preço de atacado; inFlow tem versão local, sem assinatura |

## Onde o 3DReport já está à frente

Entre os produtos pesquisados:

- **Arrastar o G-code e sair o orçamento**, com casamento automático de impressora e
  filamento, multicolor por extrusor e pedido com várias impressões. Ler o G-code
  apareceu em poucos (Custos3D no plano PRO, Printforge, calculadora da Prusa,
  3dprintpricecalculator), e em nenhum com o casamento automático.
- **Negociação** com preço mínimo e aviso de prejuízo: não apareceu em nenhum.
- **Comparar a mesma peça entre as impressoras.**
- **Catálogo que avisa quando os custos mudaram** e o preço precisa ser atualizado
  (Pitada, de confeitaria, e Craftybase recalculam o custo quando o preço do
  material muda, mas sem avisar que o preço de venda envelheceu).
- **Manutenção por componente**, com intervalo em horas e diário.
- **Análise de complexidade do STL** e visualizador 3D.
- **Imagem quadrada pro WhatsApp** com preço e prazo.
- **Grátis e sem limite**, sem conta, sem internet e com o código aberto.

## Parte 1: o que roda no computador

O que o mercado tem e o 3DReport ainda não tem, e que também não está no roadmap.
Todos os itens desta parte funcionam sem servidor.

Legenda: **Esforço** P (pequeno), M (médio), G (grande). **Impacto** A (alto), M
(médio), B (baixo). As duas colunas são estimativas pra ajudar a triagem.

### 1. Preço certo (motor de precificação)

| Ideia | Quem faz | Esforço | Impacto |
|---|---|---|---|
| **Taxa fixa por item no canal de venda**, com faixas por preço como a da Shopee em 2026. Hoje o canal só tem percentual | Regra real de Shopee e Mercado Livre; 3D Control e Printora calculam a taxa por marketplace | P/M | A |
| **Custo de embalagem e insumos por produto** (argola, ímã, parafuso, tinta, caixa) como custo. Hoje o serviço só soma no preço cobrado, sem custo (decisão 25), e o administrativo é um valor único pra todo pedido | 3DTAG, SISTEMA3D, Gestor 3D ("extras"), 3DPCC, Craftybase (lista de materiais), CakeBoss | M | A |
| Acréscimo por urgência (prazo curto custa X% a mais) | DigiFabster, Xometry (prazo por opção) | P | M |
| Tabela de preço por quantidade no PDF (1, 10, 50 unidades) | DigiFabster; matriz de preço das gráficas de bordado (Printavo, DecoNetwork) | P/M | M |
| Preço de atacado ou revenda além do varejo | Atlas3D, 3D Control (cliente final, empresa, revenda), Katana, inFlow | M | M |
| Preço mínimo por pedido, pra peça muito pequena | Ideia derivada | P | M |
| Preço de referência da concorrência em cada produto do catálogo, com aviso quando o seu está muito abaixo | Gestor 3D faz online (extensão); a versão local é um campo digitado | P | B |
| Bandeira tarifária e tarifa branca no preço do kWh | 3D Prime (tarifa da ANEEL por estado), ha-bambu-costs (tarifa por horário) | P | B |

### 2. Estoque e compras

| Ideia | Quem faz | Esforço | Impacto |
|---|---|---|---|
| **Estoque em gramas por carretel e cor, com baixa automática ao imprimir e ajuste por pesagem.** Revisita a decisão 39, que recusou a baixa automática por imprecisão (falha, teste e sobra gastam filamento sem virar pedido). Argumento novo: todo concorrente direto faz, e a imprecisão se resolve com "consumo avulso" (igual às horas avulsas da manutenção, decisão 96) e com "pesei o carretel" corrigindo o saldo | 3D Control, SISTEMA3D, Manager 3D, Atlas3D, Custos3D, Printora, Gestor 3D, Printforge, FoxTrack, Spoolman | M | A |
| Alerta de estoque baixo e aviso "não tem filamento suficiente pra este pedido" | 3D Control, Printora, Gestor 3D, Manager 3D, plugin FilamentManager do OctoPrint | P (depois do anterior) | A |
| Registro de compras de filamento (data, quantidade, preço com frete) calculando o preço por kg, pelo custo médio ou pela última compra | Custos3D (custo médio ponderado), SISTEMA3D, Craftybase, Pitada | M | M |
| Lista de compras dos pedidos aprovados: o que falta comprar pra entregar tudo | Pitada | P/M | M |
| Fornecedores | 3DTAG, 3DPCC | P | B |
| Local do carretel, registro de secagem e etiqueta QR | Spoolman, FoxTrack, Spoolio, SimplyPrint | P/M | B |
| Banco aberto de filamentos (Open Filament Database) pra enriquecer os presets; conferir a licença antes | PrintQuote3D | M | B |

### 3. Dinheiro que entra e sai

| Ideia | Quem faz | Esforço | Impacto |
|---|---|---|---|
| **Pix copia e cola e QR Code no PDF, na mensagem e na imagem**, com a chave do vendedor e o valor. O formato do Pix (BR Code) é gerado sem internet | Calc3D Pro, Kyte, Gestor 3D (gerador de QR) | P | A |
| Situação do pagamento de cada pedido (pago, falta pagar) e a forma (Pix, cartão, dinheiro). Cuidado: o roadmap diz que o app não quer virar contas a receber; aqui é só a situação, sem financeiro completo | Manuflo, Atlas3D, Calc3D Pro, 3DPCC, Omie | P/M | A |
| Recibo em PDF ao receber | Kyte, GestãoClick, apps de orçamento, planilhas de controle de pedidos | P | M |
| Despesas do negócio (compras, ferramentas, taxas) e o lucro real do mês, com fluxo de caixa | Manager 3D, SISTEMA3D, Atlas3D, 3D Control, 3DTAG, FoxTrack, ZupConfeitaria, Pitada | M | A |
| Ponto de equilíbrio e meta do mês ("faltam 40 h de máquina vendidas pra pagar os custos fixos"), a partir dos custos fixos que o app já conhece | 3DTAG (ponto de equilíbrio, DRE) | P | M |
| Resultado por canal de venda no Dashboard (hoje o `QuoteReport` não separa por canal) | 3D Control, Gestor 3D, Atlas3D | P | M |
| Retorno da impressora ("a K1 já se pagou 64%"), a partir do investimento já cadastrado | 3D Control (retorno por equipamento) | P | M |
| MEI: lembrete do DAS e barra do limite de faturamento do ano (valor configurável, porque a lei muda) | SmartMEI, Omie Fit | P | M |

### 4. Cliente e pós-venda

| Ideia | Quem faz | Esforço | Impacto |
|---|---|---|---|
| **Mensagens prontas por andamento** ("ficou pronto", "saiu pra entrega, rastreio X", "lembrete de pagamento"), abertas no WhatsApp como a mensagem do orçamento. É a versão local do portal de acompanhamento do roadmap | Custos3D (proposta formatada), Jobber, Nuvemshop | P | A |
| **Lembrete de orçamento parado** (Orçado há N dias), com uma mensagem de retomada | Jobber | P | A |
| Ficha do cliente: histórico, total gasto, ticket médio, frequência e melhores clientes. Hoje o cadastro tem nome e contato | 3D Control, FoxTrack, Manager 3D, Printforge, CakeBoss | P/M | M |
| Endereço do cliente e etiqueta de envio 10x15 | 3D Control, 3DTAG, Tiny | P/M | M |
| Código de rastreio no pedido, com link | Tiny, 3D Control (Melhor Envio) | P | M |
| Tipo de cliente (final, revendedor, empresa) e origem ("Instagram", "indicação", "feira") | 3D Control, Printforge (etiquetas), CRMs em geral | P | B |
| Datas comemorativas e aniversário do cliente (Dia das Mães, Natal) | CakeBoss, ZupConfeitaria | P | B |

### 5. Produção

| Ideia | Quem faz | Esforço | Impacto |
|---|---|---|---|
| **Calendário de produção por impressora**, com sugestão de prazo honesto ("cabe até sexta?") e fila ordenada pelo prazo. Hoje a fila soma as horas em impressão, sem datas | Printforge, AutoFarm3D, 3DTAG (capacidade do mês), Pitada, Manuflo, Printora, GrabCAD Print | M/G | A |
| Checklist das impressões de um pedido (marcar cada mesa que saiu) | FoxTrack, SISTEMA3D (etapas e checklist por item) | P | M |
| Ficha de produção em PDF, de uso interno (impressões, filamentos, cores, configurações, acabamento), e lista de separação | Custos3D, PrintFarmDesk | P | M |
| Observações internas no pedido (hoje não há campo) | Ideia derivada; comum nos CRMs | P | M |
| Consignado: peças deixadas numa loja, vendidas, devolvidas, acerto e comissão. Depende do estoque de pronta-entrega do roadmap | Atlas3D, Calc3D Pro, 3dcalculate | M | M |
| Venda rápida pra feira (PDV): toca no produto, vendeu, forma de pagamento. Mesma dependência | Atlas3D, Calc3D Pro, Kyte, Gestor 3D | M | M |

### 6. Ligação com a impressora pela rede local

| Ideia | Quem faz | Esforço | Impacto |
|---|---|---|---|
| Ler a impressora na rede de casa (modo LAN da Bambu, OctoPrint, Moonraker): marcar "Em impressão" e "Pronto" sozinho, tempo e filamento reais, falha registrada no pedido. Não precisa de nuvem, mas a Bambu já mudou o acesso local antes, então é um risco de manutenção | 3D Control Bridge, Manuflo, SimplyPrint, Repetier-Server, Bambu Farm Manager | G | M |
| Energia medida de verdade (tomada inteligente, Home Assistant) no lugar da potência do manual | Atlas3D, ha-bambu-costs | M | B |

### 7. Novos públicos

| Ideia | Quem faz | Esforço | Impacto |
|---|---|---|---|
| **Resina (SLA/MSLA):** resina em ml, álcool, desgaste da tela e do FEP, cura. Abre miniaturas, joias e odontologia, e hoje o app é só FDM | Infinity Maker, 3DTAG, Neobrix, 3dprintpricecalculator e as calculadoras de resina | G | M |

### 8. Dados e migração

| Ideia | Quem faz | Esforço | Impacto |
|---|---|---|---|
| **Importar de planilha** (clientes, filamentos, produtos) pra quem chega da planilha, que é quase todo mundo | 3D Control, FoxTrack, 3DPCC | M | A |
| Importar a planilha de pedidos exportada pela Shopee e pelo Mercado Livre: a versão local da integração com marketplace | Custos3D | M | M |
| IA pelo MCP: o app expõe os dados do computador pro assistente que a pessoa já usa (Claude, ChatGPT), sem API de IA dentro do app. Variação do item de IA do roadmap que não depende de servidor | 3D Control (IA conversando com os dados) | M | B |

### 9. Facilidade de uso

| Ideia | Quem faz | Esforço | Impacto |
|---|---|---|---|
| **Tela "Hoje"**: o que vence, orçamentos parados, estoque baixo, quem falta pagar | Manager 3D (painel operacional), Printforge | M | A |
| Modo demonstração com dados de exemplo, reaproveitando os dados do gerador de prints (`renderScreenshots`) | Custos3D (demonstração), 3D Control (plano grátis) | P | M |
| Busca geral (pedido, cliente, produto) num campo só | Ideia derivada | P/M | B |
| Mensagens prontas, importar planilha e venda rápida | Ver temas 4, 8 e 5 | | |

### O mercado reforça itens que já estão no roadmap

Não são ideias novas, mas a pesquisa dá argumento pra eles na hora de priorizar:

- **Sinal e pagamento parcial:** Invoice Ninja e Jobber têm sinal com vencimento
  próprio; Calc3D Pro controla cobranças pendentes.
- **Portal de acompanhamento pro cliente:** Custos3D, Printora, GestãoClick e 3DTAG.
- **Estoque de pronta-entrega:** 3D Control tem "ordem de produção pra estoque", sem
  venda antes.
- **Variações do mesmo produto:** 3DPCC.
- **Orçamento com opções:** DigiFabster mostra preço por prazo e por quantidade.
- **Custo de falha real:** SISTEMA3D controla perdas por item.
- **Exportar CSV:** FoxTrack e 3DPCC.
- **Compatibilidade com Spoolman:** segue sendo o padrão aberto de estoque de
  carretel.
- **Assistente de IA:** Printforge e 3D Control.
- **Acesso pelo celular:** todos os concorrentes brasileiros têm, por serem web. O
  Android do roadmap é o que responde a isso (ver Parte 2).

### Sugestão de prioridade da Parte 1

Só uma sugestão, a partir de impacto, esforço e encaixe no que o app já é. A ordem
real é do responsável do projeto.

1. Taxa fixa por item no canal de venda (corrige um preço que sai errado hoje).
2. Pix copia e cola e QR Code no orçamento.
3. Mensagens prontas por andamento e lembrete de orçamento parado.
4. Estoque em gramas com baixa automática e alerta (revisita a decisão 39).
5. Custo de embalagem e insumos por produto.
6. Situação do pagamento e recibo.
7. Despesas do mês, lucro real, ponto de equilíbrio e meta.
8. Tela "Hoje".
9. Importar de planilha (e a planilha de pedidos da Shopee e do Mercado Livre).
10. Calendário de produção com sugestão de prazo.

Depois: consignado e venda rápida (junto do estoque de pronta-entrega do roadmap),
ficha de produção, resina e ligação com a impressora pela rede local.

**Ideia de divulgação** (não é funcionalidade): uma tabela no site comparando
"3DReport, planilha e sistema por assinatura", sem citar concorrentes, destacando
"sem limite de pedidos" e "seus dados não ficam presos se você parar de pagar".

## Parte 2: funcionalidades conectadas (nuvem)

### Por que olhar pra isso

Todo concorrente brasileiro direto é web: o vendedor abre no celular, divide com o
sócio, manda link pro cliente e puxa pedido do Mercado Livre. Pra atender quem
cresce (mais de um aparelho, equipe, cliente que quer acompanhar, venda em
marketplace), o 3DReport vai precisar de algum grau de conexão.

Isso mexe num princípio que hoje é argumento de venda ("100% no seu computador",
decisão 90). O jeito de não perder esse argumento é o modelo
[local-first](https://www.inkandswitch.com/local-first/): o app continua funcionando
sem internet, os dados moram primeiro no computador, e cada recurso conectado é
opcional e ligado pela pessoa.

### Os degraus de conexão

As tabelas abaixo dizem, pra cada funcionalidade, o menor degrau que ela exige.

- **D1, integração direta:** o app chama a API de um serviço usando a conta do
  próprio vendedor. Não há servidor do projeto. Só funciona com o app aberto e não
  recebe avisos de fora (webhooks).
- **D2, publicação estática:** o app gera páginas (catálogo, acompanhamento do
  pedido) e publica numa hospedagem grátis. Não há banco nem servidor de aplicação;
  o cliente só lê, não responde.
- **D3, servidor do projeto:** contas, sincronização, páginas em que o cliente
  interage, webhooks e tarefas que rodam com o app fechado.

### Catálogo de funcionalidades conectadas

**A. Seus dados em qualquer lugar**

| Funcionalidade | Quem faz | Exige | Impacto |
|---|---|---|---|
| Sincronizar computador e celular (item do roadmap) | Todos os sistemas brasileiros | D3; ou, sem servidor, pela pasta do Drive ou Dropbox do próprio vendedor, que sincroniza mas não oferece nada voltado ao cliente | A |
| Usar pelo navegador, sem instalar | 3D Control, Printora, SISTEMA3D, Atlas3D e os demais | D3 + versão web do app | A |
| Backup na nuvem sem precisar configurar pasta (hoje o backup automático vai pra nuvem se a pasta dele estiver no Drive ou OneDrive) | Os sistemas web, por natureza | D1 (API do Drive) ou D3 | M |

**B. Equipe**

| Funcionalidade | Quem faz | Exige | Impacto |
|---|---|---|---|
| Vários usuários com permissão por módulo, aprovação do dono pro pedido criado por um membro, registro de quem fez o quê, verificação em duas etapas | 3D Control, SISTEMA3D, SimplyPrint, PrintFarmDesk, Prinate | D3 | M |

**C. Cliente conectado**

| Funcionalidade | Quem faz | Exige | Impacto |
|---|---|---|---|
| Link de acompanhamento do pedido (item do roadmap) | Custos3D, Printora, GestãoClick, 3DTAG | D2 (a página muda quando o app publica) ou D3 (ao vivo) | A |
| Catálogo online pra bio do Instagram | Pitada, Kyte, Printora (modo só catálogo) | D2 | A |
| Link do orçamento com aceite online (data e hora registradas) e sinal pago no aceite | Invoice Ninja, Jobber | D3 | M |
| Aviso automático por e-mail quando o andamento muda | Jobber, Wave | D3 | M |
| Formulário de encomenda sob medida (o cliente descreve e manda foto ou STL) | Castiron, Layers | D3 | M |
| Orçamento instantâneo: o cliente sobe o STL no site do vendedor e vê o preço | Layers, DigiFabster, PrintQuote (Shopify), Craftcloud | D2 pra mostrar o preço (o `core` já roda no navegador pelo módulo `web`, mas falta estimar peso e tempo pelo STL, a Fase 2 do roadmap); D3 pra virar pedido | M |
| Loja com carrinho e pagamento | Printora, Nuvemshop | D3 + meio de pagamento | B |

**D. Pagamentos**

| Funcionalidade | Quem faz | Exige | Impacto |
|---|---|---|---|
| Pix com confirmação automática e o pedido marcado como pago (Pix dinâmico gerado por um provedor de pagamento) | Kyte, Omie; o Calc3D Pro põe o QR no PDF (a confirmação automática não foi verificada) | D1 (consulta com o token do vendedor, só com o app aberto) ou D3 (webhook) | A |
| Link de pagamento com cartão e parcelamento | Jobber, Invoice Ninja, Kyte | D1 ou D3 | M |

**E. Marketplaces**

| Funcionalidade | Quem faz | Exige | Impacto |
|---|---|---|---|
| Pedidos do Mercado Livre entrando sozinhos, com o lucro real de cada venda | 3D Control (ativo; Shopee, Amazon, Shein e Elo7 anunciados), Tiny, Bling | **D3 obrigatório.** O Mercado Livre exige o segredo do app (`client_secret`) na troca do token mesmo com PKCE, e a Shopee assina toda chamada com a chave do parceiro (`partner_key`). Segredo não pode ir dentro de um app de código aberto. A alternativa sem servidor, cada vendedor registrar o próprio app no Mercado Livre, é complicada demais pro público | A |
| Estoque de pronta-entrega sincronizado com os anúncios (não vender o que acabou) | Craftybase, Tiny | D3 | M |
| Etsy, Shopify, Nuvemshop | Printago, Craftybase, 3D PrintForce, Manuflo | D3 | B pro público brasileiro |

**F. Envio**

| Funcionalidade | Quem faz | Exige | Impacto |
|---|---|---|---|
| Cotar frete, comprar etiqueta e rastrear | 3D Control (Melhor Envio), Tiny | D1 com a conta do vendedor, ou D3 | M |

**G. Fiscal (MEI)**

| Funcionalidade | Quem faz | Exige | Impacto |
|---|---|---|---|
| Emitir nota fiscal (NFS-e ou NF-e) a partir do pedido | Bling, SmartMEI, Omie, GestãoClick | D1 ou D3, mais certificado digital ou uma API paga de emissão. Traz risco legal e demanda de suporte | M |

**H. WhatsApp automático**

| Funcionalidade | Quem faz | Exige | Impacto |
|---|---|---|---|
| Mensagem automática a cada mudança de andamento pela API oficial; atendente com IA que responde preço e prazo e cria o pedido | Bakerly, Nuvem Chat (Nuvemshop) | D3, verificação da empresa na Meta e custo por mensagem (a Meta passou a cobrar por mensagem em 2026) | M |

**I. IA**

| Funcionalidade | Quem faz | Exige | Impacto |
|---|---|---|---|
| Conversar com os dados do negócio ("quanto lucrei com chaveiro em agosto?") | 3D Control (Claude, ChatGPT, Cursor) | D1 com a chave do próprio usuário, ou o MCP local da Parte 1 | M |
| Rascunho de orçamento e descrição de anúncio a partir da foto | Printforge | D1 | M |
| Pesquisa do preço da concorrência na Shopee e no Mercado Livre | Gestor 3D (extensão do navegador) | D1 | B |

**J. Impressoras pela internet**

| Funcionalidade | Quem faz | Exige | Impacto |
|---|---|---|---|
| Ver e controlar a impressora de fora de casa, com câmera e aviso no celular quando termina ou falha | 3D Control Bridge, SimplyPrint, Obico, Printago, Prusa Connect, Bambu Handy | D3 (uma ponte até a rede de casa) ou a nuvem do fabricante | M |
| Detecção de falha por IA na câmera | Obico, AutoFarm3D, SimplyPrint | D3 + processamento de imagem | B |

**K. Comunidade e dados coletivos**

| Funcionalidade | Quem faz | Exige | Impacto |
|---|---|---|---|
| Presets de filamento e impressora atualizados sem nova versão do app | Spoolman (banco da comunidade), Open Filament Database, SimplyPrint (130+ marcas) | D2 (um arquivo publicado que o app baixa) | M |
| Referência anônima de preço ("quanto se cobra por hora de máquina na sua região") | Ideia derivada | D3, adesão explícita e LGPD | M |
| Relatório de erro e de uso, opcionais, pra decidir o roadmap com dado | Ideia derivada (prática comum) | D3 ou serviço de terceiros | B |
| Atualização automática (baixar e instalar sozinho) | Prática comum em apps desktop | D1 (GitHub Releases); fica melhor com instalador assinado | M |

### O que o app já tem pronto pra isso

As revisões pré-lançamento (decisões 106 e 108) prepararam o app pra sincronizar
sem saber ainda como:

- **Repositórios são interfaces**, montados no `AppContainer`: trocar o armazenamento
  é trocar a implementação, sem mexer nas telas.
- **Todo registro tem carimbo** de criação, alteração e exclusão lógica
  (`StoredRecord` em `data/store/Records.kt`), com lixeira de 30 dias e marca de
  exclusão permanente, que é o que uma sincronização precisa pra saber o que apagar.
- **Ids em UUID**, inclusive nos dados iniciais, pra não colidir entre aparelhos.
- **Anexos endereçados pelo conteúdo** (SHA-256, `AttachmentStore`), que já servem de
  chave num armazenamento de arquivos na nuvem.
- **O `core` compila pra JVM e JavaScript**, e o módulo `web` já roda o
  `PricingCalculator` no navegador: app, site e um servidor podem usar exatamente a
  mesma conta.
- **Formato de dados versionado**, com migrações.
- **Uma chamada de rede já existe**, opcional: a verificação de versão nova no
  GitHub.

### O que teria que mudar, camada por camada

| # | Camada | Hoje | O que muda | Tipo de mudança |
|---|---|---|---|---|
| 1 | Armazenamento local | Arquivos JSON reescritos inteiros a cada mudança (`JsonDataFile`) | SQLite (SQLDelight, ou o SQLite de uma biblioteca de sincronização) como nova implementação das mesmas interfaces. Já previsto na decisão 108 | Implementação |
| 2 | Carimbo dos registros | Relógio do computador | Id do aparelho, versão dada pelo servidor (ou relógio lógico) e uma marca de "mudou desde a última sincronização" | Aditiva |
| 3 | Número do pedido | `lastNumber + 1` local, em `QuoteHistoryRepository` | Dois aparelhos sem internet gerariam o mesmo "#0042", e o número aparece no PDF. Opções: número reservado pelo servidor, uma faixa por aparelho ou um prefixo | Pequena, mas visível pro cliente |
| 4 | Conflito de edição | Não existe (uma instância só, com trava) | Regra de quem vence: o último que grava, por registro ou por campo; o histórico de status pode juntar os dois lados. Como o pedido é um retrato congelado, conflito deve ser raro | Decisão + código |
| 5 | Versões diferentes do app ao mesmo tempo | A pasta é migrada inteira ao abrir, e a leitura usa `ignoreUnknownKeys = true` (`JsonDataFile.kt`) | Com sincronização, um app antigo lê um registro gravado por um app novo, descarta o campo que não conhece e, ao sincronizar, apaga esse dado no outro aparelho. Precisa preservar os campos desconhecidos, ou o servidor exigir uma versão mínima do app | Código + disciplina |
| 6 | Configurações | Tudo local | Separar o que é do aparelho (janela, tema, verificação de versão) do que é do negócio (custos, marca, canais, cadastros, moeda) | Pequena |
| 7 | Rede | Uma chamada, com `java.net.http` | Cliente HTTP multiplataforma atrás de interfaces, com repetição, fila pra quando estiver sem internet e o estado da conexão na tela (como o `StorageHealth` faz com o disco) | Aditiva |
| 8 | Credenciais | Não há | Tokens guardados no cofre do sistema (Keychain, Gerenciador de Credenciais do Windows, libsecret; Keystore no Android) e login pelo navegador. Segredo de app (Mercado Livre, Shopee) fica só no servidor | Novo serviço de plataforma |
| 9 | O que o cliente vê (decisão 19) | Regra implícita em cada exportador (PDF, texto, imagem) | Página pública exige um "retrato pro cliente" explícito no domínio (nome, foto, preço, prazo, andamento), publicado no lugar do registro inteiro. Um erro aqui vaza custo e margem pro cliente | Arquitetural, pequena e crítica |
| 10 | Dono dos dados | Uma pessoa, um computador | Registros passam a pertencer a um "negócio"; usuários com papel; `StatusChange` ganha "quem mudou" | Aditiva no modelo, nova no servidor |
| 11 | Trabalho com o app fechado | Não há | Lembretes, e-mails, webhooks de Pix e de marketplace, renovação de token: só existem com servidor | Componente novo |
| 12 | Outros aparelhos | Só desktop | Android já está previsto sem mexer nas telas (ver `architecture.md`). Navegador: o Compose pra web está em Beta desde a versão 1.9 (conferir a situação na versão em uso), e o PDF (Apache PDFBox) só roda na JVM, então precisaria de um gerador multiplataforma ou de PDF feito no servidor | A maior obra do lado do app |
| 13 | Servidor | Não há | Três caminhos: (a) backend pronto, de código aberto e auto-hospedável, como Supabase (Postgres, login, arquivos), com PowerSync pra sincronização que funciona sem internet (tem SDK Kotlin Multiplatform com desktop, ainda em alpha segundo a própria PowerSync); (b) servidor próprio em Ktor reaproveitando o `core`; (c) nenhum servidor: sincronizar pela pasta do Drive ou Dropbox do vendedor, o que resolve sincronização e backup, mas nada voltado ao cliente | Componente novo |
| 14 | Segurança e LGPD | Nada sai do computador | O projeto passa a guardar dados pessoais dos clientes dos vendedores (nome, telefone, endereço): política de privacidade, termos de uso, exclusão e exportação dos dados, criptografia, plano pra incidente, logs sem dado pessoal. Uma opção que mantém o espírito "seus dados são seus" é a sincronização cifrada de ponta a ponta (o servidor guarda só dados cifrados), com o que é público publicado à parte, de propósito. O preço disso: o servidor não consegue ler os dados, então lembrete por e-mail e casamento de webhook ficam limitados | Processo + código |
| 15 | Custo | Zero | Servidor, armazenamento e e-mail custam todo mês, e o projeto vive de doação, sem edição paga (decisão 15). Caminhos: só auto-hospedado (quem quiser roda o próprio servidor, como no Spoolman); serviço oficial grátis com limites, bancado por doação; ou hospedagem paga com o código continuando aberto, como fazem Obico e Invoice Ninja (revisita a decisão 15) | Decisão de projeto |
| 16 | Operação | Não há | Monitorar, fazer backup do servidor, aplicar correções de segurança, dar suporte. É pesado pra quem mantém o projeto sozinho; um backend gerenciado reduz isso | Processo |

### Precisa mudar a arquitetura?

**No app, não precisa reescrever.** A base foi preparada pra isso nas decisões 106
e 108. O que muda no app é localizado: o motor de armazenamento (JSON pra SQLite),
uma camada de rede e sincronização, a regra explícita do que o cliente vê, a
numeração do pedido e a preservação de campos desconhecidos.

**A mudança de arquitetura de verdade é somar um componente novo:** um servidor (ou
um backend pronto) com contas, negócio e equipe, sincronização, páginas pro cliente,
webhooks e tarefas agendadas.

**E vem junto uma mudança de princípio:** de "100% no seu computador" pra "funciona
sem internet, a nuvem é opcional". Ela mexe no posicionamento (decisão 90), traz a
LGPD pra dentro do projeto e cria um custo mensal que o modelo atual (decisão 15)
não cobre.

### Caminho em degraus

Sugestão de ordem, sem decidir nada:

1. **D1, sem servidor do projeto:** IA com a chave do usuário ou pelo MCP, Pix com
   confirmação por consulta, frete e etiqueta com a conta do vendedor, atualização
   automática, presets baixados. Muda: camada de rede e cofre de credenciais.
2. **D2, publicação estática:** catálogo online e link de acompanhamento como páginas
   geradas pelo app e publicadas numa hospedagem grátis. Muda: gerador de páginas, o
   "retrato pro cliente" e a credencial de publicação. Resolve dois pedidos
   frequentes (catálogo online e acompanhamento) sem servidor.
3. **D3, servidor:** sincronização (que é o que dá sentido ao Android), equipe,
   aceite online, webhooks de Pix e do Mercado Livre, avisos automáticos. Muda:
   SQLite, sincronização, contas, LGPD e custo.
4. **Depois do D3:** loja com checkout, estoque sincronizado com os marketplaces,
   WhatsApp pela API oficial, nota fiscal, detecção de falha por IA.

### Decisões que precisariam vir antes do D3

- Quem paga o servidor (revisita a decisão 15).
- Como fica o posicionamento (revisita a decisão 90).
- Sincronização cifrada de ponta a ponta ou servidor que lê os dados.
- Serviço oficial, auto-hospedado ou os dois.
- Backend pronto ou próprio.
- Regra de conflito entre aparelhos.
- Numeração do pedido entre aparelhos.
- Quem responde pela LGPD.

## Fora do foco

Itens vistos na pesquisa que não combinam com o público ou com o momento do
3DReport, nem com nuvem:

- Integração com contabilidade estrangeira (Xero, QuickBooks).
- Marketplaces de terceirização de impressão (Craftcloud, Treatstock).
- Ferramentas de criação de modelo, como converter imagem em SVG e separar cores de
  um 3MF (Gestor 3D).
- Roteamento automático de trabalhos e encaixe de peças numa fazenda grande
  (Printago, GrabCAD Print).
- Recursos industriais: rastreabilidade de material, análise de viabilidade de peça,
  certificação (Authentise, Castor, Materialise).

## Fontes

**Concorrentes brasileiros:** [3D Control](https://3dcontrol.com.br/) ·
[SISTEMA3D](https://sistema3d.com.br/) · [Printora](https://useprintora.com.br/) ·
[Atlas3D](https://atlas3d.com.br/) · [Manager 3D](https://www.manager3d.com.br/) ·
[3DTAG](https://www.3dtag.com.br/) · [Gestor 3D](https://gestor3d.app/) ·
[Custos3D](https://appcustos3d.com.br/) · [Calc3D Pro](https://www.calc3dpro.com.br/) ·
[Calc3D](https://calc3d.com.br/) · [3D Prime](https://3dprime.com.br/) ·
[Neobrix Suite](https://www.neobrixsuite.com.br/) ·
[3D Print Flow](https://3dprintflow.com.br/) · [My3D Farm](https://my3dfarm.com.br/) ·
[GestorMaker](https://gestormaker.com.br/) · [Controlefy](https://controle3d.com/) ·
[Precifi3D](https://precifi3d.com/) ·
[Objeto3D](https://objeto3d.com.br/calculadora-custos) ·
[3dcalculate](https://github.com/rodrigoinhaia/3dcalculate)

**Taxas de marketplace em 2026:**
[E-Commerce Brasil](https://www.ecommercebrasil.com.br/artigos/shopee-acaba-com-o-teto-de-comissao-de-r-100-e-aumenta-a-taxa-fixa-cobrada-por-cada-item-vendido-em-ate-550) ·
[XP Investimentos](https://conteudos.xpi.com.br/acoes/relatorios/mercado-livre-meli34-shopee-inicia-2026-com-aumento-de-taxas/)

**Gestão de negócio de impressão 3D:** [Printforge](https://crm.printforge.com.au/) ·
[FoxTrack](https://foxtrack.studio/) · [PrintFarmHQ](https://printfarmhq.io/) ·
[PrintFarmDesk](https://www.printfarmdesk.com/) ·
[Manuflo](https://manuflo.app/blog/post-12-3d-printing-business-software) ·
[3DPCC](https://3dpcc.com/) · [Prinate](https://prinate.app/) ·
[3DPBOSS](https://3dpboss.com/) ·
[Craftybase](https://craftybase.com/3d-printing-inventory-software) ·
[3D PrintForce](https://3dprintforce.com/) · [Layers](https://layers.app/) ·
[3DPrintOps](https://www.3dprintops.com/blog/3d-printer-farm-management-software)

**Fazendas e impressoras:** [Printago](https://printago.io/) ·
[SimplyPrint](https://simplyprint.io/print-farms) ·
[3DPrinterOS](https://www.3dprinteros.com/3d-printer-fleet-management) ·
[AutoFarm3D](https://www.3dque.com/autofarm3d) ·
[OctoFarm](https://github.com/OctoFarm/OctoFarm) ·
[Obico](https://www.obico.io/failure-detection.html) ·
[Prusa Connect](https://www.prusa3d.com/p/prusa-connect/) ·
[Bambu Farm Manager](https://wiki.bambulab.com/en/software/bambu-farm-features) ·
[Repetier-Server](https://www.repetier-server.com/3d-printer-farms-and-3d-printing-services/) ·
[Polar Cloud](https://polar3d.com/solutions/k-12) ·
[AstroPrint](https://www.astroprint.com/3d-printer-farm-software) ·
[GrabCAD Print](https://grabcad.com/en/print) ·
[3D Print Log](https://www.3dprintlog.com/)

**Calculadoras e filamento:** [PrintQuote3D](https://github.com/g4l4xy/PrintQuote3D) ·
[3D-Print-Cost-Calculator](https://github.com/AndreasReitberger/3D-Print-Cost-Calculator) ·
[Calculadora da Prusa](https://blog.prusa3d.com/3d-printing-price-calculator_38905) ·
[Omni Calculator](https://www.omnicalculator.com/other/3d-printing) ·
[3dprintpricecalculator](https://3dprintpricecalculator.com) ·
[OctoPrint CostEstimation](https://plugins.octoprint.org/plugins/costestimation/) ·
[OctoPrint PrintJobHistory](https://github.com/OllisGit/OctoPrint-PrintJobHistory) ·
[PrintPal](https://printpal.io) · [Spoolman](https://github.com/Donkie/Spoolman) ·
[FilaMan](https://filaman.app/) · [SpoolEase](https://spoolease.io/) ·
[OpenSpool](https://github.com/spuder/OpenSpool) ·
[OpenTag3D](https://opentag3d.info/) ·
[ha-bambu-costs](https://github.com/Oose97/ha-bambu-costs) ·
[Spool](https://apps.apple.com/us/app/spool/id6756892049) ·
[Spoolio](https://spoolio.net)

**Orçamento instantâneo:** [DigiFabster](https://digifabster.com/pricing/) ·
[AMFG](https://www.amfg.ai/additive-manufacturing) ·
[3YOURMIND](https://www.3yourmind.com/software-suite) ·
[Craftcloud](https://craftcloud3d.com/) · [Treatstock](https://www.treatstock.com/) ·
[Xometry](https://www.xometry.com/how-xometry-works/) ·
[Protolabs Network](https://www.hubs.com/) · [Shapeways](https://www.shapeways.com/) ·
[Castor](https://get.castor.tech/) ·
[Authentise](https://www.authentise.com/solutions/flows) ·
[Materialise CO-AM](https://www.materialise.com/en/industrial/software/co-am-software-platform) ·
[PrintQuote (Shopify)](https://apps.shopify.com/3d-print-quote)

**Negócios vizinhos:** [Pitada](https://www.usepitada.com) ·
[ZupConfeitaria](https://www.zupconfeitaria.com) · [Bakerly](https://bakerlyapp.com) ·
[CakeBoss](https://cakeboss.com) ·
[Castiron (encerramento)](https://findhomegrown.com/blog/castiron-alternative-food-vendors) ·
[Bake Diary](https://www.bakediary.com/features.html) ·
[Doce Lucro](https://www.appdocelucro.com.br) ·
[DoceGestor](https://docegestor.github.io) · [Kyte](https://www.kyteapp.com/) ·
[Bling](https://www.bling.com.br/segmentos/erp-mei) · [Tiny](https://tiny.com.br/) ·
[GestãoClick](https://gestaoclick.com.br/) · [Omie](https://www.omie.com.br/) ·
[SmartMEI](https://apps.apple.com/br/app/smart-mei-brasil/id6746266914) ·
[Nuvemshop](https://www.nuvemshop.com.br/) ·
[Fazer Orçamento](https://fazerorcamento.com/) ·
[Invoice Ninja](https://invoiceninja.github.io/docs/user-guide/quotes) ·
[Jobber](https://help.getjobber.com/hc/en-us/articles/115012715008-Quote-Approvals) ·
[Zoho Invoice](https://www.zoho.com/us/invoice/) ·
[Wave](https://www.waveapps.com/invoicing) ·
[Katana](https://katanamrp.com/pricing/) ·
[inFlow](https://www.business.org/finance/inventory-management/inflow-review/) ·
[Vela](https://www.getvela.com/)

**Parte 2 (nuvem):**
[Local-first software (Ink & Switch)](https://www.inkandswitch.com/local-first/) ·
[Autenticação do Mercado Livre](https://developers.mercadolivre.com.br/en_us/authentication-and-authorization) ·
[Guia da API da Shopee](https://api2cart.com/api-technology/shopee-api/) ·
[PowerSync para Kotlin Multiplatform](https://docs.powersync.com/client-sdk-references/kotlin-multiplatform) ·
[Compose Multiplatform 1.9 (web em Beta)](https://blog.jetbrains.com/kotlin/2025/09/compose-multiplatform-1-9-0-compose-for-web-beta/) ·
[Preço da API do WhatsApp em 2026](https://www.socialhub.pro/blog/preco-whatsapp-api-2026-brasil/)
