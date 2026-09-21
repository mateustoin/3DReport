# Fórmulas de precificação

Documento de referência do motor de cálculo (`core/.../pricing/PricingCalculator.kt`).
As fórmulas foram derivadas da planilha de precificação usada atualmente
(calculadora "3D Geek Show") e ajustadas conforme decisões registradas em
[decisions.md](decisions.md).

## Entradas

### Por peça (`PrintJob`) — vêm do fatiador

| Parâmetro | Campo | Unidade |
|---|---|---|
| Filamento (nome, preço/kg, densidade, diâmetro) | `filament` | R$/kg, g/cm³, mm |
| Comprimento de filamento | `filamentLengthMeters` | m |
| Tempo de impressão | `printTimeMinutes` | min |

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
| Taxa de acabamento | `finishingRate` | fração |
| Custo administrativo (ex.: modelagem) | `administrativeCost` | R$ por orçamento |
| Margem de lucro | `profitMargin` | fração (1,0 = 100%) |

## Cálculos

```
horas              = tempo_min / 60
área (mm²)         = π · (diâmetro / 2)²
volume (cm³)       = comprimento_m · 1000 · área / 1000
peso (g)           = volume · densidade

material           = peso / 1000 · preço_kg
energia            = horas · (W / 1000) · preço_kWh
manutenção         = horas · manutenção_por_hora
falhas             = material · taxa_falhas
acabamento         = material · taxa_acabamento
valor_hora_máquina = valor_máquina / (meses · dias_mês · horas_dia)
retorno_invest.    = horas · valor_hora_máquina
administrativo     = custo_administrativo

VALOR DE PRODUÇÃO  = material + energia + manutenção + falhas
                     + acabamento + retorno_invest. + administrativo
VALOR DE VENDA     = produção · (1 + margem_lucro)
LUCRO              = venda − produção
```

Os valores são mantidos em `Double` sem arredondamento; o arredondamento para
centavos acontece apenas na exibição.

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

| Resultado | Valor |
|---|---|
| Área / peso | 2,405 mm² / 35,79 g |
| Material | R$ 3,58 |
| Energia | R$ 1,48 |
| Manutenção | R$ 0,54 |
| Falhas | R$ 0,36 |
| Acabamento | R$ 0,36 |
| Retorno de investimento (R$ 0,5625/h) | R$ 1,78 |
| **Produção** | **R$ 8,09** |
| **Venda** | **R$ 16,19** |

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
