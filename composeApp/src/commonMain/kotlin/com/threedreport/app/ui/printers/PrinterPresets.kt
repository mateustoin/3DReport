package com.threedreport.app.ui.printers

import com.threedreport.core.model.PrinterProfile

/** De onde vem a média de consumo de um [PrinterPreset]. */
enum class PowerSource {
    /** Ficha técnica ou wiki do fabricante. */
    OFFICIAL,

    /** Teste independente com medidor de tomada. */
    MEASURED,

    /** Irmã mais próxima que foi medida (dita no comentário de cada entrada). */
    ESTIMATED,
}

/**
 * Faixa de manutenção por tipo de máquina, em R$ por hora de impressão: peças que gastam divididas pelas horas
 * de vida típicas (decisão 144).
 */
enum class MaintenanceBand(val costPerHour: Double) {
    /** Aberta, de entrada: bico (R$ 30 a cada 500 h) + mesa PEI (R$ 120 a cada 1.500 h) + correias, ventoinhas e rolamentos (R$ 120 a cada 2.000 h). */
    OPEN_ENTRY(0.20),

    /** Fechada ou CoreXY rápida: hotend ou bico (R$ 150 a cada 1.000 h) + mesa (R$ 150 a cada 1.500 h) + filtros, correias e ventoinhas (R$ 100 a cada 2.000 h). */
    ENCLOSED(0.30),

    /** Multicolor (combo), dois bicos ou grande: a faixa fechada + peças da unidade multicolor (R$ 150 a cada 1.000 h). */
    MULTI(0.45),
}

/** Uso típico de todo cadastro novo de impressora, da lista ou em branco (decisão 144). */
object PrinterUsageDefaults {
    const val HOURS_PER_DAY = 12.0
    const val DAYS_PER_MONTH = 25
    const val PAYBACK_MONTHS = 12
}

/**
 * Mês dos preços de referência da lista (novembro de 2026), mostrado no aviso do formulário. Vai em número, e não em
 * texto, pra sair no idioma do app ([com.threedreport.app.ui.format.shortMonthYearText]).
 */
const val PRINTER_PRICES_YEAR = 2026
const val PRINTER_PRICES_MONTH = 11

/**
 * Impressora da lista pré-cadastrada (decisão 144, que revisa a 54). Escolher uma preenche o formulário com
 * [averagePowerWatts], a manutenção de [maintenance] e, quando houver, [referencePrice]; o uso vem de
 * [PrinterUsageDefaults]. Tudo continua editável: são valores de referência pra começar, não os da casa de quem usa.
 *
 * [averagePowerWatts] é o consumo médio imprimindo PLA, e [powerSource] diz de onde veio. [maxPowerWatts] é a
 * potência máxima da fonte (nulo quando o fabricante não publica), mostrada na lista só como contexto.
 * [referencePrice] é o preço médio no Brasil em [PRINTER_PRICES_YEAR]/[PRINTER_PRICES_MONTH], preço normal (não Pix); nulo sem fonte
 * brasileira, e aí o campo do formulário fica vazio.
 *
 * Como acrescentar uma impressora: confirme a média (oficial, medida ou estimada pela irmã mais próxima, dita no
 * comentário), a máxima e, se achar, o preço, e deixe a fonte no comentário pra quem revisar conferir. Sem
 * impressoras de resina: a calculadora modela custo a partir de comprimento e peso de filamento.
 *
 * Fontes. Bambu Lab: wiki oficial de consumo (wiki.bambulab.com/en/general/power-consumption), via a tabela de
 * 3dcalculators.com/blog/3d-printer-power-consumption-by-model (08/11/2026). Prusa: páginas de produto MK4S e CORE
 * One (prusa3d.com). Medidas: Igor's Lab (Centauri Carbon, Kobra 3, Adventurer 5M), China-Gadgets (K1 Max, K2 Plus,
 * AD5X), game2gether (K1C) e 3dsolved (Ender 3 original, 125 W com bico a 200 C e mesa a 60 C). Preços: Loja 3D
 * (loja3d.com.br, 14/11/2026), Tamo Tudo 3D (jul/2026: P1S Combo, K1C, K2 Plus, Kobra 3 Combo, Kobra X, Kobra S1
 * Combo), ArtBox3D (P2S), loja oficial Anycubic (Kobra S1, set/2026), Translaser (Centauri Carbon 2 Combo) e Mercado
 * Livre via my-best (Hi Combo, Adventurer 5M). Fora da lista por falta de dado confiável: Sovol, Qidi, Flashforge
 * Creator 5, Snapmaker U1 e Creality Ender 5 Max.
 */
data class PrinterPreset(
    val brand: String,
    val model: String,
    val averagePowerWatts: Double,
    val powerSource: PowerSource,
    val maxPowerWatts: Double?,
    val referencePrice: Double?,
    val maintenance: MaintenanceBand,
) {
    val displayName: String get() = "$brand $model"
}

private fun preset(
    brand: String,
    model: String,
    averageWatts: Int,
    source: PowerSource,
    maxWatts: Int?,
    price: Double?,
    band: MaintenanceBand,
) = PrinterPreset(brand, model, averageWatts.toDouble(), source, maxWatts?.toDouble(), price, band)

val PRINTER_PRESETS: List<PrinterPreset> = listOf(
    // Bambu Lab
    preset("Bambu Lab", "A1 mini", 80, PowerSource.OFFICIAL, 150, 2178.50, MaintenanceBand.OPEN_ENTRY),
    preset("Bambu Lab", "A1 mini Combo", 80, PowerSource.OFFICIAL, 150, 3598.50, MaintenanceBand.MULTI),
    preset("Bambu Lab", "A1", 95, PowerSource.OFFICIAL, 1300, 2998.00, MaintenanceBand.OPEN_ENTRY),
    preset("Bambu Lab", "A1 Combo", 95, PowerSource.OFFICIAL, 1300, 4498.50, MaintenanceBand.MULTI),
    preset("Bambu Lab", "P1P", 110, PowerSource.OFFICIAL, 1000, null, MaintenanceBand.ENCLOSED),
    preset("Bambu Lab", "P1S", 105, PowerSource.OFFICIAL, 1000, null, MaintenanceBand.ENCLOSED),
    preset("Bambu Lab", "P1S Combo", 105, PowerSource.OFFICIAL, 1000, 8199.00, MaintenanceBand.MULTI),
    preset("Bambu Lab", "P2S", 200, PowerSource.OFFICIAL, 1200, 6529.00, MaintenanceBand.ENCLOSED),
    preset("Bambu Lab", "P2S Combo", 200, PowerSource.OFFICIAL, 1200, 9398.50, MaintenanceBand.MULTI),
    preset("Bambu Lab", "X1 Carbon", 105, PowerSource.OFFICIAL, 1000, null, MaintenanceBand.ENCLOSED),
    preset("Bambu Lab", "X1E", 185, PowerSource.OFFICIAL, 700, null, MaintenanceBand.ENCLOSED),
    preset("Bambu Lab", "H2S", 200, PowerSource.OFFICIAL, 1250, null, MaintenanceBand.MULTI),
    preset("Bambu Lab", "H2D", 197, PowerSource.OFFICIAL, 1250, null, MaintenanceBand.MULTI),
    preset("Bambu Lab", "H2D Combo", 197, PowerSource.OFFICIAL, 1250, 22997.00, MaintenanceBand.MULTI),

    // Creality
    preset("Creality", "Ender 3 V2", 120, PowerSource.ESTIMATED, 350, null, MaintenanceBand.OPEN_ENTRY), // Ender 3 medida: 125 W
    preset("Creality", "Ender 3 V3 SE", 120, PowerSource.ESTIMATED, 350, null, MaintenanceBand.OPEN_ENTRY), // idem
    preset("Creality", "Ender 3 V3 KE", 120, PowerSource.ESTIMATED, 350, 2839.00, MaintenanceBand.OPEN_ENTRY), // idem
    preset("Creality", "Ender 3 V3", 130, PowerSource.ESTIMATED, 350, null, MaintenanceBand.OPEN_ENTRY), // K1C medida, CoreXY aberta
    preset("Creality", "Ender 3 S1", 120, PowerSource.ESTIMATED, 350, null, MaintenanceBand.OPEN_ENTRY), // Ender 3 medida
    preset("Creality", "Ender 3 V4", 120, PowerSource.ESTIMATED, null, 2924.00, MaintenanceBand.OPEN_ENTRY), // Ender 3 medida
    preset("Creality", "Hi", 130, PowerSource.ESTIMATED, 350, 3119.00, MaintenanceBand.OPEN_ENTRY), // K1C medida
    preset("Creality", "Hi Combo", 130, PowerSource.ESTIMATED, 350, 5299.00, MaintenanceBand.MULTI), // K1C medida
    preset("Creality", "K1", 144, PowerSource.ESTIMATED, 350, null, MaintenanceBand.ENCLOSED), // K1C medida, mesma plataforma
    preset("Creality", "K1C", 144, PowerSource.MEASURED, 350, 3999.00, MaintenanceBand.ENCLOSED),
    preset("Creality", "K1 Max", 175, PowerSource.MEASURED, 1000, null, MaintenanceBand.ENCLOSED),
    preset("Creality", "K2 SE", 130, PowerSource.ESTIMATED, null, 3919.00, MaintenanceBand.ENCLOSED), // K1C medida
    preset("Creality", "K2 Plus", 249, PowerSource.MEASURED, 1200, 11000.00, MaintenanceBand.MULTI),

    // Prusa
    preset("Prusa", "MINI+", 60, PowerSource.ESTIMATED, 150, null, MaintenanceBand.OPEN_ENTRY), // MK4S oficial, máquina menor
    preset("Prusa", "MK3S+", 80, PowerSource.ESTIMATED, 240, null, MaintenanceBand.OPEN_ENTRY), // MK4S oficial
    preset("Prusa", "MK4", 80, PowerSource.ESTIMATED, 240, null, MaintenanceBand.OPEN_ENTRY), // MK4S oficial, mesma plataforma
    preset("Prusa", "MK4S", 80, PowerSource.OFFICIAL, 240, null, MaintenanceBand.OPEN_ENTRY),
    preset("Prusa", "CORE One", 90, PowerSource.OFFICIAL, 240, null, MaintenanceBand.ENCLOSED),
    preset("Prusa", "XL", 200, PowerSource.ESTIMATED, 1000, null, MaintenanceBand.MULTI), // oficial 235 W em PETG

    // Elegoo
    preset("Elegoo", "Neptune 3", 120, PowerSource.ESTIMATED, 400, null, MaintenanceBand.OPEN_ENTRY), // Ender 3 medida
    preset("Elegoo", "Neptune 4", 130, PowerSource.ESTIMATED, 310, null, MaintenanceBand.OPEN_ENTRY), // Adventurer 5M medida, mesma classe
    preset("Elegoo", "Neptune 4 Pro", 130, PowerSource.ESTIMATED, 310, null, MaintenanceBand.OPEN_ENTRY), // idem
    preset("Elegoo", "Centauri Carbon", 160, PowerSource.MEASURED, 1100, null, MaintenanceBand.ENCLOSED),
    preset("Elegoo", "Centauri Carbon 2 Combo", 160, PowerSource.ESTIMATED, 1100, 4990.00, MaintenanceBand.MULTI), // Centauri Carbon medida

    // Anycubic
    preset("Anycubic", "Kobra 2 Neo", 120, PowerSource.ESTIMATED, 400, null, MaintenanceBand.OPEN_ENTRY), // Ender 3 medida
    preset("Anycubic", "Kobra 2", 120, PowerSource.ESTIMATED, 400, null, MaintenanceBand.OPEN_ENTRY), // idem
    preset("Anycubic", "Kobra 2 Pro", 120, PowerSource.ESTIMATED, 400, null, MaintenanceBand.OPEN_ENTRY), // idem
    preset("Anycubic", "Kobra 3", 205, PowerSource.MEASURED, 400, null, MaintenanceBand.OPEN_ENTRY),
    preset("Anycubic", "Kobra 3 Combo", 205, PowerSource.MEASURED, 400, 3700.00, MaintenanceBand.MULTI),
    preset("Anycubic", "Kobra 3 Max", 220, PowerSource.ESTIMATED, 400, null, MaintenanceBand.OPEN_ENTRY), // Kobra 3 medida, mesa maior
    preset("Anycubic", "Kobra S1", 150, PowerSource.ESTIMATED, 400, 3629.00, MaintenanceBand.ENCLOSED), // Centauri Carbon medida, mesma classe
    preset("Anycubic", "Kobra S1 Combo", 150, PowerSource.ESTIMATED, 400, 5600.00, MaintenanceBand.MULTI), // idem
    preset("Anycubic", "Kobra X", 130, PowerSource.ESTIMATED, null, 3369.00, MaintenanceBand.MULTI), // Adventurer 5M medida
    preset("Anycubic", "Chiron", 150, PowerSource.ESTIMATED, 360, null, MaintenanceBand.OPEN_ENTRY), // mesa grande, Ender 3 medida

    // Flashforge
    preset("Flashforge", "Adventurer 3", 60, PowerSource.ESTIMATED, 150, null, MaintenanceBand.OPEN_ENTRY), // MINI+
    preset("Flashforge", "Adventurer 4", 130, PowerSource.ESTIMATED, 350, null, MaintenanceBand.ENCLOSED), // Adventurer 5M medida
    preset("Flashforge", "Adventurer 5M", 130, PowerSource.MEASURED, 350, 1895.45, MaintenanceBand.OPEN_ENTRY),
    preset("Flashforge", "Adventurer 5M Pro", 130, PowerSource.ESTIMATED, 350, null, MaintenanceBand.ENCLOSED), // Adventurer 5M medida
    preset("Flashforge", "AD5X", 115, PowerSource.MEASURED, 650, null, MaintenanceBand.MULTI),

    // Snapmaker
    preset("Snapmaker", "A250", 120, PowerSource.ESTIMATED, 320, null, MaintenanceBand.OPEN_ENTRY), // Ender 3 medida
    preset("Snapmaker", "A350", 140, PowerSource.ESTIMATED, 320, null, MaintenanceBand.OPEN_ENTRY), // mesa maior
    preset("Snapmaker", "Artisan", 160, PowerSource.ESTIMATED, 400, null, MaintenanceBand.OPEN_ENTRY), // mesa grande
    preset("Snapmaker", "J1", 150, PowerSource.ESTIMATED, 400, null, MaintenanceBand.MULTI), // dois bicos
)

/** Os presets que combinam com [query] na marca ou no modelo (sem diferenciar maiúsculas); todos com a busca vazia. */
fun printerPresetsMatching(query: String): List<PrinterPreset> =
    PRINTER_PRESETS.filter { query.isBlank() || it.brand.contains(query.trim(), ignoreCase = true) || it.model.contains(query.trim(), ignoreCase = true) }

/**
 * Aplica [preset] numa impressora já cadastrada (a de exemplo, no primeiro uso): nome, consumo médio, manutenção da
 * faixa, preço e o uso típico ([PrinterUsageDefaults]), como num cadastro novo. O preço é o que a pessoa digitou
 * ([machinePrice]); sem ele, o de referência do preset; sem nenhum, o que a impressora já tinha. Sem [preset], só o
 * preço digitado muda e o resto fica como estava.
 */
fun PrinterProfile.withPreset(preset: PrinterPreset?, machinePrice: Double?): PrinterProfile {
    val price = machinePrice ?: preset?.referencePrice
    val investment = if (preset == null) {
        machineInvestment
    } else {
        machineInvestment.copy(
            paybackMonths = PrinterUsageDefaults.PAYBACK_MONTHS,
            printingDaysPerMonth = PrinterUsageDefaults.DAYS_PER_MONTH,
            printingHoursPerDay = PrinterUsageDefaults.HOURS_PER_DAY,
        )
    }
    return copy(
        name = preset?.displayName ?: name,
        printerPowerWatts = preset?.averagePowerWatts ?: printerPowerWatts,
        maintenanceCostPerHour = preset?.maintenance?.costPerHour ?: maintenanceCostPerHour,
        machineInvestment = price?.let { investment.copy(machinePrice = it) } ?: investment,
    )
}
