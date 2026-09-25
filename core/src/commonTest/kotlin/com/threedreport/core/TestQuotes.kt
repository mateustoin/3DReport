package com.threedreport.core

import com.threedreport.core.model.CostBreakdown
import com.threedreport.core.model.PrintCost
import com.threedreport.core.model.PrintJob
import com.threedreport.core.model.QuotedPrint

/** Impressão de teste numa impressora, sem custo próprio (os testes de relatório só olham tempo e impressora). */
internal fun quotedPrint(job: PrintJob, printerId: String = "printer", printerName: String = "Impressora") =
    QuotedPrint(job = job, printerId = printerId, printerName = printerName, cost = PrintCost(0.0, 0.0, 0.0, 0.0, 0.0, 0.0))

/** Custos de teste cujo total é [productionCost] (tudo no material, que é o que importa pro total). */
internal fun costsOf(productionCost: Double = 0.0) = CostBreakdown(
    material = productionCost,
    energy = 0.0,
    maintenance = 0.0,
    failures = 0.0,
    finishing = 0.0,
    investmentReturn = 0.0,
    administrative = 0.0,
    labor = 0.0,
    fixedCost = 0.0,
)
