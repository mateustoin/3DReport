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

### Configurações da operação (`PricingSettings`)

| Parâmetro | Campo | Unidade |
|---|---|---|
| Preço do kWh | `energyPricePerKwh` | R$ |
| Consumo da impressora | `printerPowerWatts` | W |
| Custo de manutenção por hora | `maintenanceCostPerHour` | R$/h |
| Taxa de falhas | `failureRate` | fração (0,10 = 10%) |
| Taxa de acabamento | `finishingRate` | fração |
| Custo administrativo (ex.: modelagem) | `administrativeCost` | R$ por orçamento |
| Valor da máquina | `machineInvestment.machinePrice` | R$ |
| Prazo de retorno | `machineInvestment.paybackMonths` | meses |
| Dias de uso por mês | `machineInvestment.printingDaysPerMonth` | dias |
| Horas de uso por dia | `machineInvestment.printingHoursPerDay` | h |
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

## Diferenças em relação à planilha

- **Energia:** a planilha exibe o kWh como "1,2", mas o valor real usado é 1,23.
- **Manutenção:** o campo "Depreciação por hora (%)" foi substituído por um
  custo fixo em R$ por hora (`maintenanceCostPerHour`), mais simples de entender.
- **Taxas de marketplace (Shopee) e custo de embalagem/spray:** fora do escopo
  da edição gratuita; não implementados.
