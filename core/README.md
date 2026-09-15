# Módulo `core`

Domínio e motor de cálculo de orçamentos. Kotlin Multiplatform puro, sem UI —
a única dependência externa é `kotlinx.serialization` (anotação `@Serializable`
nos modelos, para a persistência feita pelo `composeApp`).

## Conteúdo

| Arquivo | Responsabilidade |
|---|---|
| `model/Filament.kt` | Filamento salvo em catálogo (id, preço/kg, densidade, diâmetro) e cálculo de peso por metro |
| `model/PrinterProfile.kt` | Perfil de impressora salvo (id, consumo, manutenção, `MachineInvestment`) — uma pessoa pode ter várias |
| `model/PrintJob.kt` | Dados da peça: filamento, metros e minutos |
| `model/PricingSettings.kt` | Custos gerais do negócio, iguais para qualquer impressora (energia, falhas, acabamento, administrativo, margem) |
| `model/Quote.kt` | Resultado: `CostBreakdown`, produção, venda, lucro |
| `model/SavedQuote.kt` | Retrato congelado de um `Quote` salvo no histórico (nome, foto, link interno, data) |
| `pricing/PricingCalculator.kt` | `calculate(job, printer, settings): Quote` |

## Uso

```kotlin
val quote = PricingCalculator.calculate(
    job = PrintJob(
        filament = Filament(id = "pla", name = "PLA", pricePerKg = 100.0, densityGPerCm3 = 1.24),
        filamentLengthMeters = 12.0,
        printTimeMinutes = 190.0,
    ),
    printer = printer, // PrinterProfile
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
