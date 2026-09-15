# Módulo `core`

Domínio e motor de cálculo de orçamentos. Kotlin Multiplatform puro, sem UI e
sem dependências externas.

## Conteúdo

| Arquivo | Responsabilidade |
|---|---|
| `model/Filament.kt` | Filamento (preço/kg, densidade, diâmetro) e cálculo de peso por metro |
| `model/PrintJob.kt` | Dados da peça: filamento, metros e minutos |
| `model/PricingSettings.kt` | Custos da operação e `MachineInvestment` (retorno da máquina) |
| `model/Quote.kt` | Resultado: `CostBreakdown`, produção, venda, lucro |
| `pricing/PricingCalculator.kt` | `calculate(job, settings): Quote` |

## Uso

```kotlin
val quote = PricingCalculator.calculate(
    job = PrintJob(
        filament = Filament(name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
        filamentLengthMeters = 12.0,
        printTimeMinutes = 190.0,
    ),
    settings = settings, // PricingSettings
)
quote.productionCost // 8.09...
quote.salePrice      // 16.19...
```

Fórmulas: [../docs/pricing-formulas.md](../docs/pricing-formulas.md).

## Testes

```bash
./gradlew :core:jvmTest
```

`PricingCalculatorTest` reproduz o exemplo da planilha de referência.
