package com.threedreport.app.ui.printers

/**
 * Impressora do catálogo pré-cadastrado (decisão 54, revisada em
 * 2026-09-19) — escolher um preset só preenche [PrinterPreset.brand]/
 * [PrinterPreset.model] (em `name`) e [ratedPowerWatts] no formulário;
 * manutenção e investimento continuam manuais, porque dependem de quanto
 * quem cadastrou pagou e de como usa a própria máquina.
 *
 * [ratedPowerWatts] é a **potência máxima/nominal** publicada no manual ou
 * na ficha técnica oficial do fabricante (valor da fonte/PSU) — não o
 * consumo médio real durante a impressão, que costuma ser bem menor (pedido
 * explícito do responsável do projeto: preferir o valor sempre publicado e
 * verificável, mesmo superestimando o custo de energia, a uma estimativa de
 * consumo médio que várias marcas simplesmente não divulgam). Por isso o
 * texto do diálogo de escolha (`PrinterPresetDialog`) deixa claro que é o
 * valor **máximo**, pra quem escolher saber que precisa ajustar pra baixo se
 * quiser um cálculo de energia mais realista.
 *
 * Como adicionar uma impressora nova: confirme a **potência máxima/nominal**
 * no manual ou na ficha técnica oficial do fabricante e acrescente uma
 * entrada com a fonte documentada no comentário, pra quem revisar depois
 * conseguir conferir de onde veio o número. Sem impressoras de resina (a
 * calculadora do app modela custo a partir de comprimento/peso de filamento).
 */
data class PrinterPreset(val brand: String, val model: String, val ratedPowerWatts: Double)

// Levantado em 2026-09-19. Quando o fabricante publica valores diferentes por
// região de tensão (ex.: 110V vs 220V), usa-se o MAIOR — é o valor "máximo"
// de verdade (elemento resistivo puxa mais potência em tensão mais alta), e
// bate com o pedido de sempre usar o máximo, não uma média.
val PRINTER_PRESETS: List<PrinterPreset> = listOf(
    // Bambu Lab — fonte: fichas técnicas/specs oficiais do fabricante.
    PrinterPreset(brand = "Bambu Lab", model = "A1 mini", ratedPowerWatts = 150.0), // fonte oficial da PSU (24V/150W)
    PrinterPreset(brand = "Bambu Lab", model = "A1", ratedPowerWatts = 1300.0), // 1300W@220V / 350W@110V, spec oficial
    PrinterPreset(brand = "Bambu Lab", model = "P1P", ratedPowerWatts = 1000.0), // 1000W@220V / 350W@110V, spec oficial
    PrinterPreset(brand = "Bambu Lab", model = "P1S", ratedPowerWatts = 1000.0), // 1000W@220V / 350W@110V, spec oficial
    PrinterPreset(brand = "Bambu Lab", model = "X1", ratedPowerWatts = 1000.0), // mesma plataforma AC do P1 — confiança média
    PrinterPreset(brand = "Bambu Lab", model = "X1 Carbon", ratedPowerWatts = 1000.0), // idem X1 — confiança média
    PrinterPreset(brand = "Bambu Lab", model = "X1E", ratedPowerWatts = 700.0), // "≤700W operando", FAQ oficial

    // Creality — Ender 3 V2/V3/V3 SE/S1, K1 e K1C compartilham a mesma fonte
    // 24V/350W (peça de reposição oficial idêntica nos 3); K1 Max e K2 Plus
    // têm fonte própria maior.
    PrinterPreset(brand = "Creality", model = "Ender 3 V2", ratedPowerWatts = 350.0),
    PrinterPreset(brand = "Creality", model = "Ender 3 V3", ratedPowerWatts = 350.0),
    PrinterPreset(brand = "Creality", model = "Ender 3 V3 SE", ratedPowerWatts = 350.0),
    PrinterPreset(brand = "Creality", model = "Ender 3 S1", ratedPowerWatts = 350.0),
    PrinterPreset(brand = "Creality", model = "K1", ratedPowerWatts = 350.0),
    PrinterPreset(brand = "Creality", model = "K1C", ratedPowerWatts = 350.0),
    PrinterPreset(brand = "Creality", model = "K1 Max", ratedPowerWatts = 1000.0),
    PrinterPreset(brand = "Creality", model = "K2 Plus", ratedPowerWatts = 1200.0),

    // Prusa — MK3S+/MK4/MK4S/CORE One compartilham a fonte 24V/240W; MINI+
    // usa uma menor; XL usa uma bem maior (impressora grande, multi-extrusor).
    PrinterPreset(brand = "Prusa", model = "MK3S+", ratedPowerWatts = 240.0),
    PrinterPreset(brand = "Prusa", model = "MK4", ratedPowerWatts = 240.0),
    PrinterPreset(brand = "Prusa", model = "MK4S", ratedPowerWatts = 240.0),
    PrinterPreset(brand = "Prusa", model = "CORE One", ratedPowerWatts = 240.0),
    PrinterPreset(brand = "Prusa", model = "MINI+", ratedPowerWatts = 150.0),
    PrinterPreset(brand = "Prusa", model = "XL", ratedPowerWatts = 1000.0), // confiança média, sem página oficial de spec isolada

    // Elegoo — fonte: spec/varejo oficial; Neptune 3 estimado pela mesma
    // família de fonte do Neptune 4 (confiança menor, sem número oficial
    // isolado pro Neptune 3).
    PrinterPreset(brand = "Elegoo", model = "Neptune 3", ratedPowerWatts = 400.0),
    PrinterPreset(brand = "Elegoo", model = "Neptune 4", ratedPowerWatts = 400.0),
    PrinterPreset(brand = "Elegoo", model = "Centauri Carbon", ratedPowerWatts = 1100.0), // 1100W@220V / 350W@110V

    // Anycubic — linha Kobra 2/3 compartilha a mesma fonte 400W.
    PrinterPreset(brand = "Anycubic", model = "Kobra 2", ratedPowerWatts = 400.0),
    PrinterPreset(brand = "Anycubic", model = "Kobra 2 Pro", ratedPowerWatts = 400.0),
    PrinterPreset(brand = "Anycubic", model = "Kobra 2 Neo", ratedPowerWatts = 400.0),
    PrinterPreset(brand = "Anycubic", model = "Kobra 3", ratedPowerWatts = 400.0),
    PrinterPreset(brand = "Anycubic", model = "Kobra 3 Max", ratedPowerWatts = 400.0),
    PrinterPreset(brand = "Anycubic", model = "Chiron", ratedPowerWatts = 360.0), // 24V × 15A, cálculo da spec oficial

    // Flashforge — fonte: specs oficiais/varejo oficial.
    PrinterPreset(brand = "Flashforge", model = "Adventurer 3", ratedPowerWatts = 150.0),
    PrinterPreset(brand = "Flashforge", model = "Adventurer 4", ratedPowerWatts = 350.0), // fontes variam 320–350W, usado o maior
    PrinterPreset(brand = "Flashforge", model = "Adventurer 5M", ratedPowerWatts = 350.0),
    PrinterPreset(brand = "Flashforge", model = "Adventurer 5M Pro", ratedPowerWatts = 350.0),
    PrinterPreset(brand = "Flashforge", model = "AD5X", ratedPowerWatts = 650.0),

    // Snapmaker — módulo de impressão 3D isolado (sem somar módulos de CNC/
    // laser, que são acessórios vendidos à parte).
    PrinterPreset(brand = "Snapmaker", model = "A250", ratedPowerWatts = 320.0),
    PrinterPreset(brand = "Snapmaker", model = "A350", ratedPowerWatts = 320.0),
    PrinterPreset(brand = "Snapmaker", model = "Artisan", ratedPowerWatts = 400.0),
    PrinterPreset(brand = "Snapmaker", model = "J1", ratedPowerWatts = 400.0),
)
