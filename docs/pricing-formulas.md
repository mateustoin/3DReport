# Fórmulas de precificação

Documento de referência do motor de cálculo (`core/.../pricing/PricingCalculator.kt`).
As fórmulas foram derivadas da planilha de precificação usada atualmente
(calculadora "3D Geek Show") e ajustadas conforme decisões registradas em
[decisions.md](decisions.md).

## Entradas

### Por impressão (`PrintJob`)

Vêm do fatiador: uma impressão é uma mesa que a impressora roda. Um pedido
pode ter várias (decisão 105), cada uma na sua impressora. O seu tempo de
trabalho não entra aqui: é do pedido inteiro (ver "Quantidade e lote").

| Parâmetro | Campo | Unidade |
|---|---|---|
| Filamentos usados, um por linha (peça multicolor tem vários) | `filaments` | lista |
| Filamento (nome, preço/kg, densidade, diâmetro) | `filaments[i].filament` | R$/kg, g/cm³, mm |
| Comprimento desse filamento numa rodada (do G-code, já com purga e torre) | `filaments[i].lengthMeters` | m |
| Tempo de uma rodada | `printTimeMinutes` | min |
| Quantas vezes a mesma mesa roda no pedido | `runs` | vezes (padrão 1) |

### Por pedido (`Quote`)

| Parâmetro | Campo | Unidade |
|---|---|---|
| Quantidade (pedidos iguais) | `quantity` | un. |
| Seu tempo de trabalho no pedido inteiro (decisão 94) | `laborMinutes` | min |

### Perfil da impressora (`PrinterProfile`), um por impressão

Uma pessoa costuma ter várias impressoras; cada uma tem seu próprio perfil
salvo (tela Impressoras) e cada impressão do orçamento escolhe em qual roda.

| Parâmetro | Campo | Unidade |
|---|---|---|
| Consumo da impressora | `printerPowerWatts` | W |
| Custo de manutenção por hora | `maintenanceCostPerHour` | R$/h |
| Valor da máquina | `machineInvestment.machinePrice` | R$ |
| Prazo de retorno | `machineInvestment.paybackMonths` | meses |
| Dias de uso por mês | `machineInvestment.printingDaysPerMonth` | dias |
| Horas de uso por dia | `machineInvestment.printingHoursPerDay` | h |

### Configurações gerais do negócio (`PricingSettings`) — iguais para qualquer impressora

| Parâmetro | Campo | Unidade |
|---|---|---|
| Preço do kWh | `energyPricePerKwh` | R$ |
| Taxa de falhas | `failureRate` | fração (0,10 = 10%) |
| Taxa de acabamento | `finishingRate` | fração |
| Valor da sua hora de trabalho | `laborRatePerHour` | R$/h |
| Custo fixo mensal do negócio | `monthlyFixedCost` | R$ por mês |
| Horas de impressão por mês (todas as impressoras) | `productiveHoursPerMonth` | h |
| Custo administrativo (ex.: modelagem) | `administrativeCost` | R$ por orçamento |
| Margem de lucro | `profitMargin` | fração (1,0 = 100%) |

Os três campos novos (`laborRatePerHour`, `monthlyFixedCost`,
`productiveHoursPerMonth`) nascem zerados, e zero desliga a parcela
correspondente. Quem atualiza o app e não mexe em nada continua com o
mesmo preço de antes, exceto pela reserva de falha (ver "Duas regras que
mudaram" abaixo).

## Cálculos

Primeiro, cada impressão sozinha (com a impressora dela):

```
horas              = tempo_min / 60 · rodadas
área (mm²)         = π · (diâmetro / 2)²                      (de cada filamento)
volume (cm³)       = comprimento_m · 1000 · área / 1000
peso (g)           = volume · densidade

material           = Σ filamentos (peso / 1000 · preço_kg) · rodadas
energia            = horas · (W / 1000) · preço_kWh
manutenção         = horas · manutenção_por_hora
valor_hora_máquina = valor_máquina / (meses · dias_mês · horas_dia)
retorno_invest.    = horas · valor_hora_máquina
custo_fixo_hora    = custo_fixo_mensal / horas_produtivas_mês   (0 se horas = 0)
custo_fixo         = horas · custo_fixo_hora
acabamento         = material · taxa_acabamento

custo_impressão    = material + energia + manutenção + retorno_invest.
                     + custo_fixo + acabamento
```

Depois, o pedido, com o que entra **uma vez só** (somar orçamentos
separados cobraria o administrativo e o trabalho uma vez por impressão):

```
trabalho_pedido    = minutos_trabalho_pedido / 60 · valor_hora_trabalho
administrativo     = custo_administrativo

CUSTO REFEITO      = Σ impressões (custo_impressão) · quantidade + trabalho_pedido
falhas             = CUSTO REFEITO · taxa_falhas

VALOR DE PRODUÇÃO  = CUSTO REFEITO + falhas + administrativo
PREÇO BASE         = produção · (1 + margem_lucro)

deduções           = taxa_do_canal + imposto
VALOR DE VENDA     = PREÇO BASE / (1 − deduções)
PREÇO UNITÁRIO     = (venda + serviços + frete) / quantidade
LUCRO              = venda · (1 − deduções) − produção

TOTAL DO CLIENTE   = venda + serviços_por_peça · quantidade + serviços_por_pedido + frete
```

Todos os valores acima são do **pedido inteiro**. As entradas de cada
impressão (comprimentos e tempo) são de **uma rodada**, e é o app que
multiplica pelas rodadas e pela quantidade: é assim que o fatiador informa.
Se você fatiou a mesa cheia de uma vez e os números já são do lote todo,
mantenha rodadas e quantidade em 1.

Com uma impressão, um filamento e uma rodada, as fórmulas dão exatamente o
mesmo de antes da decisão 105: é a conta da planilha de referência.

Os valores são mantidos em `Double` sem arredondamento; o arredondamento para
centavos acontece apenas na exibição.

## Duas regras que mudaram (2026-09-22, decisão 76)

**1. A reserva de falha incide sobre tudo que é refeito, não só sobre o
material.** Antes, `falhas = material · taxa`. Só que uma impressão de 8 h que
falha no fim não desperdiça apenas plástico: desperdiça energia, desgaste da
máquina, hora de máquina, custo fixo e o seu tempo. Reservar um percentual só
do material subestimava a perda real em várias vezes. Agora a taxa incide
sobre o "custo refeito" — tudo que você paga de novo pra refazer a peça. O
**custo administrativo fica de fora** porque é a única parcela que não se
refaz: uma modelagem já entregue continua pronta, a impressão falhando ou não.

**2. Acabamento pode ser cobrado por tempo, e a hora de trabalho só soma.**
`acabamento = material · taxa` cobra errado em muitos casos: uma action
figure de 30 g pode dar 40 min de lixa e pintura, enquanto um suporte de
parede liso de 200 g dá 2 min, mas o suporte "pagava" quase 7x mais
acabamento, só por pesar mais. Acabamento escala com tempo, não com gramas.

Por isso existem as duas formas, e elas são independentes (decisão 93):

- **Taxa de acabamento:** percentual do material, sempre soma. Serve pra
  quem não quer cronometrar cada peça.
- **Minutos de trabalho** (com o valor da hora configurado): o que você
  informa em cada orçamento, incluindo lixar e pintar, se quiser.

Quem conta o acabamento nos minutos deixa a taxa em 0, pra não cobrar o mesmo
trabalho duas vezes. Configurar o valor da hora nunca baixa o preço: ele só
soma mão de obra.

Até a v1.38.0, informar o valor da hora zerava a taxa de acabamento sozinho,
e o preço caía até alguém preencher os minutos. Na atualização para a 1.39.0,
quem já tinha a hora configurada teve a taxa de acabamento zerada uma vez
(`PricingSettings.migrated`), que é exatamente o que o cálculo antigo fazia:
o preço dessas pessoas não mudou.

A mão de obra cobre o serviço inteiro que a peça dá e que não aparece em
nenhum outro custo: preparar o arquivo, fatiar, tirar da mesa, remover
suporte, lixar, pintar, embalar e atender o cliente. É diferente do tempo de
impressão, em que a máquina trabalha enquanto você faz outra coisa.

## Quantidade e lote (2026-09-22, decisão 77)

Duas parcelas se comportam de formas diferentes quando a quantidade sobe, e é
essa diferença que faz o preço por unidade cair sozinho:

| Parcela | Multiplica pela quantidade? |
|---|---|
| Material, energia, manutenção, retorno da máquina, custo fixo | Sim, cada peça consome o seu |
| Seu tempo de trabalho no pedido (`Quote.laborMinutes`) | **Não**, você já informa o total do pedido |
| Custo administrativo (ex.: modelagem) | **Não**, é por orçamento |
| Serviços por peça (pintura, lixamento) | Sim, são trabalho peça a peça |
| Serviços por pedido (entrega, modelagem) | **Não**, cobrados uma vez (decisão 92) |

O valor de cada serviço é digitado no orçamento (o cadastro guarda só uma
sugestão), e cada serviço marcado diz se é por peça ou por pedido. Nas
fórmulas abaixo, "serviços" é sempre essa soma já pronta:
`Σ (valor × quantidade)` dos por peça + `Σ valor` dos por pedido
(`QuoteService.total`).

**Tempo de trabalho num campo só (decisão 94).** Até a v1.38.0 havia dois
campos: tempo por peça (multiplicado pela quantidade) e preparo do pedido
(cobrado uma vez). Com 1 peça eles davam no mesmo, e com várias a pessoa
precisava separar de cabeça uma conta que ela faz como um número só. Agora é
um campo, "Seu tempo de trabalho no pedido", com o total. Exemplo com
R$ 30,00/h, 20 min pra fatiar e montar a mesa e 3 min de acabamento por peça:

| Quantidade | Você informa | Custo de trabalho |
|---|---|---|
| 1 peça | 23 min | R$ 11,50 (R$ 11,50 por peça) |
| 10 peças | 50 min (20 + 10 × 3) | R$ 25,00 (R$ 2,50 por peça) |

Se a quantidade mudar depois, o tempo não se ajusta sozinho: a tela lembra de
revisar. Desde a decisão 104 existe um campo só também no modelo
(`Quote.laborMinutes`); o tempo por peça saiu de vez.

Não existe percentual de desconto por volume em lugar nenhum do app: o lote
sai mais barato por unidade porque o trabalho do pedido não cresce na mesma
proporção das peças. Isso é mais honesto do que um desconto inventado.

## Deduções da venda e frete (2026-09-22, decisão 78)

Três coisas saem do valor que o cliente paga antes de o dinheiro virar seu, e
elas **não** se comportam do mesmo jeito.

**Taxa do canal e imposto são descontados**, então o preço sobe pra
compensar. Vender a P deixa `P · (1 − canal − imposto)` na sua mão; o app
calcula P de trás pra frente, de forma que sobre exatamente a mesma coisa de
uma venda sem dedução nenhuma. O lucro mostrado já é o líquido.

**Canal de venda e forma de pagamento são o mesmo campo, de propósito.**
Somar "Shopee 20%" com "cartão 4%" cobraria em dobro, porque a taxa do
marketplace já embute o processamento do pagamento. Cada canal cadastrado é
uma taxa só: "Shopee" 20%, "Cartão" 4%, "Pix" 0%. Quem paga os dois de
verdade (loja própria com maquininha, por exemplo) cadastra um canal com a
soma que de fato paga.

**Imposto: MEI não entra como percentual.** O DAS do MEI é um valor fixo por
mês, não um percentual da venda, então o lugar dele é o custo fixo mensal
(que já é diluído por hora de impressão). O campo de imposto serve pra quem
paga percentual sobre o faturamento, como o Simples Nacional.

**Frete não passa por nada disso.** É repasse, não produto seu: entra como
linha própria no total do cliente, não multiplica pela quantidade, não passa
pela margem e não sofre dedução. Sai do preço da peça de propósito, pra o
cliente ver o que é peça e o que é entrega.

| | Multiplica pela quantidade? | Passa pela margem? | Sofre dedução? |
|---|---|---|---|
| Peça (produção) | Sim | Sim | Sim |
| Serviços | Os por peça sim, os por pedido não | Não | Não |
| Frete | Não | Não | Não |

Exemplo: peça de R$ 17,02 (venda direta) vendida pela Shopee (20%) por quem
paga 6% de Simples. As deduções somam 26%, então o preço vira
`17,02 / 0,74 = R$ 23,00`. O cliente paga R$ 23,00, a Shopee e o imposto
levam R$ 5,98, e sobram os mesmos R$ 17,02 de antes.

## Negociação e preço mínimo (2026-09-22, decisão 79)

O preço que a margem produz é um ponto de partida, não o fim da conversa. Ao
informar um **preço fechado com o cliente**, ele vira o valor de venda de
verdade do orçamento, e não um número de simulação à parte:

```
preço_da_peça  = preço_fechado − serviços − frete
LUCRO          = preço_da_peça · (1 − deduções) − produção
MARGEM OBTIDA  = lucro / produção
```

O custo de produção não muda, então um preço abaixo dele vira lucro negativo.
É de propósito: o app avisa em vermelho quanto você está pagando pra imprimir,
em vez de deixar o prejuízo passar despercebido.

**Por que virar o preço de verdade, e não uma simulação:** se o vendedor
fecha por R$ 30 e o histórico guarda os R$ 38,91 da tabela, o Dashboard passa
a somar um faturamento que nunca existiu. Um número só, usado em todo lugar,
é a única forma de os relatórios continuarem verdadeiros.

**O preço de tabela fica guardado junto** (decisão 84): quando há preço
fechado, `Quote.tableSalePrice` guarda o valor que a margem daria, só pra uso
interno. O desconto de cada orçamento é `tabela − preço_fechado` (negativo
quando o cliente pagou acima da tabela), e o Dashboard soma esse valor no
período. Nenhuma conta de lucro usa o preço de tabela.

### Preço mínimo (ponto de equilíbrio)

```
mínimo_da_peça  = produção / (1 − deduções)
mínimo_total    = mínimo_da_peça + serviços + frete
```

Vender exatamente por esse valor significa trabalhar de graça: cobre custo e
deduções, e sobra zero. Fica sempre visível na tela, porque é o número que
você precisa ter na cabeça no meio da conversa com o cliente.

Não existe desconto percentual por volume em campo separado: quem quer dar
desconto digita o preço fechado e vê na hora o que sobra. Ter duas formas de
chegar ao mesmo número (um percentual e um valor) só criaria divergência.

## Exemplo de referência (planilha)

Coberto por `core/src/commonTest/.../PricingCalculatorTest.kt`.

| Entrada | Valor |
|---|---|
| Filamento | PLA, R$ 100,00/kg, 1,24 g/cm³, 1,75 mm |
| Comprimento / tempo | 12 m / 190 min |
| kWh / consumo | R$ 1,23 / 380 W |
| Manutenção | R$ 0,17/h |
| Falhas / acabamento | 10% / 10% |
| Máquina | R$ 2.700, 12 meses, 25 dias/mês, 16 h/dia |
| Margem | 100% |

Sem mão de obra nem custo fixo configurados (os dois zerados), que é como o
app se comporta logo depois de atualizar:

| Resultado | Valor |
|---|---|
| Área / peso | 2,405 mm² / 35,79 g |
| Material | R$ 3,58 |
| Energia | R$ 1,48 |
| Manutenção | R$ 0,54 |
| Retorno de investimento (R$ 0,5625/h) | R$ 1,78 |
| Acabamento (10% do material) | R$ 0,36 |
| Custo refeito (soma das linhas acima) | R$ 7,74 |
| Falhas (10% do custo refeito) | R$ 0,77 |
| **Produção** | **R$ 8,51** |
| **Venda** | **R$ 17,02** |

A planilha original chegava a R$ 8,09 de produção porque reservava falha só
sobre o material (R$ 0,36 em vez de R$ 0,77). A diferença de R$ 0,42 é
exatamente a reserva que faltava.

### O mesmo exemplo, cobrando o próprio trabalho

Agora com R$ 30,00/h de mão de obra, 40 min de trabalho na peça (já contando
o acabamento, então a taxa de acabamento vai a 0), R$ 800,00 de custo fixo
mensal e 200 h de impressão por mês:

| Resultado | Valor |
|---|---|
| Material + energia + manutenção + retorno | R$ 7,38 |
| Custo fixo (R$ 4,00/h × 3,17 h) | R$ 12,67 |
| Mão de obra (R$ 30,00/h × 0,67 h) | R$ 20,00 |
| Acabamento | R$ 0,00 (taxa em 0, entra nos minutos) |
| Falhas (10% do custo refeito) | R$ 4,00 |
| **Produção** | **R$ 44,05** |
| **Venda** | **R$ 88,10** |

O salto de R$ 17,02 para R$ 88,10 não é o app ficando caro: é o custo que já
existia e não estava sendo cobrado de ninguém. As 3,17 h de impressão e os 40
min de trabalho sempre estiveram lá — só saíam do seu bolso em vez do bolso do
cliente. Se o seu mercado não paga esse valor, o caminho é reduzir tempo de
trabalho ou de máquina por peça, não fingir que eles custam zero.

## Análise de STL (nível de dificuldade)

Documento de referência: `core/.../stl/StlAnalyzer.kt`. Calculado a partir
da malha de triângulos do arquivo STL anexado ao orçamento — sem depender
de nenhum fatiador externo. **Uso só interno** (não entra no PDF nem no
copiar-colar); serve pra decidir se cobra uma margem extra por
complexidade — a decisão de preço em si continua manual. Assume que as
coordenadas do STL estão em milímetros (convenção usual de fatiadores/
impressão 3D).

### Área de superfície e volume

```
área (mm²)   = Σ (triângulos) 0,5 · |produto vetorial dos dois lados do triângulo|
volume (mm³) = | Σ (triângulos) v1 · (v2 × v3) | / 6
```

O volume usa o método clássico de soma de tetraedros a partir da origem
(equivalente ao teorema da divergência aplicado a uma malha fechada) — só
é exato pra uma malha **fechada** com orientação de normais consistente
(ver "malha manifold" abaixo).

### Razão de complexidade (quociente isoperimétrico)

```
razão = área da malha / área de uma esfera de mesmo volume
área_esfera(V) = 4π · (3V / 4π)^(2/3)
```

Uma esfera perfeita tem a menor área de superfície possível pra um dado
volume (razão = 1,0, o mínimo matemático). Formas com mais detalhe/
reentrâncias/protuberâncias pro mesmo volume têm mais área de superfície,
logo uma razão maior. Ao contrário de "área ÷ volume" bruta, essa razão
não muda só por causa do **tamanho** do objeto (um cubo pequeno e um cubo
grande têm a mesma razão) — mede forma, não escala.

### % de superfície em overhang

Pra cada triângulo, compara a normal geométrica com a direção "reto pra
baixo" `(0,0,-1)`, assumindo que o eixo Z do STL é "pra cima" (convenção
de mesa de impressão). Um triângulo conta como overhang se o ângulo entre
sua normal e "reto pra baixo" for menor que `90° − ângulo_limite` — por
padrão, `ângulo_limite = 45°` (mesmo espírito do parâmetro "ângulo de
suporte" de fatiadores como Cura/PrusaSlicer).

```
overhang% = (soma da área dos triângulos em overhang) / (área total) × 100
```

### Componentes desconexos

Conta quantas "peças soltas" existem no mesmo arquivo: triângulos que
compartilham pelo menos um vértice (coordenadas arredondadas pra 0,001mm,
pra tolerar ruído de ponto flutuante entre vértices que deveriam ser
idênticos) entram no mesmo grupo, via união-busca. Uma peça "malha só"
resulta em 1 componente.

### Malha manifold

Numa malha fechada e válida, toda aresta (par de vértices) deve ser
compartilhada por **exatamente 2 triângulos**. Se alguma aresta aparece em
só 1 triângulo (a malha tem um furo/está aberta) ou em 3+ triângulos
(geometria não-manifold, ex. auto-interseção), o arquivo é sinalizado como
"não-manifold" — aviso técnico separado do nível de dificuldade, avisando
que o arquivo pode dar problema no fatiador.

### Nível de dificuldade

Cada uma das 4 métricas abaixo pontua **0 (fácil), 1 (médio) ou 2
(difícil)**; a soma (0 a 8) decide o nível final. Limiares são uma
heurística sem benchmark real — ajustáveis se um caso prático mostrar que
estão errados.

| Métrica | Fácil (0) | Médio (1) | Difícil (2) |
|---|---|---|---|
| Razão de complexidade | ≤ 1,5 | 1,5 – 3,0 | > 3,0 |
| % overhang | ≤ 10% | 10% – 30% | > 30% |
| Triângulos | ≤ 20.000 | 20.000 – 100.000 | > 100.000 |
| Componentes desconexos | 1 | 2 – 3 | > 3 |

| Soma dos pontos | Nível final |
|---|---|
| 0 – 2 | Fácil |
| 3 – 5 | Médio |
| 6 – 8 | Difícil |

Modelos com mais de 500 mil triângulos não são pré-visualizados nem
analisados nesta versão (decisão 63 em [decisions.md](decisions.md)) — o
app avisa e o STL continua sendo salvo normalmente pra recuperar depois.

## Diferenças em relação à planilha

- **Energia:** a planilha exibe o kWh como "1,2", mas o valor real usado é 1,23.
- **Manutenção:** o campo "Depreciação por hora (%)" foi substituído por um
  custo fixo em R$ por hora (`maintenanceCostPerHour`), mais simples de entender.
- **Taxas de marketplace (Shopee) e custo de embalagem/spray:** ainda não
  implementados (não há mais "edição gratuita" limitando isso — é só uma
  funcionalidade pendente, sem decisão de prioridade ainda).
