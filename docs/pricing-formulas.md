# Fórmulas de precificação

Documento de referência do motor de cálculo (`core/.../pricing/PricingCalculator.kt`).
As fórmulas foram derivadas da planilha de precificação usada atualmente
(calculadora "3D Geek Show") e ajustadas conforme decisões registradas em
[decisions.md](decisions.md).

## Entradas

### Por peça (`PrintJob`)

Os três primeiros vêm do fatiador; o tempo de trabalho é você quem informa.

| Parâmetro | Campo | Unidade |
|---|---|---|
| Filamento (nome, preço/kg, densidade, diâmetro) | `filament` | R$/kg, g/cm³, mm |
| Comprimento de filamento | `filamentLengthMeters` | m |
| Tempo de impressão | `printTimeMinutes` | min |
| Seu tempo de trabalho na peça | `laborMinutes` | min |

### Perfil da impressora escolhida (`PrinterProfile`) — uma por orçamento

Uma pessoa costuma ter várias impressoras; cada uma tem seu próprio perfil
salvo (tela Impressoras) e o orçamento escolhe qual usar.

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
| Taxa de acabamento (legado, ver abaixo) | `finishingRate` | fração |
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

```
horas              = tempo_min / 60
horas_trabalho     = minutos_trabalho / 60
área (mm²)         = π · (diâmetro / 2)²
volume (cm³)       = comprimento_m · 1000 · área / 1000
peso (g)           = volume · densidade

material           = peso / 1000 · preço_kg
energia            = horas · (W / 1000) · preço_kWh
manutenção         = horas · manutenção_por_hora
valor_hora_máquina = valor_máquina / (meses · dias_mês · horas_dia)
retorno_invest.    = horas · valor_hora_máquina
custo_fixo_hora    = custo_fixo_mensal / horas_produtivas_mês   (0 se horas = 0)
custo_fixo         = horas · custo_fixo_hora
mão_de_obra        = horas_trabalho · valor_hora_trabalho
acabamento         = material · taxa_acabamento   (0 se há valor_hora_trabalho)
administrativo     = custo_administrativo

custo_por_peça     = material + energia + manutenção + retorno_invest.
                     + custo_fixo + mão_de_obra + acabamento
preparo            = minutos_preparo / 60 · valor_hora_trabalho

CUSTO REFEITO      = custo_por_peça · quantidade + preparo
falhas             = CUSTO REFEITO · taxa_falhas

VALOR DE PRODUÇÃO  = CUSTO REFEITO + falhas + administrativo
PREÇO BASE         = produção · (1 + margem_lucro)

deduções           = taxa_do_canal + imposto
VALOR DE VENDA     = PREÇO BASE / (1 − deduções)
PREÇO UNITÁRIO     = (venda + serviços + frete) / quantidade
LUCRO              = venda · (1 − deduções) − produção

TOTAL DO CLIENTE   = venda + serviços · quantidade + frete
```

Todos os valores acima são do **pedido inteiro**. As entradas de peça
(comprimento, tempo de impressão, minutos de trabalho) são de **uma
unidade**, e é o app que multiplica pela quantidade: é assim que o fatiador
informa quando você fatia uma peça só. Se você fatiou a mesa cheia de uma
vez e os números já são do lote todo, mantenha a quantidade em 1.

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

**2. Acabamento agora é trabalho, não percentual de material.** Antes,
`acabamento = material · taxa`. Isso cobrava errado na prática: uma action
figure de 30 g pode dar 40 min de lixa e pintura, enquanto um suporte de
parede liso de 200 g dá 2 min — mas o suporte "pagava" quase 7x mais
acabamento, só por pesar mais. Acabamento escala com tempo, não com gramas.

Pra não mudar o preço de ninguém sem aviso, a troca é opcional e você controla
quando acontece:

- **Enquanto `laborRatePerHour` for zero**, nada muda: a taxa de acabamento
  continua valendo exatamente como antes.
- **Assim que você informar o valor da sua hora**, o acabamento passa a ser
  cobrado pelos minutos de trabalho informados em cada orçamento, e a taxa de
  acabamento deixa de ter efeito (a tela de Configurações avisa isso na hora).

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
| Mão de obra por peça (`PrintJob.laborMinutes`) | Sim, você lixa e embala cada uma |
| Preparo do pedido (`Quote.setupMinutes`) | **Não**, se faz uma vez só |
| Custo administrativo (ex.: modelagem) | **Não**, é por orçamento |
| Serviços opcionais (pintura, lixamento) | Sim, são trabalho por peça |

Exemplo com R$ 30,00/h de mão de obra, 3 min de trabalho por peça e 20 min de
preparo do pedido:

| Quantidade | Trabalho cobrado | Custo de trabalho |
|---|---|---|
| 1 peça | 20 min de preparo + 3 min | R$ 11,50 (R$ 11,50 por peça) |
| 10 peças | 20 min de preparo + 30 min | R$ 25,00 (R$ 2,50 por peça) |

Não existe percentual de desconto por volume em lugar nenhum do app: o lote
sai mais barato por unidade porque o preparo realmente é feito uma vez só.
Isso é mais honesto do que um desconto inventado, e continua verdadeiro
quando a peça é grande o bastante pra o preparo não pesar tanto.

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
| Serviços | Sim | Não | Não |
| Frete | Não | Não | Não |

Exemplo: peça de R$ 17,02 (venda direta) vendida pela Shopee (20%) por quem
paga 6% de Simples. As deduções somam 26%, então o preço vira
`17,02 / 0,74 = R$ 23,00`. O cliente paga R$ 23,00, a Shopee e o imposto
levam R$ 5,98, e sobram os mesmos R$ 17,02 de antes.

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
| Acabamento (legado, 10% do material) | R$ 0,36 |
| Custo refeito (soma das linhas acima) | R$ 7,74 |
| Falhas (10% do custo refeito) | R$ 0,77 |
| **Produção** | **R$ 8,51** |
| **Venda** | **R$ 17,02** |

A planilha original chegava a R$ 8,09 de produção porque reservava falha só
sobre o material (R$ 0,36 em vez de R$ 0,77). A diferença de R$ 0,42 é
exatamente a reserva que faltava.

### O mesmo exemplo, cobrando o próprio trabalho

Agora com R$ 30,00/h de mão de obra, 40 min de trabalho na peça, R$ 800,00 de
custo fixo mensal e 200 h de impressão por mês:

| Resultado | Valor |
|---|---|
| Material + energia + manutenção + retorno | R$ 7,38 |
| Custo fixo (R$ 4,00/h × 3,17 h) | R$ 12,67 |
| Mão de obra (R$ 30,00/h × 0,67 h) | R$ 20,00 |
| Acabamento | R$ 0,00 (substituído pela mão de obra) |
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
